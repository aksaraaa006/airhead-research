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

import java.util.Arrays;

/**
 * A {@code Vector} instance that keeps only the non-zero values of
 * the semantics in memory, thereby saving space at the expense of time.
 *
 * This class is thread-safe.
 */
public class SparseSemanticVector implements Vector {
    /**
     * The set of indicies which have non zero values in this vector.
     */
    private int[] indices;

    /** 
     * The non zero values for each of the indicies stored.
     */
    private double[] values;

    /**
     * The maximum size of this {@code Vector}.
     */
    private int vectorLength;

    /** 
     * Create a {@code SparseSemanticVector with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code SparseSemanticVector}.
     */
    public SparseSemanticVector(int length) {
        indices = new int[0];
        values = new double[0];
        vectorLength = length;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void addVector(Vector vector) {
        // Skip vectors of different lengths.
        if (vector.length() != length())
            return;

        if (vector instanceof SparseSemanticVector) {
            SparseSemanticVector v = (SparseSemanticVector) vector;
            // If vector is a sparse vector, simply get the non zero values and
            // add them to this instance.
            int[] otherIndicies = v.getNonZeroIndicies();
            for (int index : otherIndicies)
                add(index, vector.get(index));
        } else {
            // Otherwise, inspect all values of vector, and only add the non
            // zero values.
            for (int i = 0; i < vectorLength; ++i) {
                double value = vector.get(i);
                if (value != 0d)
                    add(i, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void add(int index, double delta) {
        int pos = getIndex(index);
        values[pos] += delta;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized void set(int index, double value) {
        int pos = getIndex(index);
        values[pos] += value;
    }

    /**
     * Get the index in indecies and values for index. If the value does not
     * exist, extend the two internal arrays to have space.
     *
     * @param index The dimension to get an array index value for.
     *
     * @return The index in {@code indicies} of {@code index}.
     */
    private int getIndex(int index) {
        int pos = Arrays.binarySearch(indices, index);
        // need to make room in the indices array
        if (pos < 0) {
            int newPos = 0 - (pos + 1);
            int[] newIndices = Arrays.copyOf(indices, indices.length + 1);
            double[] newValues = Arrays.copyOf(values, values.length + 1);

            // shift the elements down by one to make room
            for (int i = newPos; i < values.length; ++i) {
                newValues[i+1] = values[i];
                newIndices[i+1] = indices[i];
            }

            // swap the arrays
            indices = newIndices;
            values = newValues;
            pos = newPos;

            // update the position of the index in the values array
            indices[pos] = index;
        }
        return pos;
    }

    /** 
     * Return the set of non-zero indicies.  This is primarily for the purpose
     * of summing two {@code SparseSemanticVector}s efficiently.
     */
    public int[] getNonZeroIndicies() {
        return indices;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double[] toArray() {
        double[] vector = new double[vectorLength];
        for (int i = 0; i < indices.length; ++i)
            vector[indices[i]] = values[i];
        return vector;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized double get(int index) {
        int pos = Arrays.binarySearch(indices, index);
        return (pos < 0) ? 0d : values[pos];
    }

    /**
     * {@inheritDoc}
     */
    public double length() {
        return vectorLength;
    }
}
