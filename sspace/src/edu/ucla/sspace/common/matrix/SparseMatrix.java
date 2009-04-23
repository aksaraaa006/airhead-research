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
     *
     */
    public double get(int row, int col) {
	Double d = sparseMatrix.get(new Index(row, col));
	return (d == null) ? 0d : d.doubleValue();
    }

    /**
     * Returns a copy of the specified row.
     */
    public double[] getRow(int row) {
	double[] rowArr = new double[cols];

	for (int i = 0; i < cols; ++i) {
	    rowArr[i] = get(row, i);
	}
	return rowArr;
    }

    /**
     *
     */
    public int columns() {
	return cols;
    }

    /**
     *
     */
    public void set(int row, int col, double val) {
	sparseMatrix.put(new Index(row, col), val);
    }
    
    /**
     *
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