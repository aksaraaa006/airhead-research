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


/**
 * An immutable decorator for an existing {@code Vector} which provides padding
 * before and after the actual values in a decorated {@code Vector}.  This class
 * allows a {@code SemanticSpace} or a {@code Matrix} to return a {@code Vector}
 * stored internally that has a fixed size without allowing other classes the
 * ability to alter the {@code Vector}.  If the decorated {@code Vector} is
 * stored at some offset, any index less than that offset will be given a value
 * of 0, and any value after the full length of the {@code Vector} will be also
 * given a value of 0.
 *
 * </p>
 *
 * Note that the original {@code Vector} can still be alterned by the object
 * owning it, but classes given a {@code ViewDoubleAsDoubleVector} cannot make
 * modififications.
 *
 * @author Keith Stevens
 */
class ViewDoubleAsDoubleVector extends ViewAbstractDoubleVector
                               implements Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * The actual vector this {@code ViewDoubleAsDoubleVector} is decorating.
     */
    private final DoubleVector vector;

    /**
     * The index at which the values {@code vector} are stored.
     */
    private final int vectorOffset;

    /**
     * A fixed length for this {@code Vector}.  This length may be longer or
     * less than that of {@code vector}.
     */
    private final int vectorLength;

    /**
     * Create a new {@code ViewDoubleAsDoubleVector} around an already existing
     * {@code Vector} providing read only access.
     *
     * @param v The {@code Vector} to decorate.
     */
    public ViewDoubleAsDoubleVector(DoubleVector v) {
        this(v, 0, v.length());
    }

    /**
     * Create a new {@code ViewDoubleAsDoubleVector} around an already existing
     * {@code Vector} with a given offset, and a given total length.
     *
     * @param v The {@code Vector} to decorate.
     * @param offset The index at which values of {@code v} are stored in this
     *               {@code ViewDoubleAsDoubleVector}.
     * @param length The maximum length of this {@code ViewDoubleAsDoubleVector}.
     */
    public ViewDoubleAsDoubleVector(DoubleVector v, int offset, int length) {
        super(v);
        vector = v;
        vectorOffset = offset;
        vectorLength = length;
    }

    /**
     * Method not implemented.
     */
    public double add(int index, double delta) {
        throw new UnsupportedOperationException("add is not supported in an " +
                "ViewDoubleAsDoubleVector");
    }

    /**
     * Method not implemented.
     */
    public void set(int index, double value) {
        throw new UnsupportedOperationException("set is not supported in an " +
                "ViewDoubleAsDoubleVector");
    }

    /**
     * Method not implemented.
     */
    public void set(double[] values) {
        throw new UnsupportedOperationException("set is not supported in an " +
                "ViewDoubleAsDoubleVector");
    }

    /**
     * {@inheritDoc}
     */
    public double get(int index) {
        if (index < 0 || index > vectorLength)
            throw new IllegalArgumentException("Invalid index: " + index);

        if (index < vectorOffset || index > vectorOffset + vector.length())
            return 0;

        return vector.get(index-vectorOffset);
    }

    /**
     * {@inheritDoc}
     */
    public double[] toArray(int size) {
        return vector.toArray(size);
    }
}
