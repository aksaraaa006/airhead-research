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

import edu.ucla.sspace.index.IndexBuilder;
import edu.ucla.sspace.index.BeagleIndexBuilder;
import edu.ucla.sspace.index.RandomIndexBuilder;

import edu.ucla.sspace.hermit.FlyingHermit;
import edu.ucla.sspace.hermit.Hermit;

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
 *
 * <li> {@code --dimensions=<int>} how many dimensions to use for the LSA
 *      vectors.  See {@link Hermit} for default value
 * 
 * <li> {@code --preprocess=<class name>} specifies an instance of {@link
 *      edu.ucla.sspace.lsa.MatrixTransformer} to use in preprocessing the
 *      word-document matrix compiled by LSA prior to computing the SVD.  See
 *      {@link Hermit} for default value
 *
 * <li> {@code --holographsize} length of the holograph vectors used in
 *      conjuction with the lsa term-document matrix.  by default 2048.
 *
 * <li> {@code --builder} class name of the
 *      {@link edu.ucla.sspace.common.IndexBuilder} used to compose the
 *      holograph vectors.  Currently only accepts "BeagleIndexBuilder" and
 *      "RandomIndexBuilder".
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * hermit-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code hermit-semantic-space<number>.sspace} will
 * be created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see Hermit
 * @see edu.ucla.sspace.lsa.MatrixTransformer MatrixTransformer
 *
 * @author Keith Stevens 
 */
public class HermitMain extends GenericMain {
    public static final String SIMPLE_CLUSTER = "SimpleVectorClusterMap";
    public static final String EXEMPLAR_CLUSTER = "ExemplarVectorClusterMap";

    private static final int DEFAULT_DIMENSION = 2048;
    private int dimension;
    private IndexBuilder builder;
    private BottomUpVectorClusterMap clusterMap;

    private HermitMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
        options.addOption('h', "holographsize",
                          "The size of the holograph vectors",
                          true, "INT", "Process Properties");
        options.addOption('b', "builder", "Index builder to use for hermit",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('r', "replacementMap",
                          "A file which specifies mappings between terms " + 
                          "and their replacements",
                          true, "FILE", "Processing Properties");
        options.addOption('t', "threshold",
                          "The threshold for clustering similar context " +
                          "vectors", true, "DOUBLE", "Cluster Properties");
        options.addOption('s', "senseCount",
                          "The maximum number of senses Hermit should produce",
                          true, "INT", "Cluster Properties");
        options.addOption('c', "cluster",
                          "Class type to use for clustering semantic vectors",
                          true, "CLASSNAME", "Cluster Properties");
        options.addOption('S', "saveVectors",
                          "Save index vectors to a binary file",
                          true, "FILE", "Post Processing");
        options.addOption('L', "loadVectors",
                          "Load index vectors from a binary file",
                          true, "FILE", "Post Processing");
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
        dimension = (argOptions.hasOption("holographsize"))
            ? argOptions.getIntOption("holographsize")
            : DEFAULT_DIMENSION;
        String builderType = (argOptions.hasOption("builder"))
            ? argOptions.getStringOption("builder")
            : "BeagleIndexBuilder";
        if (builderType.equals("BeagleIndexBuilder")) {
            builder = new BeagleIndexBuilder(dimension);
            if (argOptions.hasOption("loadVectors"))
                builder.loadIndexVectors(new File(
                            argOptions.getStringOption("loadVectors")));
        } else if (builderType.equals("RandomIndexBuilder"))
            builder = new RandomIndexBuilder(dimension);

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
            builder.saveIndexVectors(new File(filename));
        }
    }

    public SemanticSpace getSpace() {
        return new FlyingHermit(builder, dimension, clusterMap);
    }

    public Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("threads"))
            props.setProperty(Hermit.NUM_THREADS_PROPERTY,
                              argOptions.getStringOption("threads"));
        else
            props.setProperty(Hermit.NUM_THREADS_PROPERTY,
                              "" + Runtime.getRuntime().availableProcessors());

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
