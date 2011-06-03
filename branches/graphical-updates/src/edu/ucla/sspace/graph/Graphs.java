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

    public static <E extends DirectedEdge> DirectedGraph<E> asDirectedGraph(Graph<E> g) {
        if (g == null)
            throw new NullPointerException();
        return (g instanceof DirectedGraph)
            ? (DirectedGraph<E>)g
            : new DirectedGraphAdaptor<E>(g);
    }

    public static <E extends WeightedEdge> WeightedGraph<E> asWeightedGraph(Graph<E> g) {
        throw new Error();
    }

    public static <T,E extends TypedEdge<T>> Multigraph<T,E> asMultigraph(Graph<E> g) {
        if (g == null)
            throw new NullPointerException();
        if (g instanceof Multigraph) {
            @SuppressWarnings("unchecked")
            Multigraph<T,E> m = (Multigraph<T,E>)g;
            return m;
        }
        else
            return new MultigraphAdaptor<T,E>(g);
    }

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
     * @return the total number of times an edge's endpoint was swapped with
     *         another edge's endpoint.  At its maximum value, this will be
     *         {@code shufflesPerEdge * g.size()} assuming that each swap was
     *         successful.  For dense graphs, this return value will be much
     *         less.
     *
     * @throws IllegalArgumentException if {@code shufflesPerEdge} is
     *         non-positive
     */
    public static <T extends Edge> int shufflePreserve(Graph<T> g, 
                                                       int shufflesPerEdge) {
        if (shufflesPerEdge < 1)
            throw new IllegalArgumentException("must shuffle at least once");

        System.out.printf("Shuffling %d edges%n", g.size());
        int totalShuffles = 0;

        List<T> edges = new ArrayList<T>(g.edges());
        int numEdges = edges.size();
        for (int i = 0; i < numEdges; ++i) {
            
            for (int swap = 0; swap < shufflesPerEdge; ++swap) {
//                 System.out.println("Current graph: " + g);

                // Pick another vertex to conflate with i that is not i
                int j = i; 
                while (i == j)
                    j = (int)(Math.random() * edges.size());                

                T e1 = edges.get(i);
                T e2 = edges.get(j);
                
                // For non-directed graphs, we should randomly flip the edge
                // orientation to guard against chases where some vertices only
                // appear on either to() or from().
                if (!(e1 instanceof DirectedEdge) && Math.random() < .5)
                    e1 = e1.<T>flip();
                if (!(e2 instanceof DirectedEdge) && Math.random() < .5)
                    e2 = e2.<T>flip();

//                 System.out.printf("Swapping edges %d and %d: %s, %s...", i, j, e1, e2);
                
                // Swap their end points
                T swapped1 = e1.<T>clone(e1.from(), e2.to());
                T swapped2 = e2.<T>clone(e2.from(), e1.to());
            
                // Check that the new edges do not already exist in the graph
                // and that they are not self edges
                if (g.contains(swapped1)) {
//                     System.out.println("Cannot swap. Graph already contains " + swapped1);
                    continue;
                }
                if (g.contains(swapped2)) {
//                     System.out.println("Cannot swap. Graph already contains " + swapped2);
                    continue;
                }
                else if (swapped1.from() == swapped1.to()
                         || swapped2.from() == swapped2.to()) {
//                     System.out.println("Cannot swap, self edge");
                    continue;
                }
//                 System.out.println("swap successful");
                totalShuffles++;
            
                // System.out.printf("Removing %s and %s...%n", edges.get(i), edges.get(j));

                // Remove the old edges
                boolean r1 = g.remove(edges.get(i));
                boolean r2 = g.remove(edges.get(j));
                
//                 System.out.println(r1 && r2);
                
                // Put in the swapped-end-point edges
                g.add(swapped1);
                g.add(swapped2);
                
                // Update the in-memory edges set so that if these edges are drawn
                // again, they don't point to old edges
                edges.set(i, swapped1);
                edges.set(j, swapped2);
                assert g.size() == numEdges : "Added an extra edge of either " + swapped1 + " or " + swapped2;
            }
        }
        return totalShuffles;
    }

    /**
     * Shuffles the edges of {@code g} while still preserving the <a
     * href="http://en.wikipedia.org/wiki/Degree_sequence#Degree_sequence">degree
     * sequence</a> of the graph and that edges are only swapped with those of
     * the same type.  Each edge in the graph will attempted to be conflated
     * with another edge in the graph the specified number of times.  If the
     * edge cannot be swapped (possible due to the new version of the edge
     * already existing), the attempt fails.
     *
     * <p> Note that the {@link Multigraph#subview(Set,Set)} method makes it
     * possilble to shuffle the edges for only a subset of the types in the
     * multigraph.
     *
     * @param g the graph whose elemets will be shuffled
     * @param shufflesPerEdge the number of swaps to attempt per edge.
     *
     * @return the total number of times an edge's endpoint was swapped with
     *         another edge's endpoint.  At its maximum value, this will be
     *         {@code shufflesPerEdge * g.size()} assuming that each swap was
     *         successful.  For dense graphs, this return value will be much
     *         less.
     *
     * @throws IllegalArgumentException if {@code shufflesPerEdge} is
     *         non-positive
     */
    public static <T,E extends TypedEdge<T>> int
              shufflePreserveType(Multigraph<T,E> g, int shufflesPerEdge) {

        if (shufflesPerEdge < 1)
            throw new IllegalArgumentException("must shuffle at least once");

        int totalShuffles = 0;
        Set<Integer> vertices = g.vertices();
        // Iterate through all of the types in the graph, shuffling only edges
        // of that type
        for (T type : g.edgeTypes()) {
            System.out.println("swapping edge of type " + type);
            Set<T> edgeType = Collections.singleton(type);
            // Get the view of the graph that only contains edges of the
            // specified type
            Multigraph<T,E> graphForType = g.subgraph(vertices, edgeType);
            // Shuffle the edges of that type only 
            totalShuffles += shufflePreserve(graphForType, shufflesPerEdge);
        }
        return totalShuffles;
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
                if (g.contains(e))
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