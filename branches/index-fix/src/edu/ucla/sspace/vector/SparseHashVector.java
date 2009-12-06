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

package edu.ucla.sspace.vector;

import edu.ucla.sspace.util.SparseHashArray;


/**
 * A {@code SparseVector} implementation backed by a {@code HashMap}.  This
 * provides amoritized constant time access to all get and set operations, while
 * using more space than the {@link CompactSparseVector} or {@link
 * AmortizedSparseVector} classes.
 *
 * <p> See {@see SparseHashArray} for implementation details.
 *
 * @author David Jurgens
 */
public class SparseHashVector implements DoubleVector, SparseVector {

    /**
     * The backing array that holds the vector values
     */
    private SparseHashArray<Double> vector;

    /**
     * The length of this {@code Vector}
     */
    private int length;

    /**
     * Creates a {@code SparseHashVector} with a maximum size of {@link
     * Integer#MAX_VALUE}.
     */
    public SparseHashVector() {
        vector = new SparseHashArray<Double>();
        length = Integer.MAX_VALUE;
    }

    /** 
     * Create a {@code SparseHashVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code Vector}
     */
    public SparseHashVector(int length) {
        vector = new SparseHashArray<Double>(length);
        this.length = length;
    }

    /**
     * Create a {@code SparseHashVector} from an array, saving only the non
     * zero entries.
     *
     * @param array the values of this vector
     */
    public SparseHashVector(double[] array) {
        vector = new SparseHashArray<Double>();
        this.length = array.length;
        set(array);
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
        vector = new SparseHashArray<Double>();
        for (int i = 0; i < values.length; ++i)
            if (values[i] != 0)
                vector.set(i, values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.set(index, value);
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
    public double[] toArray(int size) {
        double[] array = new double[size];
        Double[] objArray = new Double[size];
        vector.toArray(objArray);
        for (int i = 0; i < size; ++i) {
            Double d = objArray[i];
            array[i] = (d == null) ? 0 : d;
        }
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        Double d = vector.get(index);
        return (d == null) ? 0 : d;
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
        return length;
    }
}
