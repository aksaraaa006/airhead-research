package edu.ucla.sspace.common.matrix;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.ucla.sspace.common.Matrix;

/**
 * A {@code Matrix} backed by an array.
 *
 *
 * @author David Jurgens
 */
public class ArrayMatrix implements Matrix {
    
    private final int rows;

    private final int cols;
    
    private final double[] matrix;

    /**
     * Create a matrix of the provided size using a temporary file.
     */
    public ArrayMatrix(int rows, int cols) {

	this.rows = rows;
	this.cols = cols;
	matrix = new double[rows*cols];
    }

    public ArrayMatrix(int rows, int cols, double[] matrix1D) {

	this.rows = rows;
	this.cols = cols;
	if (rows < 1 || cols < 1) {
	    throw new IllegalArgumentException("invalid matrix dimensions");
	}
	if (matrix1D == null) {
	    throw new NullPointerException("provided matrix cannot be null");
	}
	if (matrix1D.length != (rows * cols)) {
	    throw new IllegalArgumentException("provided matrix is wrong size");
	}
	matrix = matrix1D;
    }
    
    private void checkIndices(int row, int col) {
	if (row < 0 || col < 0 || row >= rows || col >= cols) {
	    throw new ArrayIndexOutOfBoundsException();
	}
    }

    public double get(int row, int col) {
	int index = getIndex(row, col);
	return matrix[index];
    }

    /**
     * Returns a copy of the specified row.
     */
    public double[] getRow(int row) {
	double[] rowArr = new double[cols];
	int index = getIndex(row, 0);
	for (int i = 0; i < cols; ++i) {
	    rowArr[i] = matrix[index++];
	}
	return rowArr;
    }

    /**
     *
     */
    public int columns() {
	return cols;
    }

    private int getIndex(int row, int col) {
	return (row * cols) + col;
    }

    public void set(int row, int col, double val) {
	int index = getIndex(row, col);
	matrix[index] = val;
    }
    
    /**
     *
     */
    public double[][] toDenseArray() {
	double[][] m = new double[rows][cols];
	int i = 0;
	for (int row = 0; row < rows; ++row) {
	    for (int col = 0; col < cols; ++col) {
		m[row][col] = matrix[i++];
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

}