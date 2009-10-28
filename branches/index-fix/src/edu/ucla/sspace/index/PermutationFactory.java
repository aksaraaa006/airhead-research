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

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;


/**
 * A static factory class for generating and retrieving permutation functions.
 * Users of this class should call {@code init} before attempting to retrieve
 * any permutation functions.  {@code init} will generate a specified number of
 * permutation functions, and if while running, a function which what is created
 * during {@code init}, a null function will be returned.
 */
public class PermutationFactory {

    /**
     * A mapping from word distance to a particular permutation function.
     */
    private static Map<Integer,Function> permutationToReordering;

    /**
     * Make it uninstantiable.
     */
    private PermutationFactory() {}

    /**
     * Setup the {@code PermutationFactory} to have functions going both forward
     * and backwards up to the given {@code exponentCount}, and each function
     * will be for a {@code Vector} of length {@code vectorLength}.
     *
     * @param exponentCount The number of permutations to generate.
     * @param vectorLength The length of each {@code Vector} that will be
     *                     permuted.
     */
    public static void init(int exponentCount, int vectorLength) {
        if (permutationToReordering == null) {
            permutationToReordering = new HashMap<Integer, Function>();
            generateFunctions(exponentCount, vectorLength);
        }
    }

    /**
     * Remove all the function mappings.
     */
    public static void clear() {
        permutationToReordering = null;
    }

    /**
     * Return a particular permutation function for a given exponent.
     *
     * @param exponent The exponent to return a permutation for.
     *
     * @return A permutation function of distance {@code exponent}.
     */
    public static Function getFunction(int exponent) {
        return permutationToReordering.get(exponent);
    }

    /**
     * Generate and return the bijective mapping for each integer in the form of
     * an array based on the the current exponent of the permutation.  If the
     * mapping does not already exist, a new one will be generated and stored in
     * {@code permutationToReordering}.
     *
     * @param exponent the exponent for the current permutation 
     * @param dimensions the number of dimenensions in the index vector being
     *        permuted
     *
     * @return the mapping for each index to its new index
     */
    private static Function generateFunctions(int exponent, int dimensions) {
        // Base case: we keep the same ordering.  Create this function on the
        // fly to save space, since the base case should rarely get called.
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
        // lower exponents value to ensure that any non-existent lower-exponent
        // functions are created along the way.
        if (function == null) {
            // lookup the prior function
            int priorExponent = exponent - 1;
            Function priorFunc = generateFunctions(priorExponent, dimensions);
            
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
    public static class Function {

        public final int[] forward;
        public final int[] backward;

        public Function(int[] forward, int[] backward) {
            this.forward = forward;
            this.backward = backward;
        }

    }
}
