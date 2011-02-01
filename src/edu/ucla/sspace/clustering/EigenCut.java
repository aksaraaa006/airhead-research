package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * An interface for computing the spectral cut of a {@link Matrix}.
 *
 * @see BaseSpectralCut
 *
 * @author Keith Stevens
 */
public interface EigenCut {

    /**
     * Returns the sum of values in {@code rho}.
     */
    double rhoSum();

    /**
     * Computes the similarity between each data point and centroid of the data
     * set.
     */
    DoubleVector computeRhoSum(Matrix matrix);

    /**
     * Compute the cut with the lowest conductance for the data set.  This
     * involves the following main steps:
     *   <ol>
     *     <li> Computing the second eigen vector of the data set.</li>
     *     <li> Sorting both the eigen vector, and each dimensions
     *     corresponding data point, based on the eigen values.</li>
     *     </li> Dividing the original data matrix into two regions, which are
     *     hopefully of equal size.</li>
     *   </ol>
     * 
     * </p>
     *
     * The resulting regions are accessible by in {@link #getLeftCut} and {@link
     * #getRightCut}.
     */
    void computeCut(Matrix matrix);

    /**
     * Return the ordering of the first region with respect to the original data
     * set.
     */
    int[] getLeftReordering();

    /**
     * Returns the data set in the first (left) region.
     */
    Matrix getLeftCut();

    /**
     * Returns the data set in the second (right) region.
     */
    Matrix getRightCut();

    /**
     * Return the ordering of the second region with respect to the original
     * data set.
     */
    int[] getRightReordering();

    /**
     * Returns the K-Means objective score of the entire data set, i.e. the sum
     * of the similarity between each dataset and the centroid.
     */
    double getKMeansObjective();

    /**
     * Returns the K-Means objective computed over the two regions computed over
     * the data set.
     */
    double getKMeansObjective(double alpha, double beta,
                              int leftNumClusters, int[] leftAssignments,
                              int rightNumClusters, int[] rightAssignments);
    /**
     * Returns the score for the relaxed correlation objective when the data
     * matrix is divided into multiple clusters.  The relaxed correlation
     * objective measures both inter-cluster similarity and intra-cluster
     * dissimilarity.  A high score means that values with in a cluster are
     * highly similar and each cluster is highly distinct.  This is to be used
     * after clustering values in each sub region.
     * 
     * @param alpha The weight given to the inter-cluster similarity.
     * @param beta The weight given to the intra-cluster similarity.
     * @param leftNumClusters The number of clusters found in the left split
     * @param leftAssignments The assignments for data points in the left region
     * @param rightNumClusters The number of clusters found in the right split
     * @param rightAssignments The assignments for data points in the right
     *        region
     */
    double getSplitObjective(double alpha, double beta,
                             int leftNumClusters, int[] leftAssignments,
                             int rightNumClusters, int[] rightAssignments);

    /**
     * Returns the score for the relaxed correlation objective over the entire
     * data set, undivided.
     */
    double getMergedObjective(double alpha, double beta);
}
