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
 * A {@code Vector} instance that keeps only the non-zero values of
 * the semantics in memory, thereby saving space at the expense of time.
 *
 * {@see SparseDoubleArray} for an implementation of the functionality.
 *
 * @author Keith Stevens
 */
public class CompactSparseVector implements DoubleVector,
                                            SparseVector,
                                            Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The {@code SparseDoubleArray} which provides most of the functionality in
     * this class.
     */
    private SparseDoubleArray vector;

    /**
     * The maximum known length of this {@code Vector}.
     */
    private int knownLength;

    /**
     * Creates a {@code CompactSparseVector} that grows to the maximum size set
     * by {@link Double#MAX_VALUE}.
     */
    public CompactSparseVector() {
        vector = new SparseDoubleArray();
        knownLength = 0;
    }

    /** 
     * Create a {@code CompactSparseVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code CompactSparseVector}.
     */
    public CompactSparseVector(int length) {
        vector = new SparseDoubleArray(length);
        knownLength = length;
    }

    /**
     * Create a {@code CompactSparseVector} from an array, saving only the non
     * zero entries.
     *
     * @param array The double array to produce a sparse vector from.
     */
    public CompactSparseVector(double[] array) {
        vector = new SparseDoubleArray(array);
        knownLength = array.length;
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        updateLength(index);

        set(index, get(index) + delta);
        return get(index) + delta;
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        updateLength(values.length);

        vector = new SparseDoubleArray(values);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        updateLength(index);

        vector.setPrimitive(index, value);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /** 
     * @{inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public double[] toArray(int size) {
        updateLength(size);

        double[] array = new double[size];
        return vector.toPrimitiveArray(array);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        updateLength(index);

        return vector.getPrimitive(index);
    }

    /**
     * {@inheritDoc}
     */
    public Number getValue(int index) {
        return get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return knownLength;
    }

    /**
     * Extend the known length of this {@code Vector} if the given length is
     * longer than any previously seen length.
     *
     * @param length The new possible length of the {@code Vector}.
     */
    private void updateLength(int length) {
        if (knownLength <= length)
            knownLength = length;
    }
}
