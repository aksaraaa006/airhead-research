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

package edu.ucla.sspace.common;

import java.util.AbstractCollection;
import java.util.AbstractSet;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A space-optimized map for associating integer keys with values.  
 * <p>
 *
 * This class makes a trade off for reduced space usage at the cost of decreased
 * performace.  The {@code put}, {@code get}, {@code containsKey} and {@code
 * get} operations are all logarithmic when the map is unmodified.  If a new
 * mapping is added, or one is removed, the operation is linear in the number of
 * mappings.  Both {@code size} and {@code isEmpty} are still constant time. <p>
 *
 * This map does not allow {@code null} keys, but does allow {@code null
 * values}.
 *
 * <i>Implementation Note:</i> the {@code Iterator.remove()} method is currently
 * unsupported and will throw an exception when called.  However, a future
 * implementation will fix this.<p>
 *
 * @see TrieMap
 * @see Map
 * 
 * @author David Jurgens
 */
public class IntegerMap<V> extends AbstractMap<Integer,V> {
    
    private static final long serialVersionUID = 1L;

    /**
     * The keys stored in this map, in sorted order.  The index at which a key
     * is found corresponds to the index at which its value is found.
     */
    int[] keyIndices;

    /**
     * The values stored in this map.
     */
    Object[] values;

    /**
     * Creates a new map.
     */
    public IntegerMap() {
	keyIndices = new int[0];
	values = new Object[0];
    }

    /**
     * Checks that the key is non-{@code null} and is an {@code Integer} object,
     * and then returns its {@code int} value.
     */
    private int checkKey(Object key) {
	if (key == null) {
	    throw new NullPointerException("key cannot be null");
	} else if (!(key instanceof Integer)) {
	    throw new IllegalArgumentException("key must be an Integer");
	}
	else {
	    return ((Integer)key).intValue();
	}
    }

    /**
     * Removes all of the mappings from this map.
     */
    public void clear() {
	keyIndices = new int[0];
	values = new Object[0];
    }

