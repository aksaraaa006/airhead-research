

package edu.ucla.sspace.clustering.criterion;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.util.ArrayList;
import java.util.List;


/**
 * @author Keith Stevens
 */
public class H1Function extends HybridBaseFunction {

    /**
     * {@inheritDoc}
     */
    protected BaseFunction getInternalFunction() {
        return new I1Function(matrix, centroids, i1Costs, 
                              assignments, clusterSizes);
    }

    /**
     * {@inheritDoc}
     */
    protected BaseFunction getExternalFunction() {
        return new E1Function(matrix, centroids, e1Costs,
                              assignments, clusterSizes,
                              completeCentroid, simToComplete);
    }

    /**
     * {@inheritDoc}
     */
    public boolean maximize() {
        return true;
    }
}
