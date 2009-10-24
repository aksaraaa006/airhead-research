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

/**
 * Interface for a {@code Vector} implementation indicating that it is
 * sparse, and providing functionality to retrieve the non zero indices.
 *
 * @author Keith Stevens
 */
public interface SparseVector extends Vector {

    /**
     * Return an array of all the non zero indices in this Sparse
     * implementation.
     */
    int[] getNonZeroIndices();

    /**
     * Set the known length of the {@code SparseVector}.  For {@code
     * SparseVector} implementations which do not permit growing this should be
     * a no-op, but for any which permit growing, this should set a bound on the
     * maximum size.
     *
     * @param length The new known length of the {@code SparseVector}.
     */
    void setKnownLength(int length);
}
