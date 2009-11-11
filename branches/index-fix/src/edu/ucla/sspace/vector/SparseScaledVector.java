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
 * A decorator of a {@code Vector} which provides scales all values in the
 * vectors in constant time.
 *
 * @author Keith Stevens
 */
public class SparseScaledVector implements SparseVector {

    /**
     * The original {@code Vector} that this {@code SparseScaledVector}
     * decorates.
     */
    private final SparseVector vector;

    private final double scaleFactor;
    /**
     * Creates a new {@code SparseScaledVector} decorating an already existing
     * {@code Vector}.
     *
     * @param v The vector to decorate.
     * @param factor The value to scale all values by.
     */
    public SparseScaledVector(SparseVector v, double factor) {
        vector = v;
        scaleFactor = factor;
        if (factor == 0d)
            throw new IllegalArgumentException("A zero scaling is not allowed");
    }
    
    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        return vector.add(index, delta / scaleFactor);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return scaleFactor * vector.get(index);
    }

    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.getNonZeroIndices();
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        for (int i = 0; i < values.length; ++i)
            set(i, values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.set(index, value / scaleFactor);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        double[] array = vector.toArray(size);
        for (int i = 0; i < size; ++i)
            array[i] *= scaleFactor;
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        int length = vector.length();
        return length;
    }
}