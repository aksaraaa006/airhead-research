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

import edu.ucla.sspace.fft.FastFourierTransform;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.Queue;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Uitilize index vectors for the Beagle {code SemanticSpace}, and incorporate
 * index vectors of co-occuring words into the Semantic Vector for a focus word.
 * This is done by first generating a fixed random dense index vector for each
 * word encountered.  Then, using a window of text on the left and on the right,
 * several operations are done to incorporate the semantics of this context into
 * a semantic {@code Vector}.  First, the index vectors of co-occuring words are
 * summed to the given semantic {@code Vector}.  Then, N-Grams of the context
 * words are generated, and the circular correlation of these N-Grams are summed
 * into the given semantic {@code Vector}. </p>
 *
 * Most of this work is done {@link updateMeaningWithTerm}. </p>
 *
 * Additionally, this {@code IndexUser} can be saved, where all of the index
 * vectors generated so far will be serialized.  This serialization can then be
 * loaded up to restore the state of the {@code IndexUser}. </p>
 *
 */
public class BeagleIndexUser implements IndexUser {

    /**
     * The default index vector size, used when one is not specified.
     */
    private static final int DEFAULT_INDEX_VECTOR_SIZE = 512;

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
     * A reference to the most recently seen focus {@code Vector}.  This {@code
     * Vector} determines when the convolutions will be refreshed.
     */
    private Vector lastSeenVector;

    /**
     * N-gram convolutions of terms which co-occur with a given focus word.
     * This vector will include n-grams which start at the word prior to the
     * given focus word.
     */
    private Vector firstConvolution;

    /**
     * N-gram convolutions of terms which co-occur with a given focus word.
     * This vector will include n-grams which start at the given focus word.
     */
    private Vector secondConvolution;

    /**
     * The first permutation ordering for vectors.
     */
    private int[] permute1;

    /**
     * The second permutation ordering for vectors.
     */
    private int[] permute2;

    /**
     * Create a {@code BeagleIndexBuiler} which uses the {@code
     * DEFAULT_INDEX_VECTOR_SIZE} as the length of each {@code Vector} generated
     * in this {@code IndexUser}.
     */
    public BeagleIndexUser(Vector holder) {
        init(DEFAULT_INDEX_VECTOR_SIZE, holder);
    }

    /**
     * Create a {@code BeagleIndexUser} which uses {@code vectorLength} as
     * the size of each generated {@code Vector}.
     *
     * @param vectorLength The length of each index and semantic {@code Vector}
     *                     used in this {@code IndexUser}.
     */
    public BeagleIndexUser(int vectorLength, Vector holder) {
        init(vectorLength, holder);
    }

    /**
     * Initialize this {@code BeagleIndexUser}.
     */
    private void init(int s, Vector holder) {
        indexVectorSize = s;

        // Save the place holder vector.
        placeHolder = holder;
        lastSeenVector = new DenseVector(s);

        // Generate the permutation arrays.
        permute1 = new int[indexVectorSize];
        permute2 = new int[indexVectorSize];
        randomPermute(permute1);
        randomPermute(permute2);
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
     * {@inheritDoc}
     *
     * </p>Add a holograph encoding the co-occurance information, and the
     * ordering information of the given context.  {@code termVector} will be
     * added to the result {@code Vector}, and then the convolution of any prior
     * convoluted n-grams will be convoluted with the given {@code termVector}
     * and added to the result.  When {@code focusVector} changes to be a
     * different term, new n-gram convolutions are generated which use a
     * placeholder in place of {@code focusVector}.
     */
    public Vector generateMeaning(Vector focusVector,
                                  Vector termVector,
                                  int distance) {
        Vector result = new DenseVector(indexVectorSize);

        // Add the termVector to the result.
        Vectors.add(result, termVector);

        if (focusVector != lastSeenVector) {
            // When we change words, create the new first and second Convolution
            // of n-grams.  The first convolution is of the last seen word, and
            // a place holder, and then convoluted with the co-occuring given
            // term.  The second convolution is of a place holder and the given
            // co-occurring term.
            firstConvolution = convolute(lastSeenVector, placeHolder);
            Vectors.add(result, firstConvolution);
            firstConvolution = convolute(firstConvolution, termVector);

            secondConvolution = convolute(placeHolder, termVector);
        } else {
            // In the normal case, we just do the convolution of each stored
            // convolution with the new term vector.
            firstConvolution = convolute(firstConvolution, termVector);
            secondConvolution = convolute(secondConvolution, termVector);
        }

        // Add the newest convolutions to the result.
        Vectors.add(result, firstConvolution);
        Vectors.add(result, secondConvolution);

        return result;
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
        //long start = System.currentTimeMillis();
        // Permute both vectors.
        left = changeVector(left, permute1);
        right = changeVector(right, permute2);

        // Use the Fast Fourier Transform on each vector.
        FastFourierTransform.transform(left, 0, 1);
        FastFourierTransform.transform(right, 0, 1);

        // Multiply the two together.
        Vector result = Vectors.multiply(left, right);

            // The inverse transform completes the convolution.
        FastFourierTransform.backtransform(result, 0, 1);
        //long end = System.currentTimeMillis();
        //System.out.println("time spent fft: " + (end-start));
        return result;
    }

    /**
     * Shuffle the given vector based on the ordering information given in
     * {@code orderVector}.
     *
     * @param data The vector to be shuffled.
     * @param orderVector The ordering of values to be used.
     * 
     * @return The shuffled version of {@code data}.
     */
    private Vector changeVector(Vector data, int[] orderVector) {
        Vector result = new DenseVector(indexVectorSize);
        for (int i = 0; i < indexVectorSize; i++)
            result.set(i, data.get(orderVector[i]));
        return result;
    }
}
