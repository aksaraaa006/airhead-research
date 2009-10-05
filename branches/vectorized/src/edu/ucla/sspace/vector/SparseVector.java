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

import edu.ucla.sspace.util.SparseDoubleArray;

/**
 * A {@code Vector} instance that keeps only the non-zero values of
 * the semantics in memory, thereby saving space at the expense of time.
 * Internally this class is a wrapper for {@link SparseDoubleArray}, allowing
 * the sparse array functionaltiy to work as a {@code Vector}.
 */
public class SparseVector implements Vector {
    SparseDoubleArray vector;
    /**
     * Creates a {@code SparseVector} that grows to the maximum size set
     * by {@link Double#MAX_VALUE}.
     */
    public SparseVector() {
        vector = new SparseDoubleArray();
    }

    /** 
     * Create a {@code SparseVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code SparseVector}.
     */
    public SparseVector(int length) {
        vector = new SparseDoubleArray(length);
    }

    /**
     * {@inheritDoc}
     */
    public void addVector(Vector vector) {
        // Skip vectors of different lengths.
        if (vector.length() != length())
            return;

        if (vector instanceof SparseVector) {
            SparseVector v = (SparseVector) vector;
            // If vector is a sparse vector, simply get the non zero values and
            // add them to this instance.
            int[] otherIndicies = v.getNonZeroIndicies();
            for (int index : otherIndicies)
                add(index, vector.get(index));
        } else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < length(); ++i) {
                double value = vector.get(i);
                if (value != 0d)
                    add(i, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        set(index, get(index) + delta);
        return get(index) + delta;
    }

    public void set(double[] values) {
        vector = new SparseDoubleArray(values);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.setPrimitive(index, value);
    }

    /** 
     * Return the set of non-zero indicies.  This is primarily for the purpose
     * of summing two {@code SparseVector}s efficiently.
     */
    int[] getNonZeroIndicies() {
        return vector.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public double[] toArray(int size) {
        double[] array = new double[size];
        return vector.toPrimitiveArray(array);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.getPrimitive(index);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }
}
