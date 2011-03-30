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

package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


/**
 ** This {@link CriterionFunction} implements the basic functionality needed for
 * a majority of the hybrid functions available.  It works by first gathering a
 * handful of meta data for the data set, such as the cluster sizes, initial
 * cluster assignments, and initial centroids.  It then implements {@link
 * #update(int) update} and requires subclasses to implement functions for
 * determining the change in the criterion score due to moving a data point.
 * Hybrid {@link CriterionFunction}s utilize an internal and external criterion
 * function in order to balance between both objectives in order to create a
 * well balanced clustering.
 *
 * @author Keith Stevens
 */
public abstract class HybridBaseFunction implements CriterionFunction {

    /**
     * The {@link Matrix} holding the data points.
     */
    protected List<DoubleVector> matrix;

    /**
     * The set of cluster assignments for each cluster.
     */
    protected int[] assignments;

    /**
     * The centroids representing each cluster.
     */
    protected DoubleVector[] centroids;

    /**
     * The number of data points found in each cluster.
     */
    protected int[] clusterSizes;

    /**
     * The cost computed for each cluster.  This is maintained seperatly so that
     * update can modify only the  two relevant clusters being modified.
     */
    protected double[] e1Costs;

    /**
     * The total cost of all {@code e1Costs}.
     */
    private double e1Cost;

    /**
     * The cost computed for each cluster.  This is maintained seperatly so that
     * update can modify only the  two relevant clusters being modified.
     */
    protected double[] i1Costs;

    /**
     * The total cost of all {@code i1Costs}.
     */
    private double i1Cost;

    /**
     * The total clustering cost.
     */
    private double totalCost;

    /**
     * The summation vector of all data points.
     */
    protected DoubleVector completeCentroid;

    /**
     * The distance of each centroid to {@code completeCentroid}.
     */
    protected double[] simToComplete;

    /**
     * The internal {@link CriterionFunction} used.
     */
    private BaseFunction i1Func; 

    /**
     * The internal {@link CriterionFunction} used.
     */
    private BaseFunction e1Func; 

    /**
     * {@inheritDoc}
     */
    public void setup(Matrix m, int[] initialAssignments, int numClusters) {
        // Save the meta data we need to maintain.
        assignments = initialAssignments;
        matrix = new ArrayList<DoubleVector>(m.rows());
        for (int i = 0; i < m.rows(); ++i)
            matrix.add(Vectors.scaleByMagnitude(m.getRowVector(i)));

        // Initialize the cluster information.
        centroids = new DoubleVector[numClusters];
        clusterSizes = new int[numClusters];
        e1Costs = new double[numClusters];
        i1Costs = new double[numClusters];

        // Initialize the clusters.
        for (int c = 0; c < numClusters; ++c)
            centroids[c] = new DenseVector(m.columns());

        // Form the cluster composite vectors, i.e. unscaled centroids.
        for (int i = 0; i < m.rows(); ++i) {
            int assignment = initialAssignments[i];
            VectorMath.add(centroids[assignment], matrix.get(i));
            clusterSizes[assignment]++;
        }

        // Compute the complete summation vector.
        completeCentroid = new DenseVector(m.rows());
        for (DoubleVector v : matrix)
            VectorMath.add(completeCentroid, v);

        // Compute the distance from each centroid to the summation vector.
        for (int c = 0; c < centroids.length; ++c)
            simToComplete[c] = Similarity.cosineSimilarity(
                    centroids[c], completeCentroid);

        // Get each function used in this hybrid method.
        i1Func = getInternalFunction();
        e1Func = getExternalFunction();

        // Compute the cost of each centroid.
        for (int c = 0; c < numClusters; ++c) {
            if (clusterSizes[c] != 0) {
                // Compute the internal costs.
                i1Costs[c] = i1Func.getOldCentroidScore(
                        centroids[c], clusterSizes[c]);
                i1Cost += i1Costs[c];

                // Compute the external costs.
                e1Costs[c] = e1Func.getOldCentroidScore(
                        centroids[c], clusterSizes[c]);
                e1Cost += e1Costs[c];
            }
        }

        // Compute the total cost.
        totalCost = i1Cost / e1Cost;
    }

