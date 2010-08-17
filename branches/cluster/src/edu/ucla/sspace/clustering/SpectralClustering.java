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

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.index.DoubleVectorGenerator;
import edu.ucla.sspace.index.RandomOrthogonalVectorGenerator;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.RowMaskedMatrix;
import edu.ucla.sspace.matrix.SparseRowMaskedMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.ScaledDoubleVector;
import edu.ucla.sspace.vector.ScaledSparseDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;
import edu.ucla.sspace.vector.VectorIO;

import java.io.File;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

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
public class SpectralClustering implements Clustering {

    public static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.clustering.SpectralClustering";

    public static final String ALPHA_PROPERTY =
        PROPERTY_PREFIX + ".alpha";

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(SpectralClustering.class.getName());

    /**
     * The default intra cluster similarity weight.
     */
    private static final Double DEFAULT_ALPHA = .4;

    public Assignment[] cluster(Matrix matrix, Properties props) {
        return cluster(matrix, Integer.MAX_VALUE, props);
    }

    public Assignment[] cluster(Matrix matrix,
                                int maxClusters,
                                Properties props) {
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
            matrix = Matrices.asSparseMatrix(scaledVectors);
        } else {
            List<DoubleVector> scaledVectors = 
                new ArrayList<DoubleVector>(matrix.rows());
            for (int r = 0; r < matrix.rows(); ++r) {
                DoubleVector v = matrix.getRowVector(r);
                scaledVectors.add(new ScaledDoubleVector(v, v.magnitude()));
            }
            matrix = Matrices.asMatrix(scaledVectors);
        }

        String alphaProp = props.getProperty(ALPHA_PROPERTY);
        double alpha = (alphaProp == null)
            ? DEFAULT_ALPHA
            : Double.parseDouble(alphaProp);

        double beta = 1 - alpha;

        // Cluster the matrix recursively.
        ClusterResult r = realCluster(matrix, alpha, beta, maxClusters, 0);
        verbose("Created " + r.numClusters + " clusters");
        Assignment[] assignments = new HardAssignment[r.assignments.length];
        for (int i = 0; i < r.assignments.length; ++i)
            assignments[i] = new HardAssignment(r.assignments[i]);

