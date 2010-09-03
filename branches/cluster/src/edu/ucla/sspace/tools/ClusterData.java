package edu.ucla.sspace.tools;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import edu.ucla.sspace.util.ReflectionUtil;

import java.io.File;


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

        Assignment[] assignments;
        if (options.hasOption('n'))
            assignments = clustering.cluster(
                    data, options.getIntOption('n'), System.getProperties());
        else
            assignments = clustering.cluster(
                    data, System.getProperties());
        for (Assignment assignment : assignments)
            System.out.println(assignment.assignments()[0]);
    }
}
