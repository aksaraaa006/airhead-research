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

import java.util.AbstractSet;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import edu.ucla.sspace.util.BiMap;
import edu.ucla.sspace.util.HashBiMap;
import edu.ucla.sspace.util.IntegerMap;


/**
 * A undirected {@link Graph} implementation backed by a adjacency matrix.  This
 * class performs best for graphs with a small number of edges.
 *
 * @author David Jurgens
 */
public abstract class AbstractGraph<T extends Edge>
        implements Graph2<T>, java.io.Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The highest vertex seen thusfar in the graph.  This value is used by
     * Graph instances created with {@link #subgraph(Set)} to add new vertices
     * to the graph without overwriting existing vertices' indices.
     */
    private int highestVertex;

    /**
     * Records the number of modifications to the number of vertices within this
     * Graph.  Instances created with {@link #subgraph(Set)} use this to detect
     * structural changes that may invalidate their cached view.
     */
    private int mods;

    /**
     * The number of edges in this graph.
     */
    private int numEdges;

    /**
     * A mapping from a vertex's index to the the set of {@link Edge} instances
     * that connect it to other members of the graph.
     */
    private final Map<Integer,EdgeSet<T>> vertexToEdges;

    /**
     * Creates an empty {@code AbstractGraph}
     */
    public AbstractGraph() {
        mods = 0;
        vertexToEdges = new IntegerMap<EdgeSet<T>>();
    }    

    /**
     * Creates a new {@code AbstractGraph} with the provided set of vertices.
     */
    public AbstractGraph(Set<Integer> vertices) {
        this();
        for (Integer v : vertices)
            vertexToEdges.put(v, createEdgeSet(v));
    }    

    /**
     * Returns the {@link EdgeSet} for this vertex, adding the vertex to this
     * graph if abstent.
     */
    private EdgeSet<T> addIfAbsent(int v) {
        EdgeSet<T> edges = getEdgeSet(v);
        if (edges == null) {
            edges = createEdgeSet(v);
            vertexToEdges.put(v, edges);
            if (v > highestVertex)
                highestVertex = v;
            mods++;
        }
        return edges;
    }
        
    /**
     * {@inheritDoc}
     */
    public boolean addVertex(int v) {
        EdgeSet<T> edges = getEdgeSet(v);
        if (edges == null) {
            edges = createEdgeSet(v);
            vertexToEdges.put(v, edges);
            if (v > highestVertex)
                highestVertex = v;
            mods++;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     *
     * <p> This method is sensitive to the vertex ordering; a call will add the
     * edge to {@code vertex2} from the edge set for {@code vertex1}.
     * Subclasses should override this method if their {@link EdgeSet}
     * implementations are sensitive to the ordering of the vertex indices, or
     * if a more advanced behavior is needed.
     */
    public boolean addEdge(int vertex1, int vertex2) {
        EdgeSet<T> e1 = addIfAbsent(vertex1);
        addIfAbsent(vertex2);

        boolean isNew = e1.connect(vertex2);                   
        if (isNew) 
            numEdges++;
        
        return isNew;
    }

    /**
     * {@inheritDoc}
     *
     * <p> This method is sensitive to the vertex ordering; a call will add the
     * {@code e} to the edge set for {@code e.from()}.  Subclasses should
     * override this method if their {@link EdgeSet} implementations are
     * sensitive to the ordering of the vertex indices, or if a more advanced
     * behavior is needed.
     */
    public boolean addEdge(T e) {
        EdgeSet<T> e1 = addIfAbsent(e.from());
        addIfAbsent(e.to());

        boolean isNew = e1.add(e);                   
        if (isNew) 
            numEdges++;
        
        return isNew;
    }

    private void checkIndex(int vertex) {
        if (vertex < 0)
            throw new IllegalArgumentException("vertices must be non-negative");
    }

    /**
     * {@inheritDoc} 
     */
    public void clear() {
        vertexToEdges.clear();
        numEdges = 0;
        mods++;
    }

    /**
     * {@inheritDoc}
     */
    public void clearEdges() {
        for (EdgeSet<T> e : vertexToEdges.values())
            e.clear();
        numEdges = 0;
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsVertex(int vertex) {
        return vertexToEdges.containsKey(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean containsEdge(int vertex1, int vertex2) {
        EdgeSet<T> e1 = getEdgeSet(vertex1);
        return e1 != null && e1.connects(vertex1);
    }

    /**
     * {@inheritDoc}
     *
     * <p> This method is sensitive to the vertex ordering; a call will check
     * whether the edge set for {@code e.from()} contains {@code e}.  Subclasses
     * should override this method if their {@link EdgeSet} implementations are
     * sensitive to the ordering of the vertex indices, or if a more advanced
     * behavior is needed.
     */
    public boolean containsEdge(Edge e) {
        EdgeSet<T> e1 = getEdgeSet(e.from());
        return e1 != null && e1.contains(e);
    }

    /**
     * Returns a {@link EdgeSet} that will be used to store the edges of the
     * specified vertex
     */
    protected abstract EdgeSet<T> createEdgeSet(int vertex);

    /**
     * {@inheritDoc}
     */
    public Set<T> edges() {
        return new EdgeView();
    }

    /**
     * {@inheritDoc}
     */
    public Set<T> getAdjacencyList(int vertex) {
        return getEdgeSet(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getAdjacentVertices(int vertex) {
        EdgeSet<T> e = getEdgeSet(vertex);
        return (e == null) ? null : e.connected();
    }

    /**
     * {@inheritDoc}
     *
     * <p> This method is sensitive to the vertex ordering; a call will check
     * the edge to {@code vertex2} from the edge set for {@code vertex1}.
     * Subclasses should override this method if their {@link EdgeSet}
     * implementations are sensitive to the ordering of the vertex indices, or
     * if a more advanced behavior is needed.
     */
    public T getEdge(int vertex1, int vertex2) {
        EdgeSet<T> e = getEdgeSet(vertex1);
        return (e != null) ? e.getEdge(vertex2)
            : null;
    }

    /**
     * Returns the set of edges assocated with the vertex, or {@code null} if
     * this vertex is not in this graph.
     */
    private EdgeSet<T> getEdgeSet(int vertex) {
        return vertexToEdges.get(vertex);
    }

    /**
     * {@inheritDoc}
     */
    public boolean hasCycles() {
        throw new UnsupportedOperationException("fix me");
    }

    /**
     * {@inheritDoc}
     */
    public Iterator<Integer> iterator() {
        return new VertexSet(this, vertexToEdges.keySet()).iterator();
    }
    
    /**
     * An internal method used by subgraphs to add new vertices to this graph
     * without clobbering the existing vertices.
     *
     * @see #subgraph(Set)
     */
    int nextAvailableVertex() {
        return highestVertex + 1;
    }

    /**
     * {@inheritDoc}
     */
    public int numEdges() {
        return numEdges;
    }

    /**
     * {@inheritDoc}
     */
    public int numVertices() {
        return vertexToEdges.size();
    }

    /**
     * {@inheritDoc}  
     *
     * <p> This method is sensitive to the vertex ordering; a call will remove
     * the edge to {@code vertex2} from the edge set for {@code vertex1}.
     * Subclasses should override this method if their {@link EdgeSet}
     * implementations are sensitive to the ordering of the vertex indices, or
     * if a more advanced behavior is needed.
     */
    public boolean removeEdge(int vertex1, int vertex2) {
        EdgeSet<T> e = getEdgeSet(vertex1);
        int before = numEdges;
        if (e != null && e.disconnect(vertex2))
            numEdges--;
        return before != numEdges;        
    }

    /**
     * {@inheritDoc}  
     *
     * <p> This method is sensitive to the vertex ordering; a call will remove
     * the vertex for {@code e.to()} from the edge for {@code e.from()}.
     * Subclasses should override this method if their {@link EdgeSet}
     * implementations are sensitive to the ordering of the vertex indices, or
     * if a more advanced behavior is needed.
     */
    public boolean removeEdge(Edge e) {
        EdgeSet<T> edges = getEdgeSet(e.from());
        System.out.printf("Attempting to remove %s from %s%n", e, edges);
        int before = numEdges;
        if (edges != null && edges.remove(e))
            numEdges--;
        System.out.printf("success? %s%n", before != numEdges);
        return before != numEdges;        
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeVertex(int vertex) {
        EdgeSet<T> edges = vertexToEdges.remove(vertex);
        if (edges == null)
            return false;
        
        // Otherwise, we successfully removed the vertex
        mods++;
        // Discount all the edges that were stored in this vertices edge set
        numEdges -= edges.size();

        // Now find all the edges stored in other vertices that might point to
        // this vertex and remove them
        for (EdgeSet<T> e : vertexToEdges.values()) {
            if (e.disconnect(vertex))
                numEdges--;
        }

        // If we're removing the highest vertex, rather than do an O(n) search
        // for the next highest, just decrement the value, which is guaranteed
        // to work even though it might waste space.
        if (highestVertex == vertex)
            highestVertex--;

        return true;
    }
    
    /**
     * {@inheritDoc}
     */
    public Graph2<T> subgraph(Set<Integer> vertices) {
        return new Subgraph(vertices());
    }

    /**
     * Returns a description of the graph as the sequence of its edges.
     */
    public String toString() {
        StringBuilder sb = new StringBuilder(numEdges * 8);
        sb.append('{');
        for (EdgeSet<T> edges : vertexToEdges.values()) {
            for (T e : edges) 
                sb.append(e.toString()).append(',');
        }
        sb.setCharAt(sb.length() - 1, '}');
        return sb.toString();
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> vertices() {
        return new VertexSet(this, vertexToEdges.keySet());
    }

    /**
     * 
     */
    private static class VertexSet extends AbstractSet<Integer> {
        
        Set<Integer> vertices;

        Graph2 g;
        
        /**
         * Creates a new {@code VertexSet} that wraps calls to the {@code
         * vertices} Set with appropriate graph operations.
         *
         * @param g the graph containing the vertices
         * @param vertices The set of vertices in the set.  Note that this set
         *        is expected to change in contents as necessary with respect to
         *        the backing graph's vertices.
         */
        public VertexSet(Graph2 g, Set<Integer> vertices) {
            this.g = g;
            this.vertices = vertices;
        }

        public boolean add(Integer vertex) {
            return g.addVertex(vertex);
        }

        public boolean contains(Integer vertex) {
            return vertices.contains(vertex);
        }

        public Iterator<Integer> iterator() {
            return new VertexIterator();
        }

        public boolean remove(Integer i) {
            return g.removeVertex(i);
        }

        public int size() {
            return g.numVertices();
        }

        private class VertexIterator implements Iterator<Integer> {

            /**
             * All the vertices to return.
             */
            private Integer[] verts;

            /**
             * The next vertex to return
             */
            private int next;

            /**
             * Whether the previously returned vertex was already removed
             */
            private boolean wasRemoved;

            public VertexIterator() {
                // IMPLEMENTATION NOTE: in order to support remove(), we need to
                // wrap the remove all with a removeVertex().  However because
                // the vertices Set is also concurrently updated by the backing
                // Graph, we can't use an Iterator on that set AND call
                // removeVertex, as this would lead to a
                // ConcurrentModificationException during iteration.  (The
                // removeVertex call would change the set).  Therefore, we dump
                // all of the vertices at construction time into an array and
                // iterate over them, using the remove() method to call
                // removeVertex().
                verts = vertices.toArray(new Integer[g.numVertices()]);
                next = 0;
                wasRemoved = false;
            }

            public boolean hasNext() {
                return next < verts.length;
            }

            public Integer next() {
                if (next >= verts.length)
                    throw new NoSuchElementException();

                // Ensure that the next vertex we are about to return is still
                // in the backing graph.  If not, then skip it and keep going
                if (!g.containsVertex(verts[next]))
                    throw new ConcurrentModificationException(
                        "Vertex " + verts[next] + " is no longer in the graph");
                
                wasRemoved = false;
                return verts[next++];
            }

            public void remove() {
                if (wasRemoved)
                    throw new IllegalStateException("element already removed");
                else if (next == 0)
                    throw new IllegalStateException("no element to remove");
                // Remove the previously returned element
                g.removeVertex(verts[next-1]);
                wasRemoved = true;
            }
        }
    }

    /**
     * A wrapper around an {@link EdgeSet} that keeps track of any modifications
     * to the set so that the edge count state of the graph is kept current.
     */
    private class EdgeSetView extends AbstractSet<T> {
        
        private final EdgeSet<T> edges;

        public EdgeSetView(EdgeSet<T> edges) {
            this.edges = edges;
        }
        
        public boolean add(T e) {
            boolean wasAdded = edges.add(e);
            if (wasAdded)
                numEdges++;
            return wasAdded;
        }

        public boolean contains(Object o) {
            return edges.contains(o);
        }

        public Iterator<T> iterator() {
            return new EdgeSetViewIterator();
        }

        public boolean remove(Object o) {
            boolean wasRemoved = edges.remove(o);
            if (wasRemoved)
                numEdges--;
            return wasRemoved;
        }

        public int size() {
            return edges.size();
        }

        /**
         * A decorator around the {@code Iterator} for an {@link EdgeSet}, which
         * keeps track of any iterator-based removals and adjusts the edge count
         * in the graph accordingly.
         */
        private class EdgeSetViewIterator implements Iterator<T> {

            private final Iterator<T> backing;

            public EdgeSetViewIterator() {
                backing = edges.iterator();
            }

            public boolean hasNext() {
                return backing.hasNext();
            }

            public T next() {
                return backing.next();
            }

            public void remove() {
                backing.remove();
                // Decrement the size after the remove call in case the backing
                // EdgeSet doesn't support removal and would throw an exception.
                numEdges--;
            }
        }
    }
    
    /**
     * A view class that exposes the {@link Edge} information in this graph as a
     * {@link Set}.
     */
    private class EdgeView extends AbstractSet<T> {
        
        public EdgeView() { }

        public boolean add(T e) {
            return addEdge(e);
        }

        public boolean contains(Object o) {
            return (o instanceof Edge) && containsEdge((Edge)o);
        }

        public Iterator<T> iterator() {
            return new EdgeViewIterator();
        }

        public boolean remove(Object o) {
            return (o instanceof Edge) && removeEdge((Edge)o);
        }

        public int size() {
            return numEdges;
        }

        /**
         * A wrapper iterator around all the graph's {@link EdgeSet} iterators.
         * We use this class instead of a {@link
         * edu.ucla.sspace.util.CombinedIterator} in order to construct this
         * edge iterator on the fly (i.e., only one {@code Iterator} is held in
         * memory at a time), which offers some memory savings for graphs with a
         * large number of vertices or where the {@code EdgeSet} iterators have
         * a larger memory overhead.
         */
        private class EdgeViewIterator implements Iterator<T> {

            private final Iterator<EdgeSet<T>> vertices;
            
            private Iterator<T> edges;
            
            private Iterator<T> toRemoveFrom;

            public EdgeViewIterator() {
                vertices = vertexToEdges.values().iterator();
                advance();
            }
            
            private void advance() {
                while ((edges == null || !edges.hasNext())
                       && vertices.hasNext()) {
                    toRemoveFrom = edges;
                    edges = vertices.next().iterator();
                }
            }
            
            public boolean hasNext() {
                return edges.hasNext();
            }

            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                T next = edges.next();
                // Once we've returned an element from the set of edges, the
                // next call to remove() should remove from the current iterator
                if (toRemoveFrom != edges)
                    toRemoveFrom = edges;
                advance();
                return next;
            }
            
            public void remove() {
                if (toRemoveFrom == null)
                    throw new NoSuchElementException();
                else
                    toRemoveFrom.remove();
            }
        }

    }

    /**
     *
     *
     * All edge information is stored in the backing graph.  {@code Subgraph}
     * instances only retain information on which vertices are currently
     * represented as a part of the subgraph.  Changes to edge by other
     * subgraphs or to the backing graph are automatically detected and
     * incorporated at call time.
     */
    protected class Subgraph implements Graph2<T> {
        
        /**
         * A mapping from the index of a vertex in this subgraph to the vertex
         * in the backing map.  Note that even if this instance is a subgraph of
         * another {@code Subgraph}, this mapping is always to the backing
         * {@link AbstractGraph}.
         */
        private BiMap<Integer,Integer> vertexMapping;
        
        /**
         * The version for the last modification seen in the backing graph.
         * Instances can check against this value to see if the number of
         * vertices has changed since the state of this class was last seen.
         */
        private int lastModSeenInBacking;

        /**
         * The highest vertex seen in this subgraph, which is used to assign new
         * index numbers when a subgraph of this instance creates a new vertex.
         */
        private int highestVertexIndex;

        /**
         * If this instance is a subgraph of another {@link Subgraph}, this is
         * the parent.
         */
        private Subgraph parent;

        /**
         * Creates a new subgraph from the provided set of vertices
         */
        public Subgraph(Set<Integer> vertices) {
            this(vertices, null);
        }

        public Subgraph(Set<Integer> vertices, Subgraph parent) {       
    
            // Vertices that are added to this graph beyond what is originally
            // present are mapped to new vertex numbers in the backing graph
            vertexMapping = new HashBiMap<Integer,Integer>();

            // Each of the vertices that are participating in this subgraph is
            // remapped to a new contiguous sequence of vertex indices.  This
            // presents a consistent view of the graph as something "new",
            // especially in cases where a new vertex is added that may match an
            // index in the backing subgraph.
            for (Integer vertex : vertices)
                vertexMapping.put(vertexMapping.size(), vertex);

            this.parent = parent;
            highestVertexIndex = vertices.size() - 1;

            // Record the last modification seen to the graph
            lastModSeenInBacking = AbstractGraph.this.mods;
        }

        private void addVertexFromChild(int indexInBacking) {
            // Create a new vertex index that isn't present.
            int vIndex = ++highestVertexIndex;
            vertexMapping.put(vIndex, indexInBacking);

            // Propagate the new node up to our parent
            if (parent != null)
                parent.addVertexFromChild(indexInBacking);
        }

        
        /**
         * {@inheritDoc}
         */
        public boolean addVertex(int v) {
            checkForUpdates();
            Integer index = vertexMapping.get(v);
            if (index != null)
                return false;
            if (index > highestVertexIndex)
                highestVertexIndex = index;
            
            // Find an unmapped vertex in the backing graph that will represent
            // the vertex being added to the subgraph
            Integer indexInBackingGraph = 
                AbstractGraph.this.nextAvailableVertex();
            AbstractGraph.this.addVertex(indexInBackingGraph);

            // Update the mod count to reflect the fact that we know we've made
            // this change to the backing graph
            lastModSeenInBacking = AbstractGraph.this.mods;

            // Add the mapping from what we will use to refer to the vertex to
            // its actual index in the backing graph
            vertexMapping.put(v, indexInBackingGraph);

            // If this is a subgraph of a subgraph, propagate the change up to
            // the parent so that the change appears there.
            if (parent != null)
                parent.addVertexFromChild(indexInBackingGraph);

            return true;
        }
    
        /**
         * {@inheritDoc}
         *
         * <p> This method is sensitive to the vertex ordering; a call will add
         * the edge to {@code vertex2} from the edge set for {@code vertex1}.
         * Subclasses should override this method if their {@link EdgeSet}
         * implementations are sensitive to the ordering of the vertex indices,
         * or if a more advanced behavior is needed.         
         */
        public boolean addEdge(int vertex1, int vertex2) {
            checkForUpdates();
            // Ensure that both vertices are mapped in the backing graph
            addVertex(vertex1);
            addVertex(vertex2);
            // Added an edge in the backing graph
            boolean isNew = AbstractGraph.this.addEdge(
                vertexMapping.get(vertex1), vertexMapping.get(vertex2));

            // Update the mod count to reflect the fact that we know we've made
            // this change to the backing graph
            lastModSeenInBacking = AbstractGraph.this.mods;
            return isNew;
        }

        /**
         * {@inheritDoc}
         */
        public boolean addEdge(T e) {
            Integer mapped1 = vertexMapping.get(e.from());
            if (mapped1 == null)
                return false;
            Integer mapped2 = vertexMapping.get(e.to());
            // If we have both vertices in this subgraph, remap the edge to the
            // backing graph's indices and add it to the backing graph
            return mapped2 != null 
                && AbstractGraph.this.addEdge(e.<T>clone(mapped1, mapped2));
        }

        /**
         * Checks for structural changes to the backing graph and if any changes
         * are detected, updates the view of this subgraph to reflect those
         * changes.
         */
        private void checkForUpdates() {
            if (lastModSeenInBacking != AbstractGraph.this.mods) {
                Iterator<Map.Entry<Integer,Integer>> iter = 
                    vertexMapping.entrySet().iterator();
                while (iter.hasNext()) {
                    Map.Entry<Integer,Integer> e = iter.next();
                    int backing = e.getValue();

                    // Remove any references to vertices that are no longer in
                    // the backing graph
                    if (!AbstractGraph.this.containsVertex(backing))
                        iter.remove();
                }

                lastModSeenInBacking = AbstractGraph.this.mods;
            }
        }

        /**
         * {@inheritDoc} 
         */
        public void clear() {
            // Remove all of the vertices in the backing graph that are
            // represented in this vertex
            for (Integer backingVertex : vertexMapping.values())
                AbstractGraph.this.removeVertex(backingVertex);
            // Then clear the current mapping
            vertexMapping.clear();

            // Update the mod count to reflect the fact that we know we've made
            // this change to the backing graph
            lastModSeenInBacking = AbstractGraph.this.mods;
            highestVertexIndex = -1;
        }
        
        /**
         * {@inheritDoc}
         */
        public void clearEdges() {
            for (Integer v : vertexMapping.values()) {
                for (Integer adj
                         : AbstractGraph.this.getAdjacentVertices(v)) {
                    AbstractGraph.this.removeEdge(v, adj);
                }
            }
            // Update the mod count to reflect the fact that we know we've made
            // this change to the backing graph
            lastModSeenInBacking = AbstractGraph.this.mods;
        }
    
    
        /**
         * {@inheritDoc}
         */
        public boolean containsEdge(int vertex1, int vertex2) {
            return (!(vertexMapping.containsKey(vertex1) 
                      && vertexMapping.containsKey(vertex2)))
                && AbstractGraph.this.containsEdge(
                       vertexMapping.get(vertex1),
                       vertexMapping.get(vertex2));
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsEdge(Edge e) {
            Integer mapped1 = vertexMapping.get(e.from());
            if (mapped1 == null)
                return false;
            Integer mapped2 = vertexMapping.get(e.to());
            // If we have both vertices in this subgraph, remap the edge to the
            // backing graph's indices and see if it is contained within
            return mapped2 != null 
                && AbstractGraph.this.containsEdge(e.<T>clone(mapped1, mapped2));
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsVertex(int v) {
            checkForUpdates();
            return vertexMapping.containsKey(v);
        }

        /**
         * {@inheritDoc}
         */
        public Set<T> edges() {
            checkForUpdates();
            return new SubgraphEdgeView();
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<T> getAdjacencyList(int vertex) {
            checkForUpdates();
            throw new Error(); //new SubgraphEdgeView(vertex);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<Integer> getAdjacentVertices(int vertex) {
            checkForUpdates();
            throw new Error();
        }
    
    
        /**
         * {@inheritDoc}
         */
        public T getEdge(int vertex1, int vertex2) {
            checkForUpdates();

            // First check whether we have the edge in this subgraph
            Integer v1 = vertexMapping.get(vertex1);
            if (v1 == null)
                return null;
            Integer v2 = vertexMapping.get(vertex2);
            if (v2 == null)
                return null;
        
            // If we have the edge, get the Edge instance from the backing graph
            T backingEdge = getEdge(v1, v2);
            T correctedView = backingEdge.<T>clone(vertex1, vertex2);
            
            return correctedView;
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean hasCycles() {
            throw new UnsupportedOperationException("fix me");
        }
        
        /**
         * {@inheritDoc}
         */
        public Iterator<Integer> iterator() {
            checkForUpdates();
            return new VertexSet(this, vertexMapping.keySet()).iterator();
        }

        /**
         * {@inheritDoc}
         */
        public int numEdges() {
            // Because this is only a view of the backing graph, we can't keep
            // view-local state of the number of edges.  Therefore, we have to
            // calculate how many edges are present on the fly.
            int numEdges = 0;
            for (Integer v : vertexMapping.values()) {
                EdgeSet<T> edges = AbstractGraph.this.getEdgeSet(v);
                if (edges != null)
                    numEdges += edges.size();
            }
            return numEdges;
        }
        
        /**
         * {@inheritDoc}
         */
        public int numVertices() {
            checkForUpdates();
            return vertexMapping.size();
        }

        /**
         * {@inheritDoc}
         */
        public boolean removeEdge(int vertex1, int vertex2) {
            // no need to check
            Integer mapped1 = vertexMapping.get(vertex1);
            if (mapped1 == null)
                return false;
            Integer mapped2 = vertexMapping.get(vertex2);
            return mapped2 != null 
                && AbstractGraph.this.removeEdge(mapped1, mapped2);
        }
        
        /**
         * {@inheritDoc}
         */
        public boolean removeEdge(Edge e) {
            Integer mapped1 = vertexMapping.get(e.from());
            if (mapped1 == null)
                return false;
            Integer mapped2 = vertexMapping.get(e.to());
            // If we have both vertices in this subgraph, remap the edge to the
            // backing graph's indices and try to remove it using its logic
            return mapped2 != null 
                && AbstractGraph.this.removeEdge(e.clone(mapped1, mapped2));
        }

        /**
         * {@inheritDoc}
         */
        public boolean removeVertex(int vertex) {
            // No need to check for updates
            Integer mapped = vertexMapping.get(vertex);
            if (mapped == null) 
                return false;
            boolean removed = AbstractGraph.this.removeVertex(mapped);
            vertexMapping.remove(vertex);

            if (vertex == highestVertexIndex)
                highestVertexIndex--;

            // Update the mod count to reflect the fact that we know we've made
            // this change to the backing graph
            lastModSeenInBacking = AbstractGraph.this.mods;

            return removed;
        }
    
        /**
         * {@inheritDoc}
         */
        public Graph2<T> subgraph(Set<Integer> vertices) {
            checkForUpdates();
            // Calculate the indices for the requested vertices in the backing
            // graph
            Set<Integer> mapped = new HashSet<Integer>();
            for (Integer v : vertices) {
                Integer inBacking = vertexMapping.get(mapped);
                if (inBacking == null) {
                    throw new IllegalArgumentException("Cannot create subgraph "
                        + "for vertex that is not present: " + v);
                }
                mapped.add(inBacking);
            }
            // Create a new subgraph with those indices
            return new Subgraph(mapped, this);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            checkForUpdates();
            return new VertexSet(this, vertexMapping.keySet());
        }    

        /**
         * 
         */
        private class SubgraphAdjacencyList extends AbstractSet<T> {
            
            int vertex;

            public SubgraphAdjacencyList(int vertex) {
                this.vertex = vertex;
            }

            public boolean add(Edge e) {
                return (e.from() == vertex || e.to() == vertex)
                    && addEdge(e.from(), e.to());
            }

            public boolean contains(Edge e) {
                return (e.from() == vertex || e.to() == vertex)
                    && getEdge(e.from(), e.to()) != null;
            }

            public Iterator<T> iterator() {
                throw new Error();
            }

            public boolean remove(Edge e) {
                return (e.from() == vertex || e.to() == vertex)
                    && removeEdge(e.from(), e.to());
            }

            public int size() {
                int sz = 0;
                Iterator<T> it = iterator();
                while (it.hasNext()) {
                    it.next();
                    sz++;
                }
                return sz;
            }
        }

        /**
         * 
         */
        private class SubgraphEdgeView extends AbstractSet<T> {           

            public SubgraphEdgeView() { }

            public boolean add(T e) {
                return Subgraph.this.addEdge(e);
            }

            public boolean contains(T o) {
                return Subgraph.this.containsEdge(o);
            }

            public Iterator<T> iterator() {
                return new SubgraphEdgeIterator();
            }

            public boolean remove(Object o) {
                return (o instanceof Edge)
                    && Subgraph.this.removeEdge((Edge)o);
            }

            public int size() {
                int sz = 0;
                Iterator<T> it = iterator();
                while (it.hasNext()) {
                    it.next();
                    sz++;
                }
                return sz;
            }

            /**
             * An {@code Iterator} that filters throguh all the edges in the
             * backing graph, returning only those that
             * are present in the current subgraph.
             */
            private class SubgraphEdgeIterator implements Iterator<T> {

                /**
                 * An iterator over all of the edges in the backing graph
                 */
                private Iterator<T> allEdges;

                /**
                 * The next edge to return
                 */
                private T next;

                /**
                 * The current edge that was just returned.
                 */
                private T cur; 

                /**
                 * Creates an iterator over the edges in the current subgraph
                 * for the vertex in the 
                 */
                public SubgraphEdgeIterator() {
                    allEdges = AbstractGraph.this.edges().iterator();
                    cur = null;
                    advance();
                }

                private void advance() {
                    next = null;
                    while (allEdges.hasNext()) {
                        T n = allEdges.next();
                        // See if this edge points to any of the vertices in the
                        // current subgraph
                        if (vertexMapping.inverse().containsKey(n.from())
                                || vertexMapping.inverse().containsKey(n.to())) {
                            next = n;
                            break;
                        }
                    }
                }

                public boolean hasNext() {
                    return next != null;
                }

                public T next() {
                    if (next == null)
                        throw new NoSuchElementException();

                    // Get the next edge, which still has indices to the
                    // backing graph
                    cur = next;
                    advance();
                    
                    // Because the edge has the wrong vertices, make a call to
                    // the subclass to create a view with the correct indices.
                    Integer v1 = vertexMapping.inverse().get(cur.from());
                    Integer v2 = vertexMapping.inverse().get(cur.to());
                    T correctedView = cur.<T>clone(v1, v2);
                    return correctedView;
                }

                public void remove() {
                    if (cur == null) 
                        throw new NoSuchElementException("No edge to remove");
                    cur = null;
                    Subgraph.this.removeEdge(cur);
                }
            }
        }

        // /**
        //  * An {@code Iterator} that filters throguh all the edges in the backing
        //  * graph, returning only those that are present in the current subgraph.
        //  */
        // private class SubgraphEdgeIterator implements Iterator<T> {

        //     /**
        //      * An iterator over all of the edges in the backing graph
        //      */
        //     private Iterator<T> allEdges;
            
        //     /**
        //      * The next edge to return
        //          */
        //     private T next;
            
        //     /**
        //      * The current edge that was just returned.
        //      */
        //     private T cur; 
            
        //     /**
        //      * Creates an iterator over the edges in the current subgraph
        //      * for the vertex in the 
        //      */
        //     public SubgraphEdgeIterator() {
        //         allEdges = AbstractGraph.this.edges().iterator();
        //         cur = null;
        //             advance();
        //     }

        //     private void advance() {
        //         next = null;
        //         while (allEdges.hasNext()) {
        //             T n = allEdges.next();
        //             // See if this edge points to any of the vertices in the
        //             // current subgraph
        //             if (vertexMapping.inverse().containsKey(n.from())
        //                 || vertexMapping.inverse().containsKey(n.to())) {
        //                 next = n;
        //                 break;
        //             }
        //         }
        //     }

        //     public boolean hasNext() {
        //         return next != null;
        //     }

        //     public T next() {
        //         if (next == null)
        //             throw new NoSuchElementException();

        //         // Get the next edge, which still has indices to the
        //         // backing graph
        //         cur = next;
        //         advance();
                    
        //         // Because the edge has the wrong vertices, make a call to
        //         // the subclass to create a view with the correct indices.
        //         Integer v1 = vertexMapping.inverse().get(cur.from());
        //         Integer v2 = vertexMapping.inverse().get(cur.to());
        //         T correctedView = AbstractGraph.this.wrap(cur, v1, v2);

        //         // Just for sanity
        //         assert correctedView.from() == v1 : "subclass did not "
        //             + " correctly remap the indices: " + v1 + " != " 
        //             + correctedView.from() + " (original: " + v1 + ")";
        //         assert correctedView.to() == v2 : "subclass did not " +
        //             " correctly remap the indices: " + v2 + " != " 
        //             + correctedView.to() + " (original: " + v2 + ")";

        //         return correctedView;
        //     }

        //     public void remove() {
        //         if (cur == null) 
        //             throw new NoSuchElementException("No edge to remove");
        //         cur = null;
        //         Subgraph.this.removeEdge(cur.from(), cur.to());
        //     }
        // }
    }
}
