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

import edu.ucla.sspace.clustering.criterion.CriterionFunction;
import edu.ucla.sspace.clustering.criterion.I1Function;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.FileReader;
import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.Properties;


/**
 * An implementation of the Direct K-Means clustering available in the 
 * <a href="http://glaros.dtc.umn.edu/gkhome/views/cluto/">CLUTO Clustering
 * Software</a>.  This implementation of K-Means varies from the standard
 * implementation in a variety of ways:
 *
 * <li>
 *   <ol> After creating the initial set of clusters, each data point is
 *   assigned to a cluster if such a move improvies the overall clustering
 *   score.  This implementation is an iterative method, in that it makes the
 *   cluster change immediately and recomputes the centroid after every change.
 *   </ol>
 *   <ol> The clustering objective can be changed by using a different {@link
 *   edu.ucla.sspace.clustering.criterion.CriterionFunction CriterionFunction}.
 *   The {@link edu.ucla.sspace.clustering.criteria.I1Function I1Function}
 *   implements the standard K-Means criterion function.  Others try to optimize
 *   a variety of
 *   different objectives.</ol>
 * </li>
 *
 * @author Keith Stevens
 */
public class DirectClustering implements Clustering {

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
     * A property prefix for specifiying options when using {@link
     * DirectClustering}.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.DirectClustering";

    /**
     * The property to set the name of a {@link CriterionFunction} to use when
     * clustering the data.
     */
    public static final String CRITERIA_PROPERTY =
        PROPERTY_PREFIX + ".criteria";

    /**
     * The property to set the number of times a single run of {@link
     * DirectClustering} will be run.  If this is more than 1, the best scoring
     * run will be returned.
     */
    public static final String REPEAT_PROPERTY=
        PROPERTY_PREFIX + ".repeat";

    /**
     * A property for setting the seeding algorithm to use.
     */
    public static final String SEED_PROPERTY =
        PROPERTY_PREFIX + ".seed";

    /**
     * A property for setting the optimal assignment file when using the {@link
     * SeedAlgorithm#OPTIMAL} seeding algorithm.
     */
    public static final String OPTIMAL_ASSIGNMENT_FILE =
        PROPERTY_PREFIX + ".optimalAssignment";


    /**
     * The default seed algorithm used.
     */
    private static final String DEFAULT_SEED = "KMEANS_PLUS_PLUS";

    /**
     * The default {@link CriterionFunction} to be used while clustering.
     */
    private static final String DEFAULT_CRITERION = 
        "edu.ucla.sspace.clustering.criterion.I1Function";

    /**
     * The default number of repetitions.
     */
    private static final String DEFAULT_REPEATS= 
        "10";

    /**
     * A small number used to determine when the centroids have converged.
     */
    private static final double EPSILON = 1e-3;

    /**
     * A random number generator during seed generation.
     */
    private static final Random random = new Random();

    /**
     * Throws {@link UnsupportedOperationException}.
     */
    public Assignments cluster(Matrix matrix, Properties properties) {
        throw new UnsupportedOperationException(
                "DirectClustering requires the number of clusters to be set.");
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix,
                               int numClusters,
                               Properties properties) {
        int numRepetitions = Integer.parseInt(properties.getProperty(
                    REPEAT_PROPERTY, DEFAULT_REPEATS));
        // Get an instance of the seed generator.
        String seedProp = properties.getProperty(SEED_PROPERTY, DEFAULT_SEED);
        SeedAlgorithm seedType = SeedAlgorithm.valueOf(seedProp);

        // Create the criterion function.
        CriterionFunction criterion = ReflectionUtil.getObjectInstance(
                properties.getProperty(CRITERIA_PROPERTY, DEFAULT_CRITERION));
        return cluster(matrix, numRepetitions, numClusters,
                       seedType, criterion);
    }

    /**
     * Clusters {@link matrix} using the {@link SeedAlgorithm#KMEANS_PLUS_PLUS}
     * seeding algorithm and the default kmeans {@link CriterionFunction}.  The
     * best scoring solution out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions) {
        return cluster(matrix, numClusters, numRepetitions,
                       SeedAlgorithm.KMEANS_PLUS_PLUS, new I1Function());
    }

    /**
     * Clusters {@link matrix} using the {@link SeedAlgorithm#KMEANS_PLUS_PLUS}
     * seeding algorithm and the specified {@link CriterionFunction}. The best
     * scoring solution out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions,
                                      CriterionFunction criterion) {
        return cluster(matrix, numClusters, numRepetitions,
                       SeedAlgorithm.KMEANS_PLUS_PLUS, criterion);
    }

    /**
     * Clusters {@link matrix} using the specified {@link SeedAlgorithm}
     * and the specified {@link CriterionFunction}. The best scoring solution
     * out of {@code numRepetitions} will be returned.
     */
    public static Assignments cluster(Matrix matrix,
                                      int numClusters,
                                      int numRepetitions,
                                      SeedAlgorithm seedType,
                                      CriterionFunction criterion) {
        int[] bestAssignment = null;
        double bestScore = (criterion.maximize()) ? 0 : Double.MAX_VALUE;
        for (int i = 0; i < numRepetitions; ++i) {
            clusterInt(matrix, numClusters, seedType, criterion);
            if (criterion.maximize()) {
                if (criterion.score() > bestScore) {
                    bestScore = criterion.score();
                    bestAssignment = criterion.assignments();
                }
            } else {
                if (criterion.score() < bestScore) {
                    bestScore = criterion.score();
                    bestAssignment = criterion.assignments();
                }
            }
        }

        // Convert the array of assignments to an Assignments object.
        Assignment[] assignments = new Assignment[matrix.rows()];
        int i = 0;
        for (int assignment : bestAssignment)
            assignments[i++] = new HardAssignment(assignment);
        return new Assignments(numClusters, assignments);

    }

