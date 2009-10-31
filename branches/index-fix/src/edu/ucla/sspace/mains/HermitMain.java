/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IndexGenerator;
import edu.ucla.sspace.index.IndexUser;
import edu.ucla.sspace.index.RandomIndexUser;

import edu.ucla.sspace.hermit.FlyingHermit;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.BottomUpVectorClusterMap;
import edu.ucla.sspace.util.SimpleVectorClusterMap;
import edu.ucla.sspace.util.ExemplarVectorClusterMap;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;


/**
 * An executable class for running {@link Hermit} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 * </ul>
 *
 * <p>
 *
 * @see FlyingHermit
 *
 * @author Keith Stevens 
 */
public class HermitMain extends GenericMain {
    public static final String SIMPLE_CLUSTER = "SimpleVectorClusterMap";
    public static final String EXEMPLAR_CLUSTER = "ExemplarVectorClusterMap";

    private static final int DEFAULT_DIMENSION = 2048;
    private int dimension;
    private int prevWordsSize;
    private int nextWordsSize;
    private IndexGenerator generator;
    private Class indexUserClazz;
    private BottomUpVectorClusterMap clusterMap;

    private HermitMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
        // Add process property arguements such as the size of index vectors,
        // the generator class to use, the user class to use, the window sizes
        // that should be inspected and the set of terms to replace during
        // processing.
        options.addOption('l', "vectorLength",
                          "The size of the vectors",
                          true, "INT", "Process Properties");
        options.addOption('g', "generator", "IndexGenerator to use for hermit",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('e', "user", "IndexUser to use for hermit",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('s', "windowSize",
                          "The number of words before, and after the focus " +
                          "term to inspect",
                          true, "INT,INT", "Process Properties");
        options.addOption('p', "usePermutations",
                          "Set to true if permutations should be used",
                          false, null, "Process Properties");
        options.addOption('u', "useDenseSemantics",
                          "Set to true if dense vectors should be used",
                          false, null, "Process Properties");
        options.addOption('m', "replacementMap",
                          "A file which specifies mappings between terms " + 
                          "and their replacements",
                          true, "FILE", "Processing Properties");

        // Add arguments for setting clustering properties such as the
        // similarity threshold, maximum number of senses to create, and the
        // clustering mechanism to use. 
        options.addOption('h', "threshold",
                          "The threshold for clustering similar context " +
                          "vectors", true, "DOUBLE", "Cluster Properties");
        options.addOption('c', "senseCount",
                          "The maximum number of senses Hermit should produce",
                          true, "INT", "Cluster Properties");
        options.addOption('r', "cluster",
                          "Class type to use for clustering semantic vectors",
                          true, "CLASSNAME", "Cluster Properties");

        // Additional processing steps.
        options.addOption('S', "saveVectors",
                          "Save index vectors to a binary file",
                          true, "FILE", "Post Processing");
        options.addOption('L', "loadVectors",
                          "Load index vectors from a binary file",
                          true, "FILE", "Pre Processing");
    }

    public static void main(String[] args) {
        HermitMain hermit = new HermitMain();
        try {
            hermit.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    public void handleExtraOptions() {
        dimension = (argOptions.hasOption("vectorLength"))
            ? argOptions.getIntOption("vectorLength")
            : DEFAULT_DIMENSION;

        // Process the window size arguments;
        String windowValue = (argOptions.hasOption('w'))
            ? argOptions.getStringOption('w')
            : "5,5";
        String[] prevNext = windowValue.split(",");
        prevWordsSize = Integer.parseInt(prevNext[0]);
        nextWordsSize = Integer.parseInt(prevNext[1]);


        // Create the generator.
        String generatorType = (argOptions.hasOption("generator"))
            ? argOptions.getStringOption("generator")
            : "edu.ucla.sspace.index.RandomIndexGenerator";
        try {
            Class generatorClazz = Class.forName(generatorType);
            System.setProperty(IndexGenerator.INDEX_VECTOR_LENGTH_PROPERTY,
                               Integer.toString(dimension));
            generator = (IndexGenerator) (generatorClazz.newInstance());
        } catch (Exception e) {
            throw new Error(e);
        }

        // Create the user.
        String userType = (argOptions.hasOption("user"))
            ? argOptions.getStringOption("user")
            : "edu.ucla.sspace.index.RandomIndexUser";
        try {
            indexUserClazz = Class.forName(userType);
            System.setProperty(IndexUser.INDEX_VECTOR_LENGTH_PROPERTY,
                               Integer.toString(dimension));
            System.setProperty(IndexUser.WINDOW_SIZE_PROPERTY,
                               windowValue);
            if (argOptions.hasOption("usePermutations"))
                System.setProperty(RandomIndexUser.USE_PERMUTATION_PROPERTY,
                                   "true");
            if (argOptions.hasOption("useDenseSemantics"))
                System.setProperty(RandomIndexUser.USE_DENSE_SEMANTICS_PROPERTY,
                                   "true");
        } catch (Exception e) {
            throw new Error(e);
        }

        if (argOptions.hasOption("loadVectors"))
            generator.loadIndexVectors(
                    new File(argOptions.getStringOption("loadVectors")));

        // Process the cluster arguments.
        int maxSenseCount = (argOptions.hasOption("senseCount"))
            ? argOptions.getIntOption("senseCount")
            : 2;
        double threshold = (argOptions.hasOption("threshold"))
            ? argOptions.getDoubleOption("threshold")
            : .75;

        String clusterName = (argOptions.hasOption("cluster"))
            ? argOptions.getStringOption("cluster")
            : SIMPLE_CLUSTER;
        if (clusterName.equals(SIMPLE_CLUSTER))
            clusterMap = new SimpleVectorClusterMap(threshold, maxSenseCount);
        else if (clusterName.equals(EXEMPLAR_CLUSTER))
            clusterMap = new ExemplarVectorClusterMap(threshold, maxSenseCount);
    }

    protected void postProcessing() {
        if (argOptions.hasOption("saveVectors")) {
            String filename = argOptions.getStringOption("saveVectors");
            generator.saveIndexVectors(new File(filename));
        }
    }

    public SemanticSpace getSpace() {
        return new FlyingHermit(generator, indexUserClazz, clusterMap,
                                dimension, prevWordsSize, nextWordsSize);
    }

    public Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("replacementMap"))
            props.setProperty(IteratorFactory.TOKEN_REPLACEMENT_FILE_PROPERTY,
                              argOptions.getStringOption("replacementMap"));
        return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
         System.out.println(
                 "usage: java HermitMain [options] <output-dir>\n" + 
                 argOptions.prettyPrint());
    }
}
