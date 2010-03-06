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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.SparseMatrix;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;
import edu.ucla.sspace.util.Misc;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseHashIntegerVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.concurrent.atomic.AtomicInteger;

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
public class WaitingSenseEvalHermit implements SemanticSpace {

    /**
     * The base prefix for all {@code SenseEvalFlyingHermit} properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.SenseEvalFlyingHermit";

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
     * The Semantic Space name for SenseEvalFlyingHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "senseEval-waiting-hermit-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger HERMIT_LOGGER =
        Logger.getLogger(SenseEvalFlyingHermit.class.getName());

    /**
     * A mapping from strings to {@code IntegerVector}s which represent an index
     * vector.
     */
    private final Map<String, TernaryVector> indexMap;

    /**
     * The {@code PermutationFunction} to use for co-occurrances.
     */
    private final PermutationFunction<TernaryVector> permFunc;

    /**
     * A mapping from a term sense to it's semantic representation.  This
     * differs from {@code TermHolographs} in that it is index by keys of the
     * form "term-senseNum", and map directly to only one of the term's
     * representations.  This {@code Map} is used after {@code processSpace} is
     * called.
     */
    private final ConcurrentMap<String, SparseDoubleVector> splitSenses;

    private ConcurrentMap<String, List<SparseDoubleVector>> termContexts;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorSize;

    /**
     * The number of words in the context to save prior to the focus word.
     */
    private final int prevSize;

    /**
     * The number of words in the context to save after the focus word.
     */
    private final int nextSize;

    /**
     * Create a new instance of {@code SenseEvalFlyingHermit} which takes
     * ownership
     */
    public WaitingSenseEvalHermit(
            Map<String, TernaryVector> indexGeneratorMap,
            PermutationFunction<TernaryVector> permFunction,
            int vectorSize,
            int prevWordsSize,
            int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexMap = indexGeneratorMap;
        permFunc = permFunction;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;
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
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        // Skip empty documents.
        if (!it.hasNext())
            return;

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; it.hasNext(); ++i) {
            String term = it.next();
            if (term.equals("||||"))
                break;
            prevWords.offer(term.intern());
        }

        // Eliminate the first set of words that we don't want to inspect.
        while (prevWords.size() > prevSize)
            prevWords.remove();

        String focusWord = it.next().intern();

        // Extract the set of words to consider after the focus word.
        while (it.hasNext() && nextWords.size() < nextSize)
            nextWords.offer(it.next().intern());

        // Incorporate the context into the semantic vector for the
        // focus word.  If the focus word has no semantic vector yet,
        // create a new one, as determined by the index builder.
        SparseIntegerVector meaning = 
            new SparseHashIntegerVector(indexVectorSize);

        // Process the previous words, specifying their distance from
        // the focus word.
        int distance = -1 * prevWords.size();
        for (String term : prevWords) {
            if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                TernaryVector termVector = indexMap.get(term);
                if (permFunc != null)
                    termVector = permFunc.permute(termVector, distance);
                add(meaning, termVector);
            }
            ++distance;
        }

        distance = 1;

        // Process the next words, specifying the distance from the
        // focus word.
        for (String term : nextWords) {
            if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                TernaryVector termVector = indexMap.get(term);
                if (permFunc != null)
                    termVector = permFunc.permute(termVector, distance);
                add(meaning, termVector);
            }
            ++distance;
        }

        getTermContext(focusWord).add(Vectors.asDouble(meaning));
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
            final SparseMatrix contexts = Matrices.asSparseMatrix(contextSet);

            workQueue.offer(new Runnable() {
                    public void run() {
                        try {
                        // First cluster the context set as a sparse matrix.
                        HERMIT_LOGGER.info("Clustering term: " + senseName);
                        Assignment[] assignments = clustering.cluster(
                                contexts, numClusters, properties);
                        HERMIT_LOGGER.info("Finished clustering term: " + 
                                           senseName);

                        // Create the centroids based on the assignments made.
                        HERMIT_LOGGER.info("Creating centroids for term: " +
                                           senseName);
                        ArrayList<SparseDoubleVector> centroids = 
                            new ArrayList<SparseDoubleVector>();
                        int index = -1;
                        for (SparseDoubleVector context : contextSet) {
                            index++;
                            int[] itemAssignments =
                                assignments[index].assignments();

                            // Skip items that were not assigned to any
                            // cluster.
                            if (itemAssignments.length == 0)
                                continue;

                            int assignment = itemAssignments[0];

                            // Ensure that the list of centroids has at least an
                            // empty vector for itself.
                            for (int i = centroids.size(); i <= assignment; ++i)
                                centroids.add(
                                    new SparseHashDoubleVector(indexVectorSize));
                            // Add the context to the assigned cluster.
                            VectorMath.add(centroids.get(assignment), context);
                        }
                        HERMIT_LOGGER.info(
                                "Finished creating centroids for term: " +
                                senseName);

                        // Add the centroids to the splitSenses map.
                        for (index = 0; index < centroids.size(); ++index) {
                            String sense = (index > 0)
                                ? senseName + "-" + index
                                : senseName;
                            splitSenses.put(senseName, centroids.get(index));
                        }
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
        HERMIT_LOGGER.info("Finished processing all terms");
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
     * Adds a {@link TernaryVector} to a {@link IntegerVector}
     */
    private void add(IntegerVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }
}
