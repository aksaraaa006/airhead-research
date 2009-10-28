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
        int vectorLength = Integer.parseInt(vectorLengthProp);

        PermutationFactory.init(rightSize, vectorLength);
    }

    /**
     * {@inheritDoc}
     */
    public Vector generateMeaning(Vector focusVector,
                                  Vector termVector,
                                  int distance) {
        if (!(termVector instanceof IndexVector))
            throw new IllegalArgumentException("IndexVector expected");

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
            PermutationFactory.Function function =
                PermutationFactory.getFunction(count);

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

}
