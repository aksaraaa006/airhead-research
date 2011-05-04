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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import edu.ucla.sspace.util.Indexer;


/**
 * A collection of static utility methods for reading and writing graphs.
 */
public class GraphIO {

    public enum GraphType {
        UNDIRECTED,
        DIRECTED,
        WEIGHTED,
        MULTIGRAPH
    }

    private GraphIO() { }

    public static Graph<? extends Edge> read(File f, GraphType type) throws IOException {
        switch (type) {
        case UNDIRECTED:
            return readUndirected(f);
        case DIRECTED:
            return readDirected(f);
        default: 
            throw new Error("Reading GraphType " + type + " is current unsupported");
        }
    }

    public static Graph<Edge> readUndirected(File f) throws IOException {
        Indexer<String> vertexIndexer = new Indexer<String>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        Graph<Edge> g = new SparseUndirectedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) {
                throw new IOException("Missing vertex on line " + lineNo);
            }
            int v1 = vertexIndexer.add(arr[0]);
            int v2 = vertexIndexer.add(arr[1]);
            g.addEdge(new SimpleEdge(v1, v2));
        }
        return g;
    }

    public static Graph<DirectedEdge> readDirected(File f) throws IOException {
        Indexer<String> vertexIndexer = new Indexer<String>();
        BufferedReader br = new BufferedReader(new FileReader(f));
        Graph<DirectedEdge> g = new SparseDirectedGraph();
        int lineNo = 0;
        for (String line = null; (line = br.readLine()) != null; ) {
            ++lineNo;
            line = line.trim();
            if (line.startsWith("#"))
                continue;
            else if (line.length() == 0)
                continue;
            String[] arr = line.split("\\s+");
            if (arr.length < 2) {
                throw new IOException("Missing vertex on line " + lineNo);
            }
            int v1 = vertexIndexer.add(arr[0]);
            int v2 = vertexIndexer.add(arr[1]);
            g.addEdge(new SimpleDirectedEdge(v1, v2));
        }
        return g;
    }
}