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
    
    private int[] buckets;
    
    boolean isZeroPresent;

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
        isZeroPresent = false;
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
        if (i == 0) {
            if (!isZeroPresent) {
                isZeroPresent = true;
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
            assert curVal == 0 : "overwriting existing value";
            buckets[bucket] = i;
            size++;
            return true;
        }
    }

    private static int findIndex(int[] buckets, int i, int maxMisses) {
        // int bucket = i % buckets.length;
        // System.out.printf("checking bucket %d for %d%n", bucket, i);
        // if (buckets[bucket] == 0 || buckets[bucket] == i)
        //     return bucket;
        // else {
        //     int misses = 0;
        //     for (int j = bucket + 1 % buckets.length; 
        //              j != bucket; j = j + 1 % buckets.length, ++misses) {
                
        //         System.out.printf("checking bucket %d for %d%n", j, i);
                
        //         if (misses > maxMisses) 
        //             return -1;
                
        //         int val = buckets[j];
        //         if (val == 0 || val == i)
        //             return j;
        //     }
        // }
        
        int repeat = i % buckets.length;
        int misses = 0;
        
        for (int j = repeat; misses < buckets.length;
                 j = (j+1) % buckets.length, ++misses) {
                
            // System.out.printf("checking bucket %d for %d%n", j, i);
               
            if (misses > maxMisses) 
                return -1;
            
            int val = buckets[j];
            if (val == 0 || val == i)
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
        int bucket = findIndex(buckets, i, buckets.length);
        return buckets[bucket] == i;
    }

    @Override public void clear() {
        for (int i = 0; i < buckets.length; ++i)
            buckets[i] = 0;
        isZeroPresent = false;
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
            if (i != 0) {
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
        throw new UnsupportedOperationException();
    }

    public int size() {
        return size;
    }

    private class IntIterator implements Iterator<Integer> {

        Integer next = null;
        int i = -1;
        boolean returnedZero = false;

        public IntIterator() {
            advance();
        }

        private void advance() {
            if (isZeroPresent && !returnedZero) {
                next = 0;
                returnedZero = true;
            }
            else {
                int j = i + 1;
                for (; j < buckets.length && buckets[j] == 0 ; ++j)
                    ;
                next = (j == buckets.length) ? null : buckets[j];
                i = j;
            }
        }
        
        public boolean hasNext() {
            return next != null;
        }

        public Integer next() {
            if (next == null)
                throw new NoSuchElementException();
            Integer cur = next;
            advance();
            return cur;
        }

        public void remove() {            
            throw new UnsupportedOperationException();
        }
    }

}