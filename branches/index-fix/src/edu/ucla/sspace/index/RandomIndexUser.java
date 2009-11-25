/*
 * Copyright 2009 David Jurgens
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

import edu.ucla.sspace.vector.IndexVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;


/**
 * A permutation function that is optimized for {@link IndexVector} instances.
 * This class precomputes the permutations as necessary and only requires {@code
 * O(k)} time to compute a single permutation, where {@code k} is the number of
 * non-zero elements in the {@code IndexVector}.
 *
 * @author David Jurgens
 */
public class RandomIndexUser implements IndexUser {

    /**
     * The prefix for naming public properties.
     */
    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.index.IndexUser";

    /**
     * If this option is provided, permutations will be used while generating
     * meaning vectors.
     * {@code RandomIndexGenerator}.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY =
        PROPERTY_PREFIX + ".permute";

    /**
     * If this property is provided, dense semantic vectors will be used for
     * this {@code IndexUser}, otherwise sparse semantic vectors will be used.
     */
    public static final String USE_DENSE_SEMANTICS_PROPERTY =
        PROPERTY_PREFIX + ".dense";

    private static final String DEFAUL_PERMUTATION_FUNCTION =
        "edu.ucla.sspace.index.DefaultPermutationFuntion";

    /**
     * A static mapping for generating and retrieving permutation functions.
     * Uses of this map should be preceeded by a call to {@code init} before
     * attempting to retrieve any permutation functions.  {@code init} will
     * generate a specified number of permutation functions, and if while
     * running, a function which what is created during {@code init}, a null
     * function will be returned.
     */
    private static PermutationFunction permutationFactory = null; 

    /**
     * Set to true if {@code getEmptyVector} should return a dense vector
     * instead of a sparse vector.
     */
    private final boolean useDenseVectors;

    /**
     * The length of each {@code Vector} this {@code IndexUser} can process.
     */
    private final int vectorLength;

    /**
     * Creates an empty set of permutations.
     */
    public RandomIndexUser() {
        this(System.getProperties());
    }

    /**
     * Create a new {@code RandomIndexUser} which first initializes the {@code
     * PermutationFactory} with the expected number of permutations.
     */
    public RandomIndexUser(Properties props) {
        String windowSizeProp =
            props.getProperty(IndexUser.WINDOW_SIZE_PROPERTY, "5,5");
        String[] leftRight = windowSizeProp.split(",");
        int leftSize = Integer.parseInt(leftRight[0]);
        int rightSize = Integer.parseInt(leftRight[1]);

        String vectorLengthProp =
            props.getProperty(IndexUser.INDEX_VECTOR_LENGTH_PROPERTY);
        vectorLength = Integer.parseInt(vectorLengthProp);

        useDenseVectors =
            props.getProperty(USE_DENSE_SEMANTICS_PROPERTY) != null;
        String permFunction = props.getProperty(PERMUTATION_FUNCTION_PROPERTY);
        if (permFunction != null) 
            init(Math.max(leftSize, rightSize), vectorLength, permFunction);
    }

    public void saveStaticData(File filename) {
        try {
            ObjectOutputStream stream = new ObjectOutputStream(
                    new BufferedOutputStream(new FileOutputStream(filename)));
            stream.writeObject(permutationFactory);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public Vector generateMeaning(Vector focusVector,
                                  Vector termVector,
                                  int distance) {
        if (focusVector == null)
            throw new IllegalArgumentException("Null vector given.");

        // Take no action if the given term vector is null.
        if (termVector == null)
            return focusVector;

        if (!(termVector instanceof IndexVector))
            throw new IllegalArgumentException("IndexVector expected");

        // If no permutations are used, simply add the two vectors.
        if (permutationFactory == null)
            return Vectors.add(focusVector, termVector);

        IndexVector permutedVector =
            permutationFactory.permute((IndexVector) termVector, distance);
        return Vectors.add(focusVector, permutedVector);
    }

    /**
     * {@inheritDoc}.
     */
    public Vector getEmptyVector() {
        return (useDenseVectors)
            ? new DenseRandomIndexVector(vectorLength)
            : new SparseRandomIndexVector(vectorLength);
    }

    /**
     * Setup the {@code PermutationFactory} to have functions going both
     * forward and backwards up to the given {@code exponentCount}. Each
     * function will be for a {@code Vector} of length {@code vectorLength}.
     *
     * @param exponentCount The number of permutations to generate.
     * @param vectorLength The length of each {@code Vector} that will be
     *                     permuted.
     */
    private static void init(int exponentCount,
                             int length,
                             String permFunction) {
        if (permutationFactory == null) {
            try {
                Class permClazz = Class.forName(permFunction);
                permutationFactory =
                    (PermutationFunction) permClazz.newInstance();
                permutationFactory.permute(
                        new IndexVector(length, new int[] {}, new int[]{}),
                        exponentCount);
            } catch (Exception e) {
                throw new Error(e);
            }
        }
    }

    /**
     * Remove all the function mappings.
     */
    public static void clear() {
        permutationFactory = null;
    }

    /**
     * Returns a string which details what type of IndexUser is being used and
     * what type permutation is being used.
     *
     * @return A string desrcribing this IndexUser.
     */
    public String toString() {
        return "RandomIndexUser" + ((permutationFactory != null)
                                   ? permutationFactory.toString()
                                   : "-NoPermutation");
    }
}
