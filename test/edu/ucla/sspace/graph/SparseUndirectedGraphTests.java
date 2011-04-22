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

import java.util.*;

import edu.ucla.sspace.util.OpenIntSet;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 */
public class SparseUndirectedGraphTests { 

    @Test public void testAddVertex() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        assertTrue(g.addVertex(0));
        assertEquals(1, g.numVertices());
        assertTrue(g.containsVertex(0));
        // second add should have no effect
        assertFalse(g.addVertex(0));
        assertEquals(1, g.numVertices());
        assertTrue(g.containsVertex(0));

        assertTrue(g.addVertex(1));
        assertEquals(2, g.numVertices());
        assertTrue(g.containsVertex(1));
    }

    @Test public void testAddEdge() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        g.addEdge(new SimpleEdge(0, 1));
        assertEquals(2, g.numVertices());
        assertEquals(1, g.numEdges());
        assertTrue(g.containsEdge(new SimpleEdge(0, 1)));

        g.addEdge(new SimpleEdge(0, 2));
        assertEquals(3, g.numVertices());
        assertEquals(2, g.numEdges());
        assertTrue(g.containsEdge(new SimpleEdge(0, 2)));

        g.addEdge(new SimpleEdge(3, 4));
        assertEquals(5, g.numVertices());
        assertEquals(3, g.numEdges());
        assertTrue(g.containsEdge(new SimpleEdge(3, 4)));
    }

    @Test public void testRemoveLesserVertexWithEdges() {
        Graph2<Edge> g = new SparseUndirectedGraph();

        for (int i = 0; i < 100; ++i) {
            Edge e = new SimpleEdge(0, i);
            g.addEdge(e);
        }
       
        assertTrue(g.containsVertex(0));
        assertTrue(g.removeVertex(0));
        assertEquals(99, g.numVertices());
        assertEquals(0, g.numEdges());
    }

    @Test public void testRemoveHigherVertexWithEdges() {
        Graph2<Edge> g = new SparseUndirectedGraph();

        for (int i = 0; i < 99; ++i) {
            Edge e = new SimpleEdge(100, i);
            g.addEdge(e);
        }
        
        assertTrue(g.containsVertex(100));
        assertTrue(g.removeVertex(100));
        assertEquals(99, g.numVertices());
        assertEquals(0, g.numEdges());
    }


    @Test public void testRemoveVertex() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
        }

        for (int i = 99; i >= 0; --i) {            
            assertTrue(g.removeVertex(i));
            assertEquals(i, g.numVertices());
            assertFalse(g.containsVertex(i));
            assertFalse(g.removeVertex(i));
        }
    }


    @Test public void testRemoveEdge() {
        Graph2<Edge> g = new SparseUndirectedGraph();

        for (int i = 1; i < 100; ++i) {
            Edge e = new SimpleEdge(0, i);
            g.addEdge(e);
        }
        
        for (int i = 99; i > 0; --i) {
            Edge e = new SimpleEdge(0, i);
            assertTrue(g.removeEdge(e));
            assertEquals(i-1, g.numEdges());
            assertFalse(g.containsEdge(e));
            assertFalse(g.removeEdge(e));
        }
    }

    @Test public void testVertexIterator() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }
        assertEquals(control.size(), g.numVertices());
        for (Integer i : g.vertices())
            assertTrue(control.contains(i));        
    }

    @Test public void testEdgeIterator() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Edge> control = new HashSet<Edge>();
        for (int i = 1; i <= 100; ++i) {
            Edge e = new SimpleEdge(0, i);
            g.addEdge(e);
            control.add(e);
        }

        assertEquals(control.size(), g.numEdges());
        for (Edge e : g.edges())
            assertTrue(control.contains(e));
    }


    /******************************************************************
     *
     *
     * VertexSet tests 
     *
     *
     ******************************************************************/

    @Test public void testVertexSetAdd() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(vertices.add(100));
        assertTrue(g.containsVertex(100));
        assertEquals(101, vertices.size());
        assertEquals(101, g.numVertices());
        
        // dupe
        assertFalse(vertices.add(100));
        assertEquals(101, vertices.size());
    }

    @Test public void testVertexSetAddFromGraph() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.addVertex(100));
        assertTrue(g.containsVertex(100));
        assertTrue(vertices.contains(100));
        assertEquals(101, vertices.size());
        assertEquals(101, g.numVertices());
        
        // dupe
        assertFalse(vertices.add(100));
        assertEquals(101, vertices.size());
    }

    @Test public void testVertexSetRemove() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.containsVertex(99));
        assertTrue(vertices.remove(99));
        assertFalse(g.containsVertex(99));
        assertEquals(99, vertices.size());
        assertEquals(99, g.numVertices());
        
        // dupe
        assertFalse(vertices.remove(99));
        assertEquals(99, vertices.size());
    }

    @Test public void testVertexSetRemoveFromGraph() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        assertTrue(g.removeVertex(99));

        assertFalse(g.containsVertex(99));
        assertFalse(vertices.contains(99));
        assertEquals(99, vertices.size());
        assertEquals(99, g.numVertices());        
    }

    @Test public void testVertexSetIteratorRemove() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Integer> control = new HashSet<Integer>();
        for (int i = 0; i < 100; ++i) {
            g.addVertex(i);
            control.add(i);
        }

        Set<Integer> vertices = g.vertices();
        assertEquals(control.size(), vertices.size());
        Iterator<Integer> iter = vertices.iterator();
        assertTrue(iter.hasNext());
        Integer toRemove = iter.next();
        assertTrue(g.containsVertex(toRemove));
        assertTrue(vertices.contains(toRemove));
        iter.remove();
        assertFalse(g.containsVertex(toRemove));
        assertFalse(vertices.contains(toRemove));
        assertEquals(g.numVertices(), vertices.size());
    }
    

    /******************************************************************
     *
     *
     * EdgeView tests 
     *
     *
     ******************************************************************/

    @Test public void testEdgeViewAdd() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Edge> edges = g.edges();
        assertEquals(g.numEdges(), edges.size());
        edges.add(new SimpleEdge(0, 1));
        assertEquals(2, g.numVertices());
        assertEquals(1, g.numEdges());
        assertEquals(1, edges.size());
        assertTrue(g.containsEdge(new SimpleEdge(0, 1)));
        assertTrue(edges.contains(new SimpleEdge(0, 1)));
    }

    @Test public void testEdgeViewRemove() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Edge> edges = g.edges();
        assertEquals(g.numEdges(), edges.size());
        edges.add(new SimpleEdge(0, 1));
        edges.remove(new SimpleEdge(0, 1));
        assertEquals(2, g.numVertices());
        assertEquals(0, g.numEdges());
        assertEquals(0, edges.size());
        assertFalse(g.containsEdge(new SimpleEdge(0, 1)));
        assertFalse(edges.contains(new SimpleEdge(0, 1)));
    }

    @Test public void testEdgeViewIterator() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Edge> edges = g.edges();

        Set<Edge> control = new HashSet<Edge>();
        for (int i = 0; i < 100; i += 2)  {
            Edge e = new SimpleEdge(i, i+1);
            g.addEdge(e); // all disconnected
            control.add(e);
        }
    

        assertEquals(100, g.numVertices());
        assertEquals(50, g.numEdges());
        assertEquals(50, edges.size());
        
        Set<Edge> test = new HashSet<Edge>();
        for (Edge e : edges)
            test.add(e);
        assertEquals(control.size(), test.size());
        for (Edge e : test)
            assertTrue(control.contains(e));        
    }

    @Test public void testEdgeViewIteratorRemove() {
        Graph2<Edge> g = new SparseUndirectedGraph();
        Set<Edge> edges = g.edges();

        Set<Edge> control = new HashSet<Edge>();
        for (int i = 0; i < 100; i += 2)  {
            Edge e = new SimpleEdge(i, i+1);
            g.addEdge(e); // all disconnected
            control.add(e);
        }
    
        assertEquals(100, g.numVertices());
        assertEquals(50, g.numEdges());
        assertEquals(50, edges.size());
        
        Iterator<Edge> iter = edges.iterator();
        while (iter.hasNext()) {
            iter.next();
            iter.remove();
        }
        assertEquals(0, g.numEdges());
        assertEquals(0, edges.size());
        assertEquals(100, g.numVertices());            
    }

}