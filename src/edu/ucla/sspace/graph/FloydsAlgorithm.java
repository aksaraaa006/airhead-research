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

import java.util.Set;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;

public class FloydsAlgorithm {

    public FloydsAlgorithm() { }

    public Matrix computeAllPairsDistance(Graph<? extends Edge> g) {
        int verts = g.order();

        // dm = shorthand for distanceMatrix
        Matrix dm = new ArrayMatrix(verts, verts);
        
        // Initialize the distance matrix with the shortest path.

        // Check whether the graph has edge weights
        if (g instanceof WeightedGraph) {
            WeightedGraph wg = (WeightedGraph)g;
            for (int i = 0; i < verts; ++i) {
                for (int j = 0; j < verts; ++j) {
                    Double weight = Double.MAX_VALUE;
                    Set<WeightedEdge> edges = wg.getEdges(i, j);
                    if (edges != null) {
                        for (WeightedEdge e : edges) {
                            if (e.from() == i && e.to() == j
                                    && e.weight() < weight)
                                weight = e.weight();
                        }
                    }
                    dm.set(i, j, weight);
                }
            }
        }
        // If unweighted, assume unit distance for all edges
        else {
            for (int i = 0; i < verts; ++i) {
                Set<Integer> adjacent = g.getNeighbors(i);
                for (int j = 0; j < verts; ++j) {
                    dm.set(i, j,
                        (adjacent.contains(j)) 
                            ? 1 : Double.MAX_VALUE);
                }
            }
        }

        for (int i = 0; i < verts; ++i) {
            for (int j = 0; j < verts; ++j) {
                for (int k = 0; k < verts; ++k) {
                    dm.set(i, j, Math.min(dm.get(i,j), 
                                          dm.get(i,k) + dm.get(k,j)));
                }
            }
        }
        return dm;
    }

}