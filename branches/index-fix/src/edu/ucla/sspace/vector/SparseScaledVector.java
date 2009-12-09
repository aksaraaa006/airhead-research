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
 * A decorator for a {@link SparseVector} that scales all the values in the
 * backing vector in constant time.
 *
 * @author Keith Stevens
 *
 * @see ScaledVector
 */
public class SparseScaledVector extends ScaledVector
        implements SparseVector<Double>, Serializable  {

    private static final long serialVersionUID = 1L;

    /**
     * The original {@code Vector} that this {@code SparseScaledVector}
     * decorates.
     */
    private final SparseVector sVector;

    /**
     * Creates a new {@code SparseScaledVector} that scales all the values in
     * the provided {@code Vector}.
     *
     * @param v The vector to decorate.
     * @param factor The value to scale all values by.
     */
    public <T extends DoubleVector & SparseVector<Double>> 
                      SparseScaledVector(T v, double factor) {        
        super(v, factor);
        sVector = v;
    }
    
    /**
     * {@inheritDoc}
     */
    public int[] getNonZeroIndices() {
        return sVector.getNonZeroIndices();
    }
}
