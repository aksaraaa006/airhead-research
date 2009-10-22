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

import edu.ucla.sspace.vector.Sparse;
import edu.ucla.sspace.vector.Vector;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A {@code MatrixBuilder} implementation for creating matrix files in the
 * Matlab&cp; sparse matrix format.
 *
 * <p> This class is thread-safe.
 *
 * @author David Jurgens
 */
public class MatlabSparseMatrixBuilder implements MatrixBuilder {

    /**
     * Logger for all the {@code MatlabSparseMatrixBuilder} instances
     */
    private static final Logger LOGGER =
        Logger.getLogger(MatlabSparseMatrixBuilder.class.getName());

    /**
     * The file to which the matrix will be written
     */
    private final File matrixFile;

    /**
     * The writer used to add data to the matrix file
     */
    private final PrintWriter matrixWriter;

    /**
     * Whether the builder has finished adding data to the matrix array
     */
    private boolean isFinished;

    /**
     * The number of the column that will next be assigned
     */
    private int curColumn;

    /**
     * Creates a builder for a matrix in the {@link
     * MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE} format to be stored in a
     * temporary file.
     */
    public MatlabSparseMatrixBuilder() {
        this(getTempMatrixFile());
    }

    /**
     * Creates a builder for a matrix in the {@link
     * MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE} format, which will be stored
     * in the specified file.
     */
    public MatlabSparseMatrixBuilder(File backingFile) {
        this.matrixFile = backingFile;
        curColumn = 0;
        isFinished = false;
        try {
            matrixWriter = new PrintWriter(matrixFile);
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
            tmp = File.createTempFile("matlab-sparse-matrix", ".dat");
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        tmp.deleteOnExit();
        return tmp;
    }
    
    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(double[] column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add rows to a MatrixBuilder that is finished");
        for (int i = 0; i < column.length; ++i) {
            if (column[i] != 0d) {
                // NB: Matlab sparse format is in [row col val] format
                //
                // NOTE: Matlab indices start at 1, not 0, so update all the
                // row and column values to be Matlab formatted.
                matrixWriter.println((i + 1) + " " + (curColumn + 1) 
                                     + " " + column[i]);
            }
        }
        return ++curColumn;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(SparseArray<? extends Number> column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");

        int[] nonZero = column.getElementIndices();
        
        for (int i : nonZero) {
            // NB: Matlab sparse format is in [row col  val] format
            //
            // NOTE: Matlab indices start at 1, not 0, so update all the row
            // and column values to be Matlab formatted.
            matrixWriter.println((i + 1) + " " + (curColumn + 1) + " " +
                                 column.get(i).doubleValue());
        }
        return ++curColumn;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized int addColumn(Vector column) {
        if (isFinished)
            throw new IllegalStateException(
                "Cannot add columns to a MatrixBuilder that is finished");
        if (column instanceof Sparse) {
            Sparse s = (Sparse)column;
            for (int i : s.getNonZeroIndices()) {
                // NB: Matlab sparse format is in [row col val] format
                //
                // NOTE: Matlab indices start at 1, not 0, so update all the
                // column and column values to be Matlab formatted.
                matrixWriter.println((i + 1) + " " + (curColumn + 1) +
                                     " " + s.get(i));
            }
        }
        else {
            for (int i = 0; i < column.length(); ++i) {
                double d = column.get(i);
                if (d != 0d) {
                    // NOTE: Matlab indices start at 1, not 0, so update all
                    // the row and column values to be Matlab formatted.
                    matrixWriter.println((i + 1) + " " + (curColumn + 1) +
                                         " " + d);
                }
            }
        }
        return ++curColumn;
    }

    /**
     * {@inheritDoc} Once this method has been called, any subsequent calls will
     * have no effect and will not throw an exception.
     */
    public synchronized void finish() {
        if (!isFinished) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Finished writing matrix in MATLAB_SPARSE format "
                            + "with " + curColumn + " columns");
            }
            isFinished = true;
            matrixWriter.close();
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
     * Returns {@link MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE}.
     */
    public Format getMatrixFormat() {
        return MatrixIO.Format.MATLAB_SPARSE;
    }

    /**
     * {@inheritDoc}
     */
    public synchronized boolean isFinished() {
        return isFinished;
    }
}