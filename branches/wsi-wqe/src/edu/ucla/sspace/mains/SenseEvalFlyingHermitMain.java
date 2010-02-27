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

import edu.ucla.sspace.clustering.OnlineClustering;
import edu.ucla.sspace.clustering.OnlineKMeans;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.hermit.FlyingHermit;
import edu.ucla.sspace.hermit.SenseEvalFlyingHermit;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.LimitedOneLinePerDocumentIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Properties;


/**
 * An executable class for running {@link FlyingHermit} from the
 * command line.  This class uses command line options provided by {@link
 * GenericMain}.  In addition to those options,
 * it this class includes the following options:
 *
 * <ul>
 *
 * <li><u>Optional (At most one of)</u>
 *   <ul> 
 *
 *   </li> {@code -D}, {@code --trainSize=INT} The number of documents to use as
 *         a training set, All other documents will be considered a test set.
 *
 *   </li> {@code -C}, {@code --documentCount=INT} The number of documents
 *         within the document set to process
 *   </ul>
 *
 * <li><u>Process Properties</u>
 *   <ul>
 *
 *   </li> {@code -l}, {@code --vectorLength=INT} The size of the vectors
 *
 *   </li> {@code -g}, {@code --generator=CLASSNAME} {@link
 *         IntegerVectorGenerator} to use for hermit.  Note that this generator
 *         should be genric for {@link TernaryVector}.
 *
 *   </li> {@code -s}, {@code --windowSize=INT,INT} The number of words before,
 *         and after the focus term to inspect
 *
 *   </li> {@code -X}, {@code --windowLimit=INT} The bucket size to use for
 *         context windows
 *
 *   </li> {@code -p}, {@code --permutationFunction=CLASSNAME} The class name of
 *         the permutation function to use.  Note that this {@link
 *         PermutationFunction} should be for {@link TernaryVector}s
 *
 *   </li> {@code -P}, {@code --userPermutations} Set if permutations should be
 *         used
 *
 *   </ul>
 *
 * <li><u>Cluster Properties</u>
 *
 *   <ul>
 *
 *   </li> {@code -h}, {@code --threshold=DOUBLE} The threshold for clustering
 *         similar context vectors.
 *
 *   </li> {@code -c}, {@code --senseCount=INT} The maximum number of senses
 *         {@link FlyingHermit} should produce
 *
 *   </ul>
 *
 * <li><u>Post Processing</u>
 *
 *   <ul>
 *
 *   </li> {@code -S}, {@code --saveIndexes=FILE} Save index vectors and
 *         permutation function to a binary file
 *
 *   </ul>
 *
 * </li>
 *
 * <li><u>Pre Processing</u>
 *
 *   <ul>
 *
 *   </li> {@code -L}, {@code --loadIndexes=FILE} Load index vectors and
 *         permutation function from binary files
 *
 *   <ul>
 *
 * </li>
 *
 * </ul>
 *
 * <p>
 *
 * @see FlyingHermit
 *
 * @author Keith Stevens 
 */
public class SenseEvalFlyingHermitMain extends GenericMain {

    /**
     * The default {@link PermutationFunction} to use.
     */
    public static final String DEFAULT_FUNCTION = 
        "edu.ucla.sspace.index.TernaryPermutationFunction";

    /**
     * The default {@link IntegerVectorGenerator} to use.
     */
    public static final String DEFAULT_GENERATOR =
        "edu.ucla.sspace.index.RandomIndexVectorGenerator";

    /**
     * The default number of dimensions the space will have.
     */
    public static final int DEFAULT_DIMENSION = 10000;

    /**
     * The default number of clusters to generate.
     */
    public static final int DEFAULT_SENSE_COUNT = 2;

    /**
     * The default clustering threshold to use.
     */
    public static final double DEFAULT_THRESHOLD = .75;

    /**
     * The number of dimensions each semantic vector will use.
     */
    private int dimension;

    /**
     * The number of words prior to the focus word to consider part of the
     * window.
     */
    private int prevWordsSize;

    /**
     * The number of words after the focus word to consider part of the window.
     */
    private int nextWordsSize;

    /**
     * The {@link GeneratorMap} to use.  This may be either a new
     * map, or one deserialized from a file.
     */
    private GeneratorMap<TernaryVector> vectorMap;

    /**
     * The {@link PermutationFunction} for {@link TernaryVector}s to use while
     * generating contexts.  This may be either a new function, or one
     * deserialized from a file.
     */
    private PermutationFunction<TernaryVector> permFunction;

    /**
     * The {@link OnLineClusteringGenerator} to use for creating new cluster
     * instances.
     */
    private Generator<OnlineClustering<SparseIntegerVector>> clusterGenerator;

    /**
     * Uninstantiable.
     */
    public SenseEvalFlyingHermitMain() {
    }

    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        // Add corpus division options.
        options.addOption('T', "trainSize",
                          "The number of documents to use as a training set, " +
                          "All other documents will be considered a test set",
                          true, "INT", "Optional (At most one of)");
        options.addOption('D', "documentCount",
                          "The number of documents within the document set " +
                          "to process",
                          true, "INT", "Optional (At most one of)");

