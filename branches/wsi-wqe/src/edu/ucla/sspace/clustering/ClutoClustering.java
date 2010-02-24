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

import java.util.Properties;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A class for interacting with the <a
 * href="http://glaros.dtc.umn.edu/gkhome/cluto/cluto/overview">CLUTO</a>
 * clustering library.
 *
 * @author David Jurgens
 */
public class ClutoClustering implements Clustering {

    /**
     * A property prefix for specifiying options when using Cluto.
     */
    public static String PROPERTY_PREFIX = 
        "edu.ucla.sspace.clustering.ClutoClustering";

    /**
     * The property to set the name of a {@link #Method} that Cluto should use
     * in clustering the data.
     */
    public static String CLUSTER_METHOD = 
        PROPERTY_PREFIX + ".clusterSimilarity";

    /**
     * The method by which CLUTO should cluster the data points
     */
    public enum Method {
        
        REPEATED_BISECTIONS_REPEATED("rbr"),
        KMEANS("direct"),
        AGGLOMERATIVE("agglo"),
        NEAREST_NEIGHBOOR("graph"),
        BAGGLO("bagglo");       

        /**
         * The string abbreviation for each clustering method
         */
        private final String name;

        Method(String name) {
            this.name = name;
        }

        /**
         * Returns the name for this method that CLUTO uses on the command line.
         */
        String getClutoName() {
            return name;
        }
    }

    /**
     * The default clustering method to be used by Cluto.
     */
    private static Method DEFAULT_CLUSTER_METHOD = Method.AGGLOMERATIVE;

    /**
     * A logger to track the status of Cluto.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(ClutoClustering.class.getName());

    /**
     * Creates a new {@code ClutoClustering} instance.
     */
    public ClutoClustering() { }

    /**
     * {@inheritDoc}
     *
     * @param props the properties to use for clustering with CLUTO.  See {@link
     *        ClutoClustering} for the list of supported properties.
     */
    public Assignment[] cluster(Matrix matrix, Properties properties) {
        return cluster(matrix, System.getProperties());
    }

    /**
     * {@inheritDoc}
     *
     * @param props the properties to use for clustering with CLUTO.  See {@link
     *        ClutoClustering} for the list of supported properties.
     */
    public Assignment[] cluster(Matrix matrix, int numClusters, 
                                Properties properties) {
        Method clmethod = DEFAULT_CLUSTER_METHOD;
        String methodProp = properties.getProperty(CLUSTER_METHOD);
        if (methodProp != null) 
            clmethod = Method.valueOf(methodProp);
        return cluster(matrix, numClusters, clmethod);
    }

    /**
     * Clusters the set of rows in the given {@code Matrix} into a specified
     * number of clusters using the specified CLUTO clustering method.
     *
     * @param matrix the {@link Matrix} containing data points to cluster
     * @param numClusters the number of clusters to generate
     * @param clusterMethod the method by which cluto should cluster the rows
     *
     * @return an array of {@link ClusterAssignment}s that may contain only one
     *         assignment or multiple
     */
    public Assignment[] cluster(Matrix matrix, int numClusters, 
                                Method clusterMethod) {
        try {
            String clmethod = clusterMethod.getClutoName();
            return ClutoWrapper.cluster(matrix, clmethod, numClusters);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}
