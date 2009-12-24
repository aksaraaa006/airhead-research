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
import edu.ucla.sspace.index.IntegerVectorMapUtil;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.hermit.FlyingHermit;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.FileListDocumentIterator;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.OneLinePerDocumentIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.IntegerVector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Properties;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


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
public class FlyingHermitMain extends GenericMain {

    public static final String DEFAULT_FUNCTION = 
        "edu.ucla.sspace.index.DefaultPermutationFunction";

    public static final String DEFAULT_GENERATOR =
        "edu.ucla.sspace.index.RandomIndexVectorGenerator";

    public static final String DEFAULT_CLUSTER =
        "edu.ucla.sspace.clustering.OnlineClusteringGenerator";

    public static final int DEFAULT_DIMENSION = 10000;

    public static final int DEFAULT_SENSE_COUNT = 2;

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

    private IntegerVectorGeneratorMap<IntegerVector> vectorMap;

    private PermutationFunction<IntegerVector> permFunction;

    private OnlineClusteringGenerator clusterGenerator;

    /**
     * The replacement map, mapping original terms to conflated terms, for
     * testing {@code FlyingHermit}.  If this is not setup, {@code FlyingHermit
     * will default to inferring senses for all words.
     */
    private Map<String, String> replacementMap;

    private Set<String> acceptedWords;

