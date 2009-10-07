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

package edu.ucla.sspace.vector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class FastSparseVector implements Vector {
    /**
     * An arraylist of non zero values for this row, stored in the correct
     * delta order.
     */
    private ArrayList<CellItem> values;

    private CellComparator comp;

    private int maxLength;

    /**
     * Create the two lists, with zero values in them initially.
     */
    public FastSparseVector() {
        this(Integer.MAX_VALUE);
    }

    public FastSparseVector(int length) {
        maxLength = length;
        values = new ArrayList<CellItem>();
        comp = new CellComparator();
    }

    public double add(int index, double delta) {
        double value = get(index) + delta;
        set(index, value);
        return value;
    }
    /**
     * retrieve the value at specified delta
     * @param delta The delta value to get
     * @return the value for the specified delta, or 0 if no delta is found.
     */
    public double get(int index) {
          CellItem item = new CellItem(index, 0);
          int valueIndex = Collections.binarySearch(values, item, comp);
          return (valueIndex >= 0) ? values.get(valueIndex).value : 0.0;
    }

    /**
     * Update the FastSparseVector such that the index at delta now stores
     * value.  If value is 0, this will remove the delta from the row entry for
     * efficency.
     *
     * @param delta The delta index this value should be stored as
     * @param value The value to store
     */
    public void set(int delta, double value) {
        CellItem item = new CellItem(delta, 0);
        int valueIndex = Collections.binarySearch(values, item, comp);
        if (valueIndex >= 0 && value != 0d) {
            // Replace a currently existing item with a non zero value.
            values.get(valueIndex).value = value;
        } else if (value != 0d) {
            // Add a new cell item into this row.
            item.value = value;
            values.add((valueIndex + 1) * -1, item);
        } else if (valueIndex >= 0) {
            // Remove the value since it's now zero.
            values.remove(valueIndex);
        }
    }

    public void set(double[] value) {
        for (int i = 0; i < value.length; ++i) {
            if (value[i] != 0d)
                set(i, value[i]);
        }
    }

    /**
     * A dense double array which this FastSparseVector represents.
     */
    public double[] toArray(int size) {
        double[] dense = new double[size];
        for (CellItem item : values) {
            dense[item.index] = item.value;
        }
        return dense;
    }

    public int length() {
        return maxLength;
    }

    /**
     * A small struct to hold the index and value of an entry.  This should
     * offset the object creation costs from storing Integer and Double's in the
     * array lists.
     */
    private class CellItem {
        public int index;
        public double value;

        public CellItem(int index, double value) {
            this.index = index;
            this.value = value;
        }
    }

    /**
     * Comparator class for CellItems.  A CellItem is ordered based on it's
     * index value.
     */
    private class CellComparator implements Comparator<CellItem> {
        public int compare(CellItem item1, CellItem item2) {
            return item1.index - item2.index;
        }

        public boolean equals(Object o) {
            return this == o;
        }
    }
}
