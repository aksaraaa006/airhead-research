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

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.util.BiMap;
import edu.ucla.sspace.util.HashBiMap;
import edu.ucla.sspace.util.TreeMultiMap;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SortedMultiMap;


/**
 * An interface for algorithms that detect whether two graphs are <a
 * href="http://en.wikipedia.org/wiki/Graph_isomorphism">isomorphic</a>.
 */
//@SuppressWarnings("unchecked")
public class VF2IsomorphismTester implements IsomorphismTester {

    public VF2IsomorphismTester() { }

    /**
     * Returns {@code true} if the graphs are isomorphism of each other.
     */
    public boolean areIsomorphic(Graph2<? extends Edge> g1, 
                                 Graph2<? extends Edge> g2) {
        // If there are different number of nodes, or if the difference in
        // degrees would prevent a valid mapping, short circuit early and return
        // false.
        if (g1.numVertices() != g2.numVertices() 
                || g1.numEdges() != g2.numEdges())
            return false;
        return match(g1, g2, new HashBiMap<Integer,Integer>());
    }

    private Set<Pair<Integer>> generateCandidateMappings(
            Graph2<? extends Edge> g1, 
            Graph2<? extends Edge> g2,
            BiMap<Integer,Integer> mapping) {

        Set<Integer> unmappedG1Vertices = g1.vertices();
        unmappedG1Vertices.removeAll(mapping.keySet());
        Set<Integer> unmappedG2Vertices = g2.vertices();
        unmappedG2Vertices.removeAll(mapping.inverse().keySet());


        SortedMultiMap<Integer,Integer> degreeToG1Vertex
            = new TreeMultiMap<Integer,Integer>();
        for (Integer vertex : unmappedG1Vertices) {
            degreeToG1Vertex.put(g1.getAdjacentVertices(vertex).size(), vertex);
        }
        
        SortedMultiMap<Integer,Integer> degreeToG2Vertex
            = new TreeMultiMap<Integer,Integer>();
        for (Integer vertex : unmappedG2Vertices) {
            degreeToG1Vertex.put(g2.getAdjacentVertices(vertex).size(), vertex);
        }

        Set<Pair<Integer>> candidates = new HashSet<Pair<Integer>>();

        // If the number of degrees for each graphs vertices is different, then
        // one graph has at least one vertex that cannot be mapped to the other.
        // Therefore, prune early and return the empty set.
        if (degreeToG2Vertex.size() != degreeToG2Vertex.size())
            return new HashSet<Pair<Integer>>(); //Collections.EMPTY_SET;

        
        for (Integer degree : degreeToG1Vertex.keySet()) {
            Set<Integer> g1vertices = degreeToG1Vertex.get(degree);
            Set<Integer> g2vertices = degreeToG2Vertex.get(degree);

            // If the other graph didn't have any vertices with this degree,
            // then there won't be any full mapping, so return the empty set to
            // save work
            if (g2vertices == null)
                return new HashSet<Pair<Integer>>(); //Collections.EMPTY_SET;
            
            // Add all pair-wise combination of vertices that have the same
            // degree
            for (Integer i : g1vertices)
                for (Integer j : g2vertices)
                    candidates.add(new Pair<Integer>(i,j));

        }

        return candidates;
    }

    /**
     * Returns {@code true} if adding the candidate mapping to the current
     * mapping would still constitute a valid isomorphic mapping between g1 and
     * g2.
     */
    private boolean isFeasible(Graph2<? extends Edge> g1,
                               Graph2<? extends Edge> g2, 
                               BiMap<Integer,Integer> mapping, 
                               Pair<Integer> candidate) {
        return rPred(g1, g2, mapping, candidate)
            && rSucc(g1, g2, mapping, candidate)
            &&  rIn(g1, g2, mapping, candidate)
            && rOut(g1, g2, mapping, candidate)
            && rNew(g1, g2, mapping, candidate);
    }

