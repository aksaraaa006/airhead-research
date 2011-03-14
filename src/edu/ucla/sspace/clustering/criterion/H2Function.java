

package edu.ucla.sspace.clustering.criterion;


/**
 * @author Keith Stevens
 */
public class H2Function extends HybridBaseFunction {

    /**
     * {@inheritDoc}
     */
    protected BaseFunction getInternalFunction() {
        return new I2Function(matrix, centroids, i1Costs, 
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
