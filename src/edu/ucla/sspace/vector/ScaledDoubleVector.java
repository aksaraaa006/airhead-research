/*
 * Copyright 2010 Keith Stevens 
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
 * A decorator for {@link DoubleVector}s that scales every value in a given
 * {@link DoubleVector} by some non zero scale.
 *
 * @author Keith Stevens
 */
public class ScaledDoubleVector implements DoubleVector {

    /**
     * The original vector.
     */
    private DoubleVector vector;

    /**
     * The scale applied to each value in {@code vector}
     */
    private double scale;

    /**
     * Creates a new {@link ScaledDoubleVector} that decorates a given {@link
     * DoubleVector} by scaling each value in {@code vector} by {@code scale}.
     */
    public ScaledDoubleVector(DoubleVector vector, double scale) {
        this.vector = vector;
        this.scale = scale;
        if (scale == 0d)
            throw new IllegalArgumentException("Cannot scale a vector by 0");
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        return vector.add(index, delta/scale) * scale;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index) * scale;
    }

    /**
     * Returns the vector whose values are scaled by this instance
     */
    public DoubleVector getBackingVector() {
        return vector;
    }

    /**
     * Returns the scalar multiple used by this instance to change the values of
     * the backing vector
     */
    public double getScalar() {
        return scale;
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
    public void set(int index, double value) {
        vector.set(index, value / scale);
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        vector.set(index, value.doubleValue() / scale);
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        return vector.magnitude() * scale;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length();
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] values = vector.toArray();
        for (int i = 0; i < values.length; ++i)
            values[i] *= scale;
        return values;
    }
}
