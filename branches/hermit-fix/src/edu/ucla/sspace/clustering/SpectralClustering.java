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

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.RowMaskedMatrix;
import edu.ucla.sspace.matrix.SparseRowMaskedMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.VectorMath;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import java.util.logging.Logger;

// Temp for testing
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;


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
public class SpectralClustering implements OfflineClustering {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOGGER =
        Logger.getLogger(SpectralClustering.class.getName());

    /**
     * The default intra cluster similarity weight.
     */
    private static final Double DEFAULT_ALPHA = .4;

    /**
     * The default inter cluster similarity weight.
     */
    private static final Double DEFAULT_BETA = .6;

    /**
     * When using the relaxed correlational objective function, this variable
     * specifies the weight for inter cluster similarity. 
     */
    private final double beta;

    /**
     * When using the relaxed correlational objective function, this variable
     * specifies the weight for intra cluster similarity. 
     */
    private final double alpha;

    /**
     * Constructs a new {@code SpectralClustering} instance with default weights
     * for relaxed correlation.
     */
    public SpectralClustering() {
        this(DEFAULT_ALPHA, DEFAULT_BETA);
    }

    /**
     * Constructs a new {@code SpectralClustering} instance with give weights
     * for relaxed correlation.
     */
    public SpectralClustering(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    /**
     * {@inheritDoc}
     *
     * Variables are given notation similar to that of those in "A
     * Divide-and-Merge Methodology for Clustering" when applicable.
     */
    public int[] cluster(Matrix matrix) {

        // First compute the pair wise similarities between every row vector
        // given.
        LOGGER.fine("Computing pair wise similarities");
        PairDistances pairDistances = new PairDistances();
        for (int r = 0; r < matrix.rows(); ++r) {
            for (int c = r+1; c < matrix.rows(); ++c) {
                double sim = Similarity.cosineSimilarity(
                        matrix.getRowVector(r),
                        matrix.getRowVector(c));
                // Scale the similarity so that it is between the range of 0 and
                // 1.
                sim = (sim + 1) / 2;
                pairDistances.set(r, c, sim);
            }
        }
        LOGGER.fine("Pair wise similarities done");

        // Cluster the matrix recursively.
        ClusterResult r = realCluster(matrix, pairDistances, 0);
        return r.assignments;
    }

    private ClusterResult realCluster(Matrix matrix,
                                      PairDistances pairDistances,
                                      int depth) {
        LOGGER.fine("Clustering at depth " + depth);
        // If the matrix has only one element then the item is in it's own
        // cluster.
        if (matrix.rows() == 1)
            return new ClusterResult(new int[] { 0 }, 1);

        int vectorLength = matrix.rows();
        DoubleVector matrixRowSums = computeMatrixRowSum(matrix);

        // Compute p.
        DoubleVector p = new DenseVector(vectorLength);
        double pSum = 0;
        for (int r = 0; r < matrix.rows(); ++r) {
            double dot = Similarity.cosineSimilarity(matrix.getRowVector(r),
                                                     matrixRowSums);
            pSum += dot;
            p.set(r, dot);
        }

        // Compute pi, and D.
        DoubleVector pi = new DenseVector(vectorLength);
        DoubleVector D = new DenseVector(vectorLength);
        DoubleVector piDInverse = new DenseVector(vectorLength);
        for (int i = 0; i < vectorLength; ++i) {
            double piValue = p.get(i)/pSum;
            pi.set(i, piValue);
            D.set(i, Math.sqrt(piValue));
            piDInverse.set(i, piValue / D.get(i));
        }

        // Step 1, generate a random vector, v,  that is orthogonal to
        // pi*D-Inverse.
        System.setProperty(
                RandomOrthogonalVectorGenerator.VECTOR_LENGTH_PROPERTY,
                String.format("%d", vectorLength));
        DoubleVectorGenerator<DoubleVector> generator =
            new RandomOrthogonalVectorGenerator(piDInverse);
        DoubleVector v =
            generator.generateRandomVector(vectorLength);

        int log = (int) Statistics.log2(vectorLength);
        for (int k = 0; k < log; ++k) {
            // Step 2, repeated, (a) normalize v (b) set v = Q*v, where Q = D *
            // R-Inverse * matrix * matrix-Transpose * D-Inverse.
            normalize(v);

            // v = Q*v is broken into 4 sub steps that allow for sparse
            // multiplications. 
            // Step 2b-1) v = D-Inverse*v.
            for (int i = 0; i < vectorLength; ++ i)
                v.set(i, v.get(i) / D.get(i));
            
            // Step 2b-2) v = matrix-Transpose * v.
            DoubleVector newV = computeMatrixTransposeV(matrix, v);

            // Step 2b-3) v = matrix * v.
            computeMatrixDotV(matrix, newV, v);

            // Step 2b-4) v = D*R-Inverse * v. Note that R = p.
            for (int i = 0; i < vectorLength; ++i)
                v.set(i, v.get(i) * D.get(i) / p.get(i));
        }

        // Sort the rows of the original matrix based on their v values.
        Index[] elementIndices = new Index[v.length()];
        for (int i = 0; i < v.length(); ++i)
            elementIndices[i] = new Index(v.get(i), i);
        Arrays.sort(elementIndices);

        // Compute the conductance of the newly sorted matrix.
        DoubleVector x = new DenseVector(matrix.columns());
        DoubleVector y = new DenseVector(matrix.columns());

        // First compute x and y, which are summations of different cuts of the
        // matrix, starting with x being the first row and y being the summation
        // of all other rows.  While doing this, also compute different
        // summations of values in the p vector using the same cut.
        VectorMath.add(x, matrix.getRowVector(elementIndices[0].index));
        double lLeft = p.get(elementIndices[0].index);
        double lRight = 0;
        for (int i = 1; i < elementIndices.length; ++i) {
            VectorMath.add(y, matrix.getRowVector(elementIndices[i].index));
            lRight += p.get(elementIndices[i].index);
        }

        double u = Similarity.cosineSimilarity(x, y); 

        // Find the minimum conductance.
        double minConductance = u / Math.min(lLeft, lRight);
        int cutIndex = 0;
        for (int i = 1; i < elementIndices.length - 1; ++i) {
            // Compute the new value of u, the denominator for computing the
            // conductance.
            DoubleVector vector = matrix.getRowVector(elementIndices[i].index);
            u = u - Similarity.cosineSimilarity(x, vector) +
                    Similarity.cosineSimilarity(y, vector) + 1;

            // Shift over vectors from y to x.
            VectorMath.add(x, vector);
            VectorMath.subtract(y, vector);

            // Shift over values from the p vector.
            lLeft += p.get(elementIndices[i].index);
            lRight -= p.get(elementIndices[i].index);

            // Recompute the new conductance and check if it's the smallest.
            double conductance = u / Math.min(lLeft, lRight);
            if (conductance < minConductance) {
                minConductance = conductance;
                cutIndex = i;
            }
        }

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

        // Create the new masked distance maps.
        PairDistances leftDistances =
            new PairDistances(pairDistances, elementIndices, cutIndex, true);
        PairDistances rightDistances =
            new PairDistances(pairDistances, elementIndices, cutIndex, false);

        // Do clustering on the left and right branches.
        ClusterResult leftResult =
            realCluster(leftMatrix, leftDistances, depth+1);
        ClusterResult rightResult =
            realCluster(rightMatrix, rightDistances, depth+1);

        LOGGER.fine("Merging at depth " + depth);

        // Compute the objective when we keep the two branches split.
        double splitObjective = computeObjective(
                leftResult, rightResult, elementIndices, pairDistances);

        // Compute the objective when we merge the two branches together.
        double mergedObjective = 0;
        for (i = 0; i < matrix.rows(); ++i) {
            for (int j = i + 1; j < matrix.rows(); ++j) {
                double sim = pairDistances.get(i, j);
                mergedObjective += alpha * (1 - sim);
            }
        }

        // If the merged objective value is less than the split version, combine
        // all clusters into one.
        int[] assignments = new int[matrix.rows()];
        int numClusters = 1;
        if (mergedObjective < splitObjective) {
            LOGGER.fine("Selecting to combine sub trees at depth " + depth);
            Arrays.fill(assignments, 0);
        }
        else  {
            LOGGER.fine("Selecting to maintain sub trees at depth " + depth);

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
                    rightResult.assignments[index] + offset;
        }
        return new ClusterResult(assignments, numClusters);
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
     * Computes the relaxed correlation objective between two sets of clusters
     * that were separated according to the eigenvector.
     *
     * @param firstResult The set of cluster assignments for vectors placed into
     *                    the left sub matrix.
     * @param secondResult The set of cluster assignments for vectors placed
     *                     into the rightsub matrix.
     * @param elementIndices A mapping from indices from the left and right
     *                       submatrices to the original matrix row number
     * @param distances cosine similarities between rows in the original matrix
     */
    private double computeObjective(ClusterResult firstResult,
                                    ClusterResult secondResult,
                                    Index[] elementIndices,
                                    PairDistances distances) {
        double objective = 0;
        // Compute the inter and intra cluster similarity between vectors in the
        // left sub matrix.
        for (int i = 0; i < firstResult.assignments.length; ++i) {
            for (int j = i + 1; j < firstResult.assignments.length; ++j) {
                double sim = distances.get(elementIndices[i].index,
                                           elementIndices[j].index);
                if (firstResult.assignments[i] == firstResult.assignments[j])
                    objective += alpha * (1 - sim);
                else
                    objective += beta * sim;
            }
        }
        // Compute the inter cluster similarity between vectors in the left and
        // right sub matrix.
        int offset = firstResult.assignments.length;
        for (int i = 0; i < firstResult.assignments.length; ++i) {
            for (int j = 0; j < secondResult.assignments.length; ++j) {
                double sim = distances.get(elementIndices[i].index,
                                           elementIndices[j+offset].index);
                objective += beta * sim;
            }
        }

        // Compute the inter and intra cluster similarity between vectors in the
        // right sub matrix.
        for (int i = 0; i < secondResult.assignments.length; ++i) {
            for (int j = i + 1; j < secondResult.assignments.length; ++j) {
                double sim = distances.get(elementIndices[i+offset].index,
                                           elementIndices[j+offset].index);
                if (secondResult.assignments[i] == secondResult.assignments[j])
                    objective += alpha * (1 - sim);
                else
                    objective += beta * sim;
            }
        }
        // Compute the inter cluster similarity between vectors in the left and
        // right sub matrix.
        for (int i = 0; i < secondResult.assignments.length; ++i) {
            for (int j = 0; j < firstResult.assignments.length; ++j) {
                double sim = distances.get(elementIndices[i+offset].index,
                                           elementIndices[j].index);
                objective += beta * sim;
            }
        }
        return objective;
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
     * Normalizes using the l2 norm.
     */
    private void normalize(DoubleVector v) {
        double magnitude = 0;
        for (int i = 0; i < v.length(); ++i)
            magnitude += Math.pow(v.get(i), 2);
        for (int i = 0; i < v.length(); ++i)
            v.set(i, v.get(i) / magnitude);
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
     * A simple scaled vector.
     */
    public class ScaledVector implements DoubleVector {
        private final DoubleVector vector;

        private final double scale;

        public ScaledVector(DoubleVector v, double s) {
            vector = v;
            scale = s;
        }

        public double add(int index, double delta) {
            return vector.add(index, delta / scale);
        }

        public double get(int index) {
            return vector.get(index) * scale;
        }

        public Double getValue(int index) {
            return vector.get(index);
        }

        public void set(int index, double value) {
            vector.set(index, value/scale);
        }

        public void set(int index, Number value) {
            vector.set(index, value.doubleValue()/scale);
        }

        public double[] toArray() {
            return vector.toArray();
        }

        public int length() {
            return vector.length();
        }
    }

    /**
     * A map storing the pair wise similarites between row vectors.  This map
     * can be masked with a set of row to row replacements.
     */
    private class PairDistances implements Map<Pair<Integer>, Double> {

        /**
         * The masked set of distances.
         */
        private PairDistances distances;

        /**
         * An original map storing similarities.  Only the original map will use
         * this memeber.
         */
        private Map<Pair<Integer>, Double> map;

        /**
         * The set of mappings from a given index to the real index in {@code
         * distances}.
         */
        private int[] replacementIndices;

        /**
         * The offset of values in {@code replacementIndices}.
         */
        private int offset;

        /**
         * Constructs an original {@code PairDistances}.
         */
        public PairDistances() {
            map = new HashMap<Pair<Integer>, Double>();
            replacementIndices = null;
            distances = null;
        }

        /**
         * Constructs a masked {@code PairDistances} by using the given set of
         * index mappings.  This will either compute a masked using the first
         * {@code cut+1} elements in {@code elementIndices} or it will compute
         * the mask using the last elements in {@code elementIndices}.
         */
        public PairDistances(PairDistances distances,
                             Index[] elementIndices,
                             int cut,
                             boolean useUpToCut) {
            this.distances = distances;
            if (useUpToCut) {
                replacementIndices = new int[cut+1];
                offset = 0;
                for (int i = 0; i < cut+1; ++i)
                    replacementIndices[i] = elementIndices[i].index;
            } else {
                replacementIndices = new int[elementIndices.length - cut - 1];
                offset = cut + 1;
                for (int i = cut + 1; i < elementIndices.length; i++)
                    replacementIndices[i-offset] = elementIndices[i-offset].index;
            }
        }

        /**
         * {@inheritDoc}
         */
        public void clear() {
            map.clear();
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsKey(Object key) {
            return map.containsKey(key);
        }

        /**
         * {@inheritDoc}
         */
        public boolean containsValue(Object value) {
            return map.containsValue(value);
        }

        /**
         * {@inheritDoc}
         */
        public Set<Map.Entry<Pair<Integer>, Double>> entrySet() {
            throw new UnsupportedOperationException("not implemented");
        }

        /**
         * {@inheritDoc}
         */
        public boolean equals(Object o) {
            return map.equals(o);
        }

        /**
         * {@inheritDoc}
         */
        public Double get(Object key) {
            return map.get(key);
        }

        /**
         * {@inheritDoc}
         */
        public int hashCode() {
            return map.hashCode();
        }

        /**
         * {@inheritDoc}
         */
        public boolean isEmpty() {
            return map.isEmpty();
        }

        /**
         * {@inheritDoc}
         */
        public Set<Pair<Integer>> keySet() {
            throw new UnsupportedOperationException(
                    "A masked map does not support new insertions");
        }

        /**
         * {@inheritDoc}
         */
        public Double put(Pair<Integer> key, Double value) {
            return map.put(key, value);
        }

        /**
         * {@inheritDoc}
         */
        public void putAll(Map<? extends Pair<Integer>, ? extends Double> m) {
            throw new UnsupportedOperationException(
                    "A masked map does not support new insertions");
        }

        /**
         * {@inheritDoc}
         */
        public Double remove(Object key) {
            throw new UnsupportedOperationException(
                    "A masked map does not support removes");
        }

        /**
         * {@inheritDoc}
         */
        public int size() {
            return map.size();
        }

        /**
         * {@inheritDoc}
         */
        public Collection<Double> values() {
            return map.values();
        }

        /**
         * A setter that switches the indices so that they lowest is the first
         * in the pair and replaces indices if needed.
         */
        public void set(int i, int j, double value) {
            if (i > j) {
                int swap = j;
                j = i;
                i = swap;
            }
            if (distances == null)
                put(getPair(i, j), value);
            else
                distances.set(replacementIndices[i],
                              replacementIndices[j],
                              value);
        }

        /**
         * A getter that switches the indices so that they lowest is the first
         * in the pair and replaces indices if needed.
         */
        public double get(int i, int j) {
            if (i > j) {
                int swap = j;
                j = i;
                i = swap;
            }
            if (distances == null)
                return get(getPair(i, j));
            else
                return distances.get(replacementIndices[i],
                                     replacementIndices[j]);
        }

        /**
         * Generate a new {@code Pair}
         */
        private Pair<Integer> getPair(int i, int j) {
            if (replacementIndices == null)
                return new Pair<Integer>(i, j);
            return new Pair<Integer>(
                    replacementIndices[i],
                    replacementIndices[j]);
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

    public static void main(String[] args) throws Exception {
        SemanticSpace sspace = new StaticSemanticSpace(args[0]);
        List<DoubleVector> vectors = new ArrayList<DoubleVector>();

        Set<String> words = sspace.getWords();
        int i = 0;
        for (String word : words) {
            if (i == 100)
                break;
            i++;
            vectors.add(Vectors.asDouble(sspace.getVector(word)));
        }

        SpectralClustering clustering = null;
        if (args.length > 1)
            clustering = new SpectralClustering(
                    Double.parseDouble(args[1]),
                    Double.parseDouble(args[2]));
        else 
            clustering = new SpectralClustering();

        System.out.println("Clustering Start");
        int[] assignments =
            clustering.cluster(Matrices.asMatrix(vectors));
        System.out.println("Clustering Done");


        i = 0;
        for (String word : words) {
            if (i == 100)
                break;
            i++;
            System.out.printf("%s %d\n", word, assignments[i++]);
        }
    }
}
