package edu.ucla.sspace.common.matrix;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import edu.ucla.sspace.common.Matrix;

/**
 * A Matrix implementation that uses a binary file to read and write Returns a
 * copy of the specified rowvalues of the matrix.  The matrix is stored in
 * row-column order on disk, so in-order column accesses to elments in a row
 * will perform much better than sequential row accesses to the same column.
 *
 * <p>
 *
 * If a {@link IOException} is ever raised as a part of executing an the methods
 * of an instance, the exception is rethrown as a {@link IOError}.
 *
 * @author David Jurgens
 */
public class OnDiskMatrix implements Matrix {
    
    /**
     * The size of two ints that denote the number of rows and columns
     */
    private static final int HEADER_LENGTH = 8;

    private static final int BYTES_PER_FLOAT = 4;

    /**
     * The on-disk storage space for the matrix
     */
    private final RandomAccessFile matrix; 

    private final int rows;

    private final int cols;

    /**
     * Create a matrix of the provided size using a temporary file.
     */
    public OnDiskMatrix(int rows, int cols) throws IOException {
	this(rows, cols, File.createTempFile("OnDiskMatrix","matrix"));
    }
    
    /**
     * Create a matrix of the provided size using a file created with the
     * specified name.
     */
    public OnDiskMatrix(int rows, int cols, String filename) 
	    throws IOException {
	this(rows, cols, new RandomAccessFile(filename, "rw"));
    }

    /**
     * Create a matrix of the provided size using the provided file and the data
     * contained by that file.
     */
    public OnDiskMatrix(int rows, int cols, File f) throws IOException {
	this(rows, cols, new RandomAccessFile(f, "rw"));
    }

    OnDiskMatrix(int rows, int cols, RandomAccessFile raf) throws IOException {
	this.matrix = raf;
	this.rows = rows;
	this.cols = cols;

	// initialize the matrix in memory;
	matrix.setLength(HEADER_LENGTH + (rows * cols * BYTES_PER_FLOAT));
	matrix.seek(0);
	matrix.writeInt(rows);
	matrix.writeInt(cols);
    }

    private void checkIndices(int row, int col) {
	if (row < 0 || col < 0 || row >= rows || col >= cols) {
	    throw new ArrayIndexOutOfBoundsException();
	}
    }

    public double get(int row, int col) {
	try {
	    seek(row, col);
	    return matrix.readFloat();
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     * Returns a copy of the specified row.
     */
    public double[] getRow(int row) {
	try {
	    double[] rowArr = new double[cols];
	    seek(row, 0);
	    for (int i = 0; i < cols; ++i) {
		rowArr[i] = matrix.readFloat();
	    }
	    return rowArr;
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     *
     */
    public int columns() {
	return cols;
    }

    private void seek(long row, long col) {
	try {
	    matrix.seek((row * cols * BYTES_PER_FLOAT) + (col * BYTES_PER_FLOAT));
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    public void set(int row, int col, double val) {
	try {
	    checkIndices(row, col);
	    seek(row, col);
	    matrix.writeFloat((float)val);
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }
    
    /**
     *
     */
    public double[][] toDenseArray() {
	try {
	    matrix.seek(0);
	    double[][] m = new double[rows][cols];
	    for (int row = 0; row < rows; ++row) {
		for (int col = 0; col < cols; ++col) {
		    m[row][col] = matrix.readFloat();
		}
	    }
	    return m;
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     *
     */
    public int rows() {
	return rows;
    }

}