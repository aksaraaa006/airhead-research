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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;


/**
 * A permutation function that is optimized for {@link IndexVector} instances.
 * This class precomputes the permutations as necessary and only requires {@code
 * O(k)} time to compute a single permutation, where {@code k} is the number of
 * non-zero elements in the {@code IndexVector}.
 *
 * @author David Jurgens
 */
public class DefaultPermutationFunction implements PermutationFunction {

    /**
     * The array of permutation functions which can be applied to an {@code
     * IndexVector}.
     */
    private Function[] permutationToOrdering;

    /**
     * The length of the {@code IndexVectors} permutable by this instance.
     */
    private int indexVectorLength;
    
    public DefaultPermutationFunction() {
    }

    /**
     * {@inheritDoc}
     */
    public void setupPermutations(int vectorLength,
                                  int numPositivePermutations, 
                                  int numNegativePermutations) {
        indexVectorLength = vectorLength;
        int largestExponent = Math.max(numPositivePermutations,
                                       Math.abs(numNegativePermutations));
        permutationToOrdering = new Function[largestExponent];
        prepareFunctions(largestExponent);
    }

    /**
     * Define each of the required permutations required by this {@code
     * PermutationFunction}. 
     *
     * @param exponent The largest absolute value of an expected permutation.
     */
    private void prepareFunctions(int exponent) {
        // Create the initial no-op permutation.
        int[] func = new int[indexVectorLength];
        for (int i = 0; i < indexVectorLength; ++i)
            func[i] = i;
        Function priorFunc = new Function(func, func);

        for (int j = 0; j < exponent; ++j) {
            // Convert to an Integer Array to use Collections.shuffle()
            Integer[] objFunc = new Integer[indexVectorLength];
            for (int i = 0; i < indexVectorLength; ++i) {
                objFunc[i] = Integer.valueOf(priorFunc.forward[i]);
            }

            // Shuffle to get a new permutation.
            List<Integer> list = Arrays.asList(objFunc);
            Collections.shuffle(list, RandomIndexGenerator.RANDOM);
            
            // Convert the List to a primitive array.
            int[] forwardMapping = new int[indexVectorLength];
            int[] backwardMapping = new int[indexVectorLength];
            for (int i = 0; i < indexVectorLength; ++i) {
                forwardMapping[i] = objFunc[i].intValue();
                backwardMapping[objFunc[i].intValue()] = i;
            }            

            // Store the ordering generated.
            permutationToOrdering[j] =
                new Function(forwardMapping, backwardMapping);
            priorFunc = permutationToOrdering[j];
        }
    }

    /**
     * {@inheritDoc}
     */
    public IndexVector permute(IndexVector v , int numPermutations) {
        if (numPermutations == 0)
            return new IndexVector(v.length(),
                                   v.positiveDimensions(),
                                   v.negativeDimensions());

        int length = v.length();

        int[] oldPos = v.positiveDimensions();
        int[] oldNeg = v.negativeDimensions();

        // Create new arrays to hold the permuted locations of the vectors's
        // positive and negative values.
        int[] positive = oldPos;
        int[] negative = oldNeg;

        boolean isInverse = numPermutations < 0;
        
        int totalPermutations = Math.abs(numPermutations);

        for (int count = 0; count < totalPermutations; ++count) {            
            // load the reordering function for this iteration of the
            // permutation
            Function function = permutationToOrdering[count];

            // Select the inverse mapping if this an inverted permutation.
            int[] reordering = (isInverse) 
                ? function.backward : function.forward;
            
            // Create a copy of the previous permuted values for positive and
            // negative since permutations cannot be done in place.
            oldPos = Arrays.copyOf(positive, positive.length);
            oldNeg = Arrays.copyOf(negative, negative.length);
            
            // Re-order the positive and negative indexVectorLength, since an
            // IndexVector specifies all non-zero dimensions to be
            // positive or negative.
            for (int i = 0; i < oldPos.length; ++i) {
                positive[i] = reordering[oldPos[i]];
            }

            for (int i = 0; i < oldNeg.length; ++i) {
                negative[i] = reordering[oldNeg[i]];
            }
        }

        return new IndexVector(length, positive, negative);
    }

    /**
     * Returns the name of this class
     */
    public String toString() {
        return "DefaultPermutationFunction";
    }

    /**
     * A bijective, invertible mapping between indices.
     */
    private static class Function {

        /** 
         * The reordering arrays specify the new position for each index in a
         * {@code Vector}.
         */
        private final int[] forward;
        private final int[] backward;

        public Function(int[] forward, int[] backward) {
            this.forward = forward;
            this.backward = backward;
        }
    }
}
