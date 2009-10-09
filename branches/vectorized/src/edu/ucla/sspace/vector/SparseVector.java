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
 *
 * {@see SparseDoubleArray} for an implementation of the functionality.
 */
public class SparseVector implements Vector {

    /**
     * The {@code SparseDoubleArray} which provides most of the functionality in
     * this class.
     */
    private SparseDoubleArray vector;

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
    public double add(int index, double delta) {
        set(index, get(index) + delta);
        return get(index) + delta;
    }

    /**
     * {@inheritDoc}
     */
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
