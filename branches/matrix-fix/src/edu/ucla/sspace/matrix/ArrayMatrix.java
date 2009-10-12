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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;


/**
 * A {@code Matrix} backed by an array.
 *
 * @author David Jurgens
 */
public class ArrayMatrix implements Matrix {
    
    /**
     * The number of rows stored in this {@code ArrayMatrix}.
     */
    private final int rows;

    /**
     * The number of columns stored in this {@code ArrayMatrix}.
     */
    private final int cols;
    
    /**
     * A single dimensionaly array storing the values of this {@code
     * ArrayMatrix}.
     */
    private final double[] matrix;

    /**
     * Create a {@code ArrayMatrix} of size {@code rows} by {@code cols}.
     *
     * @param rows The number of rows this {@code ArrayMatrix} will represent.
     * @param cols The number of columns this {@code ArrayMatrix} will
     *             represent.
     */
    public ArrayMatrix(int rows, int cols) {
        this.rows = rows;
        this.cols = cols;
        matrix = new double[rows*cols];
    }

    /**
     * Create a {@code ArrayMatrix} from a two dimensional array.
     *
     * @param matrix2D The two dimensional array this {@code ArrayMatrix} will
    *                represent.
     * @throws IllegalArgumentExceptiona if matrix2d is invalid.
     */
    public ArrayMatrix(double[][] matrix2d) {

      if (matrix2d == null)
          throw new IllegalArgumentException("invalid matrix dimensions");

      rows = matrix2d.length;
      if (rows < 1)
          throw new IllegalArgumentException("invalid matrix dimensions");

      cols = matrix2d[0].length;
      if (cols < 1)
          throw new IllegalArgumentException("invalid matrix dimensions");

      matrix = new double[rows*cols];
      for (int i = 0; i < rows; ++i) {
        if (cols != matrix2d[i].length)
            throw new IllegalArgumentException("invalid matrix dimensions");
        for (int j = 0; j < cols; ++j)
            set(i, j, matrix2d[i][j]);
      }
    }

    /**
     * Create a new {@code ArrayMatrix} which is of dimensions {@code rows} by
     * {@code cols}, and takes ownership of {@code matrix1d} as the values of
     * this {@code ArrayMatrix}.  Note that {@code matrix1D} should be of length
     * {@code rows} * {@code cols}.
     *
     * @param rows The number of rows this {@code ArrayMatrix} will represent.
     * @param cols The number of columns this {@code ArrayMatrix} will
     *             represent.
     * @param matrix1D A 1 dimensional representation of the vaues this {@code
     *                 ArrayMatrix} will have.
     * @throws IllegalArgumentException if either {@code rows} or {@code cols}
     *                                  is negative, or matrix1d is invalid.
     */
    public ArrayMatrix(int rows, int cols, double[] matrix1D) {
        this.rows = rows;
        this.cols = cols;
        if (rows < 1 || cols < 1)
            throw new IllegalArgumentException("invalid matrix dimensions");
        if (matrix1D == null)
            throw new NullPointerException("provided matrix cannot be null");
        if (matrix1D.length != (rows * cols))
            throw new IllegalArgumentException("provided matrix is wrong size");

        matrix = matrix1D;
    }
    
    /**
     * Check that the indices of a requested cell are within bounds.
     *
     * @param row The row of the cell to check.
     * @param col The column of the cell to check.
     *
     * @throws ArrayIndexOutOfBoundsException if 
     */
    private void checkIndices(int row, int col) {
        if (row < 0 || row >= rows)
            throw new ArrayIndexOutOfBoundsException("row: " + row);
        else if (col < 0 || col >= cols)
            throw new ArrayIndexOutOfBoundsException("column: " + col);
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        int index = getIndex(row, col);
        return matrix[index];
    }

    /**
     * {@inheritDoc}
     */
    public Vector getRowVector(int row) {
        if (row >= rows)
            throw new ArrayIndexOutOfBoundsException("row: " + rows);

        Vector rowArr = new DenseVector(cols);
        int index = getIndex(row, 0);
        for (int i = 0; i < cols; ++i)
            rowArr.set(i, matrix[index++]);
        return rowArr;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        if (row >= rows)
            throw new ArrayIndexOutOfBoundsException("row: " + rows);

        double[] rowArr = new double[cols];
        int index = getIndex(row, 0);
        for (int i = 0; i < cols; ++i)
            rowArr[i] = matrix[index++];
        return rowArr;
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols;
    }
 
    /**
     * Returns the one-dimension index in the matrix for the row and column.
     */
    private int getIndex(int row, int col) {
        return (row * cols) + col;
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        int index = getIndex(row, col);
        matrix[index] = val;
    }

    /**
     * {@inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        if (columns.length != cols)
            throw new IllegalArgumentException(
                    "invalid number of columns: " + columns.length);

        for (int col = 0; col < cols; ++col)
            matrix[getIndex(row,col)] = columns[col];
    }
    
    /**
     * {@inheritDoc}
     */
    public void setRow(int row, Vector values) {
        if (values.length() != cols)
            throw new IllegalArgumentException(
                    "invalid number of columns: " + values.length());

        for (int col = 0; col < cols; ++col)
            matrix[getIndex(row,col)] = values.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        double[][] m = new double[rows][cols];
        int i = 0;
        for (int row = 0; row < rows; ++row) {
            for (int col = 0; col < cols; ++col)
                m[row][col] = matrix[i++];
        }
        return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows;
    }
}
