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

package edu.ucla.sspace.index;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Queue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Generate index vectors for the Beagle {code SemanticSpace}.
 * This is done by first generating a fixed random dense index vector for each
 * word encountered.
 *
 * Additionally, this {@code IndexGenerator} can be saved, where all of the index
 * vectors generated so far will be serialized.  This serialization can then be
 * loaded up to restore the state of the {@code IndexGenerator}. </p>
 *
 */
public class BeagleIndexGenerator implements IndexGenerator {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.index.BeagleIndexGenerator";

    /**
     * The default index vector size, used when one is not specified.
     */
    private static final int DEFAULT_INDEX_VECTOR_LENGTH = 512;

    /**
     * A mapping from terms to their Index Vector, stored as a {@code
     * Vector}.
     */
    private ConcurrentHashMap<String, Vector> termToRandomIndex;

    /**
     * The current size of all index vectors, and semantic vectors.
     */
    private int indexVectorLength;

    /**
     * The standard deviation used for generating a new index vector for terms.
     */
    private double stdev;

    /**
     * A random number generator which produces values for index vectors.
     */
    private Random randomGenerator;

    /**
     * Create a {@code BeagleIndexBuiler} which uses the {@code
     * DEFAULT_INDEX_VECTOR_SIZE} as the length of each {@code Vector} generated
     * in this {@code IndexGenerator}.
     */
    public BeagleIndexGenerator() {
        this(System.getProperties());
    }

    /**
     * Create a {@code BeagleIndexGenerator} which uses {@code vectorLength} as
     * the size of each generated {@code Vector}.
     *
     * @param vectorLength The length of each index and semantic {@code Vector}
     *                     used in this {@code IndexGenerator}.
     */
    public BeagleIndexGenerator(Properties prop) {
        // Generate utility classes.
        randomGenerator = new Random();

        String indexVectorProp = prop.getProperty(
                IndexGenerator.INDEX_VECTOR_LENGTH_PROPERTY);
        indexVectorLength = (indexVectorProp != null)
            ? Integer.parseInt(indexVectorProp)
            : DEFAULT_INDEX_VECTOR_LENGTH;
        termToRandomIndex = new ConcurrentHashMap<String, Vector>();

        // Generate the permutation arrays.
        stdev = 1 / Math.sqrt(indexVectorLength);
    }

    /**
     * Load index vectors from a binary file.    This will first load in the
     * ordering permutation vectors, and then the index vector for each term.
     *
     * @param file The file containing a saved BeagleIndexGenerator instance.
     */
    public void loadIndexVectors(File file) {
        try {
            DataInputStream in = new DataInputStream(new FileInputStream(file));

            // Read in the mappings. Each mapping starts off with the number of
            // letters in the word, the word, and then the index vector.
            int mappings = in.readInt();
            for (int i = 0; i < mappings; ++i) {
                int wordSize = in.readInt();
                byte[] word = new byte[wordSize];
                in.read(word);
                double[] vector = new double[indexVectorLength];
                for (int j = 0; j < indexVectorLength; ++j) {
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

            out.writeInt(termToRandomIndex.size());
            // Write out each mapping in the form of:
            // word length, word as bytes, index vector.
            for (Map.Entry<String, Vector> entry :
                     termToRandomIndex.entrySet()) {
                String word = entry.getKey();
                Vector vector = entry.getValue();
                out.writeInt(word.length());
                out.write(word.getBytes(), 0, word.length());
                for (int i = 0; i < indexVectorLength; ++i) {
                    out.writeDouble(vector.get(i));
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Generate a new random vector using a guassian distribution for each
     * value.
     */
    private Vector generateRandomVector() {
        Vector termVector = new DenseVector(indexVectorLength);
        for (int i = 0; i < indexVectorLength; i++)
            termVector.set(i, randomGenerator.nextGaussian() * stdev);
        return termVector;
    }

    /**
     * {@inheritDoc}
     */
    public Vector getIndexVector(String term) {
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
}
