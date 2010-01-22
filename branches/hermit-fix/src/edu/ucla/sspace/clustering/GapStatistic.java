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

import java.util.Random;

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
 * difference is less than the previous difference.
 *
 * @author Keith Stevens
 */
public class GapStatistic implements OfflineClustering {

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
            try {
                // Compute the score for the reference data sets with k
                // clusters.
                double gapScore = 0;
                for (int j = 0; j < numGaps; ++j) {
                    File outputFile = 
                        File.createTempFile("gap-clustering-output", ".matrix");
                    result = ClutoClustering.cluster(null,
                                                     gapFiles[j],
                                                     outputFile,
                                                     i + startSize,
                                                     METHOD);
                    outputFile.delete();

                    gapScore += Math.log(extractScore(result));
                }
                gapScore = gapScore / numGaps;

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
                if (previousGap >= gap)
                    break;

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
         * The number of rows to generate in a test data set.
         */
        private final int rows;

        /**
         * Creates a new {@code ReferenceDataGenerator} based on the given
         * matrix {@code m}.
         */
        public ReferenceDataGenerator(Matrix m) {
            rows = m.rows();
            minValues = new double[m.columns()];
            maxValues = new double[m.columns()];
            for (int r = 0; r < m.rows(); ++r) {
                for (int c = 0; c < m.columns(); ++c) {
                    double value = m.get(r, c);
                    if (value < minValues[c])
                        minValues[c] = value;
                    if (value > maxValues[c])
                        maxValues[c] = value;
                }
            }
        }

        /**
         * Creates a test file in the {@code CLUTO_DENSE} format containing
         * reference data points from a data distribution similar to the
         * original.
         */
        public File generateTestData() {
            MatrixBuilder builder = new ClutoDenseMatrixBuilder();
            for (int i = 0; i < rows; ++i) {
                double[] values = new double[minValues.length];
                for (int j = 0; j < values.length; ++j) {
                    double value = random.nextDouble();
                    values[j] = value * (maxValues[j] - minValues[j]) + 
                                minValues[j];
                }
                builder.addColumn(values);
            }
            builder.finish();
            return builder.getFile();
        }
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
            clustering = new GapStatistic(
                    1,
                    Integer.parseInt(args[1]),
                    Integer.parseInt(args[2]));
        else 
            clustering = new GapStatistic(1, 5, 5);

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
