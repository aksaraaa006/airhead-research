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
public abstract class AbstractGraph<T extends Edge,S extends EdgeSet<T>>
        implements Graph<T>, java.io.Serializable {

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
    private final Map<Integer,S> vertexToEdges;

    /**
     * Creates an empty {@code AbstractGraph}
     */
    public AbstractGraph() {
        mods = 0;
        vertexToEdges = // new IntegerMap<EdgeSet<T>>();
            new HashMap<Integer,S>();
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
        S edges = getEdgeSet(v);
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
        S edges = getEdgeSet(v);
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
     */
    public boolean addEdge(T e) {
        EdgeSet<T> from = addIfAbsent(e.from());
        EdgeSet<T> to = addIfAbsent(e.to());
        
        boolean isNew = from.add(e);                   
        if (isNew) 
            numEdges++;
        // Add this edge for the vertex to which the edge is pointing.  This
        // double-add behavior is necessary to ensure that the EdgeSet for each
        // vertices contains all the edges that connect to that vertex.
        to.add(e);
        
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
    protected abstract S createEdgeSet(int vertex);
    
    /**
     * A utility method for casting an {@link Edge} instance to the
     * parameterized type of this graph.  
     */    
    @SuppressWarnings("unchecked") private T edgeCast(Edge e) {
        // This method isolates the suppress warnings annotation to a single
        // method in order that if future language feaures make it safer to make
        // this cast, the code may be easily updated for all code paths.
        T edge = (T)e;
        return edge;
    }

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
        EdgeSet<T> e = getEdgeSet(vertex);
        return (e == null) ? null : new AdjacencyListView(e);
    }

    /**
     * {@inheritDoc}
     */
    public Set<Integer> getAdjacentVertices(int vertex) {
        EdgeSet<T> e = getEdgeSet(vertex);
        return (e == null) ? null : new AdjacentVerticesView(e.connected());
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
        Set<T> edges = e.getEdges(vertex2);
        return edges.isEmpty() ? null : edges.iterator().next();
    }

    /**
     * Returns the set of edges assocated with the vertex, or {@code null} if
     * this vertex is not in this graph.
     */
    protected S getEdgeSet(int vertex) {
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
        return new VertexView().iterator();
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
    public int order() {
        return vertexToEdges.size();
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
        EdgeSet<T> from = getEdgeSet(e.from());
        EdgeSet<T> to = getEdgeSet(e.to());
        int before = numEdges;
        if (from != null && from.remove(e)) {
            numEdges--;
            assert to.contains(e)
                : "Error in ensuring consistent from/to edge sets";
            // Remove the edge from the EdgeSet for the vertex to which this
            // edge points.
            to.remove(e);
        }

        return before != numEdges;        
    }

    /**
     * {@inheritDoc}
     */
    public boolean removeVertex(int vertex) {
        EdgeSet<T> edges = vertexToEdges.remove(vertex);
        if (edges == null)
            return false;
        // Call the internal removeVertex method to perform the remaining
        // removal logic.
        removeVertexInternal(vertex, edges);
        return true;
    }

    /**
     * Removes the edges of the provided vertex from this graph, accounting for
     * the presence of the edges in the corresponding {@link EdgeSet}'s for the
     * other vertex in each edge.  This method should only be called once a
     * vertex has been removed from the {@link #vertexToEdges} mapping.
     */
    private void removeVertexInternal(int vertex, EdgeSet<T> edges) {
        // We successfully removed the vertex
        mods++;

        // Discount all the edges that were stored in this vertices edge set
        numEdges -= edges.size();

        // Now find all the edges stored in other vertices that might point to
        // this vertex and remove them
        for (Edge e : edges) {
            // Identify the other vertex in the removed edge and remove the
            // edge from the vertex's corresponding EdgeSet.
            int otherVertex = (e.from() == vertex) ? e.to() : e.from();
            EdgeSet<T> otherEdges = vertexToEdges.get(otherVertex);
            assert otherEdges.contains(e) 
                : "Error in ensuring consistent from/to edge sets";
            otherEdges.remove(e);            
        }

        // If we're removing the highest vertex, rather than do an O(n) search
        // for the next highest, just decrement the value, which is guaranteed
        // to work even though it might waste space.
        if (highestVertex == vertex)
            highestVertex--;
    }
    
    /**
     * {@inheritDoc}
     */
    public int size() {
        return numEdges;
    }

    /**
     * {@inheritDoc}
     */
    public Graph<T> subgraph(Set<Integer> vertices) {
        return new Subgraph(vertices);
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
        return new VertexView();
    }

    /**
     * A view of this graph's vertices, which provides support for adding,
     * removing, and iterating.  This class implements all optional methods for
     * {@link Set} and {@link Iterator}.
     */
    private class VertexView extends AbstractSet<Integer> {
        
        public VertexView() { }

        public boolean add(Integer vertex) {
            return addVertex(vertex);
        }

        public boolean contains(Integer vertex) {
            return contains(vertex);
        }

        public Iterator<Integer> iterator() {
            return new VertexIterator();
        }

        public boolean remove(Integer i) {
            return removeVertex(i);
        }

        public int size() {
            return order();
        }

        private class VertexIterator implements Iterator<Integer> {

            /**
             * The Iterator over the current vertex set
             */
            private Iterator<Map.Entry<Integer,S>> vertices;

            /**
             * The next vertex to return
             */
            private Map.Entry<Integer,S> lastReturned;

            public VertexIterator() {
                vertices = vertexToEdges.entrySet().iterator();
                lastReturned = null;
            }

            public boolean hasNext() {
                return vertices.hasNext();
            }

            public Integer next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                return (lastReturned = vertices.next()).getKey();
            }

            public void remove() {
                if (lastReturned == null)
                    throw new IllegalStateException("no element to remove");
                // Note that because we need to remove edges from the vertex,
                // ideally, we would want to call removeVertex().  However, this
                // would modify the state of the vertexToEdge Map, which would
                // break the iterator (concurrent modification).  Therefore, we
                // remove the vertex from the iterator and then perform the rest
                // of the logic using removeVertexInternal()
                vertices.remove();
                Integer removed = lastReturned.getKey();
                EdgeSet<T> edges = lastReturned.getValue();
                removeVertexInternal(removed, edges);
                // Finally, null out the last removed value to prevent a double
                // removal
                lastReturned = null;
            }                
        }
    }

    /**
     * A view for the {@code Edge} adjacency list of a vertex.  This class
     * monitors for changes to edge set to update the state of this graph
     */
    private class AdjacencyListView extends AbstractSet<T> {

        private final EdgeSet<T> adjacencyList;

        public AdjacencyListView(EdgeSet<T> adjacencyList) {            
            this.adjacencyList = adjacencyList;
        }

        /**
         * Adds an edge to this vertex and adds the vertex to the graph if it
         * was not present before.
         */
        @Override public boolean add(T edge) {
            // If we've added a new edge to this vertex's adjacency list, check
            // whether we've added a new vertex to the graph
            if (adjacencyList.add(edge)) { 

                // Figure out which vertex was newly connected
                int otherVertex = (edge.from() == adjacencyList.getRoot())
                    ? edge.to() : edge.from();
                if (!vertexToEdges.containsKey(otherVertex)) {
                    addVertex(otherVertex);
                }
                // Last, add this edge to the EdgeSet for the other vertex in
                // the edge
                vertexToEdges.get(otherVertex).add(edge);
                numEdges++;
                return true;
            }
            return false;
        }

        @Override public boolean contains(Object edge) {
            return adjacencyList.contains(edge);
        }

        public Iterator<T> iterator() {
            return new AdjacencyListIterator();
        }

        @Override public boolean remove(Object o) {
            if (!(o instanceof Edge))
                return false;
            Edge edge = (Edge)o;
            // If the vertex was successfully removed, we need to remove the
            // edge from the edge set for the other vertex in the edge
            if (adjacencyList.remove(edge)) {
                // Determine the non-root vertex in the edge
                int otherVertex = (edge.from() == adjacencyList.getRoot())
                    ? edge.to() : edge.from();
                // Then remove the edge from its adjacency list as well
                vertexToEdges.get(otherVertex).remove(edge);

                numEdges--;
                return true;
            }
            return false;
        }

        public int size() {
            return adjacencyList.size();
        }

        /**
         * A decorator around the iterator for an adjacency list, which tracks
         * edges removal to update the number of edges in the graph.
         */
        private class AdjacencyListIterator implements Iterator<T> {

            private final Iterator<T> edges;

            public AdjacencyListIterator() {
                edges = adjacencyList.iterator();
            }

            public boolean hasNext() {
                return edges.hasNext();
            }

            public T next() {
                return edges.next();
            }

            public void remove() {
                edges.remove();
                numEdges--;
            }            
        }

    }

    /**
     * A view of a vertex's adjacencent vertices that monitors for additions and
     * removals to the set in order to update the state of this {@code Graph}.
     */
    private class AdjacentVerticesView extends AbstractSet<Integer> {

        /**
         * The set of adjacent vertices to a vertex.  This set is itself a view
         * to the data and is updated by the {@link EdgeList} for a vertex.
         */
        private final Set<Integer> adjacent;
        
        /**
         * Constructs a view around the set of adjacent vertices
         */
        public AdjacentVerticesView(Set<Integer> adjacent) {
            this.adjacent = adjacent;
        }

        /**
         * Throws an {@link UnsupportedOperationException} if called.
         */
        @Override public boolean add(Integer vertex) {
            throw new UnsupportedOperationException("cannot create edges "
                + "using an adjacenct vertices set; use addEdge() instead");
        }

        @Override public boolean contains(Object o) {
            return o instanceof Integer
                && adjacent.contains((Integer)o);
        }

        public Iterator<Integer> iterator() {
            return new AdjacentVerticesIterator();
        }

        @Override public boolean remove(Object o) {
            throw new UnsupportedOperationException("cannot remove edges "
                + "using an adjacenct vertices set; use removeEdge() instead");
        }

        public int size() {
            return adjacent.size();
        }

        /**
         * A decorator around the iterator for an adjacency list's vertices that tracks
         * vertex removal to update the number of edges in the graph.
         */
        private class AdjacentVerticesIterator implements Iterator<Integer> {

            private final Iterator<Integer> vertices;

            public AdjacentVerticesIterator() {
                vertices = adjacent.iterator();
            }

            public boolean hasNext() {
                return vertices.hasNext();
            }

            public Integer next() {
                return vertices.next();
            }

            public void remove() {
                throw new UnsupportedOperationException("cannot remove an edge "
                    + "to an adjacenct vertices using this iterator; use " 
                    + "removeEdge() instead");
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
         *
         * <p> This iterator also tracks successful iterator removals to update
         * the number of edges in this graph.
         */
        private class EdgeViewIterator implements Iterator<T> {

            /**
             * An iterator over the {@link EdgeSet} instances for each of the
             * vertices
             */
            private final Iterator<S> vertices;
            
            /**
             * The iterator from which the next element will be returned
             */
            private Iterator<T> edges;
                                                
            /**
             * The iterator from which the last element was returned.  Any call
             * to remove() will use this iterator
             */
            private Iterator<T> toRemoveFrom;

            private T next;

            private T cur;
            
            public EdgeViewIterator() {
                vertices = vertexToEdges.values().iterator();
                advance();
            }
            
            /**
             * Updates the state of the iterator so that {@code edges} is set to
             * an {@code Iterator} with an element to return, if such an
             * iterator exists.
             */
            private void advance() {
                next = null;
                if ((edges == null || !edges.hasNext()) && !vertices.hasNext())
                    return;
                do {
                    // Find an edge iterator with at least one edge
                    while ((edges == null || !edges.hasNext()) && vertices.hasNext())
                        edges = vertices.next().iterator();
                    // If we didn't find one, short circuit
                    if (edges == null || !edges.hasNext())
                        return;                   
                    // Get the next edge to examine
                    T e = edges.next();

                    // The backing graph stores symmetric edges in order to
                    // maintain the adjacency lists.  To account for this,
                    // we toss out edges that will have their symmetric
                    // version counted, using the edge's to and from to make
                    // the distinction.
                    if (e.from() < e.to()) 
                        next = e;
                } while (next == null); 
            }
            
            public boolean hasNext() {
                return next != null;
            }

            public T next() {
                if (!hasNext())
                    throw new NoSuchElementException();
                cur = next;
                // Once we've returned an element from the set of edges, the
                // next call to remove() should remove from the current iterator
                if (toRemoveFrom != edges)
                    toRemoveFrom = edges;
                advance();
                return cur;
            }
            
            public void remove() {
                if (cur == null)
                    throw new IllegalStateException("no element to remove");
                removeEdge(cur);
                cur = null;
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
    protected class Subgraph implements Graph<T> {
        
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

        /**
         * Creates a new subgraph with the provided set of vertices and where
         * any changes to this {@code Subgraph} are propagated to the provided
         * parent.  This constructor is intented for creating subgraphs of a
         * {@code Subgraph}.
         */
        private Subgraph(Set<Integer> vertices, Subgraph parent) {       
    
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

        /**
         * Adds a vertex that was created in a child of this subgraph, where the
         * new vertex has the specified index in the backing map.  This method
         * ensures that parent subgraphs reflect the state changes of any of
         * their children.
         */
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
            if (v > highestVertexIndex)
                highestVertexIndex = v;
            
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
         */
        public boolean addEdge(T e) {
            Integer mapped1 = vertexMapping.get(e.from());
            if (mapped1 == null) {
                addVertex(e.from());
                // Once the vertex has been added, the vertex mapping lookup
                // should return a non-null index in the backing map
                mapped1 = vertexMapping.get(e.from());
                assert mapped1 != null : "failed to correctly add a vertex in "
                    + "the backing graph";
            }
            Integer mapped2 = vertexMapping.get(e.to());
            if (mapped2 == null) {
                addVertex(e.to());
                mapped2 = vertexMapping.get(e.to());
                assert mapped2 != null : "failed to correctly add a vertex in "
                    + "the backing graph";
            }

            return AbstractGraph.this.addEdge(e.<T>clone(mapped1, mapped2));
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
            Set<Integer> inSubgraph = vertexMapping.inverse().keySet();
            for (Integer v : vertexMapping.values()) {
                for (T adj : AbstractGraph.this.getAdjacencyList(v)) {
                    // See if both vertices for v are in this subgraph
                    if ((adj.from() == v && inSubgraph.contains(adj.to()))
                            || adj.to() == v && inSubgraph.contains(adj.from()))
                        AbstractGraph.this.removeEdge(adj);
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
            Integer vertexInBacking = vertexMapping.get(vertex);
            if (vertexInBacking == null)
                return null;
            EdgeSet<T> adjList = vertexToEdges.get(vertexInBacking);

            return new SubgraphAdjacencyListView(vertex, adjList);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<Integer> getAdjacentVertices(int vertex) {
            checkForUpdates();
            // Find the adjacency list of this vertex in the backing graph
            Integer vertexInBacking = vertexMapping.get(vertex);
            if (vertexInBacking == null)
                return null;
            EdgeSet<T> adjList = vertexToEdges.get(vertexInBacking);
            return new SubgraphAdjacentVerticesView(adjList.connected());
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
         * Returns true if the provided vertex in the backing graph is
         * represented in this subgraph.
         */
        private boolean isInSubgraph(int vertexInBacking) {
            return vertexMapping.inverse().containsKey(vertexInBacking);
        }
        
        /**
         * Maps the vertices for an edge in this subgraph to those in the
         * backing graph, returning the result, or {@code null} if this edge is
         * not present in the subgraph.
         */
        private T toBacking(T inSubgraph) {
            // First check whether we have the edge in this subgraph
            Integer from = vertexMapping.get(inSubgraph.from());
            if (from == null)
                return null;
            Integer to = vertexMapping.get(inSubgraph.to());
            if (to == null)
                return null;
        
            // If we have both vertices in this subgraph, create an Edge
            // instance for the backing graph
            T inBacking = inSubgraph.<T>clone(from, to);
            return inBacking;
        }

        /**
         * {@inheritDoc}
         */
        public Iterator<Integer> iterator() {
            checkForUpdates();
            return new VertexView().iterator();
        }
        /**
         * {@inheritDoc}
         */
        public int order() {
            checkForUpdates();
            return vertexMapping.size();
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
        public int size() {
            // Because this is only a view of the backing graph, we can't keep
            // view-local state of the number of edges.  Therefore, we have to
            // calculate how many edges are present on the fly.
            int numEdges = 0;
            Set<Integer> verticesInBacking = vertexMapping.inverse().keySet();
            for (Integer v : vertexMapping.values()) {
                EdgeSet<T> edges = AbstractGraph.this.getEdgeSet(v);
                for (Integer c : edges.connected()) {
                    // Because the backing graph maintains symmetric edges for
                    // all the edge sets, we need to avoid double counting an
                    // edge.  To do this, we check whether the vertex for the
                    // from() is less than to(), which will only count the edges
                    // stored in the from() vertex's EdgeSet.
                    if (c < v && verticesInBacking.contains(c)) {
                        numEdges++;
                    }
                }
            }
            return numEdges;
        }
            
        /**
         * {@inheritDoc}
         */
        public Graph<T> subgraph(Set<Integer> vertices) {
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

            // Create a new subgraph with those indices with this Subgraph as
            // the parent to esnure any vertex additions in the child will be
            // propagated to this subgraph's vertex set.
            return new Subgraph(mapped, this);
        }
        
        /**
         * {@inheritDoc}
         */
        public Set<Integer> vertices() {
            checkForUpdates();
            return new SubgraphVertexView();
        }    

        /**
         * 
         */
        private class SubgraphVertexView extends AbstractSet<Integer> {
                    
            public SubgraphVertexView() {  
            }

            public boolean add(Integer vertex) {
                return addVertex(vertex);
            }

            public boolean contains(Integer vertex) {
                return contains(vertex);
            }

            public Iterator<Integer> iterator() {
                return new SubgraphVertexIterator();
            }

            public boolean remove(Integer i) {
                return removeVertex(i);
            }

            public int size() {
                return order();
            }

            /**
             * An iterator over the vertices in this subgraph.  This iterator is
             * intended to provide special logic for handling vertex removal via
             * the Iterator.remove() method.
             */
            private class SubgraphVertexIterator implements Iterator<Integer> {
                
                /**
                 * The last vertex that was returned from this iterator
                 */
                private Map.Entry<Integer,Integer> lastReturned;
                
                /**
                 * An iterator over the set of vertices in this subgraph
                 */
                private Iterator<Map.Entry<Integer,Integer>> vertices;

                public SubgraphVertexIterator() {
                    lastReturned = null;
                    vertices = vertexMapping.entrySet().iterator();
                }

                public boolean hasNext() {
                    return vertices.hasNext();
                }
                
                public Integer next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    return (lastReturned = vertices.next()).getKey();
                }

                public void remove() {
                    if (lastReturned == null)
                        throw new IllegalStateException("no element to remove");
                    // NOTE: because we want to remove the vertex from the
                    // backing graph as well, ideally, we would use
                    // removeVertex().  However, this would modify the state of
                    // the Map that is being iterated over.  The only safe way
                    // to remove from this subgraph's vertices is to use
                    // Iterator.remove().  We then remove the vertex from the
                    // backing graph.
                    vertices.remove();
                                        
                    AbstractGraph.this.removeVertex(lastReturned.getValue());

                    if (lastReturned.getKey() == highestVertexIndex)
                        highestVertexIndex--;
                    
                    // Update the mod count to reflect the fact that we know
                    // we've made this change to the backing graph
                    lastModSeenInBacking = AbstractGraph.this.mods;
                }
            }
        }

        /**
         * A view for the {@code Edge} adjacency list of a vertex within a
         * subgraph.  This class monitors for changes to edge set to update the
         * state of this graph
         */
        private class SubgraphAdjacencyListView extends AbstractSet<T> {

            /**
             * The adjacency list of edges in the backing graph.
             */
            private final Set<T> adjacencyList;
            
            /**
             * The root vertex in the subgraph being represnted by this
             * adjacency list. This value may be different from {@code
             * adjacencyList.getRoot()}, which is storing vertices for the
             * backing graph.
             */ 
            private final int rootInSubgraph;

            public SubgraphAdjacencyListView(int rootInSubgraph,
                                             Set<T> adjacencyList) {
                this.rootInSubgraph = rootInSubgraph;
                this.adjacencyList = adjacencyList;                
            }

            /**
             * Adds an edge to this vertex and adds the vertex to the graph if it
             * was not present before.
             */
            public boolean add(T edge) {
                // If one of the edges points to the root note for this
                // adjacency list, attempt to add it to the list.
                if (edge.from() == rootInSubgraph
                        || edge.to() == rootInSubgraph) {

                    // Before we add it, remap the vertices back to what they
                    // would be in the backing map.
                    T edgeInBacking = toBacking(edge);

                    // If the vertex points to a new vertex not present in this
                    // subgraph, then we will need to add it first.
                    if (edgeInBacking == null) {
                        int newVertex = (edge.from() == rootInSubgraph)
                            ? edge.to() : edge.from();
                        addVertex(newVertex);
                        // Once the vertex has been added, remap the edge, which
                        // should succeed since both vertices exist in this
                        // subgraph
                        edgeInBacking = toBacking(edge);
                        assert edgeInBacking != null : "Failed to create new "
                            + "vertex in subgraph for adding to an adj. list";
                    }
                    
                    boolean wasAdded = adjacencyList.add(edgeInBacking);
                    // If we successfully added an edge to this subgraph's
                    // adjacency list, then increment the total number of edges
                    // in the parent main graph.
                    if (wasAdded) 
                        numEdges++;
                    return wasAdded;
                }
                return false;
            }

            public boolean contains(Object o) {
                if (!(o instanceof Edge))
                    return false;
                
                T edge = edgeCast((Edge)o);

                // Remap the vertices back to what they would be in the backing
                // map so that the contains is checking for the correct set
                T edgeInBacking = toBacking(edge);

                return adjacencyList.contains(edgeInBacking);
            }
            
            public Iterator<T> iterator() {
                return new AdjacencyListIterator();
            }
            
            public boolean remove(Object o) {
                if (!(o instanceof Edge))
                    return false;
                
                T edge = edgeCast((Edge)o);

                // Remap the vertices back to what they would be in the backing
                // map so that the remval is checking for the correct set
                T edgeInBacking = toBacking(edge);

                // If the adjacency list was able to remove the edge, then
                // decrement the global edge count for the backing graph
                if (adjacencyList.remove(edgeInBacking)) {
                    numEdges--;
                    return true;
                }
                return false;
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
             * A decorator around the iterator for the adjacency list for a
             * vertex in a subgraph, which tracks edges removal to update the
             * number of edges in the graph.
             */
            private class AdjacencyListIterator implements Iterator<T> {
                
                private final Iterator<T> edges;
                
                private T next;

                public AdjacencyListIterator() {
                    edges = adjacencyList.iterator();
                    advance();
                }
                
                private void advance() {
                    next = null;
                    while (edges.hasNext()) {
                        T e = edges.next();
                        // Check whether this edge is represented by vertices in
                        // the subgraph and if so, retain the vertex mapping
                        Integer from = vertexMapping.inverse().get(e.from());
                        if (from == null)
                            continue;
                        Integer to = vertexMapping.inverse().get(e.to());
                        if (to == null)
                            continue;

                        // If both vertices are in the subgraph, then remap the
                        // edge's vertices to their appropriate values.
                        next = e.<T>clone(from, to);
                        break;
                    }
                }
                
                public boolean hasNext() {
                    return next != null;
                }
                
                public T next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    T cur = next;
                    advance();
                    return cur;
                }
                
                public void remove() {
                    // Note that we don't need to do any sort of remapping to
                    // call removeEdge() here because the edge information is
                    // handled internally to the EdgeSet.
                    edges.remove();
                    // However, we do need to decrement the total number of
                    // edges in the graph.
                    numEdges--;
                }            
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
                        if ((vertexMapping.inverse().containsKey(n.from())
                                 || vertexMapping.inverse().containsKey(n.to()))
                                && n.to() < n.from()) {
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

        /**
         * A view of a subgraph's vertex's adjacencent vertices (also in the
         * subgraph), which monitors for additions and removals to the set in
         * order to update the state of this {@code Subgraph}.
         */
        private class SubgraphAdjacentVerticesView extends AbstractSet<Integer> {

            /**
             * The set of adjacent vertices to a vertex.  This set is itself a view
             * to the data and is updated by the {@link EdgeList} for a vertex.
             */
            private Set<Integer> adjacent;
            
            /**
             * Constructs a view around the set of adjacent vertices
             */
            public SubgraphAdjacentVerticesView(Set<Integer> adjacent) {
                this.adjacent = adjacent;
            }
            
            /**
             * Adds an edge to this vertex and adds the vertex to the graph if it
             * was not present before.
             */
            public boolean add(Integer vertex) {
                throw new UnsupportedOperationException("cannot add edges "
                    + "using an adjacenct vertices set; use addEdge() "
                    + "instead");
            }
            
            public boolean contains(Object o) {
                if (!(o instanceof Integer))
                    return false;
                // Convert the vertex to its index in the backing graph
                Integer v = (Integer)o;
                Integer inBacking = vertexMapping.get(v);                
                return inBacking != null && adjacent.contains(inBacking);
            }
            
            public Iterator<Integer> iterator() {
                return new AdjacentVerticesIterator();
            }
            
            public boolean remove(Object o) {
                throw new UnsupportedOperationException("cannot remove edges "
                    + "using an adjacenct vertices set; use removeEdge() "
                    + "instead");
            }
            
            public int size() {
                int sz = 0;
                for (Integer inBacking : adjacent) {
                    if (isInSubgraph(inBacking))
                        sz++;
                }
                return sz;
            }

            /**
             * A decorator around the iterator for an adjacency list's vertices that tracks
             * vertex removal to update the number of edges in the graph.
             */
            private class AdjacentVerticesIterator implements Iterator<Integer> {

                private final Iterator<Integer> vertices;

                private Integer next;

                public AdjacentVerticesIterator() {
                    vertices = adjacent.iterator();
                    advance();
                }
                
                /**
                 * Finds the next adjacent vertex that is also in this subgraph.
                 */
                private void advance() {
                    next = null;
                    while (vertices.hasNext()) {
                        Integer v = vertices.next();
                        Integer inSub = vertexMapping.inverse().get(v);
                        if (inSub != null) {
                            next = inSub;
                            break;
                        }
                    }                    
                }

                public boolean hasNext() {
                    return next != null;
                }

                public Integer next() {
                    if (!hasNext())
                        throw new NoSuchElementException();
                    Integer cur = next;
                    advance();
                    return cur;
                }

                public void remove() {
                    throw new UnsupportedOperationException("cannot remove an "
                        + "edge to an adjacenct vertices using this iterator; "
                        + "use removeEdge() instead");
                }            
            }
        }        
    }
}