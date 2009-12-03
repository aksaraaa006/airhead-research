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


/**
 * An interface for functions that permute the ordering of {@code IndexVector}s.
 * Implementations are expected to be thread safe when performing permutations.
 */
public interface PermutationFunction {

    /**
     * Permutes the provided {@code IndexVector} the specified number of times.
     *
     * @param v an index vector to permute
     * @param numPermutations the number of times the permutation function
     *                        should be applied to the provided index vector.
     *
     * @return the original index vector permuted the specified number of times
     */
    IndexVector permute(IndexVector v, int numPermutations);

    /**
     * Initializes the {@code PermutationFunction} to generate all required
     * functions for positive and negative positions expected.
     *
     * @param numPositivePermutations The maximum {code numPermutationvalue}
     *                                value that can be passed to
     *                                {@code permute}.
     * @param numNegativePermutations The minimum {code numPermutationvalue}
     *                                value that can be passed to
     *                                {@code permute}.
     */
    void setupPermutations(int vectorLenght,
                           int numPositivePermutations,
                           int numNegativePermutations);
}
