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

import java.util.Arrays;

/**
 * A {@code Vector} where all values are held in memory. The underlying
 * implementation is simply an array of doubles.  <p>
*/
public class DenseVector implements Vector {

    /**
     * The values of this {@code DenseVector}.
     */
    private double[] vector;

    /**
     * Create an {@code DenseVector} with all values starting at 0 with
     * the given length.
     *
     * @param vectorLength The size of the vector to create.
     */
    public DenseVector(int vectorLength) {
        vector = new double[vectorLength];
    }

    /**
     * Create a {@code DenseVector} taking the values given by {@code
     * vector}.
     *
     * @param vector The vector values to start with.
     */
    public DenseVector(double[] vector) {
        this.vector = vector;
    }
	
    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        vector[index] += delta;
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector[index] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        vector = Arrays.copyOf(values, vector.length);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public double[] toArray(int size) {
        double[] array = new double[size];
        for (int i = 0; i < size && i < vector.length; ++i)
            array[i] = vector[i];
        return array;
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return vector.length;
    }
}
