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

import edu.ucla.sspace.util.Counter;


/**
 * A special-purpose {@link Counter} that counts graphs based on <a
 * href="http://en.wikipedia.org/wiki/Graph_isomorphism">isomorphism</a>, rather
 * than object equivalence (which may take into account vertex labeling, etc.).
 * Most commonly, isomorphism is needed when counting the number of motifs in a
 * graph and their relative occurrences.
 */
public class MotifCounter extends Counter<Graph<? extends Edge>> {

    private static final long serialVersionUID = 1L;

    /**
     * The isomorphism tester used to find graph equality
     */
    private final IsomorphismTester isoTest;
        
    /**
     * Creates a new {@code MotifCounter} with the default isomorphism tester.
     */ 
    public MotifCounter() {
        this(new VF2IsomorphismTester());
    }

    /**
     * Creates a new {@code MotifCounter} with the specified isomorphism tester.
     * Most users will not need this constructor, which is intended for special
     * cases where an {@link IsomorphismTester} is tailored to quickly match the
     * type of motifs being counted.
     */ 
    public MotifCounter(IsomorphismTester isoTest) {
        this.isoTest = isoTest;
    }

    /**
     * Counts the number of isomorphic graphs in {@code c} and includes their
     * sum in this counter.
     */
    public void add(Counter<Graph<? extends Edge>> c) {
        for (Graph<? extends Edge> g : c.items()) {
            int count = c.getCount(g);
            count(g, count);
        }
    }

    /**
     * Counts the isomorphic version of this graph, increasing the total by 1
     */
    public int count(Graph<? extends Edge> g) {
        for (Graph<? extends Edge> g2 : items()) {
            if (isoTest.areIsomorphic(g, g2)) {
                return super.count(g2);
            }
        }
        return super.count(g);
    }

    /**
     * Counts the isomorphic version of this graph, increasing its total count
     * by the specified positive amount.
     *
     * @param count a positive value for the number of times the object occurred
     *
     * @throws IllegalArgumentException if {@code count} is not a positive value.
     */
    public int count(Graph<? extends Edge> g, int count) {
        for (Graph<? extends Edge> g2 : items()) {
            if (isoTest.areIsomorphic(g, g2)) {
                return super.count(g2, count);
            }
        }
        return super.count(g, count);
    }

    /**
     * Returns the count for graphs that are isomorphic to the provided graph.
     */
    public int getCount(Graph<? extends Edge> g) {
        for (Graph<? extends Edge> g2 : items()) {
            if (isoTest.areIsomorphic(g, g2)) {
                return super.getCount(g2);
            }
        }
        return 0;
    }
}