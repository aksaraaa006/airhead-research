

package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.VectorMath;


/**
 * @author Keith Stevens
 */
public class G1Function extends BaseFunction {

    /**
     * The centroid for all data points if they were assigned to a single
     * cluster.
     */
    private DoubleVector completeCentroid;

    /**
     * The distance from each cluster to {@code completeCentroid}.
     */
    private double[] simToComplete;

    /**
     * Constructs a new {@link G1Function}.
     */
    public G1Function() {
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
     * @param completeCentroid The new summation vector of all data points
     * @param simToComplete The distance from each cluster to {@code
     *        completeCentroid}
     */
    G1Function(List<DoubleVector> matrix,
               DoubleVector[] centroids,
               double[] costs,
               int[] assignments,
               int[] clusterSizes,
               int[] clusterSizes,
               DoubleVector completeCentroid,
               double[] simToComplete) {
        super(matrix, centroids, costs, assignments, clusterSizes);
        this.completeCentroid = completeCentroid;
        this.simToComplete = simToComplete;
    }

    /**
     * {@inheritDoc}
     */
    public void setup(Matrix m, int[] initialAssignments, int numClusters) {
        super.setup(m, initialAssignments, numClusters);

        completeCentroid = new DenseVector(m.rows());
        for (DoubleVector v : matrix)
            VectorMath.add(completeCentroid, v);

        for (int c = 0; c < centroids.length; ++c)
            simToComplete[c] = Similarity.cosineSimilarity(
                    centroids[c], completeCentroid);
    }

    /**
     * {@inheritDoc}
     */
    protected double getOldCentroidScore(DoubleVector altCurrentCentroid,
                                         int altClusterSize) {
        double newScore = Similarity.cosineSimilarity(
                altCurrentCentroid, completeCentroid);
        newScore /= Math.pow(altCurrentCentroid.magnitude(), 2);
        return newScore;
    }

    protected double getNewCentroidScore(int newCentroidIndex,
                                         DoubleVector dataPoint) {
        double newScore = Similarity.cosineSimilarity(
                dataPoint, completeCentroid);
        newScore += simToComplete[newCentroidIndex];
        newScore /= modifiedMagnitudeSqrd(
                centroids[newCentroidIndex], dataPoint);
        return newScore;
    }

    /**
     * {@inheritDoc}
     */
    public boolean maximize() {
        return false;
    }
}
