/*
 * Copyright 2009 Keith Stevens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.beagle;

import edu.ucla.sspace.common.IndexBuilder;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;

import jnt.FFT.RealDoubleFFT_Radix2;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generate index vectors for the Beagle Semantic Space, and incorporate index
 * vectors of co-occuring words into the Semantic Vector for a focus word.
 */
public class BeagleIndexBuilder implements IndexBuilder {
    /**
     * The default index vector size, used when one is not specified.
     */
    private static final int DEFAULT_INDEX_VECTOR_SIZE = 512;

    /**
     * A mapping from terms to their Index Vector, stored as a {@code
     * Vector}.
     */
    private ConcurrentHashMap<String, Vector> termToRandomIndex;

    /**
     * A utility class which performs the Fast Fourier Transform, used for
     * computing the circular convolution of vectors.
     */
    private RealDoubleFFT_Radix2 fft;

    /**
     * The current size of all index vectors, and semantic vectors.
     */
    private int indexVectorSize;

    /**
     * An empty place holder vector to represent the focus word when computing
     * the circular convolution.
     */
    private Vector placeHolder;

    /**
     * The standard deviation used for generating a new index vector for terms.
     */
    private double stdev;

    /**
     * A random number generator which produces values for index vectors.
     */
    private Random randomGenerator;

    /**
     * The first permutation ordering for vectors.
     */
    private int[] permute1;

    /**
     * The second permutation ordering for vectors.
     */
    private int[] permute2;

    /**
     * The most recent created index vector.  When a new term needs an index
     * vector, this is the value saved.  Afterwords a new value must be
     * generated.
     */
    private Vector newestRandomVector;

    public BeagleIndexBuilder() {
        init(DEFAULT_INDEX_VECTOR_SIZE);
    }

    public BeagleIndexBuilder(int s) {
        init(s);
    }

    private void init(int s) {
        randomGenerator = new Random();
        termToRandomIndex = new ConcurrentHashMap<String, Vector>();
        indexVectorSize = s;
        fft = new RealDoubleFFT_Radix2(indexVectorSize);
        newestRandomVector = generateRandomVector(); 
        // Enter the zero vector for the empty string.
        termToRandomIndex.put("", getSemanticVector());
        stdev = 1 / Math.sqrt(indexVectorSize);
        permute1 = new int[indexVectorSize];
        permute2 = new int[indexVectorSize];
        randomPermute(permute1);
        placeHolder = generateRandomVector();
        randomPermute(permute2);
    }

    public int expectedSizeOfPrevWords() {
        return 1;
    }

    public int expectedSizeOfNextWords() {
        return 5;
    }

    /**
     * Load index vectors from a binary file.    This will first load in the
     * ordering permutation vectors, and then the index vector for each term.
     *
     * @param file The file containing a saved BeagleIndexBuilder instance.
     */
    public void loadIndexVectors(File file) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            // Read in the permutation vectors.
            for (int i = 0; i < indexVectorSize; ++i)
                permute1[i] = in.readInt();
            for (int i = 0; i < indexVectorSize; ++i)
                permute2[i] = in.readInt();

