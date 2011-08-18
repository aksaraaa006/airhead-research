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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;


/**
 * A view around one or more <i>disjoint</i> {@link Set} instances, which are
 * then presented as a single {@link Set}.  This class provides a way to take
 * multiple disjoint sets and treat them as a single instance for subsequent
 * operations without having to add the together or create a new {@code Set}
 * instance.
 *
 * <p> All methods run in time linear to the number of sets in the instance.
 * Unlike a {@link CombinedSet}, iterating over a {@code DisjointSets} is linear
 * in the number of elements in all sets, with no penalty for the number of sets
 * in the instance.
 *
 * @see CombinedSet
 */
public class DisjointSets<T> extends AbstractSet<T> 
        implements java.io.Serializable {
    
    private static final long serialVersionUID = 1L;

    /**
     * The disjoint sets that back this view.
     */
    private final List<Set<T>> sets;

    /**
     * Creates a wrapper around the disjoint sets
     */
    public DisjointSets(Collection<? extends Set<T>> sets) {
        if (sets == null)
            throw new NullPointerException("Sets cannot be null");
        this.sets = new ArrayList<Set<T>>();
        for (Set<T> s : sets) {
            if (s == null)
                throw new NullPointerException("Cannot wrap null set");
            this.sets.add(s);
        }
        assert areDisjoint() : "sets are not disjoint";
    }

    /**
     * A utility method of handling the assertion code, which returns true if
     * the sets are disjoint.
     */
    private boolean areDisjoint() {
        for (Set<T> s1 : sets) {
            for (Set<T> s2 : sets) {
                if (s1 == s2)
                    break;
                for (T t : s1)
                    if (s2.contains(t))
                        return false;
            }
        }
        return true;
    }

    /**
     * Adds the provided set to this {@code DisjointSets}.  Note that this does
     * <i>not</i> perform an element-wise addition of the contents of {@code
     * set}, but rather keeps a reference to {@code set} so that any changes to
     * {@code set} will be reflected in this {@code DisjointSets}.
     */
    public void append(Set<T> set) {
        if (set == null)
            throw new NullPointerException("set cannot be null");
        sets.add(set);
        assert areDisjoint() : "sets are not disjoint";
    }
    
    /**
     * Returns {@code true} if at least one of the sets in this {@code
     * DisjointSets} has the provided element.
     */
    public boolean contains(Object o) {
        for (Set<T> s : sets)
            if (s.contains(o))
                return true;
        return false;
    }

    /**
     * Clears the elements from all backing sets.
     */
    public void clear() {
        for (Set<T> s : sets)
            s.clear();
    }

    /**
     * Returns an iterator over all the unique items across all sets.
     */
    public Iterator<T> iterator() {
        List<Iterator<T>> iters = new ArrayList<Iterator<T>>(sets.size());
        for (Set<T> s : sets)
            iters.add(s.iterator());
        return new CombinedIterator<T>(iters);
    }

    /**
     * Returns {@code true} if all the sets are empty.
     */
    public boolean isEmpty() {
        for (Set<T> s : sets)
            if (!s.isEmpty())
                return false;
        return true;        
    }

    /**
     * Removes the provided element from the backing sets, returning {@code
     * true} if the element was in one set.
     */
    public boolean remove(Object o) {
        // Because the sets are disjoint, we can stop once we find the element
        // the first time
        for (Set<T> s : sets)
            if (s.remove(o))
                return true;
        return false;
    }

    /**
     * Returns the number of unique items across all sets. Note that this is not
     * a constant time operation, but rather is linear in the number of sets
     * that are represented by this instance.
     */
    public int size() {
        // Since the sets are disjoint, we can simple sum their sizes
        int size = 0;
        for (Set<T> s : sets)
            size += s.size();
        return size;
    }
}