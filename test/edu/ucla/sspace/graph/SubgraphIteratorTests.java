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
 * Tests for the {@link SubgraphIterator}.
 */
public class SubgraphIteratorTests { 

    @Test public void testConstructor() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.addEdge(new SimpleEdge(i, j));
        }    

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, 3);
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorNonpositive() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.addEdge(new SimpleEdge(i, j));
        }    

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, -1);
    }

    @Test(expected=NullPointerException.class) public void testConstructorNull() {
        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(null, 10);
    }

    @Test(expected=IllegalArgumentException.class) public void testConstructorSizeTooLarge() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // fully connected
        for (int i = 0; i < 10; i++)  {
            for (int j = i+1; j < 10;  ++j)
                g.addEdge(new SimpleEdge(i, j));
        }    

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, 20);
    }

    @Test public void testWorkedExample() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.addVertex(i);
        }
        g.addEdge(new SimpleEdge(1, 2));
        g.addEdge(new SimpleEdge(1, 3));
        g.addEdge(new SimpleEdge(1, 4));
        g.addEdge(new SimpleEdge(1, 5));
        g.addEdge(new SimpleEdge(2, 3));
        g.addEdge(new SimpleEdge(2, 6));
        g.addEdge(new SimpleEdge(2, 7));
        g.addEdge(new SimpleEdge(3, 8));
        g.addEdge(new SimpleEdge(3, 9));

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(16, numSubgraphs);
    }

    @Test public void testOneSubgraph() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.addVertex(i);
        }
        g.addEdge(new SimpleEdge(1, 2));
        g.addEdge(new SimpleEdge(1, 3));
        g.addEdge(new SimpleEdge(2, 3));

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(1, numSubgraphs);
    }
 
    @Test public void testNoSubgraph() {
        Graph<Edge> g = new SparseUndirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.addVertex(i);
        }

        SubgraphIterator<Edge> iter = new SubgraphIterator<Edge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(0, numSubgraphs);
    }

    /*
     *
     * Tests on DirectedGraph instances
     *
     */

    @Test public void testDirectedGraph() {
        Graph<DirectedEdge> g = new SparseDirectedGraph();

        // Graph from the paper
        for (int i = 0; i < 3; i++)  {
            for (int j = i+1; j < 3; j++)  {
                g.addEdge(new SimpleDirectedEdge(i, j));
                g.addEdge(new SimpleDirectedEdge(j, i));
            }
        }

        SubgraphIterator<DirectedEdge> iter = 
            new SubgraphIterator<DirectedEdge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(1, numSubgraphs);
    }

    @Test public void testDirectedGraphFan() {
        Graph<DirectedEdge> g = new SparseDirectedGraph();


        g.addEdge(new SimpleDirectedEdge(0, 1));
        g.addEdge(new SimpleDirectedEdge(0, 2));

        checkSize(1, 3, g);
        
        // Add an edge, which should increase the subgraphs to 3
        g.addEdge(new SimpleDirectedEdge(0, 3));
        checkSize(3, 3, g);

        g.addEdge(new SimpleDirectedEdge(0, 4));
        checkSize(6, 3, g);

        // Add an edge that only completes a cycle but does not increase the
        // reachable set for any vertex
        g.addEdge(new SimpleDirectedEdge(1, 2));
        checkSize(6, 3, g);

        g.addEdge(new SimpleDirectedEdge(2, 3));
        checkSize(7, 3, g);

        g.addEdge(new SimpleDirectedEdge(3, 4));
        checkSize(8, 3, g);

        g.addEdge(new SimpleDirectedEdge(1, 3));
        checkSize(9, 3, g);
    }

    private void checkSize(int expectedSize, int subgraphSize, Graph<?> g) {
        @SuppressWarnings("unchecked")
        SubgraphIterator iter = new SubgraphIterator(g, subgraphSize);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(expectedSize, numSubgraphs);
    }

    @Test public void testWorkedExampleOnDirectedGraph() {
        Graph<DirectedEdge> g = new SparseDirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.addVertex(i);
        }
        g.addEdge(new SimpleDirectedEdge(1, 2));
        g.addEdge(new SimpleDirectedEdge(1, 3));
        g.addEdge(new SimpleDirectedEdge(1, 4));
        g.addEdge(new SimpleDirectedEdge(1, 5));
        g.addEdge(new SimpleDirectedEdge(2, 3));
        g.addEdge(new SimpleDirectedEdge(2, 6));
        g.addEdge(new SimpleDirectedEdge(2, 7));
        g.addEdge(new SimpleDirectedEdge(3, 8));
        g.addEdge(new SimpleDirectedEdge(3, 9));

        SubgraphIterator<DirectedEdge> iter = new SubgraphIterator<DirectedEdge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(16, numSubgraphs);
    } 

    @Test public void testTwoComponents() {
        Graph<DirectedEdge> g = new SparseDirectedGraph();

        // Graph from the paper
        for (int i = 1; i < 9; i++)  {
            g.addVertex(i);
        }
        g.addEdge(new SimpleDirectedEdge(1, 2));
        g.addEdge(new SimpleDirectedEdge(1, 3));
        g.addEdge(new SimpleDirectedEdge(3, 5));
        g.addEdge(new SimpleDirectedEdge(4, 6));

        SubgraphIterator<DirectedEdge> iter = new SubgraphIterator<DirectedEdge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(2, numSubgraphs);
    } 

    @Test public void testProblematicGraph() {
        Graph<DirectedEdge> g = new SparseDirectedGraph();
        g.addEdge(new SimpleDirectedEdge(0,1));
        g.addEdge(new SimpleDirectedEdge(0,2));
        g.addEdge(new SimpleDirectedEdge(0,3));
        g.addEdge(new SimpleDirectedEdge(0,4));
        g.addEdge(new SimpleDirectedEdge(1,2));
        g.addEdge(new SimpleDirectedEdge(2,3));
        g.addEdge(new SimpleDirectedEdge(3,4));
        g.addEdge(new SimpleDirectedEdge(1,3));
        g.addEdge(new SimpleDirectedEdge(4,5));
        g.addEdge(new SimpleDirectedEdge(5,6));
        g.addEdge(new SimpleDirectedEdge(6,4));
        g.addEdge(new SimpleDirectedEdge(0,7));
        g.addEdge(new SimpleDirectedEdge(6,7));
        g.addEdge(new SimpleDirectedEdge(7,0));
        g.addEdge(new SimpleDirectedEdge(7,6));
        g.addEdge(new SimpleDirectedEdge(7,8));
        g.addEdge(new SimpleDirectedEdge(8,9));
        g.addEdge(new SimpleDirectedEdge(10,11));
        g.addEdge(new SimpleDirectedEdge(11,12));
        g.addEdge(new SimpleDirectedEdge(10,12));

        for (Integer v : g.vertices())
            System.out.println("g has vertex " + v);
        
        SubgraphIterator<DirectedEdge> iter = new SubgraphIterator<DirectedEdge>(g, 3);
        int numSubgraphs = 0;
        while (iter.hasNext()) {
            iter.next();
            numSubgraphs++;
        }
        assertEquals(25, numSubgraphs);        
    }

}