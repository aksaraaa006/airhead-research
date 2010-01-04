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

import java.io.Serializable;

import edu.ucla.sspace.util.SparseDoubleArray;

/**
 * A {@code Vector} instance that keeps only the non-zero values in memory,
 * thereby saving space at the expense of time.
 *
 * <p> See {@link SparseDoubleArray} for details on how the sparse
 * representation is implemented
 *
 * @author Keith Stevens
 */
public class CompactSparseVector implements SparseDoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The {@code SparseDoubleArray} which provides most of the functionality in
     * this class.
     */
    private SparseDoubleArray vector;

    /**
     * Creates a {@code CompactSparseVector} that grows to the maximum size set
     * by {@link Double#MAX_VALUE}.
     */
    public CompactSparseVector() {
        vector = new SparseDoubleArray();
    }

    /** 
     * Create a {@code CompactSparseVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code CompactSparseVector}.
     */
    public CompactSparseVector(int length) {
        vector = new SparseDoubleArray(length);
    }

    /**
     * Create a {@code CompactSparseVector} from an array, saving only the non
     * zero entries.
     *
     * @param array The double array to produce a sparse vector from.
     */
    public CompactSparseVector(double[] array) {
        vector = new SparseDoubleArray(array);
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
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /** 
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[vector.length()];
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
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }

}
