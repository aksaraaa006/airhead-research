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

import edu.ucla.sspace.clustering.OnlineClusteringGenerator;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.IntegerVectorGeneratorMap;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.hermit.FlyingHermit;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.LimitedOneLinePerDocumentIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

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
 *   </li> {@code -u}, {@code --useDenseSemantics} Set to true if dense vectors
 *         should be used
 *
 *   </ul>
 *
 * <li><u>Tokenizing Options</u>
 *
 *   <ul>
 *
 *   </li> {@code -m}, {@code --replacementMap=FILE} A file which specifies
 *         mappings between terms and their replacements
 *
 *   </li> {@code -A}, {@code --acceptanceList=FILE} A File with one word per
 *        line listing terms to generate semantics for.  This is separate from
 *        filtering and will override the replacementMap option
 *
 *   </ul>
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
 *   </li> {@code -G}, {@code --clusterGenerator=CLASSNAME} The cluster
 *         generator to use
 *
 *   </li> {@code -W}, {@code --clusterWeight=DOUBLE} If set, this weight will
 *         be used to expoentially average vectors in a cluster"
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
public class FlyingHermitMain extends GenericMain {

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
     * The default {@link OnlineClusteringGenerator} to use.
     */
    public static final String DEFAULT_CLUSTER =
        "edu.ucla.sspace.clustering.OnlineClusteringGenerator";

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
     * The {@link IntegerVectorGeneratorMap} to use.  This may be either a new
     * map, or one deserialized from a file.
     */
    private IntegerVectorGeneratorMap<TernaryVector> vectorMap;

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
    private OnlineClusteringGenerator<SparseIntegerVector> clusterGenerator;

    /**
     * The replacement map, mapping original terms to conflated terms, for
     * testing {@code FlyingHermit}.  If this is not setup, {@code FlyingHermit
     * will default to inferring senses for all words.
     */
    private Map<String, String> replacementMap;

    /**
     * The set of words to generate semantic vectors for.
     */
    private Set<String> acceptedWords;

