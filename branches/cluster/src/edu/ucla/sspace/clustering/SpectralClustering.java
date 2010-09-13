/*
 * Copyright 2009 Keith Stevens 
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


import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import java.util.logging.Logger;


/**
 * Implementation of Spectral Clustering using divide and merge methodology.
 * The implementation is based on two papers:
 *
 * <ul>
 *   <li style="font-family:Garamond, Georgia, serif">Cheng, D., Kannan, R.,
 *     Vempala, S., Wang, G.  (2006).  A Divide-and-Merge Methodology for
 *     Clustering. <i>ACM Transactions on Database Systsms</i>, <b>31</b>,
 *     1499-1525.  Available <a
 *     href=http://www-math.mit.edu/~vempala/papers/eigencluster.pdf">here</a>
 *   </li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">Kannan, R., Vempala, S.,
 *     Vetta, A.  (2000).  On clustering: Good, bad, and spectral.  
 *     <i>FOCS '00: Proceedings of the 41st Annual Symposium on Foundations of
 *   Computer Science</i> Available <a
 *     href="http://www-math.mit.edu/~vempala/papers/specfocs.ps">here</a>
 *   </li>
 * </ul>
 *
 * @author Keith Stevens
 */
public class SpectralClustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(SpectralClustering.class.getName());

    private final double alpha;

    private final double beta;

    private final Generator<EigenCut> cutterGenerator;

    public SpectralClustering(double alpha,
                              Generator<EigenCut> cutterGenerator) {
        this.alpha = alpha;
        this.beta = 1 - alpha;
        this.cutterGenerator = cutterGenerator;
    }

    public Assignment[] cluster(Matrix matrix) {
        ClusterResult r = fullCluster(scaleMatrix(matrix), 0);
        verbose("Created " + r.numClusters + " clusters");

        Assignment[] assignments = new HardAssignment[r.assignments.length];
        for (int i = 0; i < r.assignments.length; ++i)
            assignments[i] = new HardAssignment(r.assignments[i]);

        return assignments;
    }

    public Assignment[] cluster(Matrix matrix,
                                int maxClusters,
                                boolean useKMeans) {
        // Cluster the matrix recursively.
        LimitedResult[] results = limitedCluster(
                scaleMatrix(matrix), maxClusters, useKMeans);
        LimitedResult r = results[maxClusters-1];

        verbose("Created " + r.numClusters + " clusters");

        Assignment[] assignments = new HardAssignment[r.assignments.length];
        for (int i = 0; i < r.assignments.length; ++i)
            assignments[i] = new HardAssignment(r.assignments[i]);

        return assignments;
    }

    private Matrix scaleMatrix(Matrix matrix) {
        // Scale every data point such that it has a dot product of 1 with
        // itself.  This will make further calculations easier since the dot
        // product distrubutes when the cosine similarity does not.
        if (matrix instanceof SparseMatrix) {
            List<SparseDoubleVector> scaledVectors =
                new ArrayList<SparseDoubleVector>(matrix.rows());
            SparseMatrix sm = (SparseMatrix) matrix;
            for (int r = 0; r < matrix.rows(); ++r) {
                SparseDoubleVector v = sm.getRowVector(r);
                scaledVectors.add(new ScaledSparseDoubleVector(
                            v, 1/v.magnitude()));
            }
            return Matrices.asSparseMatrix(scaledVectors);
        } else {
            List<DoubleVector> scaledVectors = 
                new ArrayList<DoubleVector>(matrix.rows());
            for (int r = 0; r < matrix.rows(); ++r) {
                DoubleVector v = matrix.getRowVector(r);
                scaledVectors.add(new ScaledDoubleVector(v, v.magnitude()));
            }
            return Matrices.asMatrix(scaledVectors);
        }
    }

    private ClusterResult fullCluster(Matrix matrix,
                                      int depth) {
        verbose("Clustering at depth " + depth);

        // If the matrix has only one element or the depth is equal to the
        // maximum number of desired clusters then all items are in a single
        // cluster.
        if (matrix.rows() <= 1) 
            return new ClusterResult(new int[matrix.rows()], 1);

        EigenCut eigenCutter = cutterGenerator.generate();
        eigenCutter.computeCut(matrix);

        Matrix leftMatrix = eigenCutter.getLeftCut();
        Matrix rightMatrix = eigenCutter.getRightCut();

        // If the compute decided that the matrix should not be split, short
        // circuit any attempts to further cut the matrix.
        if (leftMatrix.rows() == matrix.rows() ||
            rightMatrix.rows() == matrix.rows())
            return new ClusterResult(new int[matrix.rows()], 1);

        verbose(String.format("Splitting into two matricies %d-%d",
                              leftMatrix.rows(), rightMatrix.rows()));

        // Do clustering on the left and right branches.
        ClusterResult leftResult =
            fullCluster(leftMatrix, depth+1);
        ClusterResult rightResult =
            fullCluster(rightMatrix, depth+1);

        verbose("Merging at depth " + depth);

        double splitObjective = eigenCutter.getSplitObjective(
                alpha, beta,
                leftResult.numClusters, leftResult.assignments,
                rightResult.numClusters, rightResult.assignments);

        // Compute the objective when we merge the two branches together.
        double mergedObjective = eigenCutter.getMergedObjective(alpha, beta);

        // If the merged objective value is less than the split version, combine
        // all clusters into one.
        int[] assignments = new int[matrix.rows()];
        int numClusters = 1;
        if (mergedObjective < splitObjective) {
            verbose("Selecting to combine sub trees at depth " + depth);
            Arrays.fill(assignments, 0);
        } else  {
            verbose("Selecting to maintain sub trees at depth " + depth);

            // Copy over the left assignments and the right assignments, where
            // the cluster id's of the right assignments are incremented to
            // avoid duplicate cluster ids.
            numClusters = leftResult.numClusters + rightResult.numClusters;

            int[] leftReordering = eigenCutter.getLeftReordering();
            int[] rightReordering = eigenCutter.getRightReordering();

            for (int index = 0; index < leftReordering.length; ++index)
                assignments[leftReordering[index]] = 
                    leftResult.assignments[index];
            for (int index = 0; index < rightReordering.length; ++index)
                assignments[rightReordering[index]] =
                    rightResult.assignments[index] + leftResult.numClusters;
        }
        return new ClusterResult(assignments, numClusters);
    }

    private LimitedResult[] limitedCluster(Matrix matrix,
                                           int maxClusters) {
        return limitedCluster(matrix, maxClusters, false);
    }

    private LimitedResult[] limitedCluster(Matrix matrix,
                                           int maxClusters,
                                           boolean useKMeans) {
        verbose("Clustering for " + maxClusters + " clusters.");

        EigenCut eigenCutter = cutterGenerator.generate();

        // If the matrix has only one element or the depth is equal to the
        // maximum number of desired clusters then all items are in a single
        // cluster.
        if (matrix.rows() <= 1 || maxClusters <= 1) {
            eigenCutter.computeRhoSum(matrix);
            double score = (useKMeans)
                ? eigenCutter.getKMeansObjective()
                : eigenCutter.getMergedObjective(alpha, beta);
            LimitedResult result = new LimitedResult(
                    new int[matrix.rows()], 1, score);
            return new LimitedResult[] {result};
        }

        eigenCutter.computeCut(matrix);

        Matrix leftMatrix = eigenCutter.getLeftCut();
        Matrix rightMatrix = eigenCutter.getRightCut();

        // If the compute decided that the matrix should not be split, short
        // circuit any attempts to further cut the matrix.
        if (leftMatrix.rows() == matrix.rows() ||
            rightMatrix.rows() == matrix.rows()) {
            eigenCutter.computeRhoSum(matrix);
            double score = (useKMeans)
                ? eigenCutter.getKMeansObjective()
                : eigenCutter.getMergedObjective(alpha, beta);
            LimitedResult result = new LimitedResult(
                    new int[matrix.rows()], 1, score);
            return new LimitedResult[] {result};
        }

        verbose(String.format("Splitting into two matricies %d-%d",
                              leftMatrix.rows(), rightMatrix.rows()));

        // Do clustering on the left and right branches.
        LimitedResult[] leftResults =
            limitedCluster(leftMatrix, maxClusters-1, useKMeans);
        LimitedResult[] rightResults =
            limitedCluster(rightMatrix, maxClusters-1, useKMeans);

        verbose("Merging at for: " + maxClusters + " clusters");

        int[] leftReordering = eigenCutter.getLeftReordering();
        int[] rightReordering = eigenCutter.getRightReordering();

        LimitedResult[] results = new LimitedResult[maxClusters];
        double mergedScore = eigenCutter.getMergedObjective(alpha, beta);
        results[0] =  new LimitedResult(new int[matrix.rows()], 1, mergedScore);
        for (int i = 0; i < leftResults.length; ++i) {
            LimitedResult leftResult = leftResults[i];
            if (leftResult == null)
                continue;

            for (int j = 0; j < rightResults.length; ++j) {
                LimitedResult rightResult = rightResults[j];
                if (rightResult == null)
                    continue;

                int numClusters =
                    leftResult.numClusters + rightResult.numClusters - 1;
                if (numClusters >= results.length)
                    continue;

                double splitObjective;
                if (useKMeans) {
                    splitObjective = eigenCutter.getKMeansObjective(
                        alpha, beta,
                        leftResult.numClusters, leftResult.assignments,
                        rightResult.numClusters, rightResult.assignments);
                } else {
                    splitObjective = eigenCutter.getSplitObjective(
                        alpha, beta,
                        leftResult.numClusters, leftResult.assignments,
                        rightResult.numClusters, rightResult.assignments);
                }

                if (results[numClusters] == null ||
                    results[numClusters].score < splitObjective) {
                    int[] assignments = new int[matrix.rows()];

                    for (int k = 0; k < leftReordering.length; ++k)
                        assignments[leftReordering[k]] = 
                            leftResult.assignments[k];
                    for (int k = 0; k < rightReordering.length; ++k)
                        assignments[rightReordering[k]] =
                            rightResult.assignments[k] + leftResult.numClusters;

                    results[numClusters] = new LimitedResult(
                            assignments, numClusters+1, splitObjective);
                }
            }
        }

        return results;
    }



    private void verbose(String out) {
        LOGGER.info(out);
    }

    private class LimitedResult {
        public int[] assignments;
        public int numClusters;
        public double score;

        public LimitedResult(int[] assignments, int numClusters, double score) {
            this.assignments = assignments;
            this.numClusters = numClusters;
            this.score = score;
        }
    }

    /**
     * A simple struct holding the cluster assignments and the number of
     * unique clusters generated.
     */
    private class ClusterResult {

        public int[] assignments;
        public int numClusters;

        public ClusterResult(int[] assignments, int numClusters) {
            this.assignments = assignments;
            this.numClusters = numClusters;
        }
    }
}
