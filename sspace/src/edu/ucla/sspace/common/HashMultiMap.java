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

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class HashMultiMap<K,V> implements MultiMap<K,V> {

    /**
     * The backing map instance
     */
    private final Map<K,Set<V>> map;

    /**
     * The number of values mapped to keys
     */
    private int range;

    public HashMultiMap() {
	map = new HashMap<K,Set<V>>();
	range = 0;
    }
    
    /**
     * Constructs this map and adds in all the mapping from the provided {@code
     * Map}
     */
    public HashMultiMap(Map<? extends K,? extends V> m) {
	this();
	for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
	    put(e.getKey(), e.getValue());
	}
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
	map.clear();
	range = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
	return map.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> get(Object key) {
	return map.get(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
	return map.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
	return map.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> put(K key, V value) {
	Set<V> values = map.get(key);
	if (values == null) {
	    values = new HashSet<V>();
	    map.put(key, values);
	}
	if (values.add(value))
	    range++;
	return values;
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> putAll(K key, Collection<V> values) {
	Set<V> vals = map.get(key);
	if (vals == null) {
	    vals = new HashSet<V>();
	    map.put(key, vals);
	}
	int oldSize = vals.size();
	vals.addAll(values);
	range += (vals.size() - oldSize);
	return vals;
    }

    /**
     * {@inheritDoc}
     */
    public int range() {
	return range;
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> remove(Object key) {
	Set<V> v = map.remove(key);
	if (v != null)
	    range -= v.size();
	return v;
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeValue(K key, V value) {
	Set<V> values = map.get(key);
	boolean removed = values.remove(value);
	if (removed)
	    range--;
	// if this was the last value mapping for this key, remove the
	// key altogether
	if (values.size() == 0)
	    map.remove(key);
	return removed;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
	return map.size();
    }

    /**
     * {@inheritDoc}
     */
    public Set<V> values() {
	Set<V> values = new HashSet<V>();
	for (K key : map.keySet())
	    values.addAll(map.get(key));
	return values;
    }
    
}