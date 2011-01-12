/*
 * Copyright 2011 Keith Stevens 
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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseScaledDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Keith Stevens
 */
public class Assignments implements Iterable<Assignment> {

    private Assignment[] assignments;

    private int numClusters;

    public Assignments(int numClusters, int numAssignments) {
        numClusters = numClusters;
        assignments = new Assignment[numAssignments];
    }

    public Assignments(int numClusters, Assignment[] initialAssignments) {
        numClusters = numClusters;
        assignments = initialAssignments;
    }

    public void set(int i, Assignment assignment) {
        assignments[i] = assignment;
    }

    public int length() {
        return assignments.length;
    }

    public Iterator<Assignment> iterator() {
        return new ArrayIterator(assignments);
    }

    public Assignment get(int i) {
        return assignments[i];
    }

    public int numClusters() {
        return numClusters;
    }

    public Assignment[] assignments() {
        return assignments;
    }

    public DoubleVector[] getCentroids(Matrix dataMatrix) {
        DoubleVector[] centroids = new DoubleVector[numClusters];
        int[] counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseVector(dataMatrix.columns());

        int row = 0;
        for (Assignment assignment : assignments) {
            if (assignment.length() != 0) {
                counts[assignment.assignments()[0]]++;
                DoubleVector centroid = centroids[assignment.assignments()[0]];
                VectorMath.add(centroid, dataMatrix.getRowVector(row));
            }
            row++;
        }

        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new ScaledDoubleVector(centroids[c], 1/counts[c]);

        return centroids;
    }

    public SparseDoubleVector[] getCentroids(SparseMatrix dataMatrix) {
        SparseDoubleVector[] centroids = new SparseDoubleVector[numClusters];
        int[] counts = new int[numClusters];
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new CompactSparseVector(dataMatrix.columns());

        int row = 0;
        for (Assignment assignment : assignments) {
            if (assignment.length() != 0) {
                counts[assignment.assignments()[0]]++;
                SparseDoubleVector centroid =
                    centroids[assignment.assignments()[0]];
                VectorMath.add(centroid, dataMatrix.getRowVector(row));
            }
            row++;
        }

        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new SparseScaledDoubleVector(
                    centroids[c], 1/counts[c]);

        return centroids;
    }

    private class ArrayIterator implements Iterator<Assignment> {

        Assignment[] values;

        int index;

        public ArrayIterator(Assignment[] values) {
            this.values = values;
            index = 0;
        }

        public void remove() {
            throw new UnsupportedOperationException(
                    "Cannot remove from an ArrayIterator");
        }

        public boolean hasNext() {
            return index < values.length;
        }

        public Assignment next() {
            return values[index++];
        }
    }
}
