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

import edu.ucla.sspace.util.DoubleEntry;
import edu.ucla.sspace.util.ObjectEntry;

import java.io.Serializable;

import java.util.Iterator;

import gnu.trove.map.TIntDoubleMap;
import gnu.trove.map.hash.TIntDoubleHashMap;
import gnu.trove.iterator.TIntDoubleIterator;


/**
 * A {@code SparseVector} implementation where index pairs are hashed to store
 * the corresponding value.  This provides amoritized constant time access to
 * all get and set operations, while using more space than the {@link
 * CompactSparseVector} or {@link AmortizedSparseVector} classes.
 *
 * <p> See {@see SparseHashArray} for implementation details.
 *
 * @author David Jurgens
 */
public class SparseHashDoubleVector // extends SparseHashVector<Double>
        implements SparseDoubleVector, Iterable<DoubleEntry>, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The backing storage for this vector
     */
    private final TIntDoubleMap vector;

    /**
     * The length of this vector
     */
    private final int length;

    /**
     * The magnitude of the vector or -1 if the value is currently invalid needs
     * to be recomputed
     */
    private double magnitude;

    /**
     * Creates a new vector of the specified length
     *
     * @param length the length of this vector
     */
    public SparseHashDoubleVector(int length) {
        this.length = length;
        vector = new TIntDoubleHashMap();
        magnitude = -1;
    }

    /**
     * Creates a new vector using the non-zero values of the specified array.
     * The created vector contains no references to the provided array, so
     * changes to either will not be reflected in the other.
     *
     * @param values the intial values for this vector to have
     */
    public SparseHashDoubleVector(double[] values) {
        this(values.length);
        for (int i = 0; i < values.length; ++i)
            if (values[i] != 0)
                vector.put(i, values[i]);
    }

    public SparseHashDoubleVector(DoubleVector values) {
        this(values.length());
        if (values instanceof SparseVector) {
            int[] nonZeros = ((SparseVector) values).getNonZeroIndices();
            for (int index : nonZeros)
                vector.put(index, values.get(index));
        } else {
            for (int index = 0; index < values.length(); ++index) {
                double value = values.get(index);
                if (value != 0d)
                    vector.put(index, value);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public double add(int index, double delta) {
        double val = vector.get(index);
        double sum = val + delta;
        if (sum == 0)
            vector.remove(index);
        else
            vector.put(index, sum);
        magnitude = -1;
        return sum;
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        return vector.get(index);
    }

    /** 
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return vector.keys();
    }

    /**
     * {@inheritDoc}
     */
    public Double getValue(int index) {
        return get(index);
    }

    /**
     * Returns an iterator over the non-{@code 0} values in this vector.  This
     * method makes no guarantee about the order in which the indices are
     * returned.
     */
    public Iterator<DoubleEntry> iterator() {
        return new DoubleIterator();
    }

    /**
     * {@inheritDoc}
     */
    public int length() {
        return length;
    }

    /**
     * {@inheritDoc}
     */
    public double magnitude() {
        // Check whether the current magnitude is valid and if not, recompute it
        if (magnitude < 0) {
            magnitude = 0;
            for (int nz : getNonZeroIndices()) {
                double d = vector.get(nz);
                magnitude += d * d;
            }
            magnitude = Math.sqrt(magnitude);
        }
        return magnitude;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, double value) {
        vector.put(index, value);
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int index, Number value) {
        vector.put(index, value.doubleValue());
        magnitude = -1;
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray() {
        double[] array = new double[length];
        TIntDoubleIterator iter = vector.iterator();
        for (int i = vector.size(); i-- > 0; ) {
            iter.advance();
            array[iter.key()] = iter.value();
        }
        return array;
    }

    /**
     * An iterator over the {@code double} values in the vector, wrapping the
     * backing {@code SparseHashArray}'s own iterator.
     */
    class DoubleIterator implements Iterator<DoubleEntry> {

        private final TIntDoubleIterator it;

        public DoubleIterator() {
            it = vector.iterator();
        }

        public boolean hasNext() {
            return it.hasNext();
        }

        public DoubleEntry next() {
            it.advance();
            return new DoubleEntry() {
                public int index() { return it.key(); }
                public double value() { return it.value(); }
            };
        }

        public void remove() {
            throw new UnsupportedOperationException(
                "Cannot remove from vector");
        }
    }
}
