package edu.ucla.sspace.clustering;

import edu.ucla.sspace.common.Statistics;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;


/**
 * @author Keith Stevens
 */
public class SpectralCut extends BaseSpectralCut {
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

        Matrix RinvData = null; // MAGIC HERE

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
