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

import java.util.Map;
import java.util.TreeMap;

/**
 * A {@code Map} implementation that grows to a fixed size and then retains only
 * the largest elements.
 */
public class BoundedSortedMap<K,V> extends TreeMap<K,V> {
  private final int bound;

  private static final long serialVersionUID = 1;

  public BoundedSortedMap(int bound) {
    super();
    this.bound = bound;
  }

  public V put(K key, V value) {
    V old = super.put(key, value);
    if (size() > bound) {
      remove(firstKey());
    }
    return old;
  }

  public void putAll(Map<? extends K,? extends V> m) {
    for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }
}