    /**
     * Returns {@code true} if the provided mapping can be used to generate a
     * valid isomorphism between g1 and g2.
     */
    private boolean match(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                          BiMap<Integer,Integer> mapping) {
        // BASE CASE: If the mapping is complete (maps all the vertices), then
        // we have a valid isomorphism, so return true.
        if (mapping.size() == g2.vertices().size()) {
            return true;
        }
        
        // Generate the candidate pairs based on the set of currently unmapped
        // vertices
        Set<Pair<Integer>> candidates = 
            generateCandidateMappings(g1, g2, mapping);

        // Evaluate each of the candidate pairs, seeing if adding the mapping
        // would generate a valid isomorphism
        for (Pair<Integer> p : candidates) {
            if (isFeasible(g1, g2, mapping, p)) {
                // Add the candidate mapping
                mapping.put(p.x, p.y);
                // If the recursive match calls found that this resulted in a
                // complete solution, then propagate the result.
                if (match(g1, g2, mapping))
                    return true;
                // Otherwise, if no match was found, restore the prior mapping
                // state in order to test the next one
                else
                    mapping.remove(p.x);
            }
        }
        return false;
    }

    private boolean rPred(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                          BiMap<Integer,Integer> mapping,
                          Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // have already been mapped
        Set<Integer> nPred = predecessors(g1, n);
        nPred.retainAll(mapping.keySet());
        found_n_mapping:
        for (int nPrime : nPred) {

            Integer mapped = mapping.get(nPrime);
            if (mapped == null)
                return false;
            for (int mPrime : predecessors(g2, m)) {
                // We found at least one match for the candidate addition of n,
                // so continue with the next predecessor
                if (mPrime == mapped) 
                    continue found_n_mapping;
            }
            // None of the predecessors of m were mapped, so this does not
            // constititute a valid mapping
            return false;
        }

        // Calculate the intersection of the predecessors of m with the nodes in
        // the current mapping that are a part of g2
        Set<Integer> mPred = predecessors(g2, m);
        mPred.retainAll(mapping.values());
        
        found_m_mapping:
        for (int mPrime : mPred) {

            Integer mapped = mapping.inverse().get(mPrime);
            if (mapped == null)
                return false;
            for (int nPrime : predecessors(g1, n)) {
                // We found at least one match for the candidate addition of m,
                // so continue with the next predecessor
                if (nPrime == mapped) 
                    continue found_m_mapping;
            }
            // None of the predecessors of n were mapped, so this does not
            // constititute a valid mapping
            return false;
        }
        
        return true;
    }

    private boolean rSucc(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                          BiMap<Integer,Integer> mapping,
                          Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // have already been mapped
        Set<Integer> nSucc = successors(g1, n);
        nSucc.retainAll(mapping.keySet());
        found_n_mapping:
        for (int nPrime : nSucc) {

            Integer mapped = mapping.get(nPrime);
            if (mapped == null)
                return false;
            for (int mPrime : successors(g2, m)) {
                // We found at least one match for the candidate addition of n,
                // so continue with the next successor
                if (mPrime == mapped) 
                    continue found_n_mapping;
            }
            // None of the successors of m were mapped, so this does not
            // constititute a valid mapping
            return false;
        }

        // Calculate the intersection of the successors of m with the nodes in
        // the current mapping that are a part of g2
        Set<Integer> mSucc = successors(g2, m);
        mSucc.retainAll(mapping.values());
        
        found_m_mapping:
        for (int mPrime : mSucc) {

            Integer mapped = mapping.inverse().get(mPrime);
            if (mapped == null)
                return false;
            for (int nPrime : successors(g1, n)) {
                // We found at least one match for the candidate addition of m,
                // so continue with the next successor
                if (nPrime == mapped) 
                    continue found_m_mapping;
            }
            // None of the successors of n were mapped, so this does not
            // constititute a valid mapping
            return false;
        }
        
        return true;
    }

    private boolean rIn(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                        BiMap<Integer,Integer> mapping,
                        Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // are note yet mapped in g1
        Set<Integer> nSucc = successors(g1, n);
        Set<Integer> tIn1 = findTIn(g1, mapping.keySet());
        nSucc.retainAll(tIn1);

        // Compute the intersection of the sucessors of m and the nodes that
        // are note yet mapped in g2
        Set<Integer> mSucc = successors(g2, m);
        Set<Integer> tIn2 = findTIn(g2, mapping.inverse().keySet());
        mSucc.retainAll(tIn2);

        if (nSucc.size() != mSucc.size())
            return false;
        
        Set<Integer> nPred = predecessors(g1, n);
        nPred.retainAll(tIn1);
        Set<Integer> mPred = predecessors(g2, m);
        mPred.retainAll(tIn2);

        return nPred.size() == mPred.size();
    }

