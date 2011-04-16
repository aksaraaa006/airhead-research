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

public class IntSet extends AbstractSet<Integer> {
    
    private final BitSet bitSet;

    public IntSet() {
        bitSet = new BitSet();
    }

    public IntSet(Collection<Integer> ints) {
        this();
        for (Integer i : ints)
            bitSet.set(i);
    }

    public boolean add(Integer i) {
        return add(i.intValue());
    }

    public boolean add(int i) {
        boolean isPresent = bitSet.get(i);
        bitSet.set(i);
        return isPresent;
    }


    public boolean contains(Integer i) {
        return contains(i.intValue());
    }

    public boolean contains(int i) {
        return bitSet.get(i);
    }

    public boolean isEmpty() {
        return bitSet.isEmpty();
    }

    public Iterator<Integer> iterator() {
        return new IntIterator();
    }

    public boolean remove(Integer i) {
        return remove(i.intValue());
    }
    
    public boolean remove(int i) {
        boolean isPresent = bitSet.get(i);
        if (isPresent)
            bitSet.set(i, false);
        return isPresent;  
    }

    public int size() {
        return bitSet.size();
    }

    private class IntIterator implements Iterator<Integer> {

        int next = -1;
        int cur = -1;

        public IntIterator() {
            advance();
        }

        private void advance() {
            if (next < -1)
                return;
            next = bitSet.nextSetBit(next + 1);
            // Keep track of when we finally go off the end
            if (next == -1)
                next = -2;
        }
        
        public boolean hasNext() {
            return next > 0;
        }

        public Integer next() {
            if (next < 0)
                throw new NoSuchElementException();
            cur = next;
            advance();
            return cur;
        }

        public void remove() {            
            if (cur == -1)
                throw new IllegalStateException("Item already removed");
            bitSet.set(cur, false);
            cur = -1;
        }
    }

}