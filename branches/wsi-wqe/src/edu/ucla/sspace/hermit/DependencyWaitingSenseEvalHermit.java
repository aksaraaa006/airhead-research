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

package edu.ucla.sspace.hermit;

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.ClusterUtil;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyPermutationFunction;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Misc;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseHashIntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrices;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.logging.Logger;


/**
 * An implementation of the Hermit WSI Semantic Space model that is specificly
 * geared towards processing the SenseEval07 dataset.  Each document is expected
 * to be the context of a single instance of a test term, with the focus word
 * preceeded by a "||||".  This class has several special cases and is not the
 * general purpose hermit implementation.  See {@link FlyingHermit} for a more
 * general implementation. 
 *
 * </p>
 *
 * This {@code SemanticSpace} is an extension of {@link RandomIndexing} which
 * attempts to infer multiple senses of a word by clustering first order
 * contexts encountered in a corpus.  Each context is simply the summation of
 * index vectors of co-occurring words in a small sliding window.  These
 * contexts are then clustered together to find instances which are similar to
 * each other, and can thus define a particular sense of a word.
 *
 * </p>
 *
 * This implementation relies heavily on a {@link IntegerVectorGenerator} and a
 * {@link GeneratorMap} for it's functionaltiy.  The {@link
 * IntegerVectorGenerator} provided defines how index vectors are created.  The
 * {@link OnlineClustering} defines how contexts are clustered together.
 *
 * </p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  <p>
 *
 * @author Keith Stevens
 */
public class DependencyWaitingSenseEvalHermit implements SemanticSpace {

    /**
     * The base prefix for all {@code DependencyWaitingSenseEvalHermit}
     * properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.DependencyWaitingSenseEvalHermit";

    public static final String CLUSTERING_PROPERTY = 
        PROPERTY_PREFIX + ".offlineClustering";

    public static final String NUM_CLUSTERS_PROPERTY = 
        PROPERTY_PREFIX + ".numClusters";

    public static final String DEFAULT_CLUSTERING =
        "edu.ucla.sspace.clustering.SpectralClustering";

    public static final String DEFAULT_NUM_CLUSTERS = "15";

    /**
     * An empty token representing a lack of a valid replacement mapping.
     */
    public static final String EMPTY_TOKEN = "";

    /**
     * The Semantic Space name for DependencyWaitingSenseEvalHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "senseEval-hermit-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = Logger.getLogger(
            DependencyWaitingSenseEvalHermit.class.getName());

    /**
     * A mapping from strings to {@code IntegerVector}s which represent an index
     * vector.
     */
    private final Map<String, TernaryVector> indexMap;

    /**
     * The {@code PermutationFunction} to use for co-occurrances.
     */
    private final DependencyPermutationFunction<TernaryVector> permFunc;

    /**
     * A mapping from a term sense to it's semantic representation.  This
     * differs from {@code TermHolographs} in that it is index by keys of the
     * form "term-senseNum", and map directly to only one of the term's
     * representations.  This {@code Map} is used after {@code processSpace} is
     * called.
     */
    private ConcurrentMap<String, SparseDoubleVector> splitSenses;

    private ConcurrentMap<String, List<SparseDoubleVector>> termContexts;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorSize;

    private final DependencyExtractor parser;
    private final DependencyPathAcceptor acceptor;
    private final DependencyPathWeight weighter;

    private final int pathLength;

