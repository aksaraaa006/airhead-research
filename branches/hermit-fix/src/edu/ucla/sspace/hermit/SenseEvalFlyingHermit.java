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

import edu.ucla.sspace.clustering.ClusterMap;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.clustering.OnlineClustering;
import edu.ucla.sspace.clustering.OnlineClusteringGenerator;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseHashIntegerVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Matrices;

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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;


/**
 * An implementation of the Hermit WSI Semantic Space model that is specificly
 * geared towards processing the SenseEval07 dataset.  Each document is expected
 * to be the context of a single instance of a test term, with the focus word
 * preceeded by a "|".  This class has several special cases and is not the
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
 * {@link ClusterMap} for it's functionaltiy.  The {@link
 * IntegerVectorGenerator} provided defines how index vectors are created.  The
 * {@link ClusterMap} defines how contexts are clustered together.
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
public class SenseEvalFlyingHermit implements SemanticSpace {

    /**
     * The base prefix for all {@code SenseEvalFlyingHermit} properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.SenseEvalFlyingHermit";

    /**
     * The property for specifying the threshold for merging clusters.
     */
    public static final String MERGE_THRESHOLD_PROPERTY = 
        PROPERTY_PREFIX + ".mergeThreshold";

    /**
     * An empty token representing a lack of a valid replacement mapping.
     */
    public static final String EMPTY_TOKEN = "";

    /**
     * The Semantic Space name for SenseEvalFlyingHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "senseEval-hermit-semantic-space";

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
    private ConcurrentMap<String, SparseIntegerVector> splitSenses;

    /**
     * The type of clustering used for {@code SenseEvalFlyingHermit}.  This
     * specifies how hermit will merge it's context vectors into different
     * senses.
     */
    private ClusterMap<SparseIntegerVector> clusterMap;

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
    public SenseEvalFlyingHermit(
            Map<String, TernaryVector> indexGeneratorMap,
            PermutationFunction<TernaryVector> permFunction,
            OnlineClusteringGenerator<SparseIntegerVector> clusterGenerator,
            int vectorSize,
            int prevWordsSize,
            int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexMap = indexGeneratorMap;
        permFunc = permFunction;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;

        clusterMap = new ClusterMap<SparseIntegerVector>(clusterGenerator);
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
        String permName = (permFunc == null) ? "" : permFunc.toString();
        return FLYING_HERMIT_SSPACE_NAME + "-" + indexVectorSize + 
               "-w" + prevSize + "_" + nextSize + "-" + permName +
               "-" + clusterMap.toString();
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

        // Get the instance id as the first token.
        String instanceId = it.next();

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

        // Add the current context vector to the cluster for the focusWord that
        // is most similar.
        clusterMap.addVector(focusWord, meaning);
    }
    
    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        Set<String> terms = new TreeSet<String>(clusterMap.keySet());
        double mergeThreshold = Double.parseDouble(
            properties.getProperty(MERGE_THRESHOLD_PROPERTY, ".25"));

        splitSenses = new ConcurrentHashMap<String, SparseIntegerVector>();

        // Merge the clusters for each of the words being tracked.
        for (String term : terms) {
            // Extract the set of clusters from the map for the term.
            OnlineClustering<SparseIntegerVector> clustering =
                clusterMap.getClustering(term);
            List<SparseIntegerVector> centroids = clustering.getCentroids();

            // Convert the centroids to double vectors so they can be turned
            // into a matrix.
            List<SparseDoubleVector> centroidDoubles =
                new ArrayList<SparseDoubleVector>(centroids.size());
            for (SparseIntegerVector centroid : centroids)
                centroidDoubles.add(Vectors.asDouble(centroid));

            // Cluster the centroids using HAC with the average link
            // critera.
            int[] assignments =
                HierarchicalAgglomerativeClustering.clusterRows(
                        Matrices.asSparseMatrix(centroidDoubles), 
                        mergeThreshold,
                        ClusterLinkage.MEAN_LINKAGE,
                        SimType.COSINE);

            SparseIntegerVector[] newCentroids =
                mergeCentroids(term, centroids, assignments);

            // Save each of the new centroids into the word space map.
            int i = 0;
            for (SparseIntegerVector centroid : newCentroids) {
                String sense = (i == 0) ? term : term + "-" + i;
                splitSenses.put(sense, centroid);
                i++;
            }
            HERMIT_LOGGER.info("term " + term + " has " + newCentroids.length +
                    " senses.");

            clusterMap.removeClusters(term);
        }

        HERMIT_LOGGER.info("Split into " + splitSenses.size() + " terms.");
    }

    /**
     * Merges the centroids into new centroids based on the assignments
     * provided.
     *
     * @param term them the centroids correspond to
     * @param centroids the original set of centroids to merge
     * @param assignments the clustering assignment of each original centroid
     *
     * @return an array of new centroids that are the sum of each original
     *         centroid in a merged cluster.
     */
    private SparseIntegerVector[] mergeCentroids(
            String term,
            List<SparseIntegerVector> centroids,
            int[] assignments) {
        // Compute the number of new custers.
        int numAssignments = 0;
        for (int assignment : assignments)
            if (assignment >= numAssignments)
                numAssignments = assignment+1;

        SparseIntegerVector[] newCentroids =
            new SparseIntegerVector[numAssignments];

        // Sum each original centroid into the new centroid.
        for (int i = 0; i < centroids.size(); ++i) {
            int assignment = assignments[i];
            SparseIntegerVector oldCentroid = centroids.get(i);

            if (newCentroids[assignment] == null)
                newCentroids[assignment] =
                    new CompactSparseIntegerVector(indexVectorSize);

            // Merge the old centroid into the new one.
            VectorMath.add(newCentroids[assignment], oldCentroid);
        }
        return newCentroids;
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
