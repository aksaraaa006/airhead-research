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

package edu.ucla.sspace.common;

/**
 * An interface specification for interacting with matrix objects.
 *
 * @see MatrixIO
 * @see Matrix.Type
 */
public interface Matrix {

    /**
     * The type of matrix instance.  This enum should be used as a hint for
     * algorithms that create {@code Matrix} instances as to what implementation
     * to use.
     */
    public enum Type {
	/**
	 * A matrix where the majority of the values are 0, and is
	 * small enough to fit into memory.
	 */
	SPARSE_IN_MEMORY,

	/**
	 * A matrix that contains very few 0 values and is small
	 * enough to fit into memory.
	 */
	DENSE_IN_MEMORY,

	/**
	 * A matrix with very few non-zero values and is sufficiently large
	 * enough that it would not fit in memory.
	 */
	SPARSE_ON_DISK,

	/**
	 * A matrix with very few zero values and is sufficiently large enough
	 * that it would not fit in memory.
	 */
	DENSE_ON_DISK	    
    }

    /**
     * Returns the value of the matrix at the provided row and column.
     */
    double get(int row, int col);

    /**
     * Returns the entire row.
     */
    double[] getRow(int row);

    /**
     * Returns the number of columns in this matrix.
     */
    int columns();
    
    /**
     * Converts the matrix to a two dimensional array.  Note that for large
     * matrices, this may exhaust all available memory.
     */
    double[][] toDenseArray();

    /**
     * Returns the number of rows in this matrix.
     */
    int rows();    

    /**
     * Sets the location at the row and column to the provided value.
     */
    void set(int row, int col, double val);

}