        // Add process property arguements such as the size of index vectors,
        // the generator class to use, the user class to use, the window sizes
        // that should be inspected and the set of terms to replace during
        // processing.
        options.addOption('l', "vectorLength",
                          "The size of the vectors",
                          true, "INT", "Process Properties");
        options.addOption('g', "generator",
                          "IntegerVectorGenerator to use for hermit.  Note " +
                          "that this generator should be genric for " +
                          "TernaryVectors",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('s', "windowSize",
                          "The number of words before, and after the focus " +
                          "term to inspect",
                          true, "INT,INT", "Process Properties");
        options.addOption('X', "windowLimit",
                          "The bucket size to use for windows",
                           true, "INT", "Process Properties");
        options.addOption('p', "permutationFunction",
                          "The class name of the permutation function to use." +
                          "  Note that this permutation function should be " +
                          "for TernaryVectors",
                          true, "CLASSNAME", "Process Properties");
        options.addOption('P', "usePermutations",
                          "Set if permutations should be used",
                          false, null, "Process Properties");
        
        // similarity threshold, maximum number of senses to create, and the
        // clustering mechanism to use. 
        options.addOption('h', "threshold",
                          "The threshold for clustering similar context " +
                          "vectors", true, "DOUBLE", "Cluster Properties");
        options.addOption('c', "senseCount",
                          "The maximum number of senses FlyingHermit should " +
                          "produce",
                          true, "INT", "Cluster Properties");

        // Additional processing steps.
        options.addOption('S', "saveIndexes",
                          "Save index vectors and permutation function to a " +
                          "binary file",
                          true, "FILE", "Post Processing");
        options.addOption('L', "loadIndexes",
                          "Load index vectors and permutation function from " +
                          "binary files",
                          true, "FILE", "Pre Processing");
    }

    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected void handleExtraOptions() {
        dimension = argOptions.getIntOption("vectorLength", DEFAULT_DIMENSION);

        // Process the window size arguments;
        if (argOptions.hasOption('s')) {
            String windowValue = argOptions.getStringOption('s');
            String[] prevNext = windowValue.split(",");
            prevWordsSize = Integer.parseInt(prevNext[0]);
            nextWordsSize = Integer.parseInt(prevNext[1]);
        } else {
            prevWordsSize = Integer.MAX_VALUE;
            nextWordsSize = Integer.MAX_VALUE;
        }

        // Setup the PermutationFunction.
        String permType = argOptions.getStringOption("permutationFunction",
                                                     DEFAULT_FUNCTION);
        permFunction = (argOptions.hasOption("usePermutations"))
            ? (PermutationFunction<TernaryVector>) getObjectInstance(permType)
            : null;

        // Setup the generator.
        String generatorType = 
            argOptions.getStringOption("generator", DEFAULT_GENERATOR);
        IntegerVectorGenerator<TernaryVector> generator;
        try {
            Class clazz = Class.forName(generatorType);
            Constructor<?> c = clazz.getConstructor(new Class[]{int.class});
            Object o = c.newInstance(new Object[] {dimension});
            generator = (IntegerVectorGenerator<TernaryVector>) o;
        } catch (Exception e) {
            throw new Error(e);
        }

        // Setup the generator map.
        if (argOptions.hasOption("loadIndexes")) {
            String savedIndexName = argOptions.getStringOption("loadIndexes");
            vectorMap = (GeneratorMap<TernaryVector>)
                SerializableUtil.load(new File(savedIndexName + ".index"),
                                      GeneratorMap.class);
            if (argOptions.hasOption("usePermutations"))
                permFunction = (PermutationFunction<TernaryVector>) 
                    SerializableUtil.load(
                            new File(savedIndexName + ".permutation"),
                            PermutationFunction.class);
        } else
            vectorMap = new GeneratorMap<TernaryVector>(generator);

        // Setup the clustering generator.
        int maxSenseCount = argOptions.getIntOption("senseCount",
                                                    DEFAULT_SENSE_COUNT);
        double threshold = argOptions.getDoubleOption("threshold",
                                                      DEFAULT_THRESHOLD);
        System.setProperty(OnlineKMeans.MAX_CLUSTERS_PROPERTY,
                           Integer.toString(maxSenseCount));
        System.setProperty(OnlineKMeans.MERGE_THRESHOLD_PROPERTY,
                           Double.toString(threshold));
        clusterGenerator = new OnlineKMeans();
    }

    /**
     * Returns an arbitrary object instance based on a class name.
     *
     * @param className The name of a desired class to instantiate.
     */
    private Object getObjectInstance(String className) {
        try {
            Class clazz = Class.forName(className);
            return clazz.newInstance();
        } catch (Exception e) {
            throw new Error(e);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void postProcessing() {
        if (argOptions.hasOption("saveIndexes")) {
            String filename = argOptions.getStringOption("saveIndexes");
            SerializableUtil.save(vectorMap, new File(filename + ".index"));
            SerializableUtil.save(permFunction,
                                  new File(filename + ".permutation"));
        }
    }

    /**
     * {@inheritDoc}
     */
    public SemanticSpace getSpace() {
        return new SenseEvalFlyingHermit(vectorMap, permFunction,
                                         clusterGenerator, dimension,
                                         prevWordsSize, nextWordsSize);
    }

    /**
     * Begin processing with {@code FlyingHermit}.
     */
    public static void main(String[] args) {
        SenseEvalFlyingHermitMain hermit = new SenseEvalFlyingHermitMain();
        try {
            hermit.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void usage() {
         System.out.println(
                 "usage: java FlyingHermitMain [options] <output-dir>\n" + 
                 argOptions.prettyPrint());
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }
}