/*
 * Copyright 2010 Keith Stevens 
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

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


public class ClusterUtil {

    /**
     * Returns the centroids of a {@link Matrix} based on the assignments for
     * each data point.  This is only a helper function for users of {@link
     * KMeansClustering} so that they can reconstruct the centroids.
     */
    public static DoubleVector[] computeCentroids(Matrix dataPoints,
                                                  Assignment[] assignments,
                                                  int numCentroids) {
        DoubleVector[] centroids = new DoubleVector[numCentroids];
        for (int c = 0; c < numCentroids; ++c)
            centroids[c] = new DenseVector(dataPoints.columns());

        double[] numAssignments = new double[numCentroids];
        for (int i = 0; i < dataPoints.rows(); ++i) {
            int assignment = assignments[i].assignments()[0];
            VectorMath.add(centroids[assignment], dataPoints.getRowVector(i));
            numAssignments[assignment]++;
        }

        for (int c = 0; c < numCentroids; ++c)
            if (numAssignments[c] > 0)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c], 1/numAssignments[c]);
        return centroids;
    }

    /**
     * Returns the K-Means objective score of a given solution.
     */
    public static double computeObjective(Matrix dataPoints,
                                          DoubleVector[] centroids,
                                          Assignment[] assignments) {
        double objective = 0;
        for (int i = 0; i < dataPoints.rows(); ++i) {
            int assignment = assignments[i].assignments()[0];
            objective += distance(
                    centroids[assignment], dataPoints.getRowVector(i));
        }
        return objective;
    }

    /**
     * Computes the euclidean distance between {@code v2} and {@code v2}.
     * {@code v1} is expected to be a dense vector representing a centroid and
     * {@code v2} is expected to be a data point.  When {@code v2} is sparse,
     * this method uses a special case formutlation of euclidean distance that
     * relies on the cached magnitude of {@code v1} in order to have a runtime
     * that is nearly linear with respect to the number of non zero values in
     * {@code v2}.  
     */
    public static double distance(DoubleVector v1, DoubleVector v2) {
        // If v2 is not sparse, just use the default implementation for
        // euclidean distance.
        if (!(v2 instanceof SparseVector))
            return Similarity.euclideanDistance(v1, v2);

        // If v2 is sparse, use a special case where we use the cached magnitude
        // of v1 and the sparsity of v2 to avoid most of the computations.
        SparseVector sv2 = (SparseVector)v2;
        int[] sv2NonZero = sv2.getNonZeroIndices();
        double sum = 0;

        // Get the magnitude for v1.  This value will only be computed once for
        // the centroid since the DenseVector caches the magnitude, thus saving
        // a large amount of computation.
        double v1Magnitude = Math.pow(v1.magnitude(), 2);

        // Compute the difference between the nonzero values of v2 and the
        // corresponding values for v1.
        for (int index : sv2NonZero) {
            double value = v1.get(index);
            // Decrement v1's value at this index from it's magnitude.
            v1Magnitude -= Math.pow(value, 2);
            sum += Math.pow(value - v2.get(index), 2);
        }

        // Since the rest of v2's values are 0, the difference between v1 and v2
        // for these values is simply the magnitude of indices which have not
        // yet been traversed in v1.  This corresponds to the modified
        // magnitude that was computed.
        sum += v1Magnitude;

        return Math.sqrt(sum);
    }
}
