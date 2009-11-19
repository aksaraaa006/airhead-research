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

package edu.ucla.sspace.index;

import edu.ucla.sspace.util.SparseIntArray;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.Serializable;


public class SparseRandomIndexVector implements SparseVector, Serializable {

    private static final long serialVersionUID = 1L;

    private final SparseIntArray intArray;

    public SparseRandomIndexVector(int length) {
        intArray = new SparseIntArray(length);
    }

    public SparseRandomIndexVector(Vector v) {
        intArray = new SparseIntArray(v.length());
        for (int i = 0; i < v.length(); ++i)
            intArray.set(i, (int) v.get(i));
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        int newValue = intArray.getPrimitive(index) + (int) delta;
        intArray.set(index, newValue);
        return newValue;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return intArray.getPrimitive(index);
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
    public void set(int index, double value) {
        intArray.set(index, (int) value);
    }

    /**
     * {@inheritDoc}
     */
    public void set(double[] values) {
        for (int i = 0; i < values.length; ++i)
            intArray.set(i, (int) values[i]);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        double[] array = new double[size];
        int maxSize = (size > length()) ? length() : size;
        for (int i = 0; i < maxSize; ++i)
            array[i] = intArray.getPrimitive(i);
        return array;
    }
}