    /**
     * Uninstantiable.
     */
    public FlyingHermitMain() {
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
        options.addOption('u', "useDenseSemantics",
                          "Set to true if dense vectors should be used",
                          false, null, "Process Properties");
        
        // Add more tokenizing options.
        options.addOption('m', "replacementMap",
                          "A file which specifies mappings between terms " + 
                          "and their replacements",
                          true, "FILE", "Tokenizing Options");
        options.addOption('A', "acceptanceList",
                          "A File with one word per line listing terms to " +
                          "generate semantics for.  This is separate from " + 
                          "filtering and will override the replacementMap " +
                          "option",
                          true, "FILE", "Tokenizing Options");

        // Add arguments for setting clustering properties such as the
        // similarity threshold, maximum number of senses to create, and the
        // clustering mechanism to use. 
        options.addOption('h', "threshold",
                          "The threshold for clustering similar context " +
                          "vectors", true, "DOUBLE", "Cluster Properties");
        options.addOption('c', "senseCount",
                          "The maximum number of senses FlyingHermit should " +
                          "produce",
                          true, "INT", "Cluster Properties");
        options.addOption('G', "clusterGenerator",
                          "The cluster generator to use",
                          true, "CLASSNAME", "Cluster Properties");
        options.addOption('W', "clusterWeight",
                          "If set, this weight will be used to expoentially " +
                          "average vectors in a cluster",
                          true, "DOUBLE", "Cluster Properties");

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
     * Prepare the replacement map for {@code FlyingHermit} based on the
     * contents of {@code filename}.  The expected input is:
     *   original term | replacement
     *
     * @param filename The filename specifying a set of term replacement.
     */
    private void prepareReplacementMap(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = null;
            replacementMap = new HashMap<String, String>();
            while ((line = br.readLine()) != null) {
                String[] wordReplacement = line.split("\\|");
                String[] words = wordReplacement[0].split("\\s+");
                StringBuffer sb = new StringBuffer();
                for (String w : words)
                    sb.append(w.trim()).append(" ");
                replacementMap.put(sb.substring(0, sb.length() - 1),
                                   wordReplacement[1].trim());
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Prepare the acceptance list for {@code FlyingHermit} based on the
     * contents of {@code filename}.  The expected input format is:
     *   word
     *
     * @param filename The filename specifying a set of words to accept.
     */
    private void prepareAcceptanceList(String filename) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = null;
            acceptedWords = new HashSet<String>();
            while ((line = br.readLine()) != null) {
                String word = line.trim();
                acceptedWords.add(word);
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
        
    /**
     * {@inheritDoc}
     */
    @SuppressWarnings("unchecked")
    protected void handleExtraOptions() {
        dimension = argOptions.getIntOption("vectorLength", DEFAULT_DIMENSION);

        // Process the window size arguments;
        String windowValue = argOptions.getStringOption('w', "5,5");
        String[] prevNext = windowValue.split(",");
        prevWordsSize = Integer.parseInt(prevNext[0]);
        nextWordsSize = Integer.parseInt(prevNext[1]);

        if (argOptions.hasOption('m')) {
            prepareReplacementMap(argOptions.getStringOption('m'));
        }
        if (argOptions.hasOption('A'))
            prepareAcceptanceList(argOptions.getStringOption('A'));

        // Setup the PermutationFunction.
        String permType = argOptions.getStringOption("permutationFunction",
                                                     DEFAULT_FUNCTION);
        permFunction = (argOptions.hasOption("usePermutations"))
            ? (PermutationFunction<TernaryVector>) getObjectInstance(permType)
            : null;

        // Setup the generator.
        String generatorType = 
            argOptions.getStringOption("generator", DEFAULT_GENERATOR);
        IntegerVectorGenerator<TernaryVector> generator =
            (IntegerVectorGenerator<TernaryVector>) getObjectInstance(
                    generatorType);

        // Setup the use of dense vectors.
        boolean useDense = argOptions.hasOption("useDenseSemantics");

        // Setup the generator map.
        if (argOptions.hasOption("loadIndexes")) {
            String savedIndexName = argOptions.getStringOption("loadIndexes");
            vectorMap = (IntegerVectorGeneratorMap<TernaryVector>)
                SerializableUtil.load(new File(savedIndexName + ".index"),
                                      IntegerVectorGeneratorMap.class);
            if (argOptions.hasOption("usePermutations"))
                permFunction = (PermutationFunction<TernaryVector>) 
                    SerializableUtil.load(
                            new File(savedIndexName + ".permutation"),
                            PermutationFunction.class);
        } else
            vectorMap = new IntegerVectorGeneratorMap<TernaryVector>(
                    generator, dimension);

        // Setup the clustering generator.
        int maxSenseCount = argOptions.getIntOption("senseCount",
                                                    DEFAULT_SENSE_COUNT);
        double threshold = argOptions.getDoubleOption("threshold",
                                                      DEFAULT_THRESHOLD);
        String clusterName =
            argOptions.getStringOption("clusterGenerator", DEFAULT_CLUSTER);
        System.setProperty(OnlineClusteringGenerator.MAX_CLUSTERS_PROPERTY,
                           Integer.toString(maxSenseCount));
        System.setProperty(OnlineClusteringGenerator.MERGE_THRESHOLD_PROPERTY,
                           Double.toString(threshold));
        if (argOptions.hasOption('W'))
            System.setProperty(OnlineClusteringGenerator.WEIGHTING_PROPERTY,
                               argOptions.getStringOption('W'));
        clusterGenerator =
            (OnlineClusteringGenerator<SparseIntegerVector>) getObjectInstance(
                    clusterName);
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
        return new FlyingHermit(vectorMap, permFunction, clusterGenerator,
                                replacementMap, acceptedWords, dimension,
                                prevWordsSize, nextWordsSize);
    }

    /**
     * Begin processing with {@code FlyingHermit}.
     */
    public static void main(String[] args) {
        FlyingHermitMain hermit = new FlyingHermitMain();
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

    /**
     * {@inheritDoc}
     */
    protected void processDocumentsAndSpace(SemanticSpace space,
                                            Iterator<Document> docIter,
                                            int numThreads,
                                            Properties props) throws Exception {
        int docSize = Integer.MAX_VALUE;
        if (argOptions.hasOption("trainSize"))
            docSize = argOptions.getIntOption("trainSize");
        else if (argOptions.hasOption("documentCount"))
            docSize = argOptions.getIntOption("documentCount");

        LimitedOneLinePerDocumentIterator trainTestIter = 
            new LimitedOneLinePerDocumentIterator(docIter, docSize, false);

        parseDocumentsMultiThreaded(space, trainTestIter, numThreads);

        long startTime = System.currentTimeMillis();
        space.processSpace(props);
        long endTime = System.currentTimeMillis();
        verbose("processed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        // If we are using a test/train set, process the test set now.
        // Otherwise we are finished.
        if (argOptions.hasOption("trainSize")) {
            // Reset the iterator so that the rest of the corpus is used for
            // testing.
            trainTestIter.reset();
            parseDocumentsMultiThreaded(space, trainTestIter, numThreads);
        }
            
        startTime = System.currentTimeMillis();
        space.processSpace(props);
        endTime = System.currentTimeMillis();
        verbose("processed space in %.3f seconds",
                ((endTime - startTime) / 1000d));
    }
}
