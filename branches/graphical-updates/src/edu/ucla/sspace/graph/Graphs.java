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

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;


/**
 * A collection of static utility methods for interacting with {@link Graph}
 * instances.  This class is modeled after the {@link Collections} class.
 *
 * <p> Unless otherwise noted, all methods will throw an {@link
 * NullPointerException} if passed a {@code null} graph.
 */
public final class Graphs {

    private Graphs() { }

    /**
     * Shuffles the edges of {@code g} while still preserving the <a
     * href="http://en.wikipedia.org/wiki/Degree_sequence#Degree_sequence">degree
     * sequence</a> of the graph.  Each edge in the graph will attempted to be
     * conflated with another edge in the graph the specified number of times.
     * If the edge cannot be swapped (possible due to the new version of the
     * edge already existing), the attempt fails.
     *
     * @param g the graph whose elemets will be shuffled
     * @param shufflesPerEdge the number of swaps to attempt per edge.
     *
     * @throws IllegalArgumentException if {@code shufflesPerEdge} is
     *         non-positive
     */
    public static <T extends Edge> void shufflePreserve(Graph<T> g, 
                                                        int shufflesPerEdge) {
        if (shufflesPerEdge < 1)
            throw new IllegalArgumentException("must shuffle at least once");

        List<T> edges = new ArrayList<T>(g.edges());
        int numEdges = edges.size();
        for (int i = 0; i < numEdges; ++i) {
            
            for (int swap = 0; swap < shufflesPerEdge; ++swap) {
                // Pick another vertex to conflate with i that is not i
                int j = i; 
                while (i == j)
                    j = (int)(Math.random() * edges.size());

                T e1 = edges.get(i);
                T e2 = edges.get(j);
                
                // Swap their end points
                T swapped1 = e1.<T>clone(e1.from(), e2.to());
                T swapped2 = e2.<T>clone(e2.from(), e1.to());
            
                // Check that the new edges do not already exist in the graph
                if (g.containsEdge(swapped1) 
                        || g.containsEdge(swapped2))
                    continue;
            
                // Remove the old edges
                g.removeEdge(e1);
                g.removeEdge(e2);
                
                // Put in the swapped-end-point edges
                g.addEdge(swapped1);
                g.addEdge(swapped2);
                
                // Update the in-memory edges set so that if these edges are drawn
                // again, they don't point to old edges
                edges.set(i, swapped1);
                edges.set(j, swapped2);
            }
        }
    }


    /**
     * To-do
     */
    public static <T extends Edge> Graph<T> synchronizedGraph(Graph<T> g) {
        throw new Error();
    }

    /**
     * Returns a pretty-print string version of the graph as an adjacency matrix
     * where a 1 indicates an edge and a 0 indicates no edge.
     */
    public static String toAdjacencyMatrixString(Graph<?> g) {
        StringBuilder sb = new StringBuilder(g.order() * (g.order() + 1));
        for (Integer from : g.vertices()) {
            for (Integer to : g.vertices()) {
                Edge e = new SimpleDirectedEdge(from, to);
                if (g.containsEdge(e))
                    sb.append('1');
                else
                    sb.append('0');
            }
            sb.append('\n');
        }
        return sb.toString();
    }

    /**
     * To-do
     */
    public static <T extends Edge> Graph<T> unmodifiable(Graph<T> g) {
         throw new Error();
    }   
}