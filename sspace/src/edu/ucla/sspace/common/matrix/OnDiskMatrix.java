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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
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

    private static final int BYTES_PER_DOUBLE = 8;

    /**
     * The on-disk storage space for the matrix
     */
    private final RandomAccessFile matrix; 

    private final int rows;

    private final int cols;

    /**
     * Create a matrix of the provided size using a temporary file.
     *
     * @throws IOError if the backing file for this matrix cannot be created
     */
    public OnDiskMatrix(int rows, int cols) {
	this(rows, cols, createTempFile());
    }
    
    /**
     *
     *
     * @throws IOError if the backing file for this matrix cannot be created
     */
    OnDiskMatrix(int rows, int cols, RandomAccessFile raf) {
	if (rows <= 0 || cols <= 0) {
	    throw new IllegalArgumentException("dimensions must be positive");
	}
	try {
	    this.matrix = raf;
	    this.rows = rows;
	    this.cols = cols;
	    
	    // initialize the matrix in memory;
	    long length = 
		(HEADER_LENGTH + ((long)rows * (long)cols * BYTES_PER_DOUBLE));
	    matrix.setLength(length);
	    matrix.seek(0);
	    matrix.writeInt(rows);
	    matrix.writeInt(cols);
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
    }

    private static RandomAccessFile createTempFile() {
	try {
	    File f = File.createTempFile("OnDiskMatrix","matrix");
	    // Make sure the temp file goes away since it can get fairly large
	    // for big matrices
	    f.deleteOnExit();
	    return new RandomAccessFile(f, "rw");
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
    }

    /**
     * Checks that the indices are within the bounds of the matrix and throws an
     * exception if they are not.
     */
    private void checkIndices(int row, int col) {
	if (row < 0 || row >= rows) {
	    throw new ArrayIndexOutOfBoundsException("row: " + row);
	}
	else if (col < 0 || col >= cols) {
	    throw new ArrayIndexOutOfBoundsException("column: " + col);
	}
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
	try {
	    seek(row, col);
	    return matrix.readDouble();
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
	try {
	    double[] rowArr = new double[cols];
	    byte[] rawBytes = new byte[cols * BYTES_PER_DOUBLE];
	    seek(row, 0);
	    // read the entire row in at once, as this will have better I/O
	    // performance than multiple successive reads
	    matrix.readFully(rawBytes, 0, rawBytes.length);
	    
	    // convert the bytes into an input stream
	    DataInputStream dis = 
		new DataInputStream(new ByteArrayInputStream(rawBytes));
	    for (int i = 0; i < cols; ++i) {
		rowArr[i] = dis.readDouble();
	    }
	    return rowArr;
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
	return cols;
    }

    /**
     * Moves the backing file pointer to the location specified by this row and
     * column.  The next {@code readDouble} call will return this location's
     * value.
     */
    private void seek(long row, long col) {
	try {
	    long index = (row * cols * BYTES_PER_DOUBLE)
		+ (col * BYTES_PER_DOUBLE) 
		+ HEADER_LENGTH;
	    if (index != matrix.getFilePointer()) {
		matrix.seek(index);
	    }
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
	try {
	    checkIndices(row, col);
	    seek(row, col);	    
	    matrix.writeDouble(val);
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }
    
    public void setRow(int row, double[] val) {
	try {
	    ByteArrayOutputStream baos = new ByteArrayOutputStream();
	    DataOutputStream dos = new DataOutputStream(baos);
	    for (int i = 0; i < val.length; ++i) {
		dos.writeDouble(val[i]);
	    }
	    setRow(row, baos.toByteArray());
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    public void setRow(int row, byte[] valuesAsBytes) {
	try {
	    seek(row, 0);
	    matrix.write(valuesAsBytes, 0, valuesAsBytes.length);
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }
    
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
	try {
	    matrix.seek(0);
	    double[][] m = new double[rows][cols];
	    for (int row = 0; row < rows; ++row) {
		for (int col = 0; col < cols; ++col) {
		    m[row][col] = matrix.readDouble();
		}
	    }
	    return m;
	} catch (IOException ioe) {
	    throw new IOError(ioe); // rethrow unchecked
	}
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
	return rows;
    }

}