    /**
     * performs one run of Direct Clustering over the data set.
     */
    private static void clusterInt(Matrix matrix,
                                   int numClusters,
                                   SeedAlgorithm seedType,
                                   CriterionFunction criterion) {
        // Select a subset of data points to be the new centroids.
        BitSet selectedCentroids =
            Statistics.randomDistribution(numClusters, matrix.rows());
        DoubleVector[] centers = new DoubleVector[numClusters];

        // Generate the initial seeds for K-Means.  We assume that the seeds
        // returned are either a subset of the data points or some set of
        // pre-normalized vectors.
        DoubleVector[] centroids = null;
        switch (seedType) {
            case RANDOM:
                centers = randomSeed(numClusters, matrix);
                break;
            case KMEANS_PLUS_PLUS:
                centers = kMeansPlusPlusSeed(numClusters, matrix);
                break;
            case OPTIMAL:
                centers = optimalSeed(numClusters, matrix);
                break;
            case ORSS:
                centers = orssSeed(numClusters, matrix);
                break;
        }

        // Convert the selection indices into vectors.
        for (int c = 0, i = selectedCentroids.nextSetBit(0); i >= 0;
                c++, i = selectedCentroids.nextSetBit(i+1))
            centers[c] = matrix.getRowVector(i);

        // Compute the initial set of assignments for each data point based on
        // the initial assignments.
        int[] initialAssignments = new int[matrix.rows()];
        for (int i = 0; i < matrix.rows(); ++i) {
            DoubleVector vector = matrix.getRowVector(i);
            double bestSimilarity = 0;
            for (int c = 0; c < numClusters; ++c) {
                double similarity = Similarity.cosineSimilarity(
                        centers[c], vector);
                if (similarity >= bestSimilarity) {
                    bestSimilarity = similarity;
                    initialAssignments[i] = c;
                }
            }
        }

        // Setup the criterion function with it's meta data.
        criterion.setup(matrix, initialAssignments, numClusters);

        // Iteratively swap each data point to a better cluster if such an
        // assignment exists.
        List<Integer> indices = new ArrayList<Integer>(matrix.rows());
        for (int i = 0; i < matrix.rows(); ++i)
            indices.add(i);

        // Iterate through each data point in random order.  For each data
        // point, try to assign it to a new cluster.  If no data point is moved
        // in an iteration, end the iterations.
        boolean changed = true;
        while(changed) {
            changed = false;
            Collections.shuffle(indices);
            for (int index : indices)
                changed = criterion.update(index);
        }
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
     * Select seeds using the K-Means++ algorithm
     */
    public static DoubleVector[] kMeansPlusPlusSeed(int numCentroids, 
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
            centroidIndex = chooseWithProbability(distances, sum, probability);
            centers[i] = dataPoints.getRowVector(centroidIndex);
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
            double distance = distance(centroid, dataPoints.getRowVector(i));
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
    public static DoubleVector[] optimalSeed(int numCentroids, 
                                             Matrix dataPoints) {
        String assignmentProp = System.getProperties().getProperty(
                OPTIMAL_ASSIGNMENT_FILE);
        if (assignmentProp == null)
            throw new IllegalArgumentException(
                    "An assignment file must be provided");

        List<DoubleVector> centroids = new ArrayList<DoubleVector>();
        List<Integer> numAssignments = new ArrayList<Integer>();
        DoubleVector[] centroidsArr;
        try {
            BufferedReader br = new BufferedReader(new FileReader(
                        assignmentProp));
            int i = 0;
            for (String line = null; (line = br.readLine()) != null; ) {
                int assignment = Integer.parseInt(line.split("\\s")[0])-1;
                while (assignment >= centroids.size()) {
                    centroids.add(new DenseVector(dataPoints.columns()));
                    numAssignments.add(0);
                }

                VectorMath.add(centroids.get(assignment), 
                               dataPoints.getRowVector(i));
                numAssignments.set(
                        assignment, numAssignments.get(assignment)+1);
                i++;
            }

            numCentroids = centroids.size();
            centroidsArr = new DoubleVector[numCentroids];
            for (int c = 0; c < numCentroids; ++c) {
                centroidsArr[c] = new ScaledDoubleVector(
                        centroids.get(c), 1/((double)numAssignments.get(c)));
                System.out.printf("size %d\n", numAssignments.get(c));
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return centroidsArr;
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
    public static DoubleVector[] orssSeed(int numCentroids, Matrix dataPoints) {
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
    public static DoubleVector[] randomSeed(int numCentroids,
                                            Matrix dataPoints) {
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
