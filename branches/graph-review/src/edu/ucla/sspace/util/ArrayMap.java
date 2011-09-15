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

package edu.ucla.sspace.util;

import java.util.AbstractMap;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.AbstractSet;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 * A {@link Map} implementation for integer keys, which backed by an array.
 * This class provides a bridge between array-based data structures and
 * Collections-based APIs that require mappings from integer keys to values.
 *
 * <p>This map does not permit {@code null} values or keys.  Furthermore, all
 * keys must be in the range of [0,l] where l is the length of the original
 * array.  Attempts to add new mappings outside this range will cause an {@link
 * IllegalArgumentException}, all other operations that exceed this boundary
 * will return {@code null} or {@code false} where appropriate.
 *
 * <p>All mutating methods to the entry set and iterators will throw {@link
 * UnsupportedOperationException} if called.
 *
 * <p>The {@link #size()} operation runs in linear time upon first call and is
 * constant time for all subsequent calls.
 *
 * <p>This map is not thread safe.
 */
public class ArrayMap<T> extends AbstractMap<Integer,T> 
        implements java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The backing array.
     */
    private final T[] array;

    /**
     * The number of mappings in this {@code Map} or {@code -1} if the number of
     * mappings has yet to be calculated.  The latter condition occurs when an
     * array is initially passed in and hasn't been checked for {@code null}
     * elements, which are considered non-existant mappings.
     */ 
    private int size;

    /**
     * Creates a {@link Map} over the non-{@code null} values in the provided a
     * array to their corresponding indices up to the specified size.  Array
     * elements with indices beyond the size of the map will not be included in
     * this mapping.  The values of the array are copied internally so changes
     * to this map will not be reflected in the array and vice versa.
     */
    public ArrayMap(T[] array, int sizeOfMap) {
        if (array == null)
            throw new NullPointerException();
        this.array = Arrays.copyOf(array, sizeOfMap);
        size = -1;
    }

    /**
     * Creates a {@link Map} over the non-{@code null} values in the provided a
     * array to their corresponding indices.  The values of the array are copied
     * internally so changes to this map will not be reflected in the array and
     * vice versa.
     */
    public ArrayMap(T[] array) {
        if (array == null)
            throw new NullPointerException();
        this.array = Arrays.copyOf(array, array.length);
        size = -1;
    }
    
    /**
     * Returns the contents of this map as a {@code Set} of key-value entries.
     */
    public Set<Map.Entry<Integer,T>> entrySet() {
        return new EntrySet();
    }

    /**
     * Returns {@code true} if key is an {@code Integer} and is mapped to a
     * non-{@code null} value in this map, {@code false} otherwise.
     */
    @Override public boolean containsKey(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            return i >= 0 && i < array.length && array[i] != null;
        }
        return false;
    }

    /**
     * Returns {@code true} if the provided value is mapped to a key in this
     * map.
     */
    @Override public boolean containsValue(Object value) {
        if (value == null)
            return false;
        for (T t : array)
            if (t != null && t.equals(value))
                return true;
        return false;
    }

    /**
     * Returns the value mapped to the provided key, or {@code null} if no such
     * value was found for the key.
     */
    @Override public T get(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            return (i < 0 || i >= array.length)
                ? null
                : array[i];
        }
        return null;
    }

    /**
     * Returns the largest {@link Integer} key allowed by this map.
     */
    public int maxKey() {
        return array.length - 1;
    }

    /**  
     * Associates the integer key with the value, returning the old value or
     * {@code null} if this is a new mapping.
     * 
     * @throws IllegalArgumentException if {@code key} < 0 or {@code key} >
     * maxKey(), or if {@code value} is {@code null}.
     */
    @Override public T put(Integer key, T value) {
        if (key < 0 || key >= array.length)
            throw new IllegalArgumentException(
                "key goes beyond bounds of the array backing this Map:" + key);
        if (value == null)
            throw new IllegalArgumentException("null values are not supported");
        T t = array[key];
        array[key] = value;
        // If we have computed the size and are adding a new element, then
        // update the number of mappings
        if (size >= 0 && t == null)
            size++;
        return t;
    }

    /**
     * Removes the key and its associated value, if any, from this mapping
     * returning the previous mapped value.
     */
    @Override public T remove(Object key) {
        if (key instanceof Integer) {
            Integer i = (Integer)key;
            if (i < 0 || i >= array.length)
                return null;
            T t = array[i];
            array[i] = null;
            if (size >= 0 && t != null)
                size--;
            return t;
        }
        return null;
    }

    /**
     * Returns the number of key-value mappings contained in this map.
     */
    @Override public int size() {
        if (size == -1) {
            size = 0;
            for (T t : array) {
                if (t != null)
                    size++;
            }
        }
        return size;
    }

    private class EntrySet extends AbstractSet<Map.Entry<Integer,T>> {        

        public boolean add(Map.Entry<Integer,T> e) {
            throw new UnsupportedOperationException();
        }

        public boolean contains(Object o) {
            if (o instanceof Map.Entry) {
                Map.Entry<?,?> e = (Map.Entry<?,?>)o;
                T t = get(e.getKey());
                return t != null && t.equals(e.getValue());
            }
            return false;
        }

        public Iterator<Map.Entry<Integer,T>> iterator() {
            return new EntryIterator();
        }

        public boolean remove(Object o) {
            throw new UnsupportedOperationException();
        }

        public int size() {
            return size;
        }
        
        private class EntryIterator implements Iterator<Map.Entry<Integer,T>> {

            private int i;

            private Map.Entry<Integer,T> next;

            public EntryIterator() {
                i = 0;
                advance();
            }
            
            public void advance() {
                next = null;
                while (i < array.length) {
                    T t = array[i];
                    i++;
                    if (t != null) {
                        next = new SimpleImmutableEntry<Integer,T>(i, t);
                        break;
                    }
                }
            }

            public boolean hasNext() {
                return next != null;
            }

            public Map.Entry<Integer,T> next() {
                if (!hasNext())
                    throw new IllegalStateException();
                Map.Entry<Integer,T> n = next;
                advance();
                return n;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        }
    }
}