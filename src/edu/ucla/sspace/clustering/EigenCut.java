package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.vector.DoubleVector;


/**
 * @author Keith Stevens
 */
public interface EigenCut {

    double rhoSum();

    DoubleVector computeRhoSum(Matrix matrix);

    void computeCut(Matrix matrix);

    int[] getLeftReordering();

    Matrix getLeftCut();

    Matrix getRightCut();

    int[] getRightReordering();

    double getKMeansObjective();

    double getKMeansObjective(double alpha, double beta,
                              int leftNumClusters, int[] leftAssignments,
                              int rightNumClusters, int[] rightAssignments);

    double getSplitObjective(double alpha, double beta,
                             int leftNumClusters, int[] leftAssignments,
                             int rightNumClusters, int[] rightAssignments);

    double getMergedObjective(double alpha, double beta);
}
