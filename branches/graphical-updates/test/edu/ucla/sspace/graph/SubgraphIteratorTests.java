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
    

}