    /**
     * Uninstantiable.
     */
    private FlyingHermitMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
        options.addOption('D', "trainSize",
                          "The number of documents to use as a training set, " +
                          "All other documents will be considered a test set",
                          true, "INT", "Required");

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
                          "IntegerVectors",
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
                          "for IntegerVectors",
                          true, "CLASSNAME", "Process Properties");
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
        options.addOption('T', "tokenizeWithReplacementMap",
                          "If true, the replacement map will be used when " +
                          "tokenizing",
                          false, null, "Tokenizing Options");

        // Add arguments for setting clustering properties such as the
        // similarity threshold, maximum number of senses to create, and the
        // clustering mechanism to use. 
        options.addOption('h', "threshold",
                          "The threshold for clustering similar context " +
                          "vectors", true, "DOUBLE", "Cluster Properties");
        options.addOption('c', "senseCount",
                          "The maximum number of senses Hermit should produce",
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
    @SuppressWarnings("unchecked")
    public void handleExtraOptions() {
        dimension = argOptions.getIntOption("vectorLength", DEFAULT_DIMENSION);

        // Process the window size arguments;
        String windowValue = argOptions.getStringOption('w', "5,5");
        String[] prevNext = windowValue.split(",");
        prevWordsSize = Integer.parseInt(prevNext[0]);
        nextWordsSize = Integer.parseInt(prevNext[1]);

        if (!argOptions.hasOption('T') && argOptions.hasOption('m')) {
            prepareReplacementMap(argOptions.getStringOption('m'));
        }
        if (argOptions.hasOption('A'))
            prepareAcceptanceList(argOptions.getStringOption('A'));

        // Setup the PermutationFunction.
        String permType = argOptions.getStringOption("permutationFunction",
                                                     DEFAULT_FUNCTION);
        permFunction =
            (PermutationFunction<IntegerVector>) getObjectInstance(permType);

        // Setup the generator.
        String generatorType = 
            argOptions.getStringOption("generator", DEFAULT_GENERATOR);
        IntegerVectorGenerator<IntegerVector> generator =
            (IntegerVectorGenerator<IntegerVector>) getObjectInstance(
                    generatorType);

        // Setup the use of dense vectors.
        boolean useDense = argOptions.hasOption("useDenseSemantics");

        // Setup the generator map.
        if (argOptions.hasOption("loadIndexes")) {
            String savedIndexName = argOptions.getStringOption("loadVectors");
            vectorMap = IntegerVectorMapUtil.load(
                    new File(savedIndexName + ".index"));
            try {
                FileInputStream fis =
                    new FileInputStream(savedIndexName + ".permutation");
                ObjectInputStream inStream = new ObjectInputStream(fis);
                permFunction =
                    (PermutationFunction<IntegerVector>) inStream.readObject();
                inStream.close();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            } catch (ClassNotFoundException cnfe) {
                throw new Error(cnfe);
            }
        } else
            vectorMap = new IntegerVectorGeneratorMap<IntegerVector>(
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
            (OnlineClusteringGenerator) getObjectInstance(clusterName);
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

    protected void postProcessing() {
        if (argOptions.hasOption("saveIndexes")) {
            String filename = argOptions.getStringOption("saveIndexes");
            IntegerVectorMapUtil.save(vectorMap, new File(filename + ".index"));
            try {
                FileOutputStream fos = new FileOutputStream(new File(
                            filename + ".permutation"));
                ObjectOutputStream outStream = new ObjectOutputStream(fos);
                outStream.writeObject(permFunction);
                outStream.close();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }
    }

    public SemanticSpace getSpace() {
        return new FlyingHermit(vectorMap, permFunction, clusterGenerator,
                                replacementMap, acceptedWords, dimension,
                                prevWordsSize, nextWordsSize);
    }

    public Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("replacementMap") &&
            argOptions.hasOption('T'))
            props.setProperty(IteratorFactory.TOKEN_REPLACEMENT_FILE_PROPERTY,
                              argOptions.getStringOption("replacementMap"));
        if (argOptions.hasOption("threads"))
            props.setProperty(FlyingHermit.THREADS_PROPERTY,
                              argOptions.getStringOption("threads"));
        return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
         System.out.println(
                 "usage: java FlyingHermitMain [options] <output-dir>\n" + 
                 argOptions.prettyPrint());
    }

    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);
        
        if (argOptions.numPositionalArgs() == 0) {
            throw new IllegalArgumentException("must specify output directory");
        }

        File outputDir = new File(argOptions.getPositionalArg(0));
        if (!outputDir.isDirectory()){
            throw new IllegalArgumentException(
                "output directory is not a directory: " + outputDir);
        }
        
        verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");
        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (verbose) {
            Logger appRooLogger = Logger.getLogger("edu.ucla.sspace");
            Handler verboseHandler = new ConsoleHandler();
            verboseHandler.setLevel(Level.FINE);
            appRooLogger.addHandler(verboseHandler);
            appRooLogger.setLevel(Level.FINE);
            appRooLogger.setUseParentHandlers(false);
        }

        // all the documents are listed in one file, with one document per line
        Iterator<Document> trainTestIters = getDocumentIterator();
        
        // Check whether this class supports mutlithreading when deciding how
        // many threads to use by default
        int numThreads = (isMultiThreaded)
            ? Runtime.getRuntime().availableProcessors()
            : 1;
        if (argOptions.hasOption("threads")) {
            numThreads = argOptions.getIntOption("threads");
        }

        boolean overwrite = true;
        if (argOptions.hasOption("overwrite")) {
            overwrite = argOptions.getBooleanOption("overwrite");
        }
        
        handleExtraOptions();

        Properties props = setupProperties();

        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (argOptions.hasOption("tokenFilter")) {
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              argOptions.getStringOption("tokenFilter"));
        }

        if (argOptions.hasOption("useStemming"))
            props.setProperty(IteratorFactory.USE_STEMMING_PROPERTY, "");

        if (argOptions.hasOption("compoundWords")) {
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundWords"));
        }
        IteratorFactory.setProperties(props);

        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.

        SemanticSpace space = getSpace(); 
        
        int trainSize = argOptions.getIntOption("trainSize", Integer.MAX_VALUE);
        parseDocumentsMultiThreaded(space, trainTestIters,
                                    numThreads, trainSize);

        long startTime = System.currentTimeMillis();
        space.processSpace(props);
        long endTime = System.currentTimeMillis();
        verbose("processed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        parseDocumentsMultiThreaded(space, trainTestIters,
                                    numThreads, Integer.MAX_VALUE);
        
        startTime = System.currentTimeMillis();
        space.processSpace(props);
        endTime = System.currentTimeMillis();
        verbose("processed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        File output = (overwrite)
            ? new File(outputDir, space.getSpaceName() + EXT)
            : File.createTempFile(space.getSpaceName(), EXT, outputDir);

        SSpaceFormat format = (argOptions.hasOption("outputFormat"))
            ? SSpaceFormat.valueOf(
                argOptions.getStringOption("outputFormat").toUpperCase())
            : getSpaceFormat();

        startTime = System.currentTimeMillis();
        SemanticSpaceIO.save(space, output, format);
        endTime = System.currentTimeMillis();
        verbose("printed space in %.3f seconds",
                ((endTime - startTime) / 1000d));

        postProcessing();
    }

    protected void parseDocumentsMultiThreaded(final SemanticSpace sspace,
                                               final Iterator<Document> docIter,
                                               int numThreads,
                                               final int numDocs)
            throws IOException, InterruptedException {

        Collection<Thread> threads = new LinkedList<Thread>();

        final AtomicInteger count = new AtomicInteger(0);
        
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new Thread() {
                public void run() {
                    // repeatedly try to process documents while some still
                    // remain
                    while (count.get() < numDocs && docIter.hasNext()) {
                        int docNumber = count.incrementAndGet();
                        Document doc = docIter.next();
                        long startTime = System.currentTimeMillis();
                        int terms = 0;
                        try {
                            sspace.processDocument(doc.reader());
                        } catch (Throwable t) {
                            t.printStackTrace();
                        }
                        long endTime = System.currentTimeMillis();
                        verbose("parsed document #%d in %.3f seconds",
                                docNumber, ((endTime - startTime) / 1000d));
                    }
                }
            };
            threads.add(t);
        }

        long threadStart = System.currentTimeMillis();
        
        // start all the threads processing
        for (Thread t : threads)
            t.start();

        verbose("Beginning processing using %d threads", numThreads);

        // wait until all the documents have been parsed
        for (Thread t : threads)
            t.join();

        verbose("parsed %d document in %.3f total seconds)",
                count.get(),
                ((System.currentTimeMillis() - threadStart) / 1000d));
    }
}
