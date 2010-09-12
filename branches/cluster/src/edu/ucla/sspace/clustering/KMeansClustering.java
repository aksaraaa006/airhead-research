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
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.VectorIO;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.BitSet;
import java.util.Random;
import java.util.Properties;

import java.util.logging.Logger;


/**
 * An implementation of the K-Means, or Llyod's, clustering algorithm.  This
 * implementation first selects a set of seed centroids by using a {@link
 * KMeansSeed} implementation.  Then, data points are assigned to the set of
 * centroids and centroids are recomputed based on the assignments.  This
 * assignment/recomputation is done until two consecutive sets of centroids do
 * not differ, according to some small margin.
 *
 * </p> 
 * Users should be aware that {@link ClutoClustering} also provides a K-Means
 * implementation.  This K-Means implementation is designed such that different
 * seeding methods can be compared against each other and to side step any bugs
 * which may be present in the CLUTO clustering package, and thus never to be
 * fixed.  One advantage is that users of this implementation can create a
 * {@link KMeansSeed} implementation that generates an optimal seeding
 * assignment for use as a golden standard.
 *
 * @see KMeansSeed
 * @author Keith Stevens
 */
public class KMeansClustering implements Clustering {

    /**
     * An enum that specifies the seeding algorithm to be used for K-Means.
     * K-Means currently supports the following seeding algorithms: </br>
     *
     * <center>
     * <table valign="top" border="1" width="800">
     *
     * <tr><td><center>Algorithm</center></td>
     *   <td><center>description</center></td></tr>
     *
     * <tr><td valign="top">{@link SeedAlgorithm#KMEANS_PLUS_PLUS}</td>
     *      <td>This seeding technique attempts to select centroids from the set
     *      of data points that are well scattered.  This is done first
     *      selecting a data point at random as the first centroid and then
     *      selected data points to be the next centroid with a probability
     *      proportional to the distance between the data point and the nearest
     *      centroid.  It is based on the following paper:
     *   <li style="font-family:Garamond, Georgia, serif">      David Arthur,
     *   Sergei Vassilvitskii, "k-means++: the advantages of careful seeding,"
     *   in <i>Symposium on Discrete Algorithms</i> and <i>Proceedings of the
     *   eighteenth annual ACM-SIAM symposium on Discrete algorithms</i>,
     *   2007</li>
     *
     * <tr><td valign="top">{@link SeedAlgorithm#OPTIMAL}</td>
     *     <td> The optimal seeds are generated for the data set.  This requires
     *          a text file with the true set of data point assignments.  This
     *          method is only to be used for evaluation of the amount of error
     *          generated by K-Means.</td>
     * </tr>
     *
     * <tr><td valign="top">{@link SeedAlgorithm#ORSS}</td>
     *     <td> Seeds are selected from the data such that they are well
     *     scattered.  This implementation has a runtime of O(nkd)
     *     and is is based on the following paper:
     *     <li style="font-family:Garamond, Georgia, serif"> Rafail Ostrovsky ,
     *     Yuval Rabani ,  Leonard J. Schulman ,  Chaitanya Swamy, "The
     *     effectiveness of lloyd-type methods for the k-means problem," in
     *     <i>47th IEEE Symposium on the Foundations of Computer Science</i>,
     *     2006</li>
     * </tr>
     *
     * <tr><td valign="top">{@link SeedAlgorithm#RANDOM}</td>
     *     <td> Seeds are pulled out of a given data set at random, with a
     *          uniform probability of being selected given to each data
     *          point.</td>
     * </tr>
     *
     * </td>
     * </table>
     */
    public enum SeedAlgorithm {
        KMEANS_PLUS_PLUS,
        OPTIMAL,
        ORSS,
        RANDOM,
    }

    /**
     * The initial property prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.KMeasnsClustering";

    /**
     * A property for setting the {@link KMeansSeed} algorithm to use.
     */
    public static final String KMEANS_SEED_PROPERTY =
        PROPERTY_PREFIX + ".kMeansSeed";

    /**
     * A property for setting the optimal assignment file when using the {@link
     * SeedAlgorithm#OPTIMAL} seeding algorithm.
     */
    public static final String KMEANS_ASSIGNMENT_FILE =
        PROPERTY_PREFIX + ".kMeansOptimalAssignment";

    private static final String DEFAULT_SEED = "KMEANS_PLUS_PLUS";
    /**
     * A small number used to determine when the centroids have converged.
     */
    private static final double EPSILON = 1e-6;

    private static final Random random = new Random();

    private static final Logger LOGGER = 
        Logger.getLogger(ClutoClustering.class.getName());

