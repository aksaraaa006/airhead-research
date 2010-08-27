package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;


/**
 * @author Keith Stevens
 */
public interface EigenCut {

    void computeCut(Matrix matrix);

    int[] getLeftReordering();

    Matrix getLeftCut();

    Matrix getRightCut();

    int[] getRightReordering();

    double getSplitObjective(double alpha, double beta,
                             int leftNumClusters, int[] leftAssignments,
                             int rightNumClusters, int[] rightAssignments);

    double getMergedObjective(double alpha, double beta);
}
