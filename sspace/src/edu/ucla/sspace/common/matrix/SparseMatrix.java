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

package edu.ucla.sspace.common.matrix;

import java.util.HashMap;
import java.util.Map;

import edu.ucla.sspace.common.Matrix;

/**
 * A sparse {@code Matrix} backed by a {@link Map}.
 *
 *
 * @author David Jurgens
 */
public class SparseMatrix implements Matrix {
    
    private final int rows;

    private final int cols;
    
    private final Map<Index,Double> sparseMatrix;

    /**
     * Create a matrix of the provided size using a temporary file.
     */
    public SparseMatrix(int rows, int cols) {

	this.rows = rows;
	this.cols = cols;
	sparseMatrix = new HashMap<Index,Double>();
    }

    /**
     *
     */    
    private void checkIndices(int row, int col) {
	if (row < 0 || col < 0 || row >= rows || col >= cols) {
	    throw new ArrayIndexOutOfBoundsException();
	}
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
	Double d = sparseMatrix.get(new Index(row, col));
	return (d == null) ? 0d : d.doubleValue();
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
	double[] rowArr = new double[cols];

	for (int i = 0; i < cols; ++i) {
	    rowArr[i] = get(row, i);
	}
	return rowArr;
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
	return cols;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
	sparseMatrix.put(new Index(row, col), val);
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
	if (columns.length != cols) {
	    throw new IllegalArgumentException(
		"invalid number of columns: " + columns.length);
	}
	for (int col = 0; col < cols; ++col) {
	    sparseMatrix.put(new Index(row, col), columns[col]);
	}
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
	double[][] m = new double[rows][cols];
	int i = 0;
	for (int row = 0; row < rows; ++row) {
	    for (int col = 0; col < cols; ++col) {
		m[row][col] = get(row, col);
	    }
	}
	return m;
    }

    /**
     *
     */
    public int rows() {
	return rows;
    }

    /**
     *
     */
    private static final class Index {

	final int row;
	final int col;

	public Index(int row, int col) {
	    this.row = row;
	    this.col = col;
	}

	public boolean equals(Object o) {
	    if (o instanceof Index) {
		Index i = (Index)o;
		return i.row == row && i.col == col;
	    }
	    return false;
	}

	public int hashCode() {
	    return row << 16 | col;
	}
    }

}