    /**
     * Returns {@code true} if this map contains a mapping for the specified key.
     */
    public boolean containsKey(Object key) {
	int k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);
	return index >= 0;
    }

    public boolean containsValue(Object value) {
	for (Object o : values) {
	    if (o == value || (o != null && o.equals(value))) {
		return true;
	    }
	}
	return false;
    }

    /**
     * Returns a {@link Set} view of the mappings contained in this map.
     */
    public Set<Entry<Integer,V>> entrySet() {
	return new EntrySet();
    }

    /**
     * Returns the value to which the specified key is mapped, or {@code null}
     * if this map contains no mapping for the key.
     */
    @SuppressWarnings("unchecked")
    public V get(Object key) {
	int k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);
	return (index >= 0) ? (V)(values[index]) : null;
    }

    /**
     * Returns a {@link Set} view of the keys contained in this map.
     */
    public Set<Integer> keySet() {
	return new KeySet();
    }

    /**
     * Adds the mapping from the provided key to the value.
     *
     * @param key
     * @param value
     *
     * @throws NullPointerException if the key is {@code null}
     * @throws IllegalArgumentException if the key is not an instance of {@link
     *         Integer}
     */
    @SuppressWarnings("unchecked")
    public V put(Integer key, V value) {

	int k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);

	if (index >= 0) {
	    V old = (V)(values[index]);
	    values[index] = value;
	    return old;
	}
	else {
	    int newIndex = 0 - (index + 1);	    
	    Object[] newValues = Arrays.copyOf(values, values.length + 1);
	    int[] newIndices = Arrays.copyOf(keyIndices, values.length + 1);

	    // shift the elements down to make room for the new value
	    for (int i = newIndex; i < values.length; ++i) {
		newValues[i+1] = values[i];
		newIndices[i+1] = keyIndices[i];
	    }

	    // insert the new value
	    newValues[newIndex] = value;
	    newIndices[newIndex] = k;

	    // switch the arrays with the lengthed versions
	    values = newValues;
	    keyIndices = newIndices;
	    
	    return null;
	}
    }

    /**
     * Removes the mapping for a key from this map if it is present and returns
     * the value to which this map previously associated the key, or {@code
     * null} if the map contained no mapping for the key.
     *
     * @param key key whose mapping is to be removed from the map 
     *
     * @return the previous value associated with key, or {@code null} if there
     * was no mapping for key.
     */
    @SuppressWarnings("unchecked")    
    public V remove(Object key) {
	int k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);

	if (index >= 0) {
	    V old = (V)(values[index]);

	    Object[] newValues = Arrays.copyOf(values, values.length - 1);
	    int[] newIndices = Arrays.copyOf(keyIndices, keyIndices.length - 1);

	    // shift the elements up to remove the values
	    for (int i = index; i < values.length - 1; ++i) {
		newValues[i] = values[i+1];
		newIndices[i] = keyIndices[i+1];
	    }

	    // update the arrays with the shorted versions
	    values = newValues;
	    keyIndices = newIndices;
	    return old;
	}

	return null;
    }

    /**
     * Returns the number of key-value mappings in this trie.
     */
    public int size() {
	return keyIndices.length;
    }

    /**
     * Returns a {@link Collection} view of the values contained in this map.
     */
    public Collection<V> values() {
	return new Values();
    }


    private class EntryIterator extends IntMapIterator<Map.Entry<Integer,V>> {

	public Map.Entry<Integer,V> next() {
	    return nextEntry();
	}
	
    }

    private class KeyIterator extends IntMapIterator<Integer> {

	public Integer next() {
	    return nextEntry().getKey();
	}
	
    }

    private class ValueIterator extends IntMapIterator<V> {

	public V next() {
	    return nextEntry().getValue();
	}
	
    }

    abstract class IntMapIterator<E> implements Iterator<E> {

	private int next;

	boolean lastRemoved;

	public IntMapIterator() {

	    next = 0;
	    lastRemoved = false;
	}

	public boolean hasNext() {
	    return next < size();
	}

	@SuppressWarnings("unchecked")
	public Entry<Integer,V> nextEntry() {
	    if (next >= size()) {
		throw new NoSuchElementException("no further elements");
	    }
	    int key = keyIndices[next];
	    V value = (V)(values[next]);
	    next++;
	    return new IntEntry(key, value);
	}
	
	// REMINDER: this class needs to work with the actual indices for the
	// key and value to avoid the logrithmic lookup
	public void remove() {
	    throw new UnsupportedOperationException();
	}
    }

    // REMINDER: this class needs to work with the actual indices for the key
    // and value to avoid the logrithmic lookup
    class IntEntry extends SimpleEntry<Integer,V> {

	private static final long serialVersionUID = 1L;
	
	public IntEntry(int key, V value) {
	    super(key, value);
	}

	public V setValue(V newValue) {
	    return IntegerMap.this.put(getKey(), newValue);
	}
    }

    class EntrySet extends AbstractSet<Entry<Integer,V>> {

	private static final long serialVersionUID = 1L;

	public void clear() {
	    clear();
	}

	public boolean contains(Object o) {
	    if (o instanceof Map.Entry) {
		Map.Entry e = (Map.Entry)o;
		Object key = e.getKey();
		Object val = e.getValue();
		Object mapVal = IntegerMap.this.get(key);
		return mapVal == val || (val != null && val.equals(mapVal));
	    }
	    return false;
	}

	public Iterator<Map.Entry<Integer,V>> iterator() {
	    return new EntryIterator();
	}
	
	public int size() {
	    return IntegerMap.this.size();
	}
    }

    class KeySet extends AbstractSet<Integer> {

	private static final long serialVersionUID = 1L;
	
	public KeySet() { }
	
	public void clear() {
	    IntegerMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsKey(o);
	}

	public Iterator<Integer> iterator() {
	    return new KeyIterator();
	}
	
	public boolean remove(Object o) {
	    return IntegerMap.this.remove(o) != null;
	}

	public int size() {
	    return IntegerMap.this.size();
	}

    }

    /**
     * A {@link Collection} view of the values contained in this trie.
     */
    private class Values extends AbstractCollection<V> {

	private static final long serialVersionUID = 1L;
	
	public void clear() {
	    IntegerMap.this.clear();
	}

	public boolean contains(Object o) {
	    return containsValue(o);
	}
	
	public Iterator<V> iterator() {
	    return new ValueIterator();
	}
	
	public int size() {
	    return IntegerMap.this.size();
	}
    }
    
}