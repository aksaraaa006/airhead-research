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

import edu.ucla.sspace.util.SparseIntArray;

import java.io.Serializable;


public class SparseIntVector implements IntegerVector,
                                        SparseVector,
                                        Serializable {

    private static final long serialVersionUID = 1L;

    private final SparseIntArray intArray;

    public SparseIntVector(int length) {
        intArray = new SparseIntArray(length);
    }

    public SparseIntVector(IntegerVector v) {
        intArray = new SparseIntArray(v.length());
        for (int i = 0; i < v.length(); ++i)
            intArray.set(i,  v.get(i));
    }

    /**
     * {@inheritDoc}
     */
    public int add(int index, int delta) {
        int newValue = intArray.getPrimitive(index) +  delta;
        intArray.set(index, newValue);
        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public int get(int index) {
        return intArray.getPrimitive(index);
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
    public int[] getNonZeroIndices() {
        return intArray.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return intArray.length();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, int value) {
        intArray.set(index,  value);
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
            intArray.set(i, values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public int[] toArray(int size) {
        int[] array = new int[size];
        int maxSize = (size > length()) ? length() : size;
        for (int i = 0; i < maxSize; ++i)
            array[i] = intArray.getPrimitive(i);
        return array;
    }
}
