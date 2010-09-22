package edu.ucla.sspace.tools;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.ClusterUtil;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.ReflectionUtil;

import edu.ucla.sspace.vector.DoubleVector;

import java.io.File;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * @author Keith Stevens
 */
public class ClusterData {
    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('m', "matrix",
                          "Cluster the data points in the given matrix",
                          true, "FILE", "Required (At least one of)");
        options.addOption('f', "matrixFormat",
                          "Specifies the matrix file format",
                          true, "String", "Optional");
        options.addOption('s', "semanticSpace",
                          "Cluster the data points in the given SemanticSpace",
                          true, "FILE", "Required (At least one of)");
        options.addOption('c', "clusterAlg",
                          "Cluster with the given algorithm",
                          true, "CLASSNAME", "Required");
        options.addOption('n', "numClusters",
                          "The desired number of clusters",
                          true, "INT", "Optional");
        options.addOption('r', "repetitions",
                          "The desired number of times the algorithm should " +
                          "be run over the data set.  If this is set, the " +
                          "solution with the highest K-Means score will be " +
                          "selected.",
                          true, "INT", "Optional");
        options.parseOptions(args);

        if (!options.hasOption('c') || !options.hasOption('m')) {
            System.out.println("usage: java ClusterData [options]\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        Clustering clustering = (Clustering) ReflectionUtil.getObjectInstance(
                options.getStringOption('c'));
        Matrix data = null;
        if (options.hasOption('m')) {
            Format format = Format.valueOf(
                    options.getStringOption('f').toUpperCase());
            File mFile = new File(options.getStringOption('m'));
            data = MatrixIO.readMatrix(mFile, format);
        } else {
            System.exit(1);
        }

        Properties props = System.getProperties();

        Assignment[] bestAssignments = null;
        double bestScore = Double.MAX_VALUE;

        int runTime = 0;

        int numReptitions = options.getIntOption('r', 1);
        for (int r = 0; r < numReptitions; ++r) {
            Assignment[] assignments;
            int numClusters;
            if (options.hasOption('n')) {
                numClusters = options.getIntOption('n');

                long start = System.currentTimeMillis();
                assignments = clustering.cluster(data, numClusters, props);
                long end = System.currentTimeMillis();
                runTime += (end - start);
            } else {
                long start = System.currentTimeMillis();
                assignments = clustering.cluster(
                    data, System.getProperties());
                long end = System.currentTimeMillis();
                runTime += (end - start);

                Set<Integer> ids = new HashSet<Integer>();
                for (Assignment assignment : assignments)
                    ids.add(assignment.assignments()[0]);
                numClusters = ids.size();
            }
            DoubleVector[] centroids = ClusterUtil.computeCentroids(
                    data, assignments, numClusters);
            double score = ClusterUtil.computeObjective(
                    data, centroids, assignments);
            if (score <= bestScore) {
                bestScore = score;
                bestAssignments = assignments;
            }
        }

        System.out.println(bestScore);
        System.out.println(runTime);
        for (Assignment assignment : bestAssignments)
            System.out.println(assignment.assignments()[0]);
    }
}
