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

import edu.ucla.sspace.common.Matrix.Type;

import edu.ucla.sspace.common.matrix.ArrayMatrix;
import edu.ucla.sspace.common.matrix.DiagonalMatrix;
import edu.ucla.sspace.common.matrix.OnDiskMatrix;
import edu.ucla.sspace.common.matrix.SynchronizedMatrix;
import edu.ucla.sspace.common.matrix.SparseMatrix;

import java.util.logging.Logger;

/**
 * A class of static methods for manipulating {@code Matrix} instances.
 *
 *
 * @see Matrix
 * @see MatrixIO
 */
public class Matrices {

    private static final Logger LOGGER = 
	Logger.getLogger(Matrices.class.getName());

    /**
     * The number of bytes in a {@code double}
     */
    private static final int BYTES_PER_DOUBLE = 8;

    /**
     * An estimate of the percentage of non-zero elements in a sparse matrix.
     */
    private static final double SPARSE_DENSITY = .00001;

    /**
     * Uninstantiable
     */
    private Matrices() { }

    /**
     *
     * @param rows the number of rows in the matrix
     * @param cols the number of columns in the matrix
     * @param isDense whether the returned matrix will contain mostly non-zero
     *        elements
     */
    public static Matrix create(int rows, int cols, boolean isDense) {

	// Estimate the number of bytes that the matrix will take up based on
	// its maximum dimensions and its sparsity.
	long size = (isDense)
	    ? (long)rows * (long)cols * BYTES_PER_DOUBLE
	    : (long)(rows * (long)cols * (BYTES_PER_DOUBLE * SPARSE_DENSITY));

	Runtime r = Runtime.getRuntime();
	// REMINDER: possibly GC here?
	long available = r.freeMemory();

	// See if it will fit into memory given how much is currently left.
	if (size < available) {
	    if (isDense) {
		if (size > Integer.MAX_VALUE) {
		    LOGGER.info("too big for ArrayMatrix; creating new " + 
				"OnDiskMatrix");
		    return new OnDiskMatrix(rows, cols);
		}
		else {
		    LOGGER.info("creating new (in memory) ArrayMatrix");
		    return new ArrayMatrix(rows, cols);
		}
	    } else {
		LOGGER.info("can fit sparse in memory; creating " + 
			    "new SparseMatrix");
		return new SparseMatrix(rows, cols);
	    }
	}
	// won't fit into memory
 	else { 
 	    LOGGER.info("cannot fit in memory; creating new OnDiskMatrix");
 	    return new OnDiskMatrix(rows, cols);
 	}
    }

    /**
     * Creates a new {@code Matrix} based on the provided type, with the
     * provided dimensions
     *
     * @param matrixType the type of matrix to create
     * @param rows the number of rows in the matrix
     * @param cols the number of columns in the matrix
     */
    public static Matrix create(int rows, int cols, Type matrixType) {
	switch (matrixType) {
	case SPARSE_IN_MEMORY:
	    return new SparseMatrix(rows, cols);
	case DENSE_IN_MEMORY:
	    return new ArrayMatrix(rows, cols);
    case DIAGONAL_IN_MEMORY:
        return new DiagonalMatrix(rows);
	case SPARSE_ON_DISK:
	    //return new SparseOnDiskMatrix(rows, cols);
	    // REMDINER: implement me
	    return new OnDiskMatrix(rows, cols);
	case DENSE_ON_DISK:
	    return new OnDiskMatrix(rows, cols);
	}
	throw new IllegalArgumentException(
	    "Unknown matrix type: " + matrixType);
    }

    /**
     * Returns a synchronized (thread-safe) matrix backed by the provided
     * matrix.
     *
     * @param m the matrix to be made thread-safe
     *
     * @return a synchronized (thread-safe) view of the provided
     *         matrix.
     */
    public static Matrix synchronizedMatrix(Matrix m) {
	return new SynchronizedMatrix(m);
    }

    public static Matrix multiply(Matrix m1, Matrix m2) {
      if (m1.columns() != m2.rows()) 
        return null;
      int size = m1.columns();
      Matrix resultMatrix = create(m1.rows(), m2.columns(), true);
      if (m2 instanceof DiagonalMatrix) {
        for (int r = 0; r < m1.rows(); ++r) {
          double[] row = m1.getRow(r);
          for (int c = 0; c < m2.columns(); ++c) {
            double value = m2.get(c, c);
            resultMatrix.set(r, c, value * row[c]);
          }
        }
        return resultMatrix;
      }
      for (int r = 0; r < m1.rows(); ++r) {
        double[] row = m1.getRow(r);
        for (int c = 0; c < m2.columns(); ++c) {
          double resultValue = 0;
          for (int i = 0; i < row.length; ++i) {
            resultValue += row[i] * m2.get(i, c);
          }
          resultMatrix.set(r, c, resultValue);
        }
      }
      return resultMatrix;
    }
    /**
     *
     */
    public static Matrix transpose(Matrix matrix) {

	// REMINDER: this should be augmented to determine whether the tranpose
	// can be computed in memory (e.g. using File.size() and
	// Runtime.freeMemory()), or whether the operation needs to be completed
	// on disk.
	
	int rows = matrix.rows();
	int cols = matrix.columns();

	// MAJOR HACK: need to use reflection or some other hint
	Matrix transpose = null;
	if (matrix instanceof SparseMatrix) 
	    transpose = new SparseMatrix(cols, rows);
	else if (matrix instanceof ArrayMatrix) 
	    transpose = new ArrayMatrix(cols, rows);
	else {
	    transpose = new OnDiskMatrix(cols, rows);
	}

	for (int row = 0; row < rows; ++row) {
	    for (int col = 0; col < cols; ++col) {
		transpose.set(col, row, matrix.get(row, col));
	    }
	}

	return transpose;
    }


}
