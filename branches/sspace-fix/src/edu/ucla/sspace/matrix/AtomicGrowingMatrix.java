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

import edu.ucla.sspace.util.IntegerMap;

import edu.ucla.sspace.vector.AtomicVector;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.util.Arrays;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;


/**
 * A concurrent, thread-safe, growable {@code Matrix} class.  This class allows
 * multiple threads to operate on the same matrix where all methods are
 * concurrent to the fullest extent possible.
 *
 * @author David Jurgens
 */
public class AtomicGrowingMatrix implements AtomicMatrix {
    
    /**
     * The read lock for reading rows from this {@code AtomicGrowingMatrix}.
     */
    private Lock rowReadLock;

    /**
     * The write lock for adding rows to this {@code AtomicGrowingMatrix}.
     */
    private Lock rowWriteLock;

    /**
     * The read lock for reading from the internal rows.
     */
    private Lock denseArrayReadLock;

    /**
     * The write lock for writing to internal rows.
     */
    private Lock denseArrayWriteLock;

    /**
     * The number of rows represented in this {@code AtomicGrowingMatrix}.
     */
    private AtomicInteger rows;

    /**
     * The number of columns represented in this {@code AtomicGrowingMatrix}.
     */
    private AtomicInteger cols;
  
    /**
     * Each row is defined as a {@link AtomicVector} which does most of the
     * work.
     */
    private final Map<Integer, AtomicVector> sparseMatrix;

    /**
     * Create an {@code AtomicGrowingMatrix} with 0 rows and 0 columns.
     */
    public AtomicGrowingMatrix() {
        this.rows = new AtomicInteger(0);
        this.cols = new AtomicInteger(0);
        sparseMatrix = new IntegerMap<AtomicVector>();
        
        ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
        rowReadLock = rwLock.readLock();
        rowWriteLock = rwLock.writeLock();

        rwLock = new ReentrantReadWriteLock();
        denseArrayReadLock = rwLock.readLock();
        denseArrayWriteLock = rwLock.writeLock();
    }
    
    /**
     * {@inheritDoc}
     */
    public double addAndGet(int row, int col, double delta) {
        AtomicVector rowEntry = getRow(row, col, true);
        return rowEntry.addAndGet(col, delta);    
    }

    /**
     * Verify that the given row and column value is non-negative
     *
     * @param row The row index to check.
     * @param the The column index to check.
     */    
    private void checkIndices(int row, int col) {
         if (row < 0 || col < 0) {
             throw new ArrayIndexOutOfBoundsException();
         }
    }

    /**
     * {@inheritDoc}
     */
    public int columns() {
        return cols.get();
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
        AtomicVector rowEntry = getRow(row, col, false);
        return (rowEntry == null) ? 0d : rowEntry.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double getAndAdd(int row, int col, double delta) {
        AtomicVector rowEntry = getRow(row, col, true);
        return rowEntry.getAndAdd(col, delta);
    }


    /**
     * {@inheritDoc}
     */
    public double[] getColumn(int column) {
        rowReadLock.lock();
        double[] values = new double[rows.get()];
        for (int row = 0; row < rows.get(); ++row)
            values[row] = get(row, column);
        rowReadLock.unlock();
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public Vector getColumnVector(int column) {
        rowReadLock.lock();
        Vector values = new CompactSparseVector(rows.get());
        for (int row = 0; row < rows.get(); ++row)
            values.set(row, get(row, column));
        rowReadLock.unlock();
        return values;
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
        AtomicVector rowEntry = getRow(row, -1, false);
        return rowEntry.toArray(cols.get());
    }

    /**
     * Gets the {@code AtomicVector} associated with the index, or {@code null}
     * if no row entry is present, or if {@code createIfAbsent} is {@code true},
     * creates the missing row and returns that.
     *
     * @param row the row to get
     * @param col the column in the row that will be accessed or {@code -1} if
     *        the entire row is needed.  This value is only used to resize the
     *        matrix dimensions if the row is to be created.
     * @param createIfAbsent {@true} if a row that is requested but not present
     *        should be created
     *
     * @return the row at the entry or {@code null} if the row is not present
     *         and it was not to be created if absent
     */
    private AtomicVector getRow(int row, int col, boolean createIfAbsent) {
        checkIndices(row, (col == -1) ? columns() : col);

        rowReadLock.lock();
        AtomicVector rowEntry = sparseMatrix.get(row);
        rowReadLock.unlock();
         
        // If no row existed, create one
        if (rowEntry == null && createIfAbsent) {
            rowWriteLock.lock();
            // ensure that another thread has not already added this row while
            // this thread was waiting on the lock
            rowEntry = sparseMatrix.get(row);
            if (rowEntry == null) {
                    rowEntry = new AtomicVector(new CompactSparseVector());

                // update the bounds as necessary
                if (row >= rows.get()) {
                    rows.set(row + 1);
                }
                if (col >= cols.get()) {
                    cols.set(col + 1);
                }
                sparseMatrix.put(row, rowEntry);
            }
            rowWriteLock.unlock();
        }
        return rowEntry;
    }

    /**
     * {@inheritDoc}
     */
    public Vector getRowVector(int row) {
        Vector v = getRow(row, -1, false);
        v.setKnownLength(columns());
        return Vectors.immutableVector(v);
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
        return rows.get();
    }

    /**
     * {@inheritDoc}
     */
    public void set(int row, int col, double val) {
        checkIndices(row, col);

        AtomicVector rowEntry = getRow(row, col, true);
        denseArrayReadLock.lock();
        rowEntry.set(col, val);
        denseArrayReadLock.unlock();
    }

    /**
     * @{inheritDoc}
     */
    public void setColumn(int column, double[] values) {
        for (int row = 0; row < rows.get(); ++row)
            set(row, column, values[row]);
    }

    /**
     * @{inheritDoc}
     */
    public void setColumn(int column, Vector values) {
        for (int row = 0; row < rows.get(); ++row)
            set(row, column, values.get(row));
    }
  
    /**
     * @{inheritDoc}
     */
    public void setRow(int row, double[] columns) {
        AtomicVector rowEntry = getRow(row, columns.length - 1, true);
        denseArrayReadLock.lock();
        rowEntry.set(columns);
        denseArrayReadLock.unlock();
    }

    /**
     * @{inheritDoc}
     */
    public void setRow(int row, Vector values) {
        AtomicVector rowEntry = getRow(row, values.length() - 1, true);
        denseArrayReadLock.lock();
        Vectors.copy(rowEntry, values);
        denseArrayReadLock.unlock();
    }
  
    /**
     * {@inheritDoc}
     */
    public double[][] toDenseArray() {
        // Grab the write lock to prevent any new rows from being updated
        rowWriteLock.lock();
        // Then grab the whole matrix lock to prevent any values from being set
        // while this method converts the rows into arrays.
        denseArrayWriteLock.lock();
        int c = cols.get();
        double[][] m = new double[rows.get()][c];
        for (Map.Entry<Integer, AtomicVector> e : sparseMatrix.entrySet()) {
            m[e.getKey()] = e.getValue().toArray(c);
        }
        denseArrayWriteLock.unlock();
        rowWriteLock.unlock();
        return m;
    }
}