        return assignments;
    }

    private ClusterResult realCluster(Matrix matrix,
                                      double alpha,
                                      double beta,
                                      int maxClusters,
                                      int depth) {
        verbose("Clustering at depth " + depth);

        // If the matrix has only one element or the depth is equal to the
        // maximum number of desired clusters then all items are in a single
        // cluster.
        if (matrix.rows() == 1 || depth == maxClusters)
            return new ClusterResult(new int[matrix.rows()], 1);

        // Compute the centroid of the entire data set.
        int vectorLength = matrix.rows();
        DoubleVector matrixRowSums = computeMatrixRowSum(matrix);

        // Compute rho, where rho[i] = dataPoint_i DOT matrixRowSums.
        DoubleVector rho = new DenseVector(vectorLength);
        double pSum = 0;
        for (int r = 0; r < matrix.rows(); ++r) {
            double dot = dotProduct(matrixRowSums, matrix.getRowVector(r));
            pSum += dot;
            rho.set(r, dot);
        }

        // Compute pi, and D.  Pi is the normalized form of rho.  D a diagonal
        // matrix with sqrt(pi) as the values along the diagonal.  Also compute
        // pi * D^-1.
        DoubleVector pi = new DenseVector(vectorLength);
        DoubleVector D = new DenseVector(vectorLength);
        DoubleVector piDInverse = new DenseVector(vectorLength);
        for (int i = 0; i < vectorLength; ++i) {
            double piValue = rho.get(i)/pSum;
            pi.set(i, piValue);
            if (piValue > 0d) {
                D.set(i, Math.sqrt(piValue));
                piDInverse.set(i, piValue / D.get(i));
            }
        }

        // Compute the second largest eigenvector of the normalized affinity
        // matrix with respect to pi*D^-1, which is similar to it's first eigen
        // vector.
        DoubleVector v = computeSecondEigenVector(matrix, piDInverse, D, rho);

        // Sort the rows of the original matrix based on the values of the eigen
        // vector.
        Index[] elementIndices = new Index[v.length()];
        for (int i = 0; i < v.length(); ++i)
            elementIndices[i] = new Index(v.get(i), i);
        Arrays.sort(elementIndices);

        // Create a reordering mapping for the indices in the original data
        // matrix and of rho.  The ith data point and rho value will be ordered
        // based on the position of the ith value in the second eigen vector
        // after it has been sorted.
        LinkedHashSet<Integer> reordering = new LinkedHashSet<Integer>();
        DoubleVector sortedRho = new DenseVector(matrix.rows());
        for (int i = 0; i < v.length(); ++i) {
            reordering.add(elementIndices[i].index);
            sortedRho.set(i, rho.get(elementIndices[i].index));
        }

        // Create the sorted matrix based on the reordering.
        Matrix sortedMatrix;
        if (matrix instanceof SparseMatrix)
            sortedMatrix = new SparseRowMaskedMatrix(
                    (SparseMatrix) matrix, reordering);
        else
            sortedMatrix = new RowMaskedMatrix(matrix, reordering);

        // Compute the index at which the best cut can be made based on the
        // reordered data matrix and rho values.
        int cutIndex = computeCut(sortedMatrix, sortedRho);

        // Compute the split masked sub matrices from the original.
        LinkedHashSet<Integer> leftMatrixRows = new LinkedHashSet<Integer>();
        LinkedHashSet<Integer> rightMatrixRows = new LinkedHashSet<Integer>();
        int i = 0;
        for (Index index : elementIndices) {
            if (i <= cutIndex)
                leftMatrixRows.add(index.index);
            else
                rightMatrixRows.add(index.index);
            i++;
        }

        // Create the split permuted matricies.
        Matrix leftMatrix = null;
        Matrix rightMatrix = null;
        if (matrix instanceof SparseMatrix) {
            leftMatrix = new SparseRowMaskedMatrix((SparseMatrix) matrix,
                                                   leftMatrixRows);
            rightMatrix = new SparseRowMaskedMatrix((SparseMatrix) matrix,
                                                    rightMatrixRows);
        } else {
            leftMatrix = new RowMaskedMatrix(matrix, leftMatrixRows);
            rightMatrix = new RowMaskedMatrix(matrix, rightMatrixRows);
        }

        verbose(String.format("Splitting into two matricies %d-%d",
                              leftMatrix.rows(), rightMatrix.rows()));

        // Do clustering on the left and right branches.
        ClusterResult leftResult =
            realCluster(leftMatrix, alpha, beta, maxClusters, depth+1);
        ClusterResult rightResult =
            realCluster(rightMatrix, alpha, beta, maxClusters, depth+1);

        verbose("Merging at depth " + depth);

        int numRows = matrix.rows();

        // Compute the objective when we keep the two branches split.
        double intraClusterScore = 
            computeIntraClusterScore(leftResult, leftMatrix) +
            computeIntraClusterScore(rightResult, rightMatrix);

        double interClusterScore = (pSum - numRows) / 2 - intraClusterScore;
        double splitObjective = alpha * intraClusterScore + interClusterScore;

        // Compute the objective when we merge the two branches together.
        double mergedObjective = alpha *
            ((numRows * (numRows + 1) / 2) - pSum/2);

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

            for (int index = 0; index < leftResult.assignments.length; ++index)
                assignments[elementIndices[index].index] =
                    leftResult.assignments[index];
            int offset = leftResult.assignments.length;
            for (int index = 0; index < rightResult.assignments.length; ++index)
                assignments[elementIndices[index + offset].index] =
                    rightResult.assignments[index] + leftResult.numClusters;
        }
        return new ClusterResult(assignments, numClusters);

    }

    /**
     * Returns a {@link DoubleVector} that is the orthonormalized version of
     * {@code v} with respect to {@code other}.  This orthonormalization is done
     * by simply modifying the value of v[0] such that it balances out the
     * similarity between {@code v} and {@code other} in all other dimensions.
     */
    private DoubleVector orthonormalize(DoubleVector v, DoubleVector other) {
        double dot = dotProduct(v, other);
        dot -= v.get(0) * other.get(0);
        dot /= other.get(0);
        v.add(0, -dot);
        return new ScaledDoubleVector(v, 1/dotProduct(v, v));
    }

    /**
     * Returns the second largest eigenvector of the a scaled form of the row
     * normalized affinity matrix.  The computation is using the power method
     * and such that the affinity matrix is never explicitly computed.  {@code
     * piDInverse} serves as a vector which is similar to the first eigen
     * vector.  The second eigen vector is assumed to be orthogonal to {@code
     * piDInverse}.  This algorithm makes O(log({@code matrix.rows()})) passes
     * through the data matrix.
     */
    private DoubleVector computeSecondEigenVector(Matrix matrix,
                                                  DoubleVector piDInverse,
                                                  DoubleVector D,
                                                  DoubleVector rho) {
        int vectorLength = piDInverse.length();
        // Step 1, generate a random vector, v,  that is orthogonal to
        // pi*D-Inverse.
        DoubleVector v = new DenseVector(vectorLength);
        for (int i = 0; i < v.length(); ++i)
            v.set(i, Math.random());

        // Make log(matrix.rows()) passes.
        int log = (int) Statistics.log2(vectorLength);
        for (int k = 0; k < log; ++k) {
            // start the orthonormalizing the eigen vector.
            v = orthonormalize(v, piDInverse);

            // Step 2, repeated, (a) normalize v (b) set v = Q*v, where Q = D *
            // R-Inverse * matrix * matrix-Transpose * D-Inverse.

            // v = Q*v is broken into 4 sub steps that allow for sparse
            // multiplications. 
            // Step 2b-1) v = D-Inverse*v.
            for (int i = 0; i < vectorLength; ++ i)
                if (D.get(i) != 0d)
                    v.set(i, v.get(i) / D.get(i));

            // Step 2b-2) v = matrix-Transpose * v.
            DoubleVector newV = computeMatrixTransposeV(matrix, v);

            // Step 2b-3) v = matrix * v.
            computeMatrixDotV(matrix, newV, v);

            // Step 2b-4) v = D*R-Inverse * v. Note that R is a diagonal matrix
            // with rho as the values along the diagonal.
            for (int i = 0; i < vectorLength; ++i) {
                double oldValue = v.get(i);
                double newValue = oldValue * D.get(i) / rho.get(i);
                v.set(i, newValue);
            }
        }

        return v;
    }

    /**
     * Returns the index at which {@code matrix} should be cut such that the
     * conductance between the two partitions is minimized.  This is done such
     * that the sparsity of the data matrix is maintained and all the entire
     * operation is linear with respect to the number of non zeros in the
     * matrix.
     */
    private int computeCut(Matrix matrix, DoubleVector rho) {
        // Compute the conductance of the newly sorted matrix.
        DoubleVector x = new DenseVector(matrix.columns());
        DoubleVector y = new DenseVector(matrix.columns());

        // First compute x and y, which are summations of different cuts of the
        // matrix, starting with x being the first row and y being the summation
        // of all other rows.  While doing this, also compute different
        // summations of values in the rho vector using the same cut.
        VectorMath.add(x, matrix.getRowVector(0));
        double rhoX = rho.get(0);
        double rhoY = 0;
        for (int i = 1; i < rho.length(); ++i) {
            VectorMath.add(y, matrix.getRowVector(i));
            rhoY += rho.get(i);
        }

        // Compute the dot product between the first possible cut.
        double u = dotProduct(x, y); 

        // Find the current conductance for the cut, assume that this is the
        // best so far.
        double minConductance = u / Math.min(rhoX, rhoY);
        int cutIndex = 0;

        // Compute the other possible cuts, ignoring the last cut, which would
        // leave no data points in a partition.  The cut with the smallest
        // conductance is maintained.
        for (int i = 1; i < rho.length() - 1; ++i) {
            // Compute the new value of u, the denominator for computing the
            // conductance.
            DoubleVector vector = matrix.getRowVector(i);
            u = u - dotProduct(x, vector) + dotProduct(y, vector) + 1;

            // Shift over vectors from y to x.
            VectorMath.add(x, vector);
            VectorMath.subtract(y, vector);

            // Shift over values from the rho vector.
            rhoX += rho.get(i);
            rhoY -= rho.get(i);

            // Recompute the new conductance and check if it's the smallest.
            double conductance = u / Math.min(rhoX, rhoY);
            if (conductance < minConductance) {
                minConductance = conductance;
                cutIndex = i;
            }
        }
        return cutIndex;
    }

    /**
     * Computes the dot product when the second vector may be sparse.
     */
    private double dotProduct(DoubleVector v1, DoubleVector v2) {
        double dot = 0;
        if (v2 instanceof SparseDoubleVector) {
            SparseDoubleVector sv2 = (SparseDoubleVector) v2;
            int[] nonZeros = sv2.getNonZeroIndices();
            for (int index : nonZeros) {
                double v2Value = v2.get(index);
                dot += v2Value * v1.get(index);
            }
        } else {
            for (int i = 0; i < v2.length(); ++i) {
                double v2Value = v2.get(i);
                dot += v2Value * v1.get(i);
            }
        }

        return dot;
    }

    /**
     * Returns the dot product between the transpose of a given matrix and a
     * given vector.  This method has special casing for a {@code SparseMatrix}.
     * This method also assumes that {@code matrix} is row based and iterates
     * over each of the values in the row before iterating over another row.
     */
    private DoubleVector computeMatrixTransposeV(Matrix matrix,
                                                 DoubleVector v) {
        DoubleVector newV = new DenseVector(matrix.columns());
        if (matrix instanceof SparseMatrix) {
            SparseMatrix smatrix = (SparseMatrix) matrix;
            for (int r = 0; r < smatrix.rows(); ++r) {
                SparseDoubleVector row = smatrix.getRowVector(r);
                int[] nonZeros = row.getNonZeroIndices();
                for (int c : nonZeros)
                    newV.add(c, row.get(c) * v.get(r));
            }
        } else {
            for (int r = 0; r < matrix.rows(); ++r)
                for (int c = 0; c < matrix.columns(); ++c)
                    newV.add(c, matrix.get(r, c) * v.get(r));
        }
        return newV;
    }

    /**
     * Computes the dot product between a given matrix and a given vector {@code
     * newV}.  The result is stored in {@code v}.  This method has special
     * casing for when {@code matrix} is a {@code SparseMatrix}.  This method
     * also assumes that {@code matrix} is row based and iterates over each of
     * the values in the row before iterating over another row.
     */
    private void computeMatrixDotV(Matrix matrix,
                                   DoubleVector newV,
                                   DoubleVector v) {
        // Special case for sparse matrices.
        if (matrix instanceof SparseMatrix) {
            SparseMatrix smatrix = (SparseMatrix) matrix;
            for (int r = 0; r < smatrix.rows(); ++r) {
                double vValue = 0;
                SparseDoubleVector row = smatrix.getRowVector(r);
                int[] nonZeros = row.getNonZeroIndices();
                for (int c : nonZeros)
                    vValue += row.get(c) * newV.get(c);
                v.set(r, vValue);
            }
        } else {
            // Handle dense matrices.
            for (int r = 0; r < matrix.rows(); ++r) {
                double vValue = 0;
                for (int c = 0; c < matrix.columns(); ++c)
                    vValue += matrix.get(r, c) * newV.get(c);
                v.set(r, vValue);
            }
        }
    }

    /**
     * Computes the inter cluster objective for a clustering result.
     *
     * @param result The set of cluster assignments for a set of vectors.
     * @param m the matrix containing each row in the cluster result.
     */
    private double computeIntraClusterScore(ClusterResult result,
                                            Matrix m) {
        DoubleVector[] centroids = new DoubleVector[result.numClusters];
        int[] centroidSizes = new int[result.numClusters];
        double intraClusterScore = 0;
        for (int i = 0; i < result.assignments.length; ++i) {
            int assignment = result.assignments[i];
            DoubleVector v = m.getRowVector(i);
            if (centroids[assignment] == null)
                centroids[assignment] = Vectors.copyOf(v);
            else {
                DoubleVector centroid = centroids[assignment];
                intraClusterScore += (centroidSizes[assignment] -
                                      dotProduct(v, centroid));
                VectorMath.add(centroid, v);
            }
            centroidSizes[assignment]++;
        }
        return intraClusterScore;
    }

    /**
     * Compute the row sums of the values in {@code matrix} and returns the
     * values in a vector of length {@code matrix.columns()}.
     */
    private <T extends Matrix> DoubleVector computeMatrixRowSum(T matrix) {
        DoubleVector rowSums = new DenseVector(matrix.columns());
        for (int r = 0; r < matrix.rows(); ++r)
            VectorMath.add(rowSums, matrix.getRowVector(r));
        return rowSums;
    }

    /**
     * Normalizes using the the largest value in the vector.
     */
    private void normalize(DoubleVector v) {
        double maxValue = 0;
        for (int i = 0; i < v.length(); ++i)
            maxValue += Math.pow(v.get(i), 2);
        maxValue = Math.sqrt(maxValue);
        for (int i = 0; i < v.length(); ++i)
            v.set(i, v.get(i) / maxValue);
    }

    private void verbose(String out) {
        LOGGER.info(out);
    }

    /**
     * A simple comparable data struct holding a row vector's weight and the
     * vector's original index in a matrix.
     */
    private class Index implements Comparable {
        public final double weight;
        public final int index;

        public Index(double weight, int index) {
            this.weight = weight;
            this.index = index;
        }

        public int compareTo(Object other) {
            Index i = (Index) other;
            return (int) (this.weight - i.weight);
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
