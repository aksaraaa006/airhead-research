package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.RowScaledMatrix;
import edu.ucla.sspace.matrix.RowScaledSparseMatrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.Generator;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;

import java.util.Properties;


/**
 * A spectral clustering implementation based on the following paper:
 *
 * <p style="font-family:Garamond, Georgia, serif"> David Cheng ,  Ravi Kannan ,
 * Santosh Vempala ,  Grant Wang (2003) On a Recursive Spectral Algorithm for
 * Clustering from Pairwise Similaritie. Available
 * <a href="http://research.microsoft.com/en-us/um/people/kannan/Papers/experimentclustering.pdf">here</a>
 *
 * </p>  This implementation implements a subclass of the {@link
 * BaseSpectralCut} and simply computes the second eigen vector for a data set.
 *
 * @author Keith Stevens
 */
public class CKVWSpectralClustering03 implements Clustering {

    /**
     * The proper prefix.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.CKVWSpectralClustering03";

    /**
     * The property used to use K-Means as the objective function.
     */
    public static final String USE_KMEANS =
        PROPERTY_PREFIX + ".useKMeans";

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix, Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SpectralCutGenerator());
        return cluster.cluster(matrix);
    }

    /**
     * {@inheritDoc}
     */
    public Assignments cluster(Matrix matrix,
                                int numClusters,
                                Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SpectralCutGenerator());
        return cluster.cluster(
                matrix, numClusters, props.getProperty(USE_KMEANS) != null);
    }

    /**
     * An internal spectral cut implementation that is based on the referred to
     * paper.  See paper for details.
     */
    public class SpectralCut extends BaseSpectralCut {

        /**
         * {@inheritDoc}
         */
        protected DoubleVector computeSecondEigenVector(Matrix matrix,
                                                        int vectorLength) {
            DoubleVector Rinv = new DenseVector(vectorLength);
            DoubleVector baseVector = new DenseVector(vectorLength);
            for (int i = 0; i < vectorLength; ++i) {
                Rinv.set(i, 1/Math.sqrt(rho.get(i)));
                baseVector.set(i, rho.get(i) * Rinv.get(i));
            }
     
            // Step 1, generate a random vector, v,  that is orthogonal to
            // pi*D-Inverse.
            DoubleVector v = new DenseVector(vectorLength);
            for (int i = 0; i < v.length(); ++i)
                v.set(i, Math.random());

            Matrix RinvData = (matrix instanceof SparseMatrix)
                ? new RowScaledSparseMatrix((SparseMatrix) matrix, Rinv)
                : new RowScaledMatrix(matrix, Rinv);

            // Make log(matrix.rows()) passes.
            int log = (int) Statistics.log2(vectorLength);
            for (int k = 0; k < log; ++k) {
                // start the orthonormalizing the eigen vector.
                v = orthonormalize(v, baseVector);
                DoubleVector newV = computeMatrixTransposeV(RinvData, v);
                computeMatrixDotV(RinvData, newV, v);
            }

            return v;
        }
    }

    /**
     * A simple generator for creating instances of the {@link SpectralCut}
     * class.
     */
    public class SpectralCutGenerator implements Generator<EigenCut> {

        /**
         * {@inheritDoc}
         */
        public EigenCut generate() {
            return new SpectralCut();
        }
    }
}
