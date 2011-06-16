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
import edu.ucla.sspace.util.Counter;
import edu.ucla.sspace.util.HashBiMap;
import edu.ucla.sspace.util.ObjectCounter;
import edu.ucla.sspace.util.OpenIntSet;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TreeMultiMap;


/**
 * An {@link IsomorphismTester} that only considers two graphs isomorphic if
 * there exists an isomoprhic mapping between their vertices that preserves the
 * edge types.  For non-{@link Multigraph} instances, this class assumes that
 * all edges have the same type.
 */
public class TypedIsomorphismTester 
        implements IsomorphismTester, java.io.Serializable {
    
    /*
     * IMPLEMENTATION NOTE:
     *
     * This class has the same functional implementaiton as the VF2 algorithm
     * but adds an another test to check whether the current mapping satisfies
     * the edge type constraints. 
     *
     */

    private static final long serialVersionUID = 1L;

    /**
     * Creates a new instance of a {@link TypedIsomorphismTester}
     */
    public TypedIsomorphismTester() { }

    /**
     * {@inheritDoc}
     */
    public Map<Integer,Integer> findIsomorphism(Graph<? extends Edge> g1, 
                                                Graph<? extends Edge> g2) {
        
        //System.out.printf("\n\nChecking:%n\t%s,%n\t%s%n", g1, g2);
        
        // If there are different number of nodes, or if the difference in
        // degrees would prevent a valid mapping, short circuit early and return
        // false.
        if (g1.order() != g2.order() || g1.size() != g2.size()) {
            // System.out.println("size and order differences");        
            return Collections.emptyMap();
        }

        // BASE CASE: check that both graphs are Multigraph instances since
        // we'll need to be comparing edge types
        if (g1 instanceof Multigraph && g2 instanceof Multigraph) {
            @SuppressWarnings("unchecked")
            Multigraph<?,? extends TypedEdge<?>> m1 = (Multigraph)g1;
            @SuppressWarnings("unchecked")
            Multigraph<?,? extends TypedEdge<?>> m2 = (Multigraph)g2;            
            try {
                return matchMultigraphs(m1, m2);
            } catch (ClassCastException cce) {
                //System.out.println("ClassCast1");        
                return Collections.emptyMap();
            }
        }
        // Otherwise see if the graph's edges are typed and so we wrap them as
        // Multigraphs
        else if (g1.size() > 0) {
            Edge e1 = g1.edges().iterator().next();
            Edge e2 = g2.edges().iterator().next();
            if (e1 instanceof TypedEdge && e2 instanceof TypedEdge) {
                // Cast to the appropriate edge type
                try {
                    @SuppressWarnings("unchecked")
                    Graph<TypedEdge<Object>> gt1 = (Graph<TypedEdge<Object>>)g1;
                    @SuppressWarnings("unchecked")
                    Graph<TypedEdge<Object>> gt2 = (Graph<TypedEdge<Object>>)g2;
                    Multigraph<?,? extends TypedEdge<?>> m1 = Graphs.asMultigraph(gt1);
                    Multigraph<?,? extends TypedEdge<?>> m2 = Graphs.asMultigraph(gt2);
                    //System.out.println("Testing");
                    return matchMultigraphs(m1, m2);
                } catch (ClassCastException cce) {
                    //System.out.println("ClassCast2");
                    return Collections.emptyMap();
                }
            }            
            else if (!(e1 instanceof TypedEdge) && !(e2 instanceof TypedEdge)) {
                // CASE: neither graph has edge types, so use the VF2 algorithm
                return new VF2IsomorphismTester().findIsomorphism(g1, g2);
            }
            // CASE: only one of the graphs has types, which probably indicates
            // the match can never happen.  Note that we might want to check
            // whether the graph with edge types has only one type
            else {
                //System.out.println("Type mismatch");
                return Collections.emptyMap();
            }
        }
        // This is really the case where there are no edges in the graph, which
        // could be sped up by just doing any arbitrary mapping between
        // vertices.
        return new VF2IsomorphismTester().findIsomorphism(g1, g2);
    }

    /**
     * {@inheritDoc}
     */
    public boolean areIsomorphic(Graph<? extends Edge> g1, Graph<? extends Edge> g2) {
        // findIsomorphism() will only return a non-empty set if there exists a
        // full mapping between the two graphs, so simply check for size to test
        // whether a mapping is possible
        return !findIsomorphism(g1, g2).isEmpty();
    }

    /**
     * Finds an isomorhphism between two {@link Multigraph} instances, returning
     * the vertex mapping or an empty map if no isomorphism exists.
     *
     * @throws ClassCastException if one of the Multigraph instances find some
     *         inconstistency with the other's types.  (This class's code will
     *         never throw the exception, only the Multigraph instances).  This
     *         exception might also be triggered if either of the parameters was
     *         wrapped using {@link Graphs#asMultigraph(Graph)} but the backing
     *         graph had mixed {@link Edge} types.
     */
    private Map<Integer,Integer> matchMultigraphs(Multigraph<?,? extends TypedEdge<?>> m1,
                                                  Multigraph<?,? extends TypedEdge<?>> m2) {
        // Test whether the two graphs have at least one type in common,
        // which ensures that there is at least some chance for a mapping.
        Set<?> m1types = m1.edgeTypes();
        Set<?> m2types = m2.edgeTypes();
        boolean foundMatch = false;
        for (Object o : m1types) {
            // As soon as we find at least one type match, start the search
            if (m2types.contains(o)) {
                return match(m1, m2, new HashBiMap<Integer,Integer>());
            }
        }
        //System.out.println("No shared types");        
        return Collections.emptyMap();
    }

    /**
     * Generates a set of candidate mappings from a vertex in {@code g1} to a
     * vertex in {@code g2}, using the an existing {@code mapping} for which
     * vertices have already been mapped.  This method applies a further constraint that two
     * vertices may be mapped only if their in-degree and out-degree are
     * equivalent.  Furthermore, if the two graphs contain a different number of
     * degrees, which implies that at least one vertex is unmappable, this
     * method returns the empty set to indicate that no mapping will satisify
     * the isomorphism constraint.
     *
     * @param mapping an existing mapping from vertices in {@code g1} to {@code
     *        g2}.  Vertices in this mapping will not be returned as candidate
     *        pairs
     */
    private Set<Pair<Integer>> generateCandidateMappings2(Multigraph<?,? extends TypedEdge<?>> g1,
                                                         Multigraph<?,? extends TypedEdge<?>> g2,
                                                         BiMap<Integer,Integer> mapping) {

        Set<Integer> unmappedG1Vertices = new HashSet<Integer>(g1.vertices());
        unmappedG1Vertices.removeAll(mapping.keySet());
        Set<Integer> unmappedG2Vertices = new HashSet<Integer>(g2.vertices());
        unmappedG2Vertices.removeAll(mapping.inverse().keySet());


        SortedMultiMap<Integer,Integer> degreeToG1Vertex
            = new TreeMultiMap<Integer,Integer>();
        for (Integer vertex : unmappedG1Vertices) {
            // System.out.printf("%d in g1 has degree %d%n", vertex, g1.getNeighbors(vertex).size());
            degreeToG1Vertex.put(g1.getNeighbors(vertex).size(), vertex);
        }
        
        SortedMultiMap<Integer,Integer> degreeToG2Vertex
            = new TreeMultiMap<Integer,Integer>();
        for (Integer vertex : unmappedG2Vertices) {
            //System.out.printf("%d in g2 has degree %d%n", vertex, g2.getNeighbors(vertex).size());
            degreeToG2Vertex.put(g2.getNeighbors(vertex).size(), vertex);
        }

        Set<Pair<Integer>> candidates = new HashSet<Pair<Integer>>();

        // If the number of degrees for each graphs vertices is different, then
        // one graph has at least one vertex that cannot be mapped to the other.
        // Therefore, prune early and return the empty set.
        if (degreeToG2Vertex.size() != degreeToG2Vertex.size())
            return Collections.<Pair<Integer>>emptySet();

        
        for (Integer degree : degreeToG1Vertex.keySet()) {
            Set<Integer> g1vertices = degreeToG1Vertex.get(degree);
            Set<Integer> g2vertices = degreeToG2Vertex.get(degree);

            // If the other graph didn't have any vertices with this degree,
            // then there won't be any full mapping, so return the empty set to
            // save work
            if (g2vertices == null)
                return Collections.<Pair<Integer>>emptySet();
            
            // Add all pair-wise combination of vertices that have the same
            // degree
            for (Integer i : g1vertices)
                for (Integer j : g2vertices)
                    candidates.add(new Pair<Integer>(i,j));

        }
        
        return candidates;
    }

    /**
     * Generates a set of candidate mappings from a vertex in {@code g1} to a
     * vertex in {@code g2}, using the an existing {@code mapping} for which
     * vertices have already been mapped.  This method applies a further constraint that two
     * vertices may be mapped only if their in-degree and out-degree are
     * equivalent.  Furthermore, if the two graphs contain a different number of
     * degrees, which implies that at least one vertex is unmappable, this
     * method returns the empty set to indicate that no mapping will satisify
     * the isomorphism constraint.
     *
     * @param mapping an existing mapping from vertices in {@code g1} to {@code
     *        g2}.  Vertices in this mapping will not be returned as candidate
     *        pairs
     */
    private Set<Pair<Integer>> generateCandidateMappings(Multigraph<?,? extends TypedEdge<?>> g1,
                                                         Multigraph<?,? extends TypedEdge<?>> g2,
                                                         BiMap<Integer,Integer> mapping) {

        Set<Integer> unmappedG1Vertices = new OpenIntSet(g1.vertices());
        unmappedG1Vertices.removeAll(mapping.keySet());
        Set<Integer> unmappedG2Vertices = new OpenIntSet(g2.vertices());
        unmappedG2Vertices.removeAll(mapping.inverse().keySet());

        OpenIntSet[] degreeToG1Vertex = new OpenIntSet[g1.order() - 1];
        OpenIntSet[] degreeToG2Vertex = new OpenIntSet[g2.order() - 1];
        
        int uniqueG1degrees = 0;
        int uniqueG2degrees = 0;

        for (Integer vertex : unmappedG1Vertices) {
            int degree = g1.getNeighbors(vertex).size();
            OpenIntSet verticesWithDegree = degreeToG1Vertex[degree];
            if (verticesWithDegree == null) {
                verticesWithDegree = new OpenIntSet();
                degreeToG1Vertex[degree] = verticesWithDegree;
                uniqueG1degrees++;
            }
            verticesWithDegree.add(vertex);
        }

        for (Integer vertex : unmappedG2Vertices) {
            int degree = g2.getNeighbors(vertex).size();
            OpenIntSet verticesWithDegree = degreeToG2Vertex[degree];
            if (verticesWithDegree == null) {
                verticesWithDegree = new OpenIntSet();
                degreeToG2Vertex[degree] = verticesWithDegree;
                uniqueG2degrees++;
            }
            verticesWithDegree.add(vertex);
        }
        

        Set<Pair<Integer>> candidates = new HashSet<Pair<Integer>>();

        // If the number of degrees for each graphs vertices is different, then
        // one graph has at least one vertex that cannot be mapped to the other.
        // Therefore, prune early and return the empty set.
        if (uniqueG1degrees != uniqueG2degrees)
            return Collections.<Pair<Integer>>emptySet();

        
        for (int degree = 0, seen = 0; degree < degreeToG1Vertex.length && seen < uniqueG1degrees; ++degree) {
        
            OpenIntSet g1vertices = degreeToG1Vertex[degree];
            OpenIntSet g2vertices = degreeToG2Vertex[degree];

            // If neither graph had this vertex, continue looking
            if (g1vertices == null && g2vertices == null)
                continue;

            // If the other graph didn't have any vertices with this degree, or
            // if they had different number of vertices with this degree, then
            // there won't be any possible full mapping, so return the empty set
            // to save work
            if ((g1vertices == null || g2vertices == null)
                    || (g1vertices.size() != g2vertices.size()))
                return Collections.<Pair<Integer>>emptySet();
            
            // Add all pair-wise combination of vertices that have the same
            // degree
            for (Integer i : g1vertices) 
                for (Integer j : g2vertices)
                    candidates.add(new Pair<Integer>(i,j));

            // Update that we've seen another unique degree
            seen++;
        }
        
        return candidates;
    }


    /**
     * Returns {@code true} if adding the candidate mapping to the current
     * mapping would still constitute a valid isomorphic mapping between g1 and
     * g2.
     */
    private boolean isFeasible(Multigraph<?,? extends TypedEdge<?>> g1,
                               Multigraph<?,? extends TypedEdge<?>> g2,
                               BiMap<Integer,Integer> mapping, 
                               Pair<Integer> candidate) {
        return typeMatch(g1, g2, mapping, candidate)
            && rPred(g1, g2, mapping, candidate)
            && rSucc(g1, g2, mapping, candidate)
            && rIn(g1, g2, mapping, candidate)
            && rOut(g1, g2, mapping, candidate)
            && rNew(g1, g2, mapping, candidate);        
//         boolean type = typeMatch(g1, g2, mapping, candidate);
//         boolean rPred = rPred(g1, g2, mapping, candidate);
//         boolean rSucc = rSucc(g1, g2, mapping, candidate);
//         boolean rIn = rIn(g1, g2, mapping, candidate);
//         boolean rOut = rOut(g1, g2, mapping, candidate);
//         boolean rNew = rNew(g1, g2, mapping, candidate);
        
//         System.out.printf("%s -- type: %s, rPred: %s, rSucc: %s, rIn: %s, rOut: %s, rNew: %s%n",
//                           candidate, type, rPred, rSucc, rIn, rOut, rNew);
//         return type && rPred && rSucc && rIn && rOut && rNew;

    }

    /**
     * Returns {@code true} if the provided mapping can be used to generate a
     * valid isomorphism between g1 and g2.
     */
    private Map<Integer,Integer> match(Multigraph<?,? extends TypedEdge<?>> g1,
                                       Multigraph<?,? extends TypedEdge<?>> g2, 
                                       BiMap<Integer,Integer> mapping) {
        // BASE CASE: If the mapping is complete (maps all the vertices), then
        // we have a valid isomorphism, so return true.
        if (mapping.size() == g2.order()) {
            // System.out.printf("found lenght match: " + mapping);
            return mapping;
        }
        
        // Generate the candidate pairs based on the set of currently unmapped
        // vertices
        Set<Pair<Integer>> candidates = 
            generateCandidateMappings(g1, g2, mapping);

        // Evaluate each of the candidate pairs, seeing if adding the mapping
        // would generate a valid isomorphism
        for (Pair<Integer> p : candidates) {
            if (isFeasible(g1, g2, mapping, p)) {
                // System.out.println(p + " is feasible");
                // Add the candidate mapping
                mapping.put(p.x, p.y);
                // if no match was found, restore the prior mapping
                // state in order to test the next one.
                if (match(g1, g2, mapping).isEmpty())
                    mapping.remove(p.x);
                // Otherwise, if the recursive match calls found that this
                // resulted in a complete solution, then propagate the result.
                else
                    return mapping;
            }
            //System.out.println(p + " is NOT feasible");
        }

        //System.out.println("no feasible candidates");
        return Collections.<Integer,Integer>emptyMap();
    }


    /**
     * Returns {@code true} if adding the candidate mapping would still preserve
     * the type constraints between the graphs
     */
    private boolean typeMatch(Multigraph<?,? extends TypedEdge<?>> g1, 
                              Multigraph<?,? extends TypedEdge<?>> g2, 
                              BiMap<Integer,Integer> mapping,
                              Pair<Integer> candidate) {
        // Consider all the new edges that are added with this candidate pair
        int x = candidate.x;
        int y = candidate.y;

        // REMINDER: there might be a more efficient way to iterate over the
        // edges by varying the behavior based on how large the mapping is
        // compared to the number of neighbors the candidate has.

        // Iterate over all the mapped vertices, checking whether the edges
        // connecting to the vertices in candidate mapping have the same type.
        for(Map.Entry<Integer,Integer> ent : mapping.entrySet()) {
            // Find the edges in g1 and g2 that are already mapped and connect
            // to the candidate mapping
            Set<? extends TypedEdge<?>> edges1 = g1.getEdges(ent.getKey(), x);
            Set<? extends TypedEdge<?>> edges2 = g2.getEdges(ent.getValue(), y);
            
            // Check that this mapping has the same type distribution
            Counter<Object> edgeTypes1 = new ObjectCounter<Object>();
            Counter<Object> edgeTypes2 = new ObjectCounter<Object>();
            for (TypedEdge<?> e : edges1)
                edgeTypes1.count(e.edgeType());
            for (TypedEdge<?> e : edges2)
                edgeTypes2.count(e.edgeType());

            // If the counters showed different number of types in each of the
            // connecting sets, the this candidate cannot match.
            if (!edgeTypes1.equals(edgeTypes2)) {
                return false;
            }
        }

        // If all the edges that are connected to this candidate have the same
        // type distribution, then the candiate could match.
        return true;
    }
        

    private boolean rPred(Multigraph<?,? extends TypedEdge<?>> g1, 
                          Multigraph<?,? extends TypedEdge<?>> g2, 
                          BiMap<Integer,Integer> mapping,
                          Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // have already been mapped
        Set<Integer> nPred = new HashSet<Integer>(predecessors(g1, n));
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
        Set<Integer> mPred = new HashSet<Integer>(predecessors(g2, m));
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

    private boolean rSucc(Multigraph<?,? extends TypedEdge<?>> g1, 
                          Multigraph<?,? extends TypedEdge<?>> g2, 
                          BiMap<Integer,Integer> mapping,
                          Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // have already been mapped
        Set<Integer> nSucc = new HashSet<Integer>(successors(g1, n));
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
        Set<Integer> mSucc = new HashSet<Integer>(successors(g2, m));
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

    private boolean rIn(Multigraph<?,? extends TypedEdge<?>> g1, 
                        Multigraph<?,? extends TypedEdge<?>> g2, 
                        BiMap<Integer,Integer> mapping,
                        Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // are note yet mapped in g1
        Set<Integer> nSucc = new HashSet<Integer>(successors(g1, n));
        Set<Integer> tIn1 = findTIn(g1, mapping.keySet());
        nSucc.retainAll(tIn1);

        // Compute the intersection of the sucessors of m and the nodes that
        // are note yet mapped in g2
        Set<Integer> mSucc = new HashSet<Integer>(successors(g2, m));
        Set<Integer> tIn2 = findTIn(g2, mapping.inverse().keySet());
        mSucc.retainAll(tIn2);

        if (nSucc.size() != mSucc.size())
            return false;
        
        Set<Integer> nPred = new HashSet<Integer>(predecessors(g1, n));
        nPred.retainAll(tIn1);
        Set<Integer> mPred = new HashSet<Integer>(predecessors(g2, m));
        mPred.retainAll(tIn2);

        return nPred.size() == mPred.size();
    }

    private boolean rOut(Multigraph<?,? extends TypedEdge<?>> g1, 
                         Multigraph<?,? extends TypedEdge<?>> g2, 
                         BiMap<Integer,Integer> mapping,
                         Pair<Integer> candidate) {
        // Rename to follow algorithm's notation in the paper
        int n = candidate.x;
        int m = candidate.y;

        // Compute the intersection of the sucessors of n and the nodes that
        // are note yet mapped in g1
        Set<Integer> nSucc = new HashSet<Integer>(successors(g1, n));
        Set<Integer> tOut1 = findTOut(g1, mapping.keySet());
        nSucc.retainAll(tOut1);

        // Compute the intersection of the sucessors of m and the nodes that
        // are note yet mapped in g2
        Set<Integer> mSucc = new HashSet<Integer>(successors(g2, m));
        Set<Integer> tOut2 = findTOut(g2, mapping.inverse().keySet());
        mSucc.retainAll(tOut2);

        if (nSucc.size() != mSucc.size())
            return false;
        
        Set<Integer> nPred = new HashSet<Integer>(predecessors(g1, n));
        nPred.retainAll(tOut1);
        Set<Integer> mPred = new HashSet<Integer>(predecessors(g2, m));
        mPred.retainAll(tOut2);

        return nPred.size() == mPred.size();
    }
    
    private boolean rNew(Multigraph<?,? extends TypedEdge<?>> g1, 
                         Multigraph<?,? extends TypedEdge<?>> g2, 
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
        nodes2.removeAll(mapping.inverse().keySet());
        nodes2.removeAll(findTOut(g2, mapping.inverse().keySet()));
        nodes2.removeAll(findTIn(g2, mapping.inverse().keySet()));

        
        // Create temporary copies of the N-tilde sets so that we can mutate
        Set<Integer> n1tmp = new HashSet<Integer>(nodes1);
        Set<Integer> n2tmp = new HashSet<Integer>(nodes2);
        n1tmp.retainAll(predecessors(g1, n));
        n2tmp.retainAll(predecessors(g2, m));

        if (n1tmp.size() != n2tmp.size()) {
            //System.out.printf("%s != %s%n", n1tmp, n2tmp);
            return false;
        }

        nodes1.retainAll(successors(g1, n));
        nodes2.retainAll(successors(g2, m));
        // System.out.printf("%s != %s%n", nodes1.size(), nodes2.size());
        return nodes1.size() == nodes2.size();        
    }

    /**
     * Returns the set of vertices that are not currently mapped, but are
     * reachable via out edges from the set of currently mapped vertices.  Note
     * that for undirected graphs, this set is made of all adjacent vertices
     * that are not currently in the mapping.
     */
    private <T,E extends TypedEdge<T>> Set<Integer> findTOut(Multigraph<T,E> g, Set<Integer> mapped) {
        Set<Integer> tOut = new HashSet<Integer>();
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
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
                Set<Integer> adjacent = g.getNeighbors(i);
                // some vertices may not have any other adjacent vertices
                if (adjacent == null)
                    continue;
                for (Integer adj : adjacent) {
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
    private Set<Integer> findTIn(Multigraph<?,? extends TypedEdge<?>> g, Set<Integer> mapped) {
        Set<Integer> tIn = new HashSet<Integer>();
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
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

    /**
     * Returns those vertices that can be reached from {@code vertex} or the empty
     * set if {@code g} is not a directed graph.
     */
    private Set<Integer> successors(Multigraph<?,? extends TypedEdge<?>> g, Integer vertex) {
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
            return dg.successors(vertex);
        }
        else
            return new HashSet<Integer>();
    }

    /**
     * Returns those vertices that point to from {@code vertex} or all the
     * adjacent vertices if {@code g} is not a directed graph.
     */
    @SuppressWarnings("unchecked")
    private Set<Integer> predecessors(Multigraph<?,? extends TypedEdge<?>> g, Integer vertex) {
        // The DirectedGraph cast seems to change the return type of the set
        // from Set<Integer> to just Set
        if (g instanceof DirectedGraph) {
            DirectedGraph<?> dg = (DirectedGraph)g;
            return dg.predecessors(vertex);
        }
        else 
            return g.getNeighbors(vertex);
    }
}