            // Read in the mappings. Each mapping starts off with the number of
            // letters in the word, the word, and then the index vector.
            int mappings = in.readInt();
            for (int i = 0; i < mappings; ++i) {
                int wordSize = in.readInt();
                byte[] word = new byte[wordSize];
                in.read(word);
                double[] vector = new double[indexVectorSize];
                for (int j = 0; j < indexVectorSize; ++j) {
                    vector[j] = in.readDouble();
                }
                termToRandomIndex.put(new String(word),
                                      new DenseVector(vector));
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Save the index vectors for this instance into a binary file. First the
     * ordering permutation vectors are stored. Afterwords, each word, and it's
     * corresponding index vector is stored.
     *
     * @param file The file which will be used to store this instance.
     */
    public void saveIndexVectors(File file) {
        try {
            DataOutputStream out =
                new DataOutputStream(new FileOutputStream(file));
            // Write out the permutation vectors.
            for (int i = 0; i < indexVectorSize; ++i)
                out.writeInt(permute1[i]);
            for (int i = 0; i < indexVectorSize; ++i)
                out.writeInt(permute2[i]);

            out.writeInt(termToRandomIndex.size());
            // Write out each mapping in the form of:
            // word length, word as bytes, index vector.
            for (Map.Entry<String, Vector> entry :
                     termToRandomIndex.entrySet()) {
                String word = entry.getKey();
                Vector vector = entry.getValue();
                out.writeInt(word.length());
                out.write(word.getBytes(), 0, word.length());
                for (int i = 0; i < indexVectorSize; ++i) {
                    out.writeDouble(vector.get(i));
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Return an empty dense semantic vector.
     */
    public Vector getSemanticVector() {
        return new DenseVector(indexVectorSize);
    }

    /**
     * Populate the given array with values 0 to {@code indexVectorSize}, and
     * then shuffly the values randomly.
     */
    private void randomPermute(int[] permute) {
        for (int i = 0; i < indexVectorSize; i++)
            permute[i] = i;
        for (int i = indexVectorSize - 1; i > 0; i--) {
            int w = (int) Math.floor(Math.random() * (i+1));
            int temp = permute[w];
            permute[w] = permute[i];
            permute[i] = permute[w];
        }
    }

    /**
     * Print out the keys stored in this index builder.
     */
    public void printAll() {
        for (Map.Entry<String, Vector> m :
                termToRandomIndex.entrySet()) {
            System.out.println(m.getKey());
        }
    }

    /**
     * Generate a new random vector using a guassian distribution for each
     * value.
     */
    private Vector generateRandomVector() {
        Vector termVector = getSemanticVector();
        for (int i = 0; i < indexVectorSize; i++)
            termVector.set(i, randomGenerator.nextGaussian() * stdev);
        return termVector;
    }

    /**
     * Generate and store a new index vector for the given term.
     *
     * @param term The term to build an index vector for, if one does not exit.
     *
     * @return The generated index vector.
     */
    private Vector getBeagleVector(String term) {
        // Check that an index vector does not already exist.
        Vector v = termToRandomIndex.get(term);
        if (v == null) {
            synchronized (this) {
                // Confirm that some other thread has not created an index
                // vector for this term.
                v = termToRandomIndex.get(term);
                if (v == null) {
                    // Generate the index vector for this term and store it.
                    v = generateRandomVector();
                    termToRandomIndex.put(term, v);
                }
            }
        }
        return v;
    }

    /**
     * Add a holograph encoding the co-occurance information, and the ordering
     * information of the given context. First, the index vectors for each word
     * in the given context will be added into {@code meaning}. Then several
     * n-grams will be generated, and composed together using the circular
     * convolution. These convolutions of n-grams will be added into {@code
     * meaning}, providing a full holograph. Other threads should not be
     * modifying {@code meaning} while this function runs.
     *
     * @param meaning The {@code Vector} representing the focus word.
     * @param prevWords The words prior to the focus word in the context.
     * @param nextWords The Words after the focus word in the context.
     */
    public void updateMeaningWithTerm(Vector meaning,
                                      Queue<String> prevWords,
                                      Queue<String> nextWords) {
        // Sum the index vectors for co-occuring words into {@code meaning}.
        for (String term: prevWords)
            plusEquals(meaning, getBeagleVector(term));
        for (String term: nextWords)
            plusEquals(meaning, getBeagleVector(term));

        // Generate the semantics of the circular convolution of n-grams.
        Vector orderVector = getSemanticVector();
        plusEquals(orderVector, groupConvolution(prevWords, nextWords));

        // Add the final context vector into meaning.
        plusEquals(meaning, orderVector);
    }

    /**
     * Generate the circular convoltion of n-grams composed of words in the
     * given context. The result of this convolution is returned as a
     * Vector.
     *
     * @param prevWords The words prior to the focus word in the context.
     * @param nextWords The Words after the focus word in the context.
     * 
     * @return The semantic vector generated from the circular convolution.
     */
    private Vector groupConvolution(Queue<String> prevWords,
                                    Queue<String> nextWords) {
        // Generate an empty Vector to hold the convolution.
        Vector result = getSemanticVector();

        // Do the convolutions starting at index 0.
        Vector tempConvolution =
            convolute(getBeagleVector(prevWords.peek()), placeHolder);

        plusEquals(result, tempConvolution);

        for (String term : nextWords) {
            tempConvolution = convolute(tempConvolution, getBeagleVector(term));
            plusEquals(result, tempConvolution);
        }
        tempConvolution = placeHolder;

        // Do the convolutions starting at index 1.
        for (String term : nextWords) {
            tempConvolution = convolute(tempConvolution, getBeagleVector(term));
            plusEquals(result, tempConvolution);
        }
        return result;
    }

    /**
     * Add the {@code right} vector to {@code left}.
     *
     * @param left The destination vector.
     * @param right The source vector for addition.
     */
    private void plusEquals(Vector left, Vector right) {
        for (int i = 0; i < indexVectorSize; ++i)
            left.add(i, right.get(i));
    }

    /**
     * Perform the circular convolution of two vectors.    The resulting vector
     * is returned.
     *
     * @param left The left vector.
     * @param right The right vector.
     *
     * @return The circular convolution of {@code left} and {@code right}.
     */
    private Vector convolute(Vector left, Vector right) {
        // Permute both vectors.
        left = changeVector(left, permute1);
        right = changeVector(right, permute2);

        // Use the Fast Fourier Transform on each vector.
        fft.transform(left.toArray(indexVectorSize), 0, 1);
        fft.transform(right.toArray(indexVectorSize), 0, 1);

        // Multiply the two together.
        Vector result = arrayTimes(left, right);

            // The inverse transform completes the convolution.
        fft.backtransform(result.toArray(indexVectorSize), 0, 1);
        return result;
    }

    /**
     * Multiply the entries in each vector together.
     *
     * @param left The left vector.
     * @param right The right vector.
     *
     * @return The multiplication of {@code left} and {@code right}.
     */
    private Vector arrayTimes(Vector left, Vector right) {
        Vector result = getSemanticVector();
        for (int i = 0; i < indexVectorSize; ++i)
            result.set(i, left.get(i) * right.get(i));
        return result;
    }

    /**
     * Shuffle the given vector based on the ordering information given in {@code
     * orderVector}.
     *
     * @param data The vector to be shuffled.
     * @param orderVector The ordering of values to be used.
     * 
     * @return The shuffled version of {@code data}.
     */
    private Vector changeVector(Vector data, int[] orderVector) {
        Vector result = getSemanticVector();
        for (int i = 0; i < indexVectorSize; i++)
            result.set(i, data.get(orderVector[i]));
        return result;
    }
}
