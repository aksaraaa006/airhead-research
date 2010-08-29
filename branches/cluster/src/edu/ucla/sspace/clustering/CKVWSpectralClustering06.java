package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;

import java.util.Properties;


/**
 * @author Keith Stevens
 */
public class CKVWSpectralClustering06 implements Clustering {

    public Assignment[] cluster(Matrix matrix, Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralCut.SuperSpectralGenerator());
        return cluster.cluster(matrix);
    }

    public Assignment[] cluster(Matrix matrix,
                                int numClusters,
                                Properties props) {
        SpectralClustering cluster = new SpectralClustering(
                .2, new SuperSpectralCut.SuperSpectralGenerator());
        return cluster.cluster(matrix, numClusters);
    }
}