    private boolean rOut(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                         BiMap<Integer,Integer> mapping,
                         Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // are note yet mapped in g1
        Set<Integer> nSucc = successors(g1, n);
        Set<Integer> tOut1 = findTOut(g1, mapping.keySet());
        nSucc.retainAll(tOut1);

        // Compute the intersection of the sucessors of m and the nodes that
        // are note yet mapped in g2
        Set<Integer> mSucc = successors(g2, m);
        Set<Integer> tOut2 = findTOut(g2, mapping.inverse().keySet());
        mSucc.retainAll(tOut2);

        if (nSucc.size() != mSucc.size())
            return false;
        
        Set<Integer> nPred = predecessors(g1, n);
        nPred.retainAll(tOut1);
        Set<Integer> mPred = predecessors(g2, m);
        mPred.retainAll(tOut2);

        return nPred.size() == mPred.size();
    }
    
    private boolean rNew(Graph2<? extends Edge> g1, Graph2<? extends Edge> g2,
                         BiMap<Integer,Integer> mapping,
                         Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Create the N-tilde sets of nodes for both graphs 
        Set<Integer> nodes1 = new HashSet<Integer>(g1.vertices());
        nodes1.removeAll(mapping.keySet());
        nodes1.removeAll(findTOut(g1, mapping.keySet()));
        nodes1.removeAll(findTIn(g1, mapping.keySet()));
        
        Set<Integer> nodes2 = new HashSet<Integer>(g2.vertices());
        nodes1.removeAll(mapping.inverse().keySet());
        nodes1.removeAll(findTOut(g2, mapping.inverse().keySet()));
        nodes1.removeAll(findTIn(g2, mapping.inverse().keySet()));
        
        // Create temporary copies of the N-tilde sets so that we can mutate
        Set<Integer> n1tmp = new HashSet<Integer>(nodes1);
        Set<Integer> n2tmp = new HashSet<Integer>(nodes2);
        n1tmp.retainAll(predecessors(g1, n));
        n2tmp.retainAll(predecessors(g2, m));

        if (n1tmp.size() != n2tmp.size())
            return false;

        nodes1.retainAll(successors(g1, n));
        nodes2.retainAll(successors(g2, m));
        return nodes1.size() == nodes2.size();        
    }

    /**
     * Returns the set of vertices that are not currently mapped, but are
     * reachable via out edges from the set of currently mapped vertices.  Note
     * that for undirected graphs, this set is made of all adjacent vertices
     * that are not currently in the mapping.
     */
    private Set<Integer> findTOut(Graph2<? extends Edge> g, Set<Integer> mapped) {
        Set<Integer> tOut = new HashSet<Integer>();
        if (g instanceof DirectedGraph2) {
            DirectedGraph2 dg = (DirectedGraph2)g;
            for (Integer i : mapped) {
                for (Integer out : dg.successors(i)) {
                    if (!mapped.contains(out))
                        tOut.add(out);
                }
            }
        }
        else {
            // Since the graph is undirected, just include all of the edges from
            // the current mapping
            for (Integer i : mapped) { 
                for (Integer adj : g.getAdjacentVertices(i)) {
                    if (!mapped.contains(adj))
                        tOut.add(adj);
                }
            }
        }
        return tOut;
    } 

    /**
     * Returns the set of vertices that are not currently mapped and have out
     * edges that point to a vertex in the set of currently mapped vertices.
     * Note that for undirected graphs, this set is empty.
     */
    private Set<Integer> findTIn(Graph2 g, Set<Integer> mapped) {
        Set<Integer> tIn = new HashSet<Integer>();
        if (g instanceof DirectedGraph2) {
            DirectedGraph2 dg = (DirectedGraph2)g;
            for (Integer i : mapped) {
                for (Integer in : dg.predecessors(i)) {
                    if (!mapped.contains(in))
                        tIn.add(in);
                }
            }
        }
        else {
            // Degenerate case for T-In for undirect graphs.  The T-Out case
            // will handle all the necessary logic q
        }
        return tIn;
    } 

    private Set<Integer> successors(Graph2 g, Integer vertex) {
        return (g instanceof DirectedGraph2) 
            ? ((DirectedGraph2)g).successors(vertex)
            : new HashSet<Integer>();
    }

    @SuppressWarnings("unchecked") // FIXME
    private Set<Integer> predecessors(Graph2 g, Integer vertex) {
        return (g instanceof DirectedGraph2) 
            ? ((DirectedGraph2)g).predecessors(vertex)
            : g.getAdjacentVertices(vertex);
    }
}