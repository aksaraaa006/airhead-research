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

package edu.ucla.sspace.graph;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.CombinedSet;
import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.graph.isomorphism.Matcher;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A special-purpose {@link Counter} that counts <b>multigraphs</b> based on <a
 * href="http://en.wikipedia.org/wiki/Graph_isomorphism">isomorphism</a>, rather
 * than object equivalence (which may take into account vertex labeling, etc.).
 * Most commonly, isomorphism is needed when counting the number of motifs in a
 * graph and their relative occurrences.
 */
public class TypedMotifCounter<T,G extends Multigraph<T,? extends TypedEdge<T>>>
        implements Counter<G>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The isomorphism tester used to find multigraph equality
     */
    private final IsomorphismTester isoTest;

    private final Map<Counter<T>,Map<G,Integer>> typesToGraphs;

    private int sum;
    
    /**
     * Creates a new {@code MotifCounter} with the default isomorphism tester.
     */ 
    public TypedMotifCounter() {
        this(new Matcher());
    }

    /**
     * Creates a new {@code MotifCounter} with the specified isomorphism tester.
     * Most users will not need this constructor, which is intended for special
     * cases where an {@link IsomorphismTester} is tailored to quickly match the
     * type of motifs being counted.
     */ 
    public TypedMotifCounter(IsomorphismTester isoTest) {
        this.isoTest = isoTest;
        typesToGraphs = new HashMap<Counter<T>,Map<G,Integer>>();
        sum = 0;
    }

    /**
     * Counts the number of isomorphic graphs in {@code c} and includes their
     * sum in this counter.
     */
    public void add(Counter<? extends G> c) {
        for (Map.Entry<? extends G,Integer> e : c) {
            count(e.getKey(), e.getValue());
        }
    }

    /**
     * Counts the isomorphic version of this graph, increasing the total by 1
     */
    public int count(G g) {
        return count(g, 1);
    }

    /**
     * Counts the isomorphic version of this graph, increasing its total count
     * by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(G g, int count) {
        if (count < 1)
            throw new IllegalArgumentException("Count must be positive");
        sum += count;
        Counter<T> typeCounts = new ObjectCounter<T>(g.edgeTypes());
        Map<G,Integer> graphs = typesToGraphs.get(typeCounts);
        if (graphs == null) {
            graphs = new HashMap<G,Integer>();
            typesToGraphs.put(typeCounts, graphs);
            graphs.put(g, count);
            return count;
        }
        else {
            for (Map.Entry<G,Integer> e : graphs.entrySet()) {
                if (isoTest.areIsomorphic(g, e.getKey())) {
                    int newCount = e.getValue() + count;
                    e.setValue(newCount);
                    return newCount;
                }
            }
            graphs.put(g, count);
            return count;
        }
    }    

    /**
     * Returns the count for graphs that are isomorphic to the provided graph.
     */
    public int getCount(G g) {
        Counter<T> typeCounts = new ObjectCounter<T>(g.edgeTypes());
        Map<G,Integer> graphs = typesToGraphs.get(typeCounts);
        if (graphs == null) 
            return 0;
        
        for (Map.Entry<G,Integer> e : graphs.entrySet()) {
            if (isoTest.areIsomorphic(g, e.getKey())) 
                return e.getValue();
        }
        return 0;
    }
    
    /**
     * {@inheritDoc}
     */
    public double getFrequency(G obj) {
        double count = getCount(obj);
        return (sum == 0) ? 0 : count / sum;
    }

    /**
     * {@inheritDoc}
     */
    public Set<G> items() {
        List<Set<G>> sets = new ArrayList<Set<G>>(typesToGraphs.size());
        for (Map<G,Integer> m : typesToGraphs.values())
            sets.add(m.keySet());
        return new CombinedSet<G>(sets);
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Map.Entry<G,Integer>> iterator() {
        List<Iterator<Map.Entry<G,Integer>>> iters = 
            new ArrayList<Iterator<Map.Entry<G,Integer>>>(typesToGraphs.size());
        for (Map<G,Integer> m : typesToGraphs.values())
            iters.add(m.entrySet().iterator());
        return new CombinedIterator<Map.Entry<G,Integer>>(iters);
    }
    
    /**
     * {@inheritDoc}
     */
    public G max() {
        int maxCount = -1;
        G max = null;
        for (Map<G,Integer> m : typesToGraphs.values()) { 
            for (Map.Entry<G,Integer> e : m.entrySet()) {
                if (e.getValue() > maxCount) {
                    maxCount = e.getValue();
                    max = e.getKey();
                }
            }
        }
        return max;
    }

    /**
     * {@inheritDoc}
     */
    public G min() {
        int minCount = Integer.MAX_VALUE;
        G min = null;
        for (Map<G,Integer> m : typesToGraphs.values()) {
            for (Map.Entry<G,Integer> e : m.entrySet()) {
                if (e.getValue() < minCount) {
                    minCount = e.getValue();
                    min = e.getKey();
                }
            }
        }
        return min;
    }

    /**
     * {@inheritDoc}
     */
    public void reset() {
        typesToGraphs.clear();
        sum = 0;
    }

    /**
     * {@inheritDoc}
     */
    public int size() {
        int sz = 0;
        for (Map<G,Integer> m : typesToGraphs.values())
            sz += m.size();
        return sz;
    }

    /**
     * {@inheritDoc}
     */
    public int sum() {
        return sum;
    }
}