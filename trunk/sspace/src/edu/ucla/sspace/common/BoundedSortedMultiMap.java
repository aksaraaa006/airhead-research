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
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import edu.ucla.sspace.common.BoundedSortedMap.ReverseComparator;

/**
 * A {@code MultiMap} implementation that grows to a fixed size and then retains only
 * a fixed number of either keys or mappings.  All keys used in this class
 * must implements {@link Comparable}.
 * 
 * @see BoundedSortedMap
 */
public class BoundedSortedMultiMap<K,V> extends TreeMultiMap<K,V> {

    private static final long serialVersionUID = 1;

    /**
     * The maximum number of keys or values to key, depending on how the bound
     * is applied.
     */
    private final int bound;

    /**
     * {@code true} if the bound should apply to the number of keys, or {@code
     * false} if the number of value
     */
    private final boolean isKeyBound;

    /**
     * Creates an instance that will retain the specified number of mappings
     * that are associated with the highest keys.
     *
     * @param bound the maximum number of key-value mappings to retain
     */
    public BoundedSortedMultiMap(int bound) {
	this(bound, false, true);
    }

    /**
     * Creates an instance with the specified bound that will retain either the
     * specified number of highest keys if {@code keyBound} is {@code true}, or
     * the total number of mappings with the highest keys if {@code keyBound} is
     * {@code false}.  If {@code keyBound} is false, an arbitrary mapping for
     * the lowest key will be removed.
     *
     * @param bound the number to retain
     * @param keyBound {@code true} if the bound should apply to the number of
     *        keys, regardless of the number of values to which those keys map,
     *        or {@code false} if the bound should apply to the total number of
     *        key-value mappings in this map
     */
    public BoundedSortedMultiMap(int bound, boolean keyBound) {
	this(bound, keyBound, true);
    }

    /**
     * Creates an instance with the specified bound that will retain either the
     * specified number of keys if {@code keyBound} is {@code true}, or the
     * total number of mappings if {@code keyBound} is {@code false}.  If {@code
     * retainHighest} is {@code true}, the higest keys will be retained; else
     * the lowest keys will be be retained.  If {@code keyBound} is false, an
     * arbitrary mapping for the appropriate key will be removed.
     *
     * @param bound the number to retain
     * @param keyBound {@code true} if the bound should apply to the number of
     *        keys, regardless of the number of values to which those keys map,
     *        or {@code false} if the bound should apply to the total number of
     *        key-value mappings in this map
     * @param retainHighest {@code true} if the higest keys should be kept,
     *        {@code false} if the lowest keys should be kept
     */
    public BoundedSortedMultiMap(int bound, boolean keyBound, 
				 boolean retainHighest) {
	super(((retainHighest) ? null : new ReverseComparator<K>()));
	this.isKeyBound = keyBound;
	this.bound = bound;
    }

    /**
     * Adds the key-value mapping to this map, and if the total number of
     * mappings exceeds the bounds, removes either the currently lowest element,
     * or if reversed, the currently highest element.  If this map is bound by
     * the number of keys, a {@code put} operation may result in the {@code
     * range} decreasing, even though the number of keys stays constant.
     *
     * @param key {@inheritDoc}
     * @param value {@inheritDoc}
     */
    public boolean put(K key, V value) {
	boolean added = super.put(key, value);
	if (isKeyBound) {
	    if (size() > bound) {
		remove(firstKey());
	    } 
	}
	else if (range() > bound) {
	    K first = firstKey();
	    Set<V> values = get(first);
	    // Arbitrarily remove the first key in the set.  Note that the set
	    // is guaranteed to be non-empty, so the iterator call to next()
	    // will always succeed.
	    remove(first, values.iterator().next());
	}
	return added;
    }

    /**
     * Adds all of the key-value mapping to this map, and if the total number of
     * mappings exceeds the bounds, removes either the currently lowest element,
     * or if reversed, the currently highest element.  If this map is bound by
     * the number of keys, a {@code put} operation may result in the {@code
     * range} decreasing, even though the number of keys stays constant.
     *
     * @param key {@inheritDoc}
     * @param values {@inheritDoc}
     */
    public boolean putMulti(K key, Collection<V> values) {
	boolean added = false;
	for (V v : values) {
	    if (put(key, v))
		added = true;
	}
	return added;
    }

    /**
     * Adds all of the key-value mapping to this map, and if the total number of
     * mappings exceeds the bounds, removes either the currently lowest element,
     * or if reversed, the currently highest element.  If this map is bound by
     * the number of keys, a {@code put} operation may result in the {@code
     * range} decreasing, even though the number of keys stays constant.
     *
     * @param m {@inheritDoc}
     */
    public void putAll(Map<? extends K,? extends V> m) {
	for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
	    put(e.getKey(), e.getValue());
	}
    }
}
