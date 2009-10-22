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

import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.SparseArray;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.logging.Logger;


/**
 * A {@code MatrixBuilder} implementation for creating matrix files in the <a
 * href="http://tedlab.mit.edu/~dr/svdlibc/SVD_F_SB.html">SVDLIBC sparse
 * binary</a> matrix format.
 *
 * <p> The {@code addColumn} method is capable of throwing an {@code IOError} if any
 * {@code IOException} occurs while operating on the underlying matrix file.
 *
 * <p> This class is thread-safe.
 *
 * @author David Jurgens
 */
public class SvdlibcSparseBinaryMatrixBuilder implements MatrixBuilder {

    /**
     * Logger for the {@code SvdlibcSparseBinaryMatrixBuilder} class
     */
    private static final Logger LOGGER =
        Logger.getLogger(SvdlibcSparseBinaryMatrixBuilder.class.getName());

    /**
     * The file to which the matrix will be written
     */
    private final File matrixFile;

    /**
     * The writer used to add data to the transposed matrix file
     */
    private final DataOutputStream matrixDos;

    /**
     * Whether the builder has finished adding data to the matrix array
     */
    private boolean isFinished;

    /**
     * The number of the column that will next be assigned.  Once this matrix
     * has been finished, this value will reflect the total number of rows in
     * the matrix.
     */
    private int curCol;

    /**
     * The total number of columns in the matrix.  This value is continuously
     * updated as new columns are seen and is not valid until the matrix has been
     * finished.
     */
    private int numRows;

    /**
     * The total number of non-zero values in the matrix.  This value is
     * continuously updated as new columns are seen and is not valid until the
     * matrix has been finished.
     */
    private int nonZeroValues;

    /**
     * Creates a builder for a matrix in the {@link
     * MatrixIO.Format.SVDLIBC_SPARSE_BINARY SVDLIBC_SPARSE_BINARY} format to be
     * stored in a temporary file.
     */
    public SvdlibcSparseBinaryMatrixBuilder() {
        this(getTempMatrixFile());
    }

