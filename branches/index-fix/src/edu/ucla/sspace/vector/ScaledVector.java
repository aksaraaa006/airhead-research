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


/**
 * A decorator for a {@link Vector} that scales all values in the backing vector
 * in constant time.
 *
 * <p> Should the backing vector be a {@link SparseVector}, users are encouraged
 * to use {@link SparseScaledVector}.
 *
 * @author Keith Stevens
 *
 * @see SparseScaledVector
 */
public class ScaledVector implements DoubleVector, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The original {@code Vector} that this {@code ScaledVector} decorates.
     */
    private final DoubleVector vector;

    /**
     * The scalar used to adjust the values in the backing vector
     */
    private final double scaleFactor;

    /**
     * Create a new {@code ScaledVector} that scales the values in the provided
     * {@code Vector}.
     *
     * @param v the vector to decorate
     * @param factor the value used to scale all values
     */
    public ScaledVector(DoubleVector v, double factor) {
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
    public Double getValue(int index) {
        return get(index);
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
    public void set(int index, Number value) {
        set(index, value.doubleValue());
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = vector.toArray();
        for (int i = 0; i < array.length; ++i)
            array[i] *= scaleFactor;
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }
}
