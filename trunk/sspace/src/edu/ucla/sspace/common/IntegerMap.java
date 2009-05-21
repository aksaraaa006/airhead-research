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

@SuppressWarnings("unchecked")
public class IntegerMap<V> extends AbstractMap<Integer,V> {
    
    private static final long serialVersionUID = 1L;

    int[] keyIndices;
    Object[] values;

    public IntegerMap() {
	keyIndices = new int[0];
	values = new Object[0];
    }

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

    public void clear() {
	keyIndices = new int[0];
	values = new Object[0];
    }

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

    public Set<Entry<Integer,V>> entrySet() {
	return new EntrySet();
    }

    public V get(Object key) {
	int k = checkKey(key);
	int index = Arrays.binarySearch(keyIndices, k);
	return (index >= 0) ? (V)(values[index]) : null;
    }

    public Set<Integer> keySet() {
	return new KeySet();
    }

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

    public int size() {
	return keyIndices.length;
    }

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