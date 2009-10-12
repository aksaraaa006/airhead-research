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

package edu.ucla.sspace.vector;


/**
 * A collection of methods on {@code Vector}s, following a format similar to
 * that of {@link java.util.Collections}.  The methods collected here will allow
 * decoratored {@code Vector}s to be generated, along with basis algabraic
 * methods.
 *
 * @author Keith Stevens
 */
public class Vectors {

    /**
     * A private constructor to make this class uninstantiable.
     */
    private Vectors() { }

    /**
     * Return an {@code ImmutableVector} for the given {@code Vector}.
     *
     * @param vector The {@code Vector} to decorate as immutable.
     * @return An immutable version of {@code vector}.
     */
    public static Vector immutableVector(Vector vector) {
        return new ImmutableVector(vector);
    }

    /**
     * Return an {@code AtomicVector} for the given {@code Vector}.
     *
     * @param vector The {@code Vector} to decorate as atomic.
     * @return An atomic version of {@code vector}.
     */
    public static Vector atomicVector(Vector vector) {
        return new AtomicVector(vector);
    }

    /**
     * Adds the second {@code Vector} to the first {@code Vector} and returhs
     * the result.
     *
     * @param vector1 The destination vector to be summed onto.
     * @param vector2 The source vector to sum from.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector add(Vector vector1, Vector vector2) {
        // Skip vectors of different lengths.
        if (vector2.length() != vector1.length())
            return null;

        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof Sparse) {
            addSparseValues(vector1, (Sparse) vector2);
        } else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i);
                // In the case that vector1 is sparse, only add non zero values.
                if (value != 0d)
                    vector1.add(i, value);
            }
        }
        return vector1;
    }

    /**
     * Return a new {@code Vector} which is the summation of {@code vector2} and
     * {@code vector1}.
     *
     * @param vector1 The first vector to used in a summation.
     * @param vector2 The second vector to be used in a summation.
     * @return The summation of {code vector1} and {@code vector2}.
     */
    public static Vector addUnmodified(Vector vector1, Vector vector2) {
        // Skip vectors of different lengths.
        if (vector2.length() != vector1.length())
            return null;

        Vector finalVector;
        // If vector is a sparse vector, simply get the non zero values and
        // add them to this instance.
        if (vector2 instanceof Sparse) {
            finalVector = new SparseVector(vector1.length());
            addSparseValues(finalVector, (Sparse) vector1);
            addSparseValues(finalVector, (Sparse) vector2);
        } else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            finalVector = new DenseVector(vector1.length());
            for (int i = 0; i < vector2.length(); ++i) {
                double value = vector2.get(i) + vector1.get(i);
                if (value != 0d)
                    finalVector.add(i, value);
            }
        }
        return finalVector;
    }

    /**
     * Add the values from a {@code SparseVector} to a {@code Vector}.  Only the
     * non-zero indices will be traversed to save time.
     *
     * @param destination The vector to write new values to.
     * @param source The vector to read values from.
     */
    private static void addSparseValues(Vector destination,
                                        Sparse source) {
        int[] otherIndices = source.getNonZeroIndices();
        for (int index : otherIndices)
            destination.add(index, source.get(index));
    }
}
