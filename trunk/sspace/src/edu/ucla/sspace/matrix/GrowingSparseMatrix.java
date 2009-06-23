/*
 * Copyright 2009 Keith Stevens 
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A growing sparse {@code Matrix} based on the Yale Sparse Matrix Format.
 * Each row is allocated a linked list structure which keeps the non-zero column
 * values in column order.  Lookups are O(log n) where n is the number of
 * non-zero values for the largest row.  Inserting is currently dependent on the
 * insertion time of an ArrayList.  Calls to set and setColumn can expand the
 * matrix by both rows and columns.
 *
 * @author Keith Stevens 
 */
public class GrowingSparseMatrix implements Matrix {
    
  private int rows;

  private int cols;
  
  /**
   * Each row is defined as a {@link RowEntry} which does most of the work.
   */
  private final ArrayList<RowEntry> sparseMatrix;

  /**
   */
  public GrowingSparseMatrix() {
    this.rows = 0;
    this.cols = 0;
    sparseMatrix = new ArrayList<RowEntry>();
  }

  /**
   *
   */    
  private void checkIndices(int row, int col) {
    if (row < 0 || col < 0 || row >= rows || col >= cols) {
      throw new ArrayIndexOutOfBoundsException();
    }
  }

  /**
   * {@inheritDoc}
   */
  public double get(int row, int col) {
    return sparseMatrix.get(row).getValue(col);
  }

  /**
   * {@inheritDoc}
   */
  public double[] getRow(int row) {
    return sparseMatrix.get(row).getRow(cols);
  }

  /**
   * {@inheritDoc}
   */
  public int columns() {
    return cols;
  }

  /**
   * {@inheritDoc}
   * The size of the matrix will be expanded if either row or col is larger than
   * the largest previously seen row or column value.  When the matrix is
   * expanded by either dimension, the values for the new row/column will all be
   * assumed to be zero.
   */
  public void set(int row, int col, double val) {
    if (row >= sparseMatrix.size()) {
      while (sparseMatrix.size() <= row) {
        sparseMatrix.add(new RowEntry());
      }
    }
    if (col == cols) {
      cols++;
    } else if (col > cols) {
      cols = col;
    }
    sparseMatrix.get(row).setValue(col, val);
  }

  /**
   * {@inheritDoc}
   * The size of the matrix will be expanded if either row or col is larger than
   * the largest previously seen row or column value.  When the matrix is
   * expanded by either dimension, the values for the new row/column will all be
   * assumed to be zero.
   */
  public void setRow(int row, double[] columns) {
    if (columns.length < cols) {
      throw new IllegalArgumentException(
      "invalid number of columns: " + columns.length);
    } else {
      cols = columns.length;
    }
    if (row >= sparseMatrix.size())
      while (sparseMatrix.size() <= row)
        sparseMatrix.add(new RowEntry());
    for (int col = 0; col < cols; ++col) {
      double val = columns[col];
      if (val != 0) {
        sparseMatrix.get(row).setValue(col, val);
      }
    }
  }
  
  /**
   * {@inheritDoc}
   */
  public double[][] toDenseArray() {
    double[][] m = new double[rows][cols];
    for (int r = 0; r < rows; ++r) {
      m[r] = sparseMatrix.get(r).getRow(cols);
    }
    return m;
  }

  /**
   *
   */
  public int rows() {
    return sparseMatrix.size();
  }

  public static final class RowEntry {
    /**
     * An arraylist of non zero values for this row, stored in the correct
     * column order.
     */
    private ArrayList<Double> values;
    /**
     * An arraylist of which column indexes are stored for this row in sorted
     * order.
     */
    private ArrayList<Integer> columnIndexes;

    /**
     * Create the two lists, with zero values in them initially.
     */
    public RowEntry() {
      values = new ArrayList<Double>();
      columnIndexes = new ArrayList<Integer>();
    }

    /**
     * retrieve the value at specified column
     * @param column The column value to get
     * @return the value for the specified column, or 0 if no column is found.
     */
    public double getValue(int column) {
      int valueIndex = Collections.binarySearch(columnIndexes, column);
      return (valueIndex >= 0) ? values.get(valueIndex) : 0.0;
    }

    /**
     * Update the RowEntry such that the index at column now stores value.  If
     * value is 0, this will remove the column from the row entry for efficency.
     *
     * @param column The column index this value should be stored as
     * @param value The value to store
     */
    public void setValue(int column, double value) {
      int valueIndex = Collections.binarySearch(columnIndexes, column);
      if (valueIndex >= 0 && value != 0d) {
        values.set(valueIndex, value);
      } else if (value != 0d) {
        values.add((valueIndex + 1) * -1, value);
        columnIndexes.add((valueIndex+1) * -1, column);
      } else if (valueIndex >= 0) {
        values.remove(valueIndex);
        columnIndexes.remove(valueIndex);
      }
    }

    /**
     * A dense double array which this RowEntry represents.
     */
    public double[] getRow(int columnSize) {
      double[] dense = new double[columnSize];
      for (int i = 0; i < columnIndexes.size(); ++i) {
        dense[columnIndexes.get(i).intValue()] = values.get(i).doubleValue();
      }
      return dense;
    }
  }
}
