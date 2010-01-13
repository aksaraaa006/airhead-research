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

import edu.ucla.sspace.util.SparseHashArray;


/**
 * A {@code SparseVector} implementation backed by a {@code HashMap}.  This
 * provides amoritized constant time access to all get and set operations, while
 * using more space than the {@link CompactSparseVector} or {@link
 * AmortizedSparseVector} classes.
 *
 * <p> See {@see SparseHashArray} for implementation details.
 *
 * @author David Jurgens
 */
public class SparseHashVector<T extends Number>
    implements SparseVector<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The backing array that holds the vector values
     */
    protected SparseHashArray<Number> vector;

    /** 
     * Create a {@code SparseHashVector} with the given size, having no
     * non-zero values.
     *
     * @param length The length of this {@code Vector}
     */
    public SparseHashVector(int length) {
        vector = new SparseHashArray<Number>(length);
    }

    /**
     * Create a {@code SparseHashVector} from an array, saving only the non
     * zero entries.
     *
     * @param array the values of this vector
     */
    public SparseHashVector(T[] array) {
        vector = new SparseHashArray<Number>(array);
    }

    /** 
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.getElementIndices();
    }

    /**
     * {@inheritDoc}
     */
    public Number getValue(int index) {
        Number val = vector.get(index);
        return (val == null) ? 0 : val;
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
    public void set(int index, Number value) {
        vector.set(index, (value.intValue() == 0) ? null : value);
    }
}