    /**
     * {@inheritDoc}
     */
    public boolean update(int currentVectorIndex) {
        int currentClusterIndex = assignments[currentVectorIndex];

        // Setup the inital cost for the individual changes.
        double bestE1Cost = 0;
        double bestI1Cost = 0;

        // Setup the initial total cost for the individual changes.
        double bestE1Score = e1Cost;
        double bestI1Score = i1Cost;

        // Setup the best cost and index for the best cost.
        double bestNewCost = 0;
        int bestDeltaIndex = -1;

        // Get the current vector.
        DoubleVector vector = matrix.get(currentVectorIndex);

        // Get the base cost for removing the data point from the current
        // cluster.
        double baseE1Cost = 0;
        double baseI1Cost = 0;

        // Get the current centroid without the current data point assigned to
        // it.  Compute the cost delta with that point removed from the cluster.
        DoubleVector altCurrentCentroid = BaseFunction.subtract(
                centroids[currentClusterIndex], vector);
        // Remove the cost of the current cost and add the cost of the altered
        // centroid for external and internal functions.
        if (clusterSizes[currentClusterIndex] > 1) {
            bestE1Score -= e1Costs[currentClusterIndex];
            baseE1Cost = e1Func.getOldCentroidScore(
                    altCurrentCentroid, clusterSizes[currentClusterIndex] - 1);
            bestE1Score += baseE1Cost;

            bestI1Score -= i1Costs[currentClusterIndex];
            baseI1Cost = i1Func.getOldCentroidScore(
                    altCurrentCentroid, clusterSizes[currentClusterIndex] - 1);
            bestI1Score += baseI1Cost; 
        }

        // Compute the cost delta for moving that data point to each of the
        // other possible clusters.
        for (int i = 0; i < centroids.length; ++i) {
            // Skip the cluster the data point is already assigned to.
            if (currentClusterIndex == i)
                continue;

            // Compute the cost of adding the data point to the current
            // alternate cluster.  Do this by first removing the old cost and
            // then adding the new cost of the changed centroid for external and
            // internal functions.
            double newE1Score = bestE1Score - e1Costs[i];
            double newE1Cost = e1Func.getNewCentroidScore(i, vector);
            newE1Score += newE1Cost;

            double newI1Score = bestI1Score - i1Costs[i];
            double newI1Cost = i1Func.getNewCentroidScore(i, vector);
            newI1Score += newI1Cost;

            // If the new score is better than the old score, update the best
            // values.
            double newScore = newI1Score / newE1Score;
            if (newScore > totalCost && newScore > bestNewCost) {
                bestNewCost = newScore;
                bestE1Score = newE1Score;
                bestI1Score = newI1Score;
                bestE1Cost = newE1Cost;
                bestI1Cost = newI1Cost;
                bestDeltaIndex = i;
            }
        }

        // If the best delta index was modified, make an update and return true.
        if (bestDeltaIndex >= 0) {
            e1Costs[currentClusterIndex] += baseE1Cost;
            i1Costs[currentClusterIndex] += baseI1Cost;

            e1Costs[bestDeltaIndex] += bestE1Cost;
            i1Costs[bestDeltaIndex] += bestI1Cost;

            // Update the sizes.
            clusterSizes[currentClusterIndex]--;
            clusterSizes[bestDeltaIndex]++;

            // Update the centroids.
            centroids[currentClusterIndex] = altCurrentCentroid;
            centroids[bestDeltaIndex] = VectorMath.add(
                centroids[bestDeltaIndex], vector);

            // Update the assignment.
            assignments[currentVectorIndex] = bestDeltaIndex;
            return true;
        }

        // Otherwise, this data point cannot be relocated, so return false.
        return false;
    }

    /**
     * {@inheritDoc}
     */
    public int[] assignments() {
        return assignments;
    }

    /**
     * {@inheritDoc}
     */
    public DoubleVector[] centroids() {
        return centroids;
    }

    /**
     * {@inheritDoc}
     */
    public int[] clusterSizes() {
        return clusterSizes;
    }

    /**
     * {@inheritDoc}
     */
    public double score() {
        return totalCost;
    }

    /**
     * Returns the internal {@link CriterionFunction}.
     */
    protected abstract BaseFunction getInternalFunction();

    /**
     * Returns the external {@link CriterionFunction}.
     */
    protected abstract BaseFunction getExternalFunction();
}
