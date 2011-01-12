package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class CKVWSpectralClustering06 implements Clustering {

    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.CKVWSpectralClustering06";

    public static final String USE_KMEANS =
        PROPERTY_PREFIX + ".useKMeans";

    public Assignments cluster(Matrix matrix, Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralGenerator());
        return cluster.cluster(matrix);
    }

    public Assignments cluster(Matrix matrix,
                                int numClusters,
                                Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralGenerator());
        return cluster.cluster(
                matrix, numClusters, props.getProperty(USE_KMEANS) != null);
    }

    public class SuperSpectralCut extends BaseSpectralCut {

        protected DoubleVector computeSecondEigenVector(Matrix matrix,
                                                        int vectorLength) {
           // Compute pi, and D.  Pi is the normalized form of rho.  D a
           // diagonal matrix with sqrt(pi) as the values along the diagonal.
           // Also compute pi * D^-1.
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

            // Create the second largest eigenvector of the a scaled form of the
            // row normalized affinity matrix.  The computation is using the
            // power method such that the affinity matrix is never explicitly
            // computed.
            // piDInverse serves as a vector which is similar to the first eigen
            // vector.  The second eigen vector is assumed to be orthogonal to
            // piDInverse.  This algorithm makes O(log(matrix.rows())) passes
            // through the data matrix.
         
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

                // Step 2, repeated, (a) normalize v (b) set v = Q*v, where Q =
                // D * R-Inverse * matrix * matrix-Transpose * D-Inverse.

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

                // Step 2b-4) v = D*R-Inverse * v. Note that R is a diagonal
                // matrix with rho as the values along the diagonal.
                for (int i = 0; i < vectorLength; ++i) {
                    double oldValue = v.get(i);
                    double newValue = oldValue * D.get(i) / rho.get(i);
                    v.set(i, newValue);
                }
            }

            return v;
        }
    }

    public class SuperSpectralGenerator implements Generator<EigenCut> {

        public EigenCut generate() {
            return new SuperSpectralCut();
        }
    }
}