    /**
     * Not implemented.
     */
    public Assignment[] cluster(Matrix dataPoints, Properties props) {
        throw new UnsupportedOperationException(
                "KMeansClustering requires that the " +
                "number of clusters be specified");
    }

    /**
     * {@inheritDoc}
     */
    public Assignment[] cluster(Matrix dataPoints, 
                                int numClusters,
                                Properties props) {
        // Get an instance of the seed generator.
        String seedProp = props.getProperty(KMEANS_SEED_PROPERTY, DEFAULT_SEED);
        SeedAlgorithm seedType = SeedAlgorithm.valueOf(seedProp);

        // Generate the initial seeds for K-Means.  We assume that the seeds
        // returned are either a subset of the data points or some set of
        // pre-normalized vectors.
        DoubleVector[] centroids = null;
        switch (seedType) {
            case RANDOM:
                centroids = randomSeed(numClusters, dataPoints);
                break;
            case KMEANS_PLUS_PLUS:
                centroids = kMeansPlusPlusSeed(numClusters, dataPoints);
                break;
            case OPTIMAL:
                centroids = optimalSeed(numClusters, dataPoints);
                break;
            case ORSS:
                centroids = orssSeed(numClusters, dataPoints);
                break;
        }

        // Initialize the assignments.
        Assignment[] assignments = new Assignment[dataPoints.rows()];

        // Iterate over the data points by first assigning data points to
        // existing centroids and then re-computing the centroids.  This
        // iteration will continue until the set of existing centroids and the
        // re-computed centroids do not differ by some margin of error.
        boolean converged = false;
        while (!converged) {
            // Setup the new set of centroids to be emtpy vectors..
            DoubleVector[] newCentroids = new DoubleVector[numClusters];
            for (int c = 0; c < numClusters; ++c)
                newCentroids[c] = new DenseVector(dataPoints.columns());
            double[] numAssignments = new double[numClusters];

            // Iterate through each data point.  Find the nearest centroid and
            // assign the data point to that centroid.  Also recompute the
            // centroids by adding the data point to a new centroid based on the
            // assignment.
            for (int d = 0; d < dataPoints.rows(); ++d) {
                DoubleVector dataPoint = dataPoints.getRowVector(d);

                // Find the nearest centroid.
                int nearestCentroid = 0;
                double minDistance = Double.MAX_VALUE;
                for (int c = 0; c < numClusters; ++c) {
                    double distance = distance(centroids[c], dataPoint);
                    if (distance < minDistance) {
                        minDistance = distance;
                        nearestCentroid = c;
                    }
                }

                // Include this data point in the new centroid.
                VectorMath.add(newCentroids[nearestCentroid], dataPoint);
                // Make the assignment.
                assignments[d] = new HardAssignment(nearestCentroid);
                numAssignments[nearestCentroid]++;
            }

            // Determine whether or not the centroids have changed at all.  If
            // there has been no significant change, then we can stop iterating
            // and use the most recent set of assignments.  Also, to save an
            // iteration, scale the new centroids based on the number of a data
            // points assigned to it.
            double centroidDifference = 0;
            for (int c = 0; c < numClusters; ++c) {
                // Scale the new centroid.
                if (numAssignments[c] > 0)
                    newCentroids[c] = new ScaledDoubleVector(
                            newCentroids[c], 1/numAssignments[c]);

                // Compute the difference.
                centroidDifference += Math.pow(
                        distance(centroids[c], newCentroids[c]), 2);
            }
            converged = centroidDifference < EPSILON;
            centroids = newCentroids;
        }

        // Return the last set of assignments made.
        return assignments;
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

    /**
     * Returns the centroids of a {@link Matrix} based on the assignments for
     * each data point.  This is only a helper function for users of {@link
     * KMeansClustering} so that they can reconstruct the centroids.
     */
    public static DoubleVector[] computeCentroids(Matrix dataPoints,
                                                  Assignment[] assignments,
                                                  int numCentroids) {
        DoubleVector[] centroids = new DoubleVector[numCentroids];
        double[] numAssignments = new double[numCentroids];
        for (int i = 0; i < dataPoints.rows(); ++i) {
            int assignment = assignments[i].assignments()[0];
            VectorMath.add(centroids[i], dataPoints.getRowVector(i));
            numAssignments[i]++;
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
            objective += distance(centroids[assignment],
                                       dataPoints.getRowVector(i));
        }
        return objective;
    }

    /**
     * Select seeds using the K-Means++ algorithm
     */
    public DoubleVector[] kMeansPlusPlusSeed(int numCentroids, 
                                             Matrix dataPoints) {
        int[] centroids = new int[numCentroids];
        // Select the first centroid randomly.
        DoubleVector[] centers = new DoubleVector[numCentroids];
        int centroidIndex = random.nextInt(dataPoints.rows());
        centers[0] = dataPoints.getRowVector(centroidIndex);

        // Compute the distance each data point has with the first centroid.
        double[] distances = new double[dataPoints.rows()];
        computeDistances(distances, false, dataPoints, centers[0]);

        // For each of the remaining centroids, select one of the data points,
        // p, with probability
        // p(dist(p, last_centroid)^2/sum_p(dist(p, last_centroid)^2))
        // This is an attempt to pick the data point which is furthest away from
        // the previously selected data point.
        for (int i = 1; i < numCentroids; ++i) {
            double sum = distanceSum(distances);
            double probability = random.nextDouble();
            centers[i] = dataPoints.getRowVector(
                    chooseWithProbability(distances, sum, probability));
            computeDistances(distances, true, dataPoints, centers[i]);
        }

        return centers;
    }

    /**
     * Computes the distance between each data point and the given centroid.  If
     * {@code selectMin} is set to true, then this will only overwrite the
     * values in {@code distances} if the new distance is smaller.  Otherwise
     * the new distance will always be stored in {@code distances}.
     *
     * @param distances An array of distances that need to be updated.
     * @param selectMin Set to true a new distance must smaller than the
     *                  current values in {@code distances}.
     * @param dataPoints The set of data points.
     * @param centroid The centroid to compare against.
     */
    private static void computeDistances(double[] distances,
                                         boolean selectMin,
                                         Matrix dataPoints,
                                         DoubleVector centroid) {
        for (int i = 0; i < distances.length; ++i) {
            double distance = Similarity.euclideanDistance(
                    centroid, dataPoints.getRowVector(i));
            if (!selectMin || selectMin && distance < distances[i])
                distances[i] = distance;
        }
    }

    /**
     * Returns the sum of distances squared.
     */
    private static double distanceSum(double[] distances) {
        double sum = 0;
        for (double distance : distances)
            sum += Math.pow(distance, 2);
        return sum;
    }

    /**
     * Returns a data point index i with probability 
     *   p(distances[i]^2/sum)
     */
    private static int chooseWithProbability(double[] distances,
                                             double sum,
                                             double probability) {
        for (int j = 0; j < distances.length; ++j) {
            double probOfDistance = Math.pow(distances[j], 2) / sum;
            probability -= probOfDistance;
            if (probability <= EPSILON) {
                return j;
            }
        }
        return distances.length-1;
    }

    /**
     * Select seeds using the Optimal algorithm.
     */
    public DoubleVector[] optimalSeed(int numCentroids, Matrix dataPoints) {
        String assignmentProp = System.getProperties().getProperty(
                KMEANS_ASSIGNMENT_FILE);
        if (assignmentProp == null)
            throw new IllegalArgumentException(
                    "An assignment file must be provided");

        DoubleVector[] centroids = new DoubleVector[numCentroids];
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                        assignmentProp));
            int numAssignments[] = new int[numCentroids];
            int i = 0;
            for (String line = null; (line = br.readLine()) != null; ) {
                int assignment = Integer.parseInt(line.split("\\s")[1]);
                if (centroids[assignment] == null)
                    centroids[assignment] = new DenseVector(
                            dataPoints.columns());
                VectorMath.add(centroids[assignment], 
                               dataPoints.getRowVector(i));
                numAssignments[assignment]++;
                i++;
            }