    /**
     * Creates a builder for a matrix in the {@link
     * MatrixIO.Format.SVDLIBC_SPARSE_BINARY SVDLIBC_SPARSE_BINARY} format,
     * which will be stored in the specified file.
     */
    public SvdlibcSparseBinaryMatrixBuilder(File backingFile) {
        this.matrixFile = backingFile;
        curCol = 0;
        numRows = 0;
        nonZeroValues = 0;
        isFinished = false;
        try {
            // Interact with the matrix using a RandomAccessFile
            //matrixRaf = new RandomAccessFile(backingFile, "rw");
            matrixDos = new DataOutputStream(
                new BufferedOutputStream(new FileOutputStream(backingFile)));

            // write the 12 byte header to advance the file pointer to where the
            // matrix data will start.  The header will be back filled once the
            // matrix data has been finalized
            for (int i = 0; i < 3; ++i)
                matrixDos.writeInt(0);

        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns a temporary file that will be deleted on JVM exit.
     *'
     * @return a temporary file used to store a matrix
     */
    private static File getTempMatrixFile() {
        File tmp = null;
        try {
            tmp = File.createTempFile("svdlibc-sparse-binary-matrix", ".dat");
            tmp.deleteOnExit();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return tmp;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(double[] column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        // Update the size of the matrix based on the size of the array
        if (column.length > numRows)
            numRows = column.length;

        // Identify how many non-zero values are present in the column
        int nonZero = 0;
        for (int i = 0; i < column.length; ++i) {
            if (column[i] != 0d) 
                nonZero++;
        }

        // Update the total number of non-zero values for the entire matrix
        nonZeroValues += nonZero;

        // Write the column to file
        try {
            matrixDos.writeInt(nonZero);
            for (int i = 0; i < column.length; ++i) {
                if (column[i] != 0d) {
                    matrixDos.writeInt(i); // write the row index
                    matrixDos.writeFloat((float)column[i]);
                }
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        return ++curCol;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(SparseArray<? extends Number> column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        if (column.length() > numRows)
            numRows = column.length();

        // SparseArray instances can take on the maximum possible array size
        // when the array length isn't specified.  This ruins the matrix size
        // specification since the matrix shouldn't actually be that big.
        // However, because this is an implementation artifact, we can't check
        // for it explicitly with an exception.  Therefore, put in an assert to
        // indicate what is likely going on if asserts are enabled for debugging
        // they symptoms.
        assert column.length() != Integer.MAX_VALUE : "adding a column whose " +
            "length is Integer.MAX_VALUE (was likley left unspecified in the " +
            " constructor).";           

        int[] nonZero = column.getElementIndices();
        nonZeroValues += nonZero.length;
        try {
            matrixDos.writeInt(nonZero.length);
            for (int i : nonZero) {
                matrixDos.writeInt(i); // write the row index
                matrixDos.writeFloat(column.get(i).floatValue());
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return ++curCol;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(Vector column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        if (column.length() > numRows)
            numRows = column.length();

        // Vector instances can take on the maximum possible array size when the
        // vector length isn't specified.  This ruins the matrix size
        // specification since the matrix shouldn't actually be that big.
        // However, because this is an implementation artifact, we can't check
        // for it explicitly with an exception.  Therefore, put in an assert to
        // indicate what is likely going on if asserts are enabled for debugging
        // they symptoms.
        assert column.length() != Integer.MAX_VALUE : "adding a column whose " +
            "length is Integer.MAX_VALUE (was likley left unspecified in the " +
            " constructor).";

        // Special case for sparse Vectors, for which we already know the
        // non-zero indices for the column
        if (column instanceof SparseVector) {
            SparseVector s = (SparseVector)column;
            int[] nonZero = s.getNonZeroIndices();
            nonZeroValues += nonZero.length;
            try {
                matrixDos.writeInt(nonZero.length);
                for (int i : nonZero) {
                    double val = s.get(i);
                    matrixDos.writeInt(i); // write the row index
                    matrixDos.writeFloat((float)val);
                } 
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
        // For dense Vectors, find which values are non-zero and write only
        // those
        else {
            int nonZero = 0;
            for (int i = 0; i < column.length(); ++i) {
                double d = column.get(i);
                if (d != 0d) 
                    nonZero++;                    
            }

            // Update the matrix count
            nonZeroValues += nonZero;
            try {
                matrixDos.writeInt(nonZero);
                // Write the number of non-zero values in the column
                for (int i = 0; i < column.length(); ++i) {
                    double value = column.get(i);
                    if (value != 0d) {
                        matrixDos.writeInt(i); // write the row index
                        matrixDos.writeFloat((float)value);
                    }
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
        return ++curCol;
    }

    /**
     * {@inheritDoc} Once this method has been called, any subsequent calls will
     * have no effect and will not throw an exception.
     */
    public synchronized void finish() {
        if (!isFinished) {
            isFinished = true;
            try {
                matrixDos.close();
                // Re-open as a random access file so we can overwrite the 3 int
                // header that specifies the number of dimensions and values
                RandomAccessFile matrixRaf =
                    new RandomAccessFile(matrixFile, "rw");

                // Back fill the dimensions of the matrix and the number of
                // non-zero values as the 3 int header in the file
                matrixRaf.writeInt(numRows);
                matrixRaf.writeInt(curCol);
                matrixRaf.writeInt(nonZeroValues);
                matrixRaf.close();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public synchronized File getFile() {
        if (!isFinished)
            throw new IllegalStateException(
                "Cannot access matrix file until finished has been called");
        return matrixFile;
    }

    /**
     * Returns {@link MatrixIO.Format.SVDLIBC_SPARSE_BINARY
     * SVDLIBC_SPARSE_BINARY}.
     */
    public Format getMatrixFormat() {
        return MatrixIO.Format.SVDLIBC_SPARSE_BINARY;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }
}