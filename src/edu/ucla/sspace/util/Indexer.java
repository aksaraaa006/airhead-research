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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/** 
 * A utility class for mapping a set of objects to unique indices.
 */
public class Indexer<T> implements Iterable<Map.Entry<T,Integer>>,
                                   java.io.Serializable {

    private static final long serialVersionUID = 1L;

    private final Map<T,Integer> indices;

    public Indexer() {
        indices = new HashMap<T,Integer>();
    }

    public Indexer(Collection<? extends T> items) {
        this();
        for (T item : items)
            add(item);
    }

    public int add(T item) {
        Integer i = indices.get(item);
        if (i == null) {
            synchronized(indices) {                
                i = indices.get(item);
                if (i == null) {
                    i = indices.size();
                    indices.put(item, i);
                }
            }
        }
        return i;
    }

    public boolean contains(T item) {
        return indices.containsKey(item);
    }

    public int get(T item) {
        return indices.get(item);
    }

    public Iterator<Map.Entry<T,Integer>> iterator() {
        return Collections.unmodifiableSet(indices.entrySet()).iterator();
    }

    public int size() {
        return indices.size();
    }
}