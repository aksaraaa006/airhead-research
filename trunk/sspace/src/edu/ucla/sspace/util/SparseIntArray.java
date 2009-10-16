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

package edu.ucla.sspace.util;

import java.util.Arrays;


/**
 * A sparse {@code int} array.  This class trades increased space efficiency at
 * the cost of decreased performance.<p>
 *
 * This class also provides additional primitive accessor methods.  This allows
 * users to invoke {@code get} and {@code set} without marshalling primitive
 * types to their {@link Integer} equivalents unnecessarily.<p>
 *
 * The {@code get} operation runs in logarithmic time.  The {@code set}
 * operation runs in consant time if setting an existing non-zero value to a
 * non-zero value.  However, if the {@code set} invocation sets a zero value to
 * non-zero, the operation is linear with the size of the array.<p>
 *
 * Instance offer a space savings of retaining only the non-zero indices and
 * values.  For large array with only a few values set, this offers a huge
 * savings.  However, as the cardinality of the array grows in relation to its
 * size, a dense {@code int[]} array will offer better performance in both space
 * and time.  This is especially true if the sparse array instance approaches a
 * cardinality to size ratio of {@code .5}.
 *
 * @see SparseArray
 */
public class SparseIntArray implements SparseArray<Integer> {

    /**
     * The maximum length of this array
     */
    private final int maxLength;
  
    /**
     * The maximum known length of this array
     */
    private int knownLength;

    /**
     * A list of all the non-zero indices
     */
    private int[] indices;

    /**
     * A list of all the values that correspond to the indices in the {@code
     * indices} array.
     */
    private int[] values;
     
    /**
     * Creates a sparse {@code int} array that grows to the maximum size set by
     * {@link Integer#MAX_VALUE}.
     */
    public SparseIntArray() {
        this(Integer.MAX_VALUE);
    }

    /**
     * Creates a sparse {@code int} array with a fixed length
     */
    public SparseIntArray(int length) {
        if (length < 0)
            throw new IllegalArgumentException("length must be non-negative");
        maxLength = length;
        knownLength = (maxLength == Integer.MAX_VALUE) ? 0 : length;
        
        indices = new int[0];
        values = new int[0];
    }

    /**
     * Creates a sparse array copy of the provided array, retaining only the
     * non-zero values.  The length of the provided array is used to set the
     * maximum size of this sparse array.
     */
    public SparseIntArray(int[] array) {
        maxLength = array.length;
        knownLength = array.length;

        // Find how many non-zero elements there are
        int nonZero = 0;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] != 0)
                nonZero++;
        }

        indices = new int[nonZero];
        values = new int[nonZero];
        int index = 0;
        for (int i = 0; i < array.length; ++i) {
            if (array[i] != 0) {
                indices[index] = i;
                values[index++] = array[i];
            }
        }    
    }

    /**
     * {@inheritDoc}
     */
    public int cardinality() {
        return indices.length;
    }

    /**
     * {@inheritDoc}
     */ 
    public Integer get(int index) {
        return getPrimitive(index);
    }

    /**
     * Returns the indices of the array that contain non-{@code 0} values.
     *
     * @return the indices that contain values
     */
    public int[] getElementIndices() {
        return indices;
    }
    
    /**
     * Retrieve the value at specified index or 0 if no value had been
     * specified.
     *
     * @param index the position in the array
     *
     * @return the primitive value at that position
     *
     * @throws ArrayIndexOutOfBoundException if the index is greater than
     *         the maximum length of the array.
     */
    public int getPrimitive(int index) {
        if (maxLength == Integer.MAX_VALUE && knownLength < index)
            knownLength = index;
        else if (index < 0 || index >= maxLength) {
            throw new ArrayIndexOutOfBoundsException("invalid index: " + 
                                 index);
        }

        int pos = Arrays.binarySearch(indices, index);
        int value = (pos >= 0) ? values[pos] : 0;
        return value;
    }

    /**
     * Returns the maximum length of this array.
     */
    public int length() {
        return knownLength;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Integer value) {
        setPrimitive(index, value.intValue());
    }

    /**
     * Sets the value of the index to the value using the Java primitives
     * without auto-boxing.
     */
    public void setPrimitive(int index, int value) {
        int pos = Arrays.binarySearch(indices, index);

        if (value != 0) {
            // need to make room in the indices array
            if (pos < 0) {
                int newPos = 0 - (pos + 1);
                int[] newIndices = new int[indices.length + 1];
                int[] newValues = new int[values.length + 1];
            
                // copy the existing array contents that are valid in their
                // current positions
                for (int i = 0; i < newPos; ++i) {
                    newValues[i] = values[i];
                    newIndices[i] = indices[i];
                }

                // shift the elements down by one to make room
                for (int i = newPos; i < values.length; ++i) {
                    newValues[i+1] = values[i];
                    newIndices[i+1] = indices[i];
                }
                    
                // swap the arrays
                indices = newIndices;
                values = newValues;
                pos = newPos;
                    
                 // update the position of the pos in the values array
                indices[pos] = index;
            }
            values[pos] = value;
        }
        // The value is zero but previously held a spot in the matrix, so
        // remove its position and shift everything over
        else if (value == 0 && pos >= 0) {
            int newLength = indices.length - 1;
            int[] newIndices = new int[newLength];
            int[] newValues = new int[newLength];
            for (int i = 0, j = 0; i < indices.length; ++i) {
                if (i != pos) {
                    newIndices[j] = indices[i];
                    newValues[j] = values[i];            
                    j++;
                }
            }

            // swap the arrays
            indices = newIndices;
            values = newValues;
        }

        // note that in the even of a set with value 0 where the pos was
        // not present, this method is a no-op
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <E> E[] toArray(E[] array) {
        for (int i = 0, j = 0; i < array.length; ++i) {
            int index = -1;
            if (j < indices.length && (index = indices[j]) == i) {
                array[i] = (E)(Integer.valueOf(values[j]));
                j++;
            }
            else
                array[i] = (E)(Integer.valueOf(0));
        }

        return array;
    }

    /**
     * Sets the values of the provided array using the contents of this array.
     * If the provided array is longer than this array, the additional values
     * are left unchanged.
     */
    public int[] toPrimitiveArray(int[] array) {
        for (int i = 0, j = 0; i < array.length; ++i) {
            int index = -1;
            if (j < indices.length && (index = indices[j]) == i) {
                array[i] = values[j];
                j++;
            }
            else
                array[i] = 0;
        }

        return array;
    }
}
