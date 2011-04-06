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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.KPartiteLinkClustering;
import edu.ucla.sspace.clustering.LinkClustering;
import edu.ucla.sspace.clustering.WeightedLinkClustering;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.mains.OptionDescriptions;

import edu.ucla.sspace.matrix.GrowingSparseMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.util.BiMap;
import edu.ucla.sspace.util.HashBiMap;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.LineReader;
import edu.ucla.sspace.util.LoggerUtil;
import edu.ucla.sspace.util.MultiMap;

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Logger;
import java.util.logging.Level;

// Logger helper methods
import static edu.ucla.sspace.util.LoggerUtil.info;
import static edu.ucla.sspace.util.LoggerUtil.verbose;


/**
 * A utility class for running {@link LinkClustering} from the command line.
 */
public class LinkClusteringTool {

    private static final Logger LOGGER = 
        Logger.getLogger(LinkClusteringTool.class.getName());

    public static void main(String[] args) {
        ArgOptions opts = new ArgOptions();
        
        opts.addOption('h', "help", "Generates a help message and exits",
                          false, null, "Program Options");
        opts.addOption('w', "weighted", "Uses a weighted edge simiarity",
                          false, null, "Program Options");
        opts.addOption('p', "kpartite", "Uses the k-partite link clustering",
                          false, null, "Program Options");
        opts.addOption('o', "offCore", "Keeps the edge similarity matrix " +
                          "out of main memory, which slows processing but " +
                          "allows much larger graphs to be clustered",
                          false, null, "Program Options");
        opts.addOption('d', "printDensities", "Prints all the cluster " +
                       "densities to the specified file", true, "FILE",
                       "Program Options");
        opts.addOption('a', "saveAllSolutions", "Saves the communities for all"+
                       "possible partitionings", true, "FILE_PREFIX",
                       "Program Options");
        opts.addOption('n', "saveNthSolutions", "Saves only every nth solution"+
                       " when -a is used", true, "INT", "Program Options");
        opts.addOption('v', "verbose", "Turns on verbose output",
                          false, null, "Program Options");
        opts.addOption('V', "verbVerbose", "Turns on very verbose output",
                          false, null, "Program Options");


        opts.parseOptions(args);

        if (opts.numPositionalArgs() < 2 || opts.hasOption("help")) {
            usage(opts);
            return;
        }

        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (opts.hasOption('v')) 
            LoggerUtil.setLevel(Level.FINE);
        if (opts.hasOption('V')) 
            LoggerUtil.setLevel(Level.FINER);

        boolean isWeighted = opts.hasOption('w');
        boolean isKPartite = opts.hasOption('p');
        if (opts.hasOption('o')) {
            Properties p = System.getProperties();
            p.setProperty(LinkClustering.
                          KEEP_SIMILARITY_MATRIX_IN_MEMORY_PROPERTY, "false");
        }

        // This map will store the mapping from a vertex key to its row in the
        // matrix.
        BiMap<String,Integer> keyToRow = new HashBiMap<String,Integer>();
       
        // If the user specifies the k-partite option, this will store the
        // mapping from partition name to index.
        BiMap<String,Integer> partitionToIndex 
            = new HashBiMap<String,Integer>();
        List<Integer> partitionMapping = new ArrayList<Integer>();

        // This matrix is what LinkClustering will use for input.
        SparseMatrix sm = new GrowingSparseMatrix();
        
        int numEdges = 0;

        LOGGER.info("Loading graph file");
        
        int lineNo = 0;
        for (String line : new LineReader(new File(opts.getPositionalArg(0)))) {
            if (line.startsWith("#")) {
                lineNo++;
                continue;
            }
            String[] arr = line.split("\\s+");
            
            // Skip blank lines
            if (arr.length == 0) {
                lineNo++;
                continue;
            }
            
            if (isKPartite && arr.length != 5
                    || isWeighted && arr.length < 3
                    || arr.length < 2) {
                System.out.printf("missing data on line %d:%n%s%n", 
                                  lineNo, line);
                return;
            }

            String ver1 = arr[0];
            String ver2 = arr[1];

            // Get the weight, if the user sets it.  Otherwise, use a uniform
            // weight.
            double weight = 1;
            if (isWeighted) {
                try {
                    weight = Double.parseDouble(arr[2]);
                } catch (NumberFormatException nfe) {
                    System.out.printf("invalid weight on line %d:%n%s%n", 
                                      lineNo, line);
                    return;
                }

                if (weight == 0) {
                    System.out.printf("Ignoring 0-weighted edge on line "+
                                      "%d:%n%s%n", lineNo, line);
                    lineNo++;
                    continue;
                }
            }

            // Get the row mapping for both vertices
            Integer ver1row = keyToRow.get(ver1);
            if (ver1row == null) {
                ver1row = keyToRow.size();
                keyToRow.put(ver1, ver1row);
            }

            Integer ver2row = keyToRow.get(ver2);
            if (ver2row == null) {
                ver2row = keyToRow.size();
                keyToRow.put(ver2, ver2row);
            }

            // Set the weight/connection between the two vertices in the matrix
            sm.set(ver1row, ver2row, weight);

            // NOTE: this could include duplicate edges, but we skip the check.
            ++numEdges;
            
           if (isKPartite) {
                String vert1partition = arr[3];
                Integer v1pId = partitionToIndex.get(vert1partition);
                if (v1pId == null) {
                    v1pId = partitionToIndex.size();
                    partitionToIndex.put(vert1partition, v1pId);
                }
                
                // Check that vertex 1 isn't in a different partition
                Integer p = partitionMapping.get(ver1row);
                if (p != null && !p.equals(v1pId)) {
                    throw new IllegalStateException(String.format(
                        "Line %d: vertex %d is specified as being in two " +
                        "partitions: %s and %d", lineNo, ver1, 
                        partitionToIndex.inverse().get(p), vert1partition));
                }
                partitionMapping.set(ver1row, v1pId);

                String vert2partition = arr[4];
                Integer v2pId = partitionToIndex.get(vert2partition);
                if (v2pId == null) {
                    v2pId = partitionToIndex.size();
                    partitionToIndex.put(vert2partition, v2pId);
                }
                
                // Check that vertex 1 isn't in a different partition
                p = partitionMapping.get(ver2row);
                if (p != null && !p.equals(v2pId)) {
                    throw new IllegalStateException(String.format(
                        "Line %d: vertex %d is specified as being in two " +
                        "partitions: %s and %d", lineNo, ver2, 
                        partitionToIndex.inverse().get(p), vert2partition));
                }
                partitionMapping.set(ver2row, v2pId);
            }

            lineNo++;
        }

        info(LOGGER, "Loaded %d vertices and %d edges%s",
             sm.rows(), numEdges, 
             ((isKPartite) 
              ? " and " + partitionToIndex.size() + " partitions"
              : "" ));

        if (sm.rows() != sm.columns())
            sm.set(Math.max(sm.rows(), sm.columns()), 
                   Math.max(sm.rows(), sm.columns()), 0);
        
        Assignment[] assignments = null;
        LinkClustering lc = null;
        // Special case for k-partite to use the partite overload
        if (isKPartite) {
            KPartiteLinkClustering kplc = new KPartiteLinkClustering();
            lc = kplc;
            assignments = kplc.cluster(sm, System.getProperties(),
                                       partitionToIndex.size(), 
                                       partitionMapping);
        }
        else {
            lc = (isWeighted)
                ? new WeightedLinkClustering()
                : new LinkClustering();
            assignments = lc.cluster(sm, System.getProperties());
        }
        
        info(LOGGER, "writing partitioning with highest density");
        writeCommunities(assignments, keyToRow.inverse(), 
                         opts.getPositionalArg(1));

        if (opts.hasOption('a')) {
            String outputSolPrefix = opts.getStringOption('a');
            int stepSize = (opts.hasOption('n'))
                ? opts.getIntOption('n')
                : 1;
            int numSolutions = lc.numberOfSolutions();
            for (int i = 0; i < numSolutions; i+= stepSize) {
                Assignment[] solution = lc.getSolution(i);
                writeCommunities(solution, keyToRow.inverse(),
                                 outputSolPrefix + "_" + i);
            }
        }

        if (opts.hasOption('d')) {
            String densitiesFile = opts.getStringOption('d');
            try {
                PrintWriter pw = new PrintWriter(densitiesFile);
                int numSolutions = lc.numberOfSolutions();
                for (int i = 0; i < numSolutions; i++) {
                    pw.println(i + " " + lc.getSolutionDensity(i));
                }
                pw.close();
            }
            catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
    }

    private static void writeCommunities(Assignment[] assignments, 
                                         BiMap<Integer,String> rowToKey,
                                         String fileName) {
        // Write the result
        MultiMap<String,Integer> clusterToRows = 
            new HashMultiMap<String,Integer>();

        // Calculate the cluster mapping
        for (int i = 0; i < assignments.length; ++i) {
            for (int clusterId : assignments[i].assignments()) {
                clusterToRows.put("cluster_" + clusterId, i);
            }
        }                   
        
        try {
            PrintWriter output = new PrintWriter(fileName);
            for (String clusterId : clusterToRows.keySet()) {
                Set<Integer> rows = clusterToRows.get(clusterId);
                StringBuilder sb = new StringBuilder();
                sb.append(clusterId);
                for (Integer row : rows) {
                    String t = rowToKey.get(row);
                    sb.append(' ').append(t);
                }
                output.println(sb);
            }
            
            output.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Prints the options and supported commands used by this program.
     *
     * @param options the options supported by the system
     */
    private static void usage(ArgOptions options) {
        System.out.println(
            "Link Clustering 1.0, " +
            "based on the community detection method of\n\n" +
            "\tYong-Yeol Ahn, James P. Bagrow, and Sune Lehmann. 2010.\n" +
            "\tLink communities reveal multiscale complexity in networks.\n" +
            "\tNature, (466):761–764, August.\n\n" +
            "usage: java -jar lc.jar [options] edge_file communities.txt \n\n" 
            + options.prettyPrint() +
            "\nThe edge file format is:\n" +
            "   vertex1 vertex2 [weight] [partition_for_v1 partition_for_v2]\n" +
            "where vertices may be named using any contiguous sequence of " +
            "characters or\n" +
            "numbers.  Weights may be any non-zero double value.  Partitions " +
            "can be\n" +
            "named using any contiguous sequence of characters or numbers.  " +
            "Lines beginning\n" +
            "with '#' are treated as comments and skipped.\n\n"+
            OptionDescriptions.HELP_DESCRIPTION);
    }

}