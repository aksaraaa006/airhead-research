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

import java.util.Arrays;
import java.util.Map;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.ucla.sspace.util.IntegerMap;

/**
 *
 */
public class AtomicGrowingMatrix implements ConcurrentMatrix {
    
    private Lock rowReadLock;
    private Lock rowWriteLock;

    private Lock denseArrayReadLock;
    private Lock denseArrayWriteLock;

    private AtomicInteger rows;
    private AtomicInteger cols;
  
    /**
     * Each row is defined as a {@link RowEntry} which does most of the work.
     */
    private final Map<Integer,RowEntry> sparseMatrix;

    /**
     */
    public AtomicGrowingMatrix() {
	this.rows = new AtomicInteger(0);
	this.cols = new AtomicInteger(0);
	sparseMatrix = new IntegerMap<RowEntry>();
	
	ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	rowReadLock = rwLock.readLock();
	rowWriteLock = rwLock.writeLock();

	rwLock = new ReentrantReadWriteLock();
	denseArrayReadLock = rwLock.readLock();
	denseArrayWriteLock = rwLock.writeLock();

    }
    
    /**
     *
     */    
    private void checkIndices(int row, int col) {
// 	if (row < 0 || col < 0 || row >= rows || col >= cols) {
// 	    throw new ArrayIndexOutOfBoundsException();
// 	}
    }

    public double getAndAdd(int row, int col, double delta) {
	RowEntry rowEntry = getRow(row, col, true);
	return rowEntry.getAndAdd(col, delta);
    }

    public double addAndGet(int row, int col, double delta) {
	RowEntry rowEntry = getRow(row, col, true);
	return rowEntry.addAndGet(col, delta);	
    }

    /**
     * {@inheritDoc}
     */
    public double get(int row, int col) {
	RowEntry rowEntry = getRow(row, col, false);
	return (rowEntry == null) ? 0d : rowEntry.get(col);
    }

    /**
     * {@inheritDoc}
     */
    public double[] getRow(int row) {
	RowEntry rowEntry = getRow(row, -1, false);
	return rowEntry.toArray(cols.get());
    }

    /**
     * Gets the {@code RowEntry} associated with the index, or {@code null} if
     * no row entry is present, or if {@code createIfAbsent} is {@code true},
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
    private RowEntry getRow(int row, int col, boolean createIfAbsent) {
	rowReadLock.lock();
// 	System.out.printf("(%d,%d) : %s%n", row, col, sparseMatrix);
	RowEntry rowEntry = sparseMatrix.get(row);
	rowReadLock.unlock();
     
	// If no row existed, create one
	if (rowEntry == null && createIfAbsent) {
	    rowWriteLock.lock();
	    // ensure that another thread has not already added this row while
	    // this thread was waiting on the lock
	    rowEntry = sparseMatrix.get(row);
	    if (rowEntry == null) {
		rowEntry = new RowEntry();
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
	//System.out.printf("row %d -> %s%n", row, rowEntry);
	return rowEntry;
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
    public void set(int row, int col, double val) {
	RowEntry rowEntry = getRow(row, col, true);
	denseArrayReadLock.lock();
	rowEntry.set(col, val);
	denseArrayReadLock.unlock();
    }

    /**
     *
     */
    public void setRow(int row, double[] columns) {
	RowEntry rowEntry = getRow(row, columns.length - 1, true);
	denseArrayReadLock.lock();
	rowEntry.set(columns);
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
	for (Map.Entry<Integer,RowEntry> e : sparseMatrix.entrySet()) {
	    m[e.getKey()] = e.getValue().toArray(c);
	}
	denseArrayWriteLock.unlock();
	rowWriteLock.unlock();
	return m;
    }

    /**
     * {@inheritDoc}
     */
    public int rows() {
	return rows.get();
    }

    public static final class RowEntry {

	private final Lock readLock;
	private final Lock writeLock;

	private int[] indices;
	private double[] values;
       
	/**
	 * Create the two lists, with zero values in them initially.
	 */
	public RowEntry() {
	    indices = new int[0];
	    values = new double[0];
	    ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
	    readLock = rwLock.readLock();
	    writeLock = rwLock.writeLock();
	}

	public double addAndGet(int col, double delta) {
	    writeLock.lock();
 	    int index = Arrays.binarySearch(indices, col);
 	    double value = (index >= 0) ? values[index] : 0.0;
	    set(col, value + delta);
// 	    System.out.printf("value is %f, setting to %f%n", value, value + delta);
	    writeLock.unlock();
	    return value + delta;
	}

	public double getAndAdd(int col, double delta) {
	    writeLock.lock();
	    double value = get(col);
	    set(col, value + delta);
	    writeLock.unlock();
	    return value;
	}

	/**
	 * retrieve the value at specified column
	 * @param column The column value to get
	 * @return the value for the specified column, or 0 if no column is found.
	 */
	public double get(int column) {
	    readLock.lock();
	    int index = Arrays.binarySearch(indices, column);
// 	    System.out.printf("get(%d), indices: %s, values %s%n", column,
// 			      Arrays.toString(indices), Arrays.toString(values));
	    double value = (index >= 0) ? values[index] : 0.0;
	    //System.out.printf("get(%d) = %f%n", column, value);
	    readLock.unlock();
	    return value;
	}

	/**
	 * Update the RowEntry such that the index at column now stores value.  If
	 * value is 0, this will remove the column from the row entry for efficency.
	 *
	 * @param column The column index this value should be stored as
	 * @param value The value to store
	 */
	public void set(int column, double value) {
	    writeLock.lock();
	    int index = Arrays.binarySearch(indices, column);

	    if (value != 0) {
		// need to make room in the indices array
		if (index < 0) {
		    int newIndex = 0 - (index + 1);
		    int[] newIndices = Arrays.copyOf(indices, indices.length + 1);
		    double[] newValues = Arrays.copyOf(values, values.length + 1);
		    
		    // shift the elements down by one to make room
		    for (int i = newIndex; i < values.length; ++i) {
			newValues[i+1] = values[i];
			newIndices[i+1] = indices[i];
		    }
		    
		    // swap the arrays
		    indices = newIndices;
		    values = newValues;
		    index = newIndex;
		    
		    // update the position of the index in the values array
		    indices[index] = column;
		}
// 		System.out.printf("indices[%d] -> %f%n", index, value);
		values[index] = value;
	    }
	    // The value is zero but previously held a spot in the matrix, so
	    // remove its position and shift everything over
	    else if (value == 0 && index >= 0) {
		int newLength = indices.length - 1;
		int[] newIndices = new int[newLength];
		double[] newValues = new double[newLength];
		for (int i = 0, j = 0; i < indices.length; ++i) {
		    if (i != index) {
			newIndices[j] = indices[i];
			newValues[j] = values[i];			
			j++;
		    }
		}
		// swap the arrays
		indices = newIndices;
		values = newValues;
	    }

	    // note that in the even of a set with value 0 where the index was
	    // not present, this method is a no-op

	    writeLock.unlock();
	}

	public void set(double[] row) {
	    writeLock.lock();
	    // iterate throug once to find the number of non-zero indices
	    int nonZero = 0;
	    for (int i = 0; i < row.length; ++i) {
		if (row[i] != 0)
		    nonZero++;
	    }
	    // check to see whether we need to create new arrays or not
	    if (nonZero != indices.length) {
		indices = new int[nonZero];
		values = new double[nonZero];
	    }
	    for (int i = 0, j = 0; i < row.length; ++i) {
		if (row[i] != 0) {
		    indices[j] = i;
		    values[j] = row[i];
		    j++;
		}
	    }
	    writeLock.unlock();
	}

	/**
	 * A dense double array which this RowEntry represents.
	 */
	public double[] toArray(int columnSize) {
	    readLock.lock();
	    double[] dense = new double[columnSize];
	    for (int i = 0; i < indices.length; ++i) {
		dense[indices[i]] = values[i];
	    }
	    readLock.unlock();
	    return dense;
	}
    }
}
