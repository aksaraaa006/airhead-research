/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SparseMatrix;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class for interacting with the <a
 * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>
 * clustering library.
 *
 * @author David Jurgens
 */
public class ClutoClustering {

    private static final Logger LOGGER = 
        Logger.getLogger(ClutoClustering.class.getName());

    /**
     * Uninstantiable
     */
    private ClutoClustering() { }

    /**
     * Clusters the rows of the matrix into the specified number of clusters
     * using a hierarchical agglomerative clustering algorithm.  Rows that have
     * no distinguishing features will not be clustered and instead assigned to
     * a cluster index of -1.
     *
     * @param m a matrix whose rows are to be clustered
     * @param numClusters the number of clusters into which the matrix should
     *        divided
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.  Rows that were not able to
     *         be clustered will be assigned a -1 value.
     *
     * @throws IOError if any {@link IOException} occurs when marshalling data
     *         to and from Cluto, or during Cluto's execution.
     */
    public static int[] agglomerativeCluster(Matrix m, int numClusters) {
        try {
            return cluster(m, numClusters, "agglo");
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Clusters the rows of the matrix into the specified number of clusters
     * using the string {@code method} to indicate to Cluto which type of
     * clustering to use.
     *
     * @param m a matrix whose rows are to be clustered
     * @param numClusters the number of clusters into which the matrix should
     *        divided
     * @param method a string recognized by Cluto that indicates which
     *        clustering algorithm should be used
     *
     * @return an array where each element corresponds to a row and the value is
     *         the cluster number to which that row was assigned.  Cluster
     *         numbers will start at 0 and increase.  Rows that were not able to
     *         be clustered will be assigned a -1 value.
     */
    private static int[] cluster(Matrix m, int numClusters, String method) 
            throws IOException {
        LOGGER.log(Level.FINE, "clustering {0} data points with {1} features",
                   new Object[] { m.rows(), m.columns() });
        File matrixFile = File.createTempFile("cluto-input",".matrix");
        matrixFile.deleteOnExit();        
        // NOTE: Cluto seems to have allocation problems on sparse matrices that
        // are dense.  Therefore, try to estimate whether to use a dense matrix
        // format based on the matrix type
        MatrixIO.writeMatrix(m, matrixFile, 
                             ((m instanceof SparseMatrix) 
                              ? MatrixIO.Format.CLUTO_SPARSE
                              : MatrixIO.Format.CLUTO_DENSE));
        File outputFile = File.createTempFile("cluto-output", ".matrix");
        outputFile.deleteOnExit();
        // NOTE: the defaults for Agglomerative clustering are cosine similarity
        // and using mean-link (UPGMA) clustering, which is what we want.
        String commandLine = "vcluster " +
            "-clmethod=" + method + " " +
            "-clustfile=" + outputFile  +
            " " + matrixFile +
            " " + numClusters;
        LOGGER.fine("executing: " + commandLine);
        Process cluto = Runtime.getRuntime().exec(commandLine);
        
        BufferedReader stdout = new BufferedReader(
            new InputStreamReader(cluto.getInputStream()));
        BufferedReader stderr = new BufferedReader(
            new InputStreamReader(cluto.getErrorStream()));
        
        if (LOGGER.isLoggable(Level.FINE)) {
            StringBuilder output = new StringBuilder("Cluto output:\n");
            for (String line = null; (line = stdout.readLine()) != null; ) {
                output.append(line).append("\n");
            }
            LOGGER.fine(output.toString());
        }
	    
        int exitStatus = 0;
        try {
            exitStatus = cluto.waitFor();
            
        } catch (InterruptedException ie) {
            LOGGER.log(Level.SEVERE, "Cluto", ie);
        }
        
        LOGGER.finer("Cluto exit status: " + exitStatus);

        // If Cluto was successful in generating the clustering the rows, read
        // in the results file to generate the output.
        if (exitStatus == 0) {
            int[] clusterAssignment = new int[m.rows()];
            // The cluster assignmnet file is formatted as each row (data point)
            // having its cluster label specified on a separate line.  We can
            // read these in sequence to generate the output array.
            BufferedReader br = new BufferedReader(new FileReader(outputFile));
            for (int i = 0; i < clusterAssignment.length; ++i)
                clusterAssignment[i] = Integer.parseInt(br.readLine());
            return clusterAssignment;
        }
        else {
            StringBuilder sb = new StringBuilder();
            for (String line = null; (line = stderr.readLine()) != null; ) {
                sb.append(line).append("\n");
            }
            // warning or error?
            LOGGER.warning("Cluto exited with error status.  " + exitStatus +
                               " stderr:\n" + sb.toString());
            throw new Error("Clustering failed");
        }
    }
}