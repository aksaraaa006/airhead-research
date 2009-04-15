package edu.ucla.sspace.common;

import java.util.Map;
import java.util.TreeMap;

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
      remove(lastKey());
    }
    return old;
  }

  public void putAll(Map<? extends K,? extends V> m) {
    for (Map.Entry<? extends K,? extends V> e : m.entrySet()) {
      put(e.getKey(), e.getValue());
    }
  }
}
