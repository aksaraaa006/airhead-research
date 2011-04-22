/*
 * Copyright 2011 David Jurgens 
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

package edu.ucla.sspace.util;

import java.util.AbstractSet;
import java.util.BitSet;
import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

public class OpenIntSet extends AbstractSet<Integer> {

    private static final int EMPTY_MARKER = 0; // default array value
    private static final int DELETED_MARKER = Integer.MAX_VALUE;
    
    private int[] buckets;
    
    private boolean isEmptyMarkerValuePresent;
    private boolean isDeletedMarkerValuePresent;


    int size;

    public OpenIntSet() {
        this(4);
    }

    public OpenIntSet(int size) {
        // find the next power of two greater than the size
        int n = 1;
        for (int i = 1; i < 32; ++i) {
            if ((n << i) >= size) {
                buckets = new int[n << i];
                break;
            }
        }
        isEmptyMarkerValuePresent = false;
        isDeletedMarkerValuePresent = false;
        size = 0;
    }

    public OpenIntSet(Collection<Integer> ints) {
        this(ints.size());
        addAll(ints);
    }

    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        // Special case for the value that indicates an empty value in the
        // backing table
        if (i == EMPTY_MARKER) {
            if (!isEmptyMarkerValuePresent) {
                isEmptyMarkerValuePresent = true;
                size++;
                return true;
            }
            return false;
        }
        // Special case for the value that indicates that a value has been
        // deleted in the backing table
        if (i == DELETED_MARKER) {
            if (!isDeletedMarkerValuePresent) {
                isDeletedMarkerValuePresent = true;
                size++;
                return true;
            }
            return false;
        }

        int maxMisses = buckets.length >> 2; // div by 4
        int bucket = findIndex(buckets, i, maxMisses);
        while (bucket == -1) {
            rebuildTable();
            bucket = findIndex(buckets, i, maxMisses);
        }

        int curVal = buckets[bucket];
        // System.out.printf("Index for %d: %d, currently %d%n", i, bucket, curVal);
        if (curVal == i) 
            return false;
        else {
            assert (curVal == EMPTY_MARKER || curVal == DELETED_MARKER)
                : "overwriting existing value";
            buckets[bucket] = i;
            size++;
            return true;
        }
    }

    private static int findIndex(int[] buckets, int i, int maxMisses) {
       
        int repeat = i % buckets.length;
        int misses = 0;
        
        for (int j = repeat; misses < buckets.length;
                 j = (j+1) % buckets.length, ++misses) {
                
            // Check whether the linear probe has exceeded the number of
            // allowable steps
            if (misses > maxMisses) 
                return -1;           

            // If the value indicates an available space to put i (it's empty or
            // contains a deleted value), or if the space already contains i,
            // then return the index
            int val = buckets[j];
            if (val == EMPTY_MARKER || val == DELETED_MARKER || val == i)
                return j;        
        }
        
        assert false : "unhandled branch in findIndex";
        return -1;
    }
    
    @Override public boolean contains(Object o) {
        if (o instanceof Integer)
            return contains(((Integer)o).intValue());
        else 
            throw new ClassCastException();
    }

    public boolean contains(int i) {
        // Special cases for the two marker values
        if (i == EMPTY_MARKER)
            return isEmptyMarkerValuePresent;
        else if (i == DELETED_MARKER)
            return isDeletedMarkerValuePresent;

        // Otherwise, find which bucket this value should be in and check if the
        // value is in that bucket.
        int bucket = findIndex(buckets, i, buckets.length);
        return buckets[bucket] == i;
    }

    @Override public void clear() {
        for (int i = 0; i < buckets.length; ++i)
            buckets[i] = 0;
        isEmptyMarkerValuePresent = false;
        isDeletedMarkerValuePresent = false;
        size = 0;
    }

    @Override public boolean isEmpty() {
        return size == 0;
    }

    public Iterator<Integer> iterator() {
        return new IntIterator();
    }

    private void rebuildTable() {
        int newSize = buckets.length << 1;
        int[] newBuckets = new int[newSize];
        // Rehash all of the existing elements
        for (int i : buckets) {
            // For all non-empty, non-deleted cells, find the new index for that
            // cell's value in the new table
            if (i != EMPTY_MARKER && i != DELETED_MARKER) {
                int index = findIndex(newBuckets, i, newSize);
                newBuckets[index] = i;
            }
        }
        buckets = newBuckets;
    }

    public boolean remove(Integer i) {
        return remove(i.intValue());
    }
    
    public boolean remove(int i) {
        boolean wasPresent = false;
        // Special cases for the two marker values
        if (i == EMPTY_MARKER) {
            wasPresent = isEmptyMarkerValuePresent;
            isEmptyMarkerValuePresent = false;
        }
        else if (i == DELETED_MARKER) {
            wasPresent = isDeletedMarkerValuePresent;
            isDeletedMarkerValuePresent = false;
        }
        else {
            // Find where i would be located in the table
            int bucket = findIndex(buckets, i, buckets.length);
            // If the bucket contained the value to be removed, then perform a
            // lazy delete and just mark its index as deleted.  This saves time
            // having to shift over all the elements in the table.
            if (buckets[bucket] == i) {
                buckets[bucket] = DELETED_MARKER;
                wasPresent = true;
            }
        }
        
        // If the value to be removed was present the shrink the size of the set
        if (wasPresent)
            size--;
        return wasPresent;
    }

    public int size() {
        return size;
    }

    private class IntIterator implements Iterator<Integer> {
        
        int cur;
        int next;
        int nextIndex;
        boolean alreadyRemoved;

        boolean returnedEmptyMarker = false;
        boolean returnedDeletedMarker = false;


        public IntIterator() {
            nextIndex = -1;
            cur = -1;
            next = -1;
            alreadyRemoved = true; // causes NSEE if remove() called first
            returnedEmptyMarker = false;
            returnedDeletedMarker = false;
            advance();
        }

        private void advance() {
            if (!returnedEmptyMarker && isEmptyMarkerValuePresent) {
                next = EMPTY_MARKER;
            }
            else if (!returnedDeletedMarker && isDeletedMarkerValuePresent) {
                next = DELETED_MARKER;
            }
            else {
                int j = nextIndex + 1;
                while (j < buckets.length && 
                       (buckets[j] == EMPTY_MARKER
                        || buckets[j] == DELETED_MARKER)) {
                    ++j;
                }
                    
                // if (j == buckets.length) {
                //     nextIndex = -1;
                //     next = -1;
                // }
                // else {
                //     nextIndex = j;
                // next = b
                nextIndex = (j == buckets.length) ? -1 : j;
                next = (nextIndex >= 0) ? buckets[nextIndex] : -1;
            }
        }
        
        public boolean hasNext() {
            return nextIndex >= 0
                || (!returnedEmptyMarker && isEmptyMarkerValuePresent)
                || (!returnedDeletedMarker && isDeletedMarkerValuePresent);
        }

        public Integer next() {
            if (!hasNext())
                throw new NoSuchElementException();
            cur = next;
            if (next == EMPTY_MARKER) {
                returnedEmptyMarker = true;
            }
            else if (next == DELETED_MARKER) {
                returnedDeletedMarker = true;
            }
            advance();
        
            alreadyRemoved = false;
            return cur;
        }

        public void remove() {
            if (alreadyRemoved)
                throw new NoSuchElementException();
            alreadyRemoved = true;
            OpenIntSet.this.remove(cur);
        }
    }

}