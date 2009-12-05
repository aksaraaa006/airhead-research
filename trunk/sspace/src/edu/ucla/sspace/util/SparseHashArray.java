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

import java.io.Serializable;

import java.util.HashMap;
import java.util.Map;


/**
 * A sparse array backed by a {@link HashMap}.  This class offers amortized
 * constant time access to array indices at the expense of space.<p>
 *
 * Instance offer a space savings of retaining only the non-zero indices and
 * values.  For large array with only a few values set, this offers a huge
 * savings.  However, as the cardinality of the array grows in relation to its
 * size, a dense array will offer better performance in both space and time.
 * This is especially true if the sparse array instance approaches a cardinality
 * to size ratio of {@code .5}.<p>
 *
 * This class offers much better performance than {@link IntegerMap}, but will
 * use significantly more space as the cardinality increases.  In addition, this
 * class will marshall primitive types into their reified object forms.
 *
 * @see SparseArray
 * @see IntegerMap
 */
public class SparseHashArray<T> implements SparseArray<T>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The maximum length of this array
     */
    private final int maxLength;
  
    /**
     * A mapping from an array index to its value
     */
    private Map<Integer,T> indexToValue;

    /**
     * Creates a sparse array that grows to the maximum size set by {@link
     * Integer#MAX_VALUE}.
     */
    public SparseHashArray() {
	this(Integer.MAX_VALUE);
    }

    /**
     * Creates a sparse array with a fixed length
     */
    public SparseHashArray(int length) {
	if (length < 0)
	    throw new IllegalArgumentException("length must be non-negative");
	maxLength = length;
	
        indexToValue = new HashMap<Integer,T>();
    }

    /**
     * Creates a sparse array copy of the provided array, retaining only the
     * non-zero values.  The length of the provided array is used to set the
     * maximum size of this sparse array.
     */
    public SparseHashArray(T[] array) {

	maxLength = array.length;

	// Find how many non-zero elements there are
	int nonZero = 0;
	for (int i = 0; i < array.length; ++i) {
	    if (array[i] != null)
		indexToValue.put(i, array[i]);
	}
    }

    /**
     * {@inheritDoc}
     */
    public int cardinality() {
	return indexToValue.size();
    }

    /**
     * {@inheritDoc}
     */ 
    public T get(int index) {
	return indexToValue.get(index);
    }

    /**
     * Returns the indices of the array that contain non-{@code 0} values.
     *
     * @return the indices that contain values
     */
    public int[] getElementIndices() {
        Integer[] indices = indexToValue.keySet().toArray(new Integer[0]);
        int[] primitive = new int[indices.length];
        for (int i = 0; i < indices.length; ++i)
            primitive[i] = indices[i].intValue();
        return primitive;
    }
    
    /**
     * Returns the maximum length of this array.
     */
    public int length() {
	return maxLength;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, T value) {
	indexToValue.put(index, value);
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    public <E> E[] toArray(E[] array) {
	for (int i = 0; i < array.length; ++i) {
            T j = indexToValue.get(i);
            if (j != null)
                array[i] = (E)j;
	}
	return array;
    }
}
