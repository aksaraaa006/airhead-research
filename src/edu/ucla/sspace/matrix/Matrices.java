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

import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.SVD.Algorithm;

import java.util.logging.Logger;


/**
 * A class of static methods for manipulating and creating {@code Matrix}
 * instances.
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
		    LOGGER.finer("too big for ArrayMatrix; creating new " + 
				 "OnDiskMatrix");
		    return new OnDiskMatrix(rows, cols);
		}
		else {
		    LOGGER.finer("creating new (in memory) ArrayMatrix");
		    return new ArrayMatrix(rows, cols);
		}
	    } else {
		LOGGER.finer("can fit sparse in memory; creating " + 
			     "new SparseMatrix");
		return new SparseMatrix(rows, cols);
	    }
	}
	// won't fit into memory
 	else { 
 	    LOGGER.finer("cannot fit in memory; creating new OnDiskMatrix");
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
     * Returns a {@link MatrixBuilder} in the default format of the fastest
     * available {@link SVD.Algorithm SVD algorithm}.
     *
     * @return a matrix builder to be used in creating a matrix for use with the
     *         {@link SVD} class
     */
    public static MatrixBuilder getMatrixBuilderForSVD() {
        Algorithm fastest = SVD.getFastestAvailableAlgorithm();
        // In the unlikely case that this is called when no SVD support is
        // available, return a default instance rather than error out.  This
        // enables programs that call this method without invoking the SVD (or
        // those that do so optionally) to continue working without error.
        if (fastest == null) {
            LOGGER.warning("no SVD support detected.  Returning default " +
                           "matrix builder instead");
            return new MatlabSparseMatrixBuilder();
        }
        
        switch (fastest) {
        case SVDLIBC:
            return new SvdlibcSparseBinaryMatrixBuilder();

        // In all other cases, use the sparse Matlab format, as it covers both
        // Matlab and Octave.  This format doesn't matter much for Jama or Colt,
        // as both formats need to have the matrix loaded back into memory in
        // order to perform the SVD.
        default:
            return new MatlabSparseMatrixBuilder();
        }
    }

    /**
     * Returns {@code true} if the format is likely to produce a dense matrix.
     * Due to the actual matrix contents being unknown, the return value is
     * actual a best-effort guess.
     *
     * @param format the format of a matrix on disk
     * @return {@code true} if the matrix is likely to be a dense matrix
     */
    static boolean isDense(Format format) {
	switch (format) {
	case DENSE_TEXT:
	case SVDLIBC_DENSE_TEXT:
	case SVDLIBC_DENSE_BINARY:
	    return true;
	case MATLAB_SPARSE:
	case SVDLIBC_SPARSE_TEXT:
	case SVDLIBC_SPARSE_BINARY:
	    return false;
	default:
	    // We should never get here unless another format is added and this
	    // method is never updated
	    assert false : format;
	}
        return true;
    }

    private static Matrix multiplyRightDiag(Matrix m1, Matrix m2) {
	Matrix resultMatrix = create(m1.rows(), m2.columns(), true);
	for (int r = 0; r < m1.rows(); ++r) {
	    double[] row = m1.getRow(r);
	    for (int c = 0; c < m2.columns(); ++c) {
		double value = m2.get(c, c);
		resultMatrix.set(r, c, value * row[c]);
	    }
	}
	return resultMatrix;
    }

    private static Matrix multiplyBothDiag(Matrix m1, Matrix m2) {
	Matrix resultMatrix = new DiagonalMatrix(m1.rows());
	for (int i = 0; i < m1.rows(); ++i) {
	    resultMatrix.set(i, i, m1.get(i, i) * m2.get(i, i));
	}
	return resultMatrix;
    }

    private static Matrix multiplyLeftDiag(Matrix m1, Matrix m2) {
	Matrix resultMatrix = create(m1.rows(), m2.columns(), true);
	for (int r = 0; r < m1.rows(); ++r) {
	    double element = m1.get(r, r);
	    double[] m2Row = m2.getRow(r);
	    for (int c = 0; c < m2.columns(); ++c) {
		resultMatrix.set(r, c, element * m2Row[c]);
	    }
	}
	return resultMatrix;
    }

    /**
     * 
     */
    public static Matrix multiply(Matrix m1, Matrix m2) {
	if (m1.columns() != m2.rows()) 
	    return null;
	if (m2 instanceof DiagonalMatrix) {
            if (m1 instanceof DiagonalMatrix)
                return multiplyBothDiag(m1, m2);
            else
                return multiplyRightDiag(m1, m2);
	} else if (m1 instanceof DiagonalMatrix) {
            return multiplyLeftDiag(m1, m2);
	}

	int size = m1.columns();
	Matrix resultMatrix = create(m1.rows(), m2.columns(), true);
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
     * Returns a new {@code Matrix} that has been resized from the original,
     * truncating values if smaller, or adding zero elements if larger.
     */
    public static Matrix resize(Matrix matrix, int rows, int columns) {
	// REMDINER: the third argument decides whether the matrix is dense or
	// not.  If new sparse matrices are added, there should be addiional
	// cases.  Ideally, we should put in a package method for determining
	// whether a given matrix instance is sparse and/or on disk. -jurgens
	Matrix resized = create(rows, columns, 
                                !(matrix instanceof SparseMatrix ||
                                  matrix instanceof DiagonalMatrix));
	int r = Math.min(rows, matrix.rows());
	int c = Math.min(columns, matrix.columns());
	for (int row = 0; row < r; ++row) {
	    for (int col = 0; col < c; ++col) {
		resized.set(row, col, matrix.get(row, col));
	    }
	}

	return resized;
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

    /**
     * Returns the transpose of the input matrix, i.e. where every element (i,j)
     * in the output has the value of the element at (j,i) in the input.
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
