package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.ClutoDenseMatrixBuilder;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.Properties;
import java.util.Random;

import java.util.logging.Logger;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;


/**
 * A {@link} OfflineClustering} implementation that iteratively computes the
 * k-means clustering of a data set and fines the value of k that produced the
 * most significant advantage compared to other values of k.  This approach
 * attempts to find a "knee" or "bend" in the graph of objective scores for
 * k-means with different values of k.  This clustering method is an
 * implementation of the method specified in the following paper:
 *
 *   <li style="font-family:Garamond, Georgia, serif">Pedersen, T and Kulkarni,
 *   A. (2006) Automatic Cluster Stopping with Criterion Functions and the Gap
 *   Statistic <i>Sixth Annual Meeting of the North American Chapter of the
 *   Association for Computational Linguistics</i>, <b>6</b>, 276-279.
 *   Available <a
 *   href="http://www.d.umn.edu/~tpederse/Pubs/naacl06-demo.pdf">here</a>
 *   </li>
 *
 * </p>
 *
 * Three measures for finding the knee are provided: PK1, PK2, and PK3
 *
 * @author Keith Stevens
 */
public class AutomaticStopClustering implements OfflineClustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(AutomaticStopClustering.class.getName());

    /**
     * A property prefix used for properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.AutomaticStopClustering";

    /**
     * The number of clusters to start clustering at.
     */
    public static final String NUM_CLUSTERS_START = 
        PROPERTY_PREFIX + ".numClusterStart";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String NUM_CLUSTERS_END = 
        PROPERTY_PREFIX + ".numClusterEnd";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String CLUSTERING_METHOD = 
        PROPERTY_PREFIX + ".clusteringMethod";

    /**
     * The number of clusters to stop clustering at.
     */
    public static final String PK1_THRESHOLD = 
        PROPERTY_PREFIX + ".pk1Threshold";

    /**
     * The default number of clusters at which to start clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_START = "1";

    /**
     * The default number of clusters at which to stop clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_END = "10";

    /**
     * The default objective method to use.
     */
    private static final String DEFAULT_CLUSTERING_METHOD = "PK3";

    /**
     * The default threshold when using the pk1 objective method.
     */
    private static final String DEFAULT_PK1_THRESHOLD = "-.70";

    /**
     * The available stopping criteria.  For each measure, let I2(k) be the
     * objective method for evaluating the quality of the k-means clustering
     * with k clusters.
     */
    public enum Measure {

        /**
         * For each number of clusters k, the score for is defined as 
         *   W(k) = (I2(k) - mean(I2(k_i))) / std(I2(k_i))
         *
         * This method will select the smallest k such that W(k) is greater than
         * or equal to some threshold.
         */
        PK1,

        /**
         * For each number of clusters k, the score is defined as
         *   W(k) = I2(k) / I2(k-1)
         *
         * This method will select the smallest k such that W(k) is greater than
         * 1 + std(I2(k-1))
         */
        PK2,

        /**
         * For each number of clusters k, the score is defined as
         *   W(k) = 2 * I2(k) / (I2(k-1) + I2(k+1))
         *
         * This method will select the smallest k such that W(k) is greater than
         * 1 + std(I2(k-1))
         */
        PK3,
    }

    /**
     * A random number generator for creating reference data sets.
     */
    private static final Random random = new Random();

    /**
     * The cluto clustering method name for k-means clustering.
     */
    private static final String METHOD = ClutoClustering.KMEANS;

    /**
     * The number of k clusters to start evaluating at.
     */
    private final int startSize;

    /**
     * The number of iterations to evaluate.
     */
    private final int numIterations;

    /**
     * The evalution measure to use to determine the optimal value of k.
     */
    private final Measure measure;

    /**
     * If the {@code PK1} measure is used, this specifies the threshold used.
     */
    private final double pk1Threshold;

    /**
     * Creates a new instance of the {@code AutomaticStopClustering} using
     * system properties.
     */
    public AutomaticStopClustering() {
        this(System.getProperties());
    }

    /**
     * Creates a new instance of the {@code AutomaticStopClustering} using
     * provided properties.
     */
    public AutomaticStopClustering(Properties props) {
        startSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_START, DEFAULT_NUM_CLUSTERS_START));

        int endSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_END, DEFAULT_NUM_CLUSTERS_END));

        numIterations = endSize - startSize;

        measure = Measure.valueOf(props.getProperty(
                    CLUSTERING_METHOD, DEFAULT_CLUSTERING_METHOD));

        pk1Threshold = Double.parseDouble(props.getProperty(
                    PK1_THRESHOLD, DEFAULT_PK1_THRESHOLD));
    }

    /**
     * Creates a new {@code AutomaticStopClustering} that will compute k-means iteratively
     * were k ranges from {@code start} to {@code end} with {@code measure}
     * defining what {@link Measure} will be used to determine where the knee in
     * the objective method scores occurs.  If the {@link PK1} measure is used,
     * {@code threshold} defines the threshold that needs to be met.
     */
    public AutomaticStopClustering(int start, 
                                   int end,
                                   Measure measure,
                                   double threshold) {
        startSize = start;
        numIterations = end - start;
        this.measure = measure;
        pk1Threshold = threshold;
    }

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Iteratively computes the k-means clustering of the dataset {@code m}
     * using a specified method for determineing when to automaticaly stop
     * clustering.
     */
    public int[] cluster(Matrix m) {
        // Transfer the data set to a cluto matrix file.
        File matrixFile = null;
        try {
            matrixFile = File.createTempFile("cluto-input",".matrix");
            matrixFile.deleteOnExit();
            MatrixIO.writeMatrix(m, matrixFile, Format.CLUTO_DENSE);
        } catch (IOException ioe) {
            throw new IOError(ioe); 
        }

        double[] objectiveWeights = new double[numIterations];
        File[] outFiles = new File[numIterations];
        // Compute the gap statistic for each iteration.
        String result = null;
        for (int i = 0; i < numIterations; ++i) {
            LOGGER.fine("Clustering with " + (startSize + 1) + " clusters");

            try {
                // Compute the score for the original data set with k clusters.
                outFiles[i] = 
                    File.createTempFile("autostop-clustering-out", ".matrix");
                outFiles[i].deleteOnExit();
                result = ClutoClustering.cluster(null,
                                                 matrixFile,
                                                 outFiles[i],
                                                 i + startSize,
                                                 METHOD);

                objectiveWeights[i] = extractScore(result);
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        // Compute the best index based on the measure being used.
        int bestK = -1;
        switch (measure) {
            case PK1:
                bestK = computePk1Measure(objectiveWeights);
                break;
            case PK2:
                bestK = computePk2Measure(objectiveWeights);
                break;
            case PK3:
                bestK = computePk3Measure(objectiveWeights);
                break;
        }

        // Extract the cluster assignments based on the best found value of k.
        int[] assignments = new int[m.rows()];
        try {
            ClutoClustering.extractAssignment(outFiles[bestK], assignments);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        return assignments;
    }

    /**
     * Compute the smallest k that satisfies the Pk1 method.
     */
    private int computePk1Measure(double[] objectiveScores) {
        // Compute the average of the objective scores.
        double average = 0;
        for (int k = 0; k < objectiveScores.length; ++k)
            average += objectiveScores[k];
        average /= objectiveScores.length;

        // Compute the standard deviation of the objective scores.
        double stdev = 0;
        for (int k = 0; k < objectiveScores.length; ++k)
            stdev += Math.pow(objectiveScores[k], 2);
        stdev /= objectiveScores.length;
        stdev = Math.sqrt(stdev);

        // Find the smallest k such that the pk1 score surpasses the threshold.
        for (int k = 0; k < objectiveScores.length; ++k) {
            objectiveScores[k] = (objectiveScores[k] - average) / stdev;
            if (objectiveScores[k] > pk1Threshold)
                return k;
        }

        return 0;
    }

    /**
     * Compute the smallest k that satisfies the Pk3 method.
     */
    private int computePk2Measure(double[] objectiveScores) {
        // Compute each Pk2 score and the average score.
        double average = 0;
        for (int k = objectiveScores.length - 1; k > 0; --k) {
            objectiveScores[k] /= objectiveScores[k-1];
            average += objectiveScores[k];
        }
        average /= (objectiveScores.length - 1);

        // Compute the standard deviation of the PK2 scores.
        double stdev = 0;
        for (int k = 1; k < objectiveScores.length; ++k)
            stdev += Math.pow(objectiveScores[k] - average, 2);
        stdev /= (objectiveScores.length - 2);
        stdev = Math.sqrt(stdev);

        // Find the point where the score is the smallest value greater than 1 +
        // stdev of the PK1 scores.
        double referencePoint = 1 + stdev;
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int k = 1; k < objectiveScores.length; ++k) {
            if (objectiveScores[k] < bestScore &&
                objectiveScores[k] >= referencePoint) {
                bestIndex = k;
                bestScore = objectiveScores[k];
            }
        }

        return bestIndex;
    }

    /**
     * Compute the smallest k that satisfies the Pk3 method.
     */
    private int computePk3Measure(double[] objectiveScores) {
        // Compute each Pk3 score and the average score.
        double average = 0;
        double[] weightedScores = new double[objectiveScores.length - 2];
        for (int k = 1; k < objectiveScores.length - 1 ; ++k) {
            weightedScores[k-1] = 2 * objectiveScores[k] / 
                (objectiveScores[k-1] + objectiveScores[k+1]);
            average += weightedScores[k-1];
        }
        average /= (objectiveScores.length - 2);

        // Compute the standard deviation of PK3 scores.
        double stdev = 0;
        for (int k = 0; k < weightedScores.length; ++k)
            stdev += Math.pow(weightedScores[k] - average, 2);
        stdev /= (objectiveScores.length - 2);
        stdev = Math.sqrt(stdev);

        // Find the point where the score is the smallest value greater than 1 +
        // stdev of the PK3 scores.
        double referencePoint = 1 + stdev;
        int bestIndex = 0;
        double bestScore = Double.MAX_VALUE;
        for (int k = 0; k < weightedScores.length; ++k) {
            if (weightedScores[k] < bestScore &&
                weightedScores[k] >= referencePoint) {
                bestIndex = k;
                bestScore = weightedScores[k];
            }
        }

        return bestIndex + 1;
    }

    /**
     * Extracts the score of the objective function for a given set of
     * clustering assignments.  This requires scraping the output from Cluto to
     * find the line specifiying the score.
     */
    private double extractScore(String clutoOutput) throws IOException {
        double score = 0;
        BufferedReader reader =
            new BufferedReader(new StringReader(clutoOutput));
        String line = null;
        while ((line = reader.readLine()) != null) {
            if (line.contains("[I2=")) {
                String[] split = line.split("=");
                int endOfIndex = split[1].indexOf("]");
                return Double.parseDouble(split[1].substring(0, endOfIndex));
            }
        }
        return 0;
    }

    public static void main(String[] args) throws IOException {
        SemanticSpace sspace = new StaticSemanticSpace(args[0]);
        List<DoubleVector> vectors = new ArrayList<DoubleVector>();

        Set<String> words = sspace.getWords();
        int i = 0;
        for (String word : words) {
            /*
            if (i == 100)
                break;
            i++;
            */
            vectors.add(Vectors.asDouble(sspace.getVector(word)));
        }

        OfflineClustering clustering = null;
        if (args.length > 1)
            clustering = new AutomaticStopClustering(
                    1, Integer.parseInt(args[1]), Measure.PK3, 0);
        else 
            clustering = new AutomaticStopClustering(
                    1, 5, Measure.PK3, 0);

        System.out.println("Clustering Start");
        int[] assignments = clustering.cluster(Matrices.asMatrix(vectors));
        System.out.println("Clustering Done");
        i = 0;
        for (String word : words) {
            /*
            if (i == 100)
                break;
                */
            System.out.printf("%s %d\n", word, assignments[i++]);
        }
    }
}