    /**
     * Create a new instance of {@code DependencyWaitingSenseEvalHermit}
     * which takes ownership
     */
    public DependencyWaitingSenseEvalHermit(
            Map<String, TernaryVector> indexGeneratorMap,
            DependencyPermutationFunction<TernaryVector> permFunction,
            DependencyExtractor parser,
            DependencyPathAcceptor acceptor,
            DependencyPathWeight weighter,
            int vectorSize,
            int pathLength) {
        indexVectorSize = vectorSize;
        indexMap = indexGeneratorMap;
        permFunc = permFunction;
        this.parser = parser;
        this.pathLength = pathLength;
        this.acceptor = acceptor;
        this.weighter = weighter;

        splitSenses = new ConcurrentHashMap<String, SparseDoubleVector>();
        termContexts =
            new ConcurrentHashMap<String, List<SparseDoubleVector>>();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(splitSenses.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        return Vectors.immutable(splitSenses.get(term));
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return FLYING_HERMIT_SSPACE_NAME + "-" + indexVectorSize;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return indexVectorSize;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        String instanceId = document.readLine();
        String instanceWord = document.readLine();

        for (DependencyTreeNode[] nodes = null;
                (nodes = parser.parse(document)) != null; ) {

            // Skip empty documents.
            if (nodes.length == 0)
                continue;

            for (int i = 0; i < nodes.length; ++i) {
                String focusWord = nodes[i].word();

                // Skip paths for words that are not anchored on the instance's
                // word.
                if (!focusWord.equals(instanceWord))
                    continue;

                SparseIntegerVector meaning = 
                    new SparseHashIntegerVector(indexVectorSize);

                Iterator<DependencyPath> pathIter = new DependencyIterator(
                        nodes, acceptor, weighter, i, pathLength);

                while (pathIter.hasNext()) {
                    LinkedList<DependencyRelation> path =
                        pathIter.next().path();
                    TernaryVector termVector = 
                        indexMap.get(path.peekLast().token());
                    if (permFunc != null)
                        termVector = permFunc.permute(termVector, path);
                    add(meaning, termVector);
                }

                getTermContext(focusWord).add(Vectors.asDouble(meaning));
            }
        }
    }

    private List<SparseDoubleVector> getTermContext(String term) {
        List<SparseDoubleVector> termContextList = termContexts.get(term);
        if (termContextList == null) {
            synchronized (termContexts) {
                termContextList = termContexts.get(term);
                if (termContextList == null) {
                    termContextList = Collections.synchronizedList(
                            new ArrayList<SparseDoubleVector>());
                    termContexts.put(term, termContextList);
                }
            }
        }
        return termContextList;
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(final Properties properties) {
        // Get the number of clusters to be used.
        final int numClusters = Integer.parseInt(properties.getProperty(
                    NUM_CLUSTERS_PROPERTY, DEFAULT_NUM_CLUSTERS));
        // Get the clustering algorithm to use.
        final Clustering clustering = (Clustering) Misc.getObjectInstance(
                properties.getProperty(CLUSTERING_PROPERTY,
                                       DEFAULT_CLUSTERING));

        // Set up the concurrent data structures so we can process the documents
        // concurrently
        final BlockingQueue<Runnable> workQueue =
            new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i) {
            Thread t = new WorkerThread(workQueue);
            t.start();
        }
        final Semaphore termsProcessed = new Semaphore(0); 
        final int numTerms = termContexts.size();

        // Process each word's context set in a worker thread.
        for (Map.Entry<String, List<SparseDoubleVector>> entry :
                termContexts.entrySet()) {
            // Get the root word being discriminated and list of observed
            // contexts.
            final String senseName = entry.getKey();
            final List<SparseDoubleVector> contextSet = entry.getValue();

            workQueue.offer(new Runnable() {
                public void run() {
                    try {
                        clusterTerm(senseName, contextSet, 
                                    clustering, numClusters, properties);
                    } catch (Exception e) {
                        e.printStackTrace();
                    } finally {
                        termsProcessed.release();
                    }
                }
            });
        }
        try {
            termsProcessed.acquire(numTerms);
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }
        LOGGER.info("Finished processing all terms");
    }

    private void clusterTerm(String senseName,
                             List<SparseDoubleVector> contextSet,
                             Clustering clustering,
                             int numClusters,
                             Properties properties) {
        SparseMatrix contexts = Matrices.asSparseMatrix(contextSet);
        // First cluster the context set as a sparse matrix.
        LOGGER.info("Clustering term: " + senseName);
        Assignment[] assignments = clustering.cluster(
                contexts, numClusters, properties);
        LOGGER.info("Finished clustering term: " + senseName);

        // Create the centroids based on the assignments made.
        LOGGER.info("Creating centroids for term: " + senseName);
        List<SparseDoubleVector> centroids = 
            ClusterUtil.generateCentroids(
                contextSet, assignments, indexVectorSize);

        // Add the centroids to the splitSenses map.
        for (int index = 0; index < centroids.size(); ++index) {
            String sense = (index > 0)
                ? senseName + "-" + index
                : senseName;
            splitSenses.put(sense, centroids.get(index));
        }

        LOGGER.info("Finished creating centroids for term: " + senseName);
    }

    /**
     * Adds a {@link TernaryVector} to a {@link IntegerVector}
     */
    private void add(IntegerVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }
}
