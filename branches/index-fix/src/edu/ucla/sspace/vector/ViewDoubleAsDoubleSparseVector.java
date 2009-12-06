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
 * @author Keith Stevens
 */
class ViewDoubleAsDoubleSparseVector extends ViewDoubleAsDoubleVector 
                                     implements SparseVector, Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * The actual vector this {@code ViewDoubleAsDoubleSparseVector} is
     * decorating.
     */
    private final SparseVector vector;

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
     * Create a new {@code ViewDoubleAsDoubleSparseVector} around an already
     * existing {@code Vector} providing read only access.
     *
     * @param v The {@code Vector} to decorate.
     */
    public ViewDoubleAsDoubleSparseVector(DoubleVector v) {
        this(v, 0, v.length());
    }

    /**
     * Create a new {@code ViewDoubleAsDoubleSparseVector} around an already
     * existing {@code Vector} with a given offset, and a given total length.
     *
     * @param v The {@code Vector} to decorate.
     * @param offset The index at which values of {@code v} are stored in this
     *               {@code ViewDoubleAsDoubleSparseVector}.
     * @param length The maximum length of this {@code
     *               ViewDoubleAsDoubleSparseVector}.
     */
    public ViewDoubleAsDoubleSparseVector(DoubleVector v,
                                          int offset,
                                          int length) {
        super(v, offset, length);
        vector = (SparseVector) v;
        vectorOffset = offset;
        vectorLength = length;
    }

    public int[] getNonZeroIndices() {
        if (vectorOffset == 0)
            return vector.getNonZeroIndices();
        return null;
    }
}