            for (int c = 0; c < numCentroids; ++c)
                centroids[c] = new ScaledDoubleVector(
                        centroids[c], 1/((double)numAssignments[c]));

        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return centroids;
    }

    /**
     * Select seeds using a modification of the ORSS algorithm.
     * This involves 4 steps:
     *
     * <ol>
     *   <li>First, the cost of an optimal solution to the K-Means
     *       problem is solved for the 1 cluster case along (called
     *       optimalSingleCost) with the centroid for this case (called c).</li>
     *   <li>Then, an intial seed is selected from the set of data points (X)
     *       with 
     *       probability p((optimalSingleCost + n*euclieanDist(x,c)^2)/2*n*optimalSingleCost) </li>
     *   <li>The second centroid is selected from X with probability
     *        p(euclideanDist(x, c_1)^2/(optimalSingleCost + n*euclideanDist(c,
     *        c_1)^2)) </li>
     *   <li>The remaining k-2 centroids are selected from X with probability
     *       p(euclideanDist(x, c_nearest)^2)
     *   </li>
     * </ol>
     */
    public DoubleVector[] orssSeed(int numCentroids, Matrix dataPoints) {
        int numDataPoints = dataPoints.rows();
        DoubleVector[] centroids = new DoubleVector[numCentroids];

        // Compute the centroid assuming that there is only one cluster to be
        // found.  This is required for computing the optimal cost of a single
        // cluster solution.
        DoubleVector singleCentroid = new DenseVector(dataPoints.columns());
        for (int i = 0; i < numDataPoints; ++i)
            VectorMath.add(singleCentroid, dataPoints.getRowVector(i));
        singleCentroid = new ScaledDoubleVector(
                singleCentroid, 1/((double)numDataPoints));

        // Compute the optimal cost of the single cluster case.
        double optimalSingleCost = 0;
        double[] distances = new double[numDataPoints];
        for (int i = 0; i < numDataPoints; ++i) {
            distances[i] = KMeansClustering.distance(
                    singleCentroid, dataPoints.getRowVector(i));
            optimalSingleCost += distances[i];
        }

        // Select the first centroid.  We pick the first centroid based on the
        // probability p((optimalSingleCost + n*euclideanDistance(x, c)^2)/
        //               2*n*optimalSingleCost)
        double probability = Math.random();
        double optimalDistance = 0;
        for (int i = 0; i < numDataPoints; ++i) {
            // Get the distance between each data point and the single
            // centroid.
            double distance = distances[i];
            // Compute the probability of selecting this data point.
            double cost = optimalSingleCost + numDataPoints*distance*distance;
            cost /= (2 * numDataPoints * optimalSingleCost);

            // Determine whether or not we should select the data point based on
            // it's probability.  Also save the distance from this new centroid
            // to the single centroid.
            probability -= cost;
            if (probability <= EPSILON) {
                centroids[0] = dataPoints.getRowVector(i);
                optimalDistance = distance;
                break;
            }
        }

        // Select the second centroid.
        probability = Math.random();
        
        // Precompute the normalizing factor for the probability of selecting a
        // data point.  In short this should be:
        //   optimalSingleCost * n*|euclideanDist(c, c_1)|^2
        double normalFactor = 
            optimalSingleCost + numDataPoints*Math.pow(optimalDistance, 2);

        // Store the set minimum distance each data point has to a single
        // centroid.  This is used later on for selecting the last k-2
        // centroids.
        double[] minDistances = new double[numDataPoints];
        boolean selected = false;
        for (int i = 0; i < numDataPoints; ++i) {
            // Compute the distance.  Since the first centroid is the only one,
            // we know its the closest so store the distance.
            double distance = Similarity.euclideanDistance(
                    centroids[0], dataPoints.getRowVector(i));
            minDistances[i] = distance;

            // Determine whether or not we should select this data point as the
            // second centroid.
            double cost = Math.pow(distance, 2) / normalFactor;
            probability -= cost;
            if (probability <= EPSILON && !selected) {
                centroids[1] = dataPoints.getRowVector(i);
                selected = true;
            }
        }

        // Select the remaining k-2 centroids.
        // We pick a data point to be a new centroid based on the following
        // probability:
        //   p(|euclideanDist(dataPoint, nearestCentroid)|^2)
        // Since we store the minimum distance each data point has to any given
        // centroid in minDistances, all we need to do is update that with the
        // distance between each data point and the most recently selected
        // centroid.
        for (int c = 2; c < numCentroids; ++c) {
            selected = false;
            probability = Math.random();
            for (int i = 0; i < numDataPoints; ++i) {
                // Compute the distance between each data point and the most
                // recently selected centroid.  Update the minimum distance
                // array if the recent centroid is nearer.
                double distance = Similarity.euclideanDistance(
                        centroids[c-1], dataPoints.getRowVector(i));
                if (distance < minDistances[c])
                    minDistances[i] = distance;

                // Determine whether or not we should select this data point as
                // the next centroid.
                double cost = Math.pow(minDistances[i], 2);
                probability -= cost;
                if (probability <= EPSILON && !selected) {
                    centroids[c] = dataPoints.getRowVector(i);
                    selected = true;
                }
            }
        }

        return centroids;
    }

    /**
     * Select seeds at random
     */
    public DoubleVector[] randomSeed(int numCentroids, Matrix dataPoints) {
        // Select a subset of data points to be the new centroids.
        BitSet selectedCentroids =
            Statistics.randomDistribution(numCentroids, dataPoints.rows());
        DoubleVector[] centers = new DoubleVector[numCentroids];

        // Convert the selection indices into vectors.
        for (int c = 0, i = selectedCentroids.nextSetBit(0); i >= 0;
                c++, i = selectedCentroids.nextSetBit(i+1)) {
            centers[c] = dataPoints.getRowVector(i);
        }
        return centers;
    }
}
