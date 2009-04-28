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
     *
     */
    public enum Type {
	/**
	 *
	 */
	SPARSE_IN_MEMORY,

	/**
	 *
	 */
	DENSE_IN_MEMORY,

	/**
	 *
	 */
	SPARSE_ON_DISK,

	/**
	 *
	 */
	DENSE_ON_DISK	    
    }

    /**
     *
     */
    double get(int row, int col);

    /**
     *
     */
    double[] getRow(int row);

    /**
     *
     */
    int columns();
    
    /**
     *
     */
    double[][] toDenseArray();

    /**
     *
     */
    int rows();    

    /**
     *
     */
    void set(int row, int col, double val);

}