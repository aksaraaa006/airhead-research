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


public class DenseIntVector implements IntegerVector, Serializable {

    private static final long serialVersionUID = 1L;

    private int[] vector;

    public DenseIntVector(int length) {
        vector = new int[length];
    }

    public DenseIntVector(IntegerVector v) {
        vector = new int[v.length()];
        for (int i = 0; i < v.length(); ++i)
            vector[i] = v.get(i);
    }

    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        vector[index] += delta;
        return vector[index];
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return vector[index];
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
        return vector.length;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        vector[index] = value;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        set(index, value.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public void set(int[] values) {
        for (int i = 0; i < values.length; ++i)
            vector[i] = values[i];
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray(int size) {
        int[] array = new int[size];
        int maxSize = (size > length()) ? length() : size;
        for (int i = 0; i < maxSize; ++i)
            array[i] = vector[i];
        return array;
    }
}
