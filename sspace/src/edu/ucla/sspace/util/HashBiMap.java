/*
 * Copyright 2010 Keith Stevens
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

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


/**
 * A {@link HashMap} based implementation of a {@link BiMap}.  Given an existing
 * {@link Map}, all values will be mapped to their original keys in a new hash
 * map.
 *
 * @author Keith Stevens
 */
public class HashBiMap<K, V> implements BiMap<K, V> {

    /**
     * The original mapping.
     */
    private Map<K, V> originalMap;

    /**
     * An inverse mapping from values to keys.
     */
    private BiMap<V, K> reverseMap;

    /**
     * Creates an empty {@link HashBiMap}.
     */
    public HashBiMap() {
        reverseMap = new HashBiMap<V, K>(new HashMap<V, K>(), this);
    }

    /**
     * Creates a new {@link HashBiMap} from an existing {@link Map}
     */
    public HashBiMap(Map<K, V> map) {
        originalMap = map;

        // Iterate through the original map and create the inverse mappings.
        Map<V, K> otherMap = new HashMap<V, K>();
        for (Map.Entry<K, V> entry : map.entrySet())
            otherMap.put(entry.getValue(), entry.getKey());
        reverseMap = new HashBiMap<V, K>(otherMap, this);
    }

    /**
     * Internally creates a {@link BiMap} that is the reversed form of an
     * existing {@link BiMap}.  The original mapping is not recomputed for the
     * {@link BiMap} created in public  above constructor.
     */
    private HashBiMap(Map<K, V> map, BiMap<V, K> reverse) {
        this.originalMap = map;
        this.reverseMap = reverse;
    }

    /**
     * {@inheritDoc}
     */
    public void clear() {
        originalMap.clear();
        reverseMap.clear();
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsKey(Object key) {
        return originalMap.containsKey(key);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsValue(Object key) {
        return originalMap.containsValue(key);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Map.Entry<K, V>> entrySet() {
        return originalMap.entrySet();
    }

    /**
     * {@inheritDoc}
     */
    public boolean equals(Object o) {
        return originalMap.equals(o);
    }

    /**
     * {@inheritDoc}
     */
    public V get(Object o) {
        return originalMap.get(o);
    }

    /**
     * {@inheritDoc}
     */
    public int hashCode() {
        return originalMap.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    public BiMap<V, K> inverse() {
        return reverseMap;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isEmpty() {
        return originalMap.isEmpty();
    }

    /**
     * {@inheritDoc}
     */
    public Set<K> keySet() {
        return originalMap.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public V put(K key, V value) {
        reverseMap.put(value, key);
        return originalMap.put(key, value);
    }

    /**
     * {@inheritDoc}
     */
    public void putAll(Map<? extends K, ? extends V> m) {
        originalMap.putAll(m);
        for (Map.Entry<? extends K,? extends V> e : m.entrySet())
            reverseMap.put(e.getValue(), e.getKey());
    }

    /**
     * {@inheritDoc}
     */
    public V remove(Object key) {
        V removed = originalMap.remove(key);
        if (removed != null)
            reverseMap.remove(removed);
        return removed;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        return originalMap.size();
    }

    /**
     * {@inheritDoc}
     */
    public Collection<V> values() {
        return originalMap.values();
    }
}
