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

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.IndexVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.Arrays;
import java.util.Collections;
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
    public static final String USE_PERMUTATION_PROPERTY =
        PROPERTY_PREFIX + ".permute";

    /**
     * If this property is provided, dense semantic vectors will be used for
     * this {@code IndexUser}, otherwise sparse semantic vectors will be used.
     */
    public static final String USE_DENSE_SEMANTICS_PROPERTY =
        PROPERTY_PREFIX + ".dense";

    /**
     * A static mapping for generating and retrieving permutation functions.
     * Uses of this map should be preceeded by a call to {@code init} before
     * attempting to retrieve any permutation functions.  {@code init} will
     * generate a specified number of permutation functions, and if while
     * running, a function which what is created during {@code init}, a null
     * function will be returned.
     */
    private static Map<Integer,Function> permutationToReordering;

    private final boolean usePermutations;

    private final boolean useDenseVectors;

    private final int vectorLength;
    /**
     * Create an empty set of permutations.
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

        usePermutations = props.getProperty(USE_PERMUTATION_PROPERTY) != null;
        useDenseVectors =
            props.getProperty(USE_DENSE_SEMANTICS_PROPERTY) != null;
        init(rightSize, vectorLength);
    }

    /**
     * {@inheritDoc}
     */
    public Vector generateMeaning(Vector focusVector,
                                  Vector termVector,
                                  int distance) {
        if (!(termVector instanceof IndexVector))
            throw new IllegalArgumentException("IndexVector expected");

        // If no permutations are used, simply add the two vectors.
        if (!usePermutations)
            return Vectors.add(focusVector, termVector);

        IndexVector v = (IndexVector) termVector;

        int length = v.length();

        int[] oldPos = v.positiveDimensions();
        int[] oldNeg = v.negativeDimensions();

        // create new arrays to hold the permuted locations of the vectors's
        // positive and negative values.
        //
        // NB: we use a copy here to ensure that the function works for the 0
        // permutation (i.e. effectively a no-op);
        int[] positive = Arrays.copyOf(oldPos, oldPos.length);
        int[] negative = Arrays.copyOf(oldNeg, oldNeg.length);

        boolean isInverse = distance < 0;
        
        // NB: because we use the signum and !=, this loop will work for both
        // positive and negative numbers of permutations
        int totalPermutations = Math.abs(distance);

        for (int count = 1; count <= totalPermutations; ++count) {            
            // load the reordering funcion for this iteration of the permutation
            Function function = permutationToReordering.get(count);

            // based on whether this is an inverse permutation, select whether
            // to use the forward or backwards mapping.
            int[] reordering = (isInverse) 
                ? function.backward : function.forward;
            
            // create a copy of the previous permuted values for positive and
            // negative.  We need this array because the permutation cannot be
            // done in place
            oldPos = Arrays.copyOf(positive, positive.length);
            oldNeg = Arrays.copyOf(negative, negative.length);
            
            // The reordering array specifies for index i the positive of i in
            // the permuted array.  Since the positive and negative indices are
            // the only non-zero indicies, we can simply create new arrays for
            // them of the same length and then set their new positions based on
            // the values in the reordering array.
            for (int i = 0; i < oldPos.length; ++i)
                positive[i] = reordering[oldPos[i]];

            for (int i = 0; i < oldNeg.length; ++i)
                negative[i] = reordering[oldNeg[i]];
        }

        IndexVector permutedVector =
            new IndexVector(length, positive, negative);

        return Vectors.add(focusVector, permutedVector);
    }

    /**
     * {@inheritDoc}.
     */
    public Vector getEmtpyVector() {
        return (useDenseVectors)
            ? new DenseRandomIndexVector(vectorLength)
            : new SparseRandomIndexVector(vectorLength);
    }

    /**
     * Setup the {@code PermutationFactory} to have functions going both
     * forward and backwards up to the given {@code exponentCount}, and each
     * function will be for a {@code Vector} of length {@code vectorLength}.
     *
     * @param exponentCount The number of permutations to generate.
     * @param vectorLength The length of each {@code Vector} that will be
     *                     permuted.
     */
    private static void init(int exponentCount, int length) {
        if (permutationToReordering == null) {
            permutationToReordering = new HashMap<Integer, Function>();
            generateFunctions(exponentCount, length);
        }
    }

    /**
     * Remove all the function mappings.
     */
    public static void clear() {
        permutationToReordering = null;
    }

    /**
     * Generate and return the bijective mapping for each integer in the
     * form of an array based on the the current exponent of the
     * permutation.  If the mapping does not already exist, a new one will
     * be generated and stored in {@code permutationToReordering}.
     *
     * @param exponent the exponent for the current permutation 
     * @param dimensions the number of dimenensions in the index vector
     *                   being permuted
     *
     * @return the mapping for each index to its new index
     */
    private static Function generateFunctions(int exponent,
                                              int dimensions) {
        // Base case: we keep the same ordering.  Create this function on
        // the fly to save space, since the base case should rarely get
        // called.
        if (exponent == 0) {
            int[] func = new int[dimensions];
            for (int i = 0; i < dimensions; ++i) {
                func[i] = i;
            }
            Function function = new Function(func, func);
            permutationToReordering.put(exponent, new Function(func, func));
            return function;
        }

        exponent = Math.abs(exponent);

        Function function = permutationToReordering.get(exponent);
        
        // If there wasn't a funcion for that exponent then created one by
        // permuting the lower exponents value.  Use recursion to access the
        // lower exponents value to ensure that any non-existent
        // lower-exponent functions are created along the way.
        if (function == null) {
            // lookup the prior function
            int priorExponent = exponent - 1;
            Function priorFunc = generateFunctions(priorExponent,
                                                   dimensions);
            
            // convert to an object based array to use Collections.shuffle()
            Integer[] objFunc = new Integer[dimensions];
            for (int i = 0; i < dimensions; ++i) {
                objFunc[i] = Integer.valueOf(priorFunc.forward[i]);
            }

            // then shuffle it to get a new permutation
            java.util.List<Integer> list = Arrays.asList(objFunc);
            Collections.shuffle(list, RandomIndexGenerator.RANDOM);
            
            // convert back to a primitive array
            int[] forwardMapping = new int[dimensions];
            int[] backwardMapping = new int[dimensions];
            for (int i = 0; i < dimensions; ++i) {
                forwardMapping[i] = objFunc[i].intValue();
                backwardMapping[objFunc[i].intValue()] = i;
            }            
            function = new Function(forwardMapping, backwardMapping);

            // store it in the function map for later usee
            permutationToReordering.put(exponent, function);
        }

        return function;
    }

    /**
     * A bijective, invertible mapping between indices.
     */
    private static class Function {

        public final int[] forward;
        public final int[] backward;

        public Function(int[] forward, int[] backward) {
            this.forward = forward;
                this.backward = backward;
        }
    }
}
