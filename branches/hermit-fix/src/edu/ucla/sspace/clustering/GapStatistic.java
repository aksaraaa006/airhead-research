package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.ClutoDenseMatrixBuilder;
import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashSet;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

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
 * k-means clustering of a data set and compares it to a random sample of
 * reference data points.  This will recompute k-means with incresing values of
 * k until the difference between the original data set and the reference data
 * sets begins to decline.  Clustering will stop at the first k value where this
 * difference is less than the previous difference.  This clustering method is
 * an implementation of the method specified in the following paper:
 *
 *   <li style="font-family:Garamond, Georgia serif">R. Tibshirani, G. Walther,
 *   and T. Hastie. (2001). Estimating the number of clusters in a dataset via
 *   the Gap statistic. <i>Journal of the Royal Statistics Society (Series
 *   B)</i>, 411–423. Available <a
 *   href="http://www-stat.stanford.edu/~tibs/ftp/gap.ps">here</a>
 *   </li>
 *
 * @author Keith Stevens
 */
public class GapStatistic implements OfflineClustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(GapStatistic.class.getName());

    /**
     * A property prefix used for properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.GapStatistic";

    /**
     * The number of clusters to start clustering at.
     */
    public static final String NUM_CLUSTERS_START = 
        PROPERTY_PREFIX + ".numClusterStart";

    /**
     * The number of reference data sets to use.
     */
    public static final String NUM_REFERENCE_DATA_SETS = 
        PROPERTY_PREFIX + ".numReferenceDataSets";

    /**
     * The default number of clusters at which to start clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_START = "1";

    /**
     * The default number of clusters at which to stop clustering.
     */
    private static final String DEFAULT_NUM_CLUSTERS_END = "10";

    /**
     * The default number of reference data sets to use.
     */
    private static final String DEFAULT_NUM_REFERENCE_DATA_SETS = "5";

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
     * The number of reference data sets to use.
     */
    private final int numGaps;

    /**
     * Creates a new instance of the {@code GapStatistic} using system
     * properties.
     */
    public GapStatistic() {
        this(System.getProperties());
    }

    /**
     * Creates a new instance of the {@code GapStatistic} using the provided
     * properties.
     */
    public GapStatistic(Properties props) {
        startSize = Integer.parseInt(props.getProperty(
                NUM_CLUSTERS_START, DEFAULT_NUM_CLUSTERS_START));

        int endSize = Integer.parseInt(props.getProperty(
                OfflineProperties.MAX_NUM_CLUSTER_PROPERTY,
                DEFAULT_NUM_CLUSTERS_END));

        numIterations = endSize - startSize;

        numGaps = Integer.parseInt(props.getProperty(
                NUM_REFERENCE_DATA_SETS, DEFAULT_NUM_REFERENCE_DATA_SETS));
    }

    /**
     * Creates a new {@code GapStatistic} that will compute k-means iteratively
     * were k ranges from {@code start} to {@code end} and {@code gaps}
     * reference data sets are used.
     */
    public GapStatistic(int start, int end, int gaps) {
        startSize = start;
        numIterations = end - start;
        numGaps = gaps;
    }

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Iteratively computes the k-means clustering of the dataset {@code m}
     * using the the Gap Statistic .
     */
    public int[] cluster(Matrix m) {
        verbose("Generating the reference data set");
        // Generate the reference data sets.
        ReferenceDataGenerator generator = new ReferenceDataGenerator(m);
        File[] gapFiles = new File[numGaps];
        for (int i = 0; i < numGaps; ++i)
            gapFiles[i] = generator.generateTestData();

        // Transfer the data set to a cluto matrix file.
        File matrixFile = null;
        try {
            matrixFile = File.createTempFile("cluto-input",".matrix");
            matrixFile.deleteOnExit();
            MatrixIO.writeMatrix(m, matrixFile, Format.CLUTO_DENSE);
        } catch (IOException ioe) {
            throw new IOError(ioe); 
        }

        // Setup files to store  store what the previous gap statistic was and
        // the previous clustering assignment. 
        File previousFile = null;
        double previousGap = Double.MIN_VALUE;

        // Compute the gap statistic for each iteration.
        String result = null;
        for (int i = 0; i < numIterations; ++i) {
            int k = i + startSize;
            try {
                verbose("Clustering reference data for %d clusters\n", k);

                // Compute the score for the reference data sets with k
                // clusters.
                double gapScore = 0;
                for (int j = 0; j < numGaps; ++j) {
                    File outputFile = 
                        File.createTempFile("gap-clustering-output", ".matrix");
                    result = ClutoClustering.cluster(null,
                                                     gapFiles[j],
                                                     outputFile,
                                                     k,
                                                     METHOD);
                    outputFile.delete();

                    gapScore += Math.log(extractScore(result));
                }
                gapScore = gapScore / numGaps;

                verbose("Clustering original data for %d clusters\n", k);
                // Compute the score for the original data set with k clusters.
                File outFile =
                    File.createTempFile("gap-clustering-output", ".matrix");
                outFile.deleteOnExit();
                result = ClutoClustering.cluster(null,
                                                 matrixFile,
                                                 outFile,
                                                 i + startSize,
                                                 METHOD);

                // Compute the difference between the two scores.  If the
                // current score is less than the previous score, then the
                // previous assignment is considered best.
                double gap = Math.log(extractScore(result));
                gap = gapScore - gap;
                if (previousGap >= gap) {
                    verbose("Found best clustering with %d clusters\n", (k-1));
                    break;
                }

                // Otherwise, continue clustering with higher values of k.
                previousGap = gap;
                previousFile = outFile;
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        // Extract the cluster assignments based on the best found value of k.
        int[] assignments = new int[m.rows()];
        try {
            ClutoClustering.extractAssignment(previousFile, assignments);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        return assignments;
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

    /**
     * A simple data set generator that generates new vectors based on the range
     * of values each feature in the vector can take one.
     */
    private class ReferenceDataGenerator {

        /**
         * The minimum value for each feature.
         */
        private final double[] minValues;

        /**
         * The maximum value for each feature.
         */
        private final double[] maxValues;

        /**
         * The average number of non zero values in a single row.
         */
        private final double averageNumValuesPerRow;

        /**
         * The standard deviation of the number of non zero values in a single
         * row.
         */
        private final double stdevNumValuesPerRow;

        /**
         * The number of rows to generate in a test data set.
         */
        private final int rows;

        /**
         * Creates a new {@code ReferenceDataGenerator} based on the given
         * matrix {@code m}.
         */
        public ReferenceDataGenerator(Matrix m) {
            // Initialize the bounds.
            rows = m.rows();
            minValues = new double[m.columns()];
            maxValues = new double[m.columns()];
            int[] numNonZeros = new int[m.rows()];
            double averageNumNonZeros = 0;

            for (int r = 0; r < m.rows(); ++r) {
                for (int c = 0; c < m.columns(); ++c) {
                    double value = m.get(r, c);
                    // Get the max and minimum value for the row.
                    if (value < minValues[c])
                        minValues[c] = value;
                    if (value > maxValues[c])
                        maxValues[c] = value;

                    // Calculate the number of non zeros per row.
                    if (value != 0d) {
                        numNonZeros[r]++;
                        averageNumNonZeros++;
                    }
                }
            }

            // Finalize the average number of non zeros per row.
            averageNumValuesPerRow = averageNumNonZeros / m.rows();

            // Compute the standard deviation of the number of non zeros per
            // row.
            double stdev = 0;
            for (int nonZeroCount : numNonZeros)
                stdev += Math.pow(averageNumValuesPerRow- nonZeroCount, 2);

            // Finalize the standar deviation.
            stdevNumValuesPerRow = Math.sqrt(stdev / m.rows());
            
        }

        /**
         * Creates a test file in the {@code CLUTO_DENSE} format containing
         * reference data points from a data distribution similar to the
         * original.
         */
        public File generateTestData() {
            MatrixBuilder builder = new ClutoDenseMatrixBuilder();
            for (int i = 0; i < rows; ++i) {
                int cols = minValues.length;
                double[] values = new double[cols];

                // If the average number of values per row is significantly
                // smaller than the total number of columns then select a subset
                // to be non zero.
                if (averageNumValuesPerRow < cols / 2) {
                    Set<Integer> nonZeros = new HashSet<Integer>();
                    int numNonZeros =
                        (int) (random.nextGaussian() * stdevNumValuesPerRow +
                               averageNumValuesPerRow);
                    for (int j = 0; j < numNonZeros; ++j) {
                        // Get the next index to set.
                        int col = -1;
                        while (!nonZeros.contains(col = random.nextInt(cols)))
                            ;

                        // Set the column's value.
                        nonZeros.add(col);
                        double value = random.nextDouble();
                        values[col] = value *
                                      (maxValues[col] - minValues[col]) + 
                                      minValues[col];
                    }
                } else {
                    // Set all values in the column.
                    for (int j = 0; j < cols; ++j) {
                        double value = random.nextDouble();
                        values[j] = value * (maxValues[j] - minValues[j]) + 
                                    minValues[j];
                    }
                }
                builder.addColumn(values);
            }
            builder.finish();
            return builder.getFile();
        }
    }

    public static void main(String[] args) throws IOException {
        Matrix m = MatrixIO.readMatrix(new File(args[0]),
                                       Format.SVDLIBC_SPARSE_TEXT,
                                       Type.SPARSE_IN_MEMORY);
        OfflineClustering cluster = new GapStatistic();
        int[] assignments = cluster.cluster(m);
    }

    protected void verbose(String msg) {
        LOGGER.fine(msg);
    }

    protected void verbose(String format, Object... args) {
        LOGGER.fine(String.format(format, args));        
    }
}
