
package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.vector.DoubleVector;

import java.util.List;


/**
 * @author Keith Stevens
 */
public class I1Function extends BaseFunction {

    /**
     * Constructs a new {@link BaseFunction}.
     */
    public I1Function() {
    }

    /**
     * A package private constructor for all {@link CriterionFunction}s
     * subclassing from this {@link BaseFunction}.  This is to facilitate the
     * implementation of {@link HybridBaseFunction}.  The provided objects are
     * intended to replace those that would have been computed by {@link
     * #setup(Matrix, int[], int) setup} so that one class can do this work once
     * and then share the computed values with other functions.
     *
     * @param matrix The list of normalized data points that are to be
     *        clustered
     * @param centroids The set of centroids associated with the dataset.
     * @param costs The set of costs for each centroid.
     * @param assignments The initial assignments for each cluster.
     * @param clusterSizes The size of each cluster.
     */
    I1Function(List<DoubleVector> matrix,
               DoubleVector[] centroids,
               double[] costs,
               int[] assignments,
               int[] clusterSizes) {
        super(matrix, centroids, costs, assignments, clusterSizes);
    }

    /**
     * {@inheritDoc}
     */
    protected double getOldCentroidScore(DoubleVector altCurrentCentroid,
                                         int altClusterSize) {
        return Math.pow(altCurrentCentroid.magnitude(), 2) / altClusterSize;
    }

    /**
     * {@inheritDoc}
     */
    protected double getNewCentroidScore(int newCentroidIndex,
                                         DoubleVector dataPoint) {
        return modifiedMagnitudeSqrd(centroids[newCentroidIndex], dataPoint) / 
               (clusterSizes[newCentroidIndex]+1);
    }

    /**
     * {@inheritDoc}
     */
    public boolean maximize() {
        return true;
    }
}
