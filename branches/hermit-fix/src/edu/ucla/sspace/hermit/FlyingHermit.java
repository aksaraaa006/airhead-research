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

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.clustering.OnlineClustering;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Generator;
import edu.ucla.sspace.util.GeneratorMap;

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
 * An implementation of the {@code FlyingHermit} Semantic Space model. 
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
 * {@link OnlineClusteringGenerator} defines how contexts are clustered
 * together.
 *
 * </p>
 *
 * This class implements {@link Filterable}, which allows for fine-grained
 * control of which semantics are retained.  The {@link #setSemanticFilter(Set)}
 * method can be used to speficy which words should have their semantics
 * retained.  Note that the words that are filtered out will still be used in
 * computing the semantics of <i>other</i> words.  This behavior is intended for
 * use with a large corpora where retaining the semantics of all words in memory
 * is infeasible.
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
public class FlyingHermit implements SemanticSpace, Filterable {

    /**
     * The base prefix for all {@code FlyingHermit} properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.FlyingHermit";

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
     * The Semantic Space name for FlyingHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "flying-hermit-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger HERMIT_LOGGER =
        Logger.getLogger(FlyingHermit.class.getName());

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
     * A mapping from tokens to conflated terms.  If this is null, it is assumed
     * {@code FlyingHermit} should generate senses for all non empty tokens.  If
     * it is not null, then only tokens which have mappings will have a set of
     * senses generated and stored in the semantic space.
     */
    private Map<String, String> replacementMap;

    /**
     * The set of words to produce semantics for.
     */
    private Set<String> acceptedWords; 

    /**
     * An accuracy map to record the frequency statistics for each word cluster.
     * This is used to label the clusters once they have been formed.
     */
    private AccuracyMap accuracyMap;

    /**
     * The type of clustering used for {@code FlyingHermit}.  This specifies how
     * hermit will merge it's context vectors into different senses.
     */
    private GeneratorMap<OnlineClustering<SparseIntegerVector>> clusterMap;

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
     * A flag specifying if this {@code FlyingHermit} instance has been
     * compacted, and should enter testing mode.
     */
    private boolean compacted;

    /**
     * Create a new instance of {@code FlyingHermit} which takes ownership
     */
    public FlyingHermit(
            Map<String, TernaryVector> indexGeneratorMap,
            PermutationFunction<TernaryVector> permFunction,
            Generator<OnlineClustering<SparseIntegerVector>> clusterGenerator,
            Map<String, String> remap,
            Set<String> accepted,
            int vectorSize,
            int prevWordsSize,
            int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexMap = indexGeneratorMap;
        permFunc = permFunction;
        replacementMap = remap;
        acceptedWords = accepted;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;
        compacted = false;

        accuracyMap = new AccuracyMap();
        clusterMap = new GeneratorMap<OnlineClustering<SparseIntegerVector>>(
                clusterGenerator);
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
        Queue<String> nextOriginals = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
            addNextWord(it, nextWords, nextOriginals);

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            String original = nextOriginals.remove();

            if (it.hasNext())
                addNextWord(it, nextWords, nextOriginals);

            // Only process words which have a suitable replacement.
            if (acceptWord(focusWord, original)) {
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

                // Compare the most recent vector to all the saved vectors.  If
                // the vector with the highest similarity has a similarity over
                // a threshold, incorporate this {@code IntegerVector} to that
                // winner.  Otherwise add this {@code IntegerVector} as a new
                // vector for the term.
                int clusterNum;

                if (!compacted) {
                    OnlineClustering<SparseIntegerVector> clustering =
                        clusterMap.get(focusWord);
                    clusterNum = clustering.addVector(meaning);
                } else
                    clusterNum = assignVector(focusWord, meaning);

                // Count the accuracy of the current cluster assignment for
                // words which have a replacement different from themselves.
                if (!focusWord.equals(original)) {
                    accuracyMap.addInstance(focusWord, clusterNum, original);
                }
            }

            // Push the focus word into previous word set for the next focus
            // word.
            prevWords.offer(focusWord);
            if (prevWords.size() > prevSize)
                prevWords.remove();
        }
    }
    
    /**
     * Returns the sense identifier of {@code focusWord} that is most similar to
     * the provided context.
     */
    private int assignVector(String focusWord, SparseIntegerVector context) {
        int bestSenseIndex = 0;
        double bestSimilarity = Integer.MIN_VALUE;

        // Iterate over the possible sense numbers that can exist for the word.
        for (int i = 0; i < Integer.MAX_VALUE; ++i) {
            // Get the representation for the requested sense.
            String sense = (i == 0) ? focusWord : focusWord + "-" + i;
            SparseIntegerVector centroid = splitSenses.get(sense);

            // No sense exists so return the best found so far.
            if (centroid == null)
                return bestSenseIndex;

            // Compute the similarity between the context and the centroid.  If
            // it's most similar, update the best index found.
            double similarity = Similarity.cosineSimilarity(context, centroid);
            if (similarity > bestSimilarity) {
                bestSimilarity = similarity;
                bestSenseIndex = i;
            }
        }
        // This should never happen.
        return -1;
    }

    /**
     * Extracts the next word from a token iterator and add the token, and it's
     * replacement to the two provided queues.  If the {@code replacementMap}
     * exists, and the map contains a replacement mapping for the next token, it
     * will store that token in {@code nextOriginals}.  If the map exists and
     * no replacement is found the empty token will be stored in {@code
     * nextOriginals}.  Lastly, if the map does not exist, the current token
     * will also be stored in {@code nextOriginals}
     *
     * @param it The token iterator to extract a token from.
     * @param nextWords The {@code Queue} which contains the set of words to the
     *                  right of the focus word.
     * @param nextOriginals The {@code Queue} which contains replacement
     *                         tokens for the set of words to the right of 
     *                         the focus word.
     */
    private void addNextWord(Iterator<String> it,
                             Queue<String> nextWords,
                             Queue<String> nextOriginals) {
        String term = it.next();
        String replacement = term;
        if (replacementMap != null) {
            replacement = replacementMap.get(term);
            replacement = (replacement != null) ? replacement : term;
        }

        nextWords.offer(replacement.intern());
        nextOriginals.offer(term.intern());
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        Set<String> terms = new TreeSet<String>(clusterMap.keySet());
        if (!compacted) {
            double mergeThreshold = Double.parseDouble(
                properties.getProperty(MERGE_THRESHOLD_PROPERTY, ".25"));

            splitSenses = new ConcurrentHashMap<String, SparseIntegerVector>();

            // Merge the clusters for each of the words being tracked.
            for (String term : terms) {
                // Extract the set of clusters from the map for the term.
                OnlineClustering<SparseIntegerVector> clustering =
                    clusterMap.get(term);
                List<SparseIntegerVector> centroids = clustering.getCentroids();
                int[] centroidSizes = new int[centroids.size()];
                for (int i = 0; i < centroids.size(); ++i)
                    centroidSizes[i] = clustering.getCentroidSize(i);

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
                    mergeCentroids(term, centroids, centroidSizes, assignments);

                // Save each of the new centroids into the word space map.
                int i = 0;
                for (SparseIntegerVector centroid : newCentroids) {
                    String sense = (i == 0) ? term : term + "-" + i;
                    splitSenses.put(sense, centroid);
                    i++;
                }

                clusterMap.remove(term);
                accuracyMap.setClusterNames(term);
            }

            // Reset the accuracy map.
            Map<String, String> clusterNames = accuracyMap.clusterTitleMap;
            accuracyMap = new AccuracyMap();
            accuracyMap.clusterTitleMap = clusterNames;

            HERMIT_LOGGER.info("Split into " + splitSenses.size() + " terms.");

            compacted = true;
        } else {
            HERMIT_LOGGER.info("Emitting Accuracy Counts");
            accuracyMap.printCounts();

            // Reset the accuracy map.
            Map<String, String> clusterNames = accuracyMap.clusterTitleMap;
            accuracyMap = new AccuracyMap();
            accuracyMap.clusterTitleMap = clusterNames;
        }
    }

    /**
     * Merges the centroids into new centroids based on the assignments
     * provided.
     *
     * @param term them the centroids correspond to
     * @param centroids the original set of centroids to merge
     * @param centroidSizes the size of each original centroid
     * @param assignments the clustering assignment of each original centroid
     *
     * @return an array of new centroids that are the sum of each original
     *         centroid in a merged cluster.
     */
    private SparseIntegerVector[] mergeCentroids(
            String term,
            List<SparseIntegerVector> centroids,
            int[] centroidSizes,
            int[] assignments) {
        // Compute the number of new custers.
        int numAssignments = 0;
        for (int assignment : assignments)
            if (assignment >= numAssignments)
                numAssignments = assignment+1;

        SparseIntegerVector[] newCentroids =
            new SparseIntegerVector[numAssignments];
        int[] newCentroidSizes = new int[numAssignments];
        int[] earliestIndex = new int[numAssignments];

        // Sum each original centroid into the new centroid. In addition,
        // compute the new size of each centroid and move the values in the
        // {@code assignmentMap} to correspond with how the original clusters
        // were merged.
        for (int i = 0; i < centroids.size(); ++i) {
            int assignment = assignments[i];
            SparseIntegerVector oldCentroid = centroids.get(i);

            // Set up the first instance in the new centroid.
            if (newCentroids[assignment] == null) {
                earliestIndex[assignment] = i;
                newCentroids[assignment] =
                    new CompactSparseIntegerVector(indexVectorSize);
            } else
                // Move instance counts from the old cluster to the index of
                // the first cluster moved to the new assignment.
                accuracyMap.moveInstances(term, i, earliestIndex[assignment]);

            newCentroidSizes[assignment] += centroidSizes[i];

            // Merge the old centroid into the new one.
            VectorMath.add(newCentroids[assignment], oldCentroid);
        }
        return newCentroids;
    }

    /**
     * {@inheritDoc}
     */
    public void setSemanticFilter(Set<String> wordsToProcess) {
        if (acceptedWords == null)
            acceptedWords = new HashSet<String>();

        acceptedWords.clear();
        acceptedWords.addAll(wordsToProcess);
    }

    /**
     * Returns true if the word should be processed.
     */
    private boolean acceptWord(String focusWord, String original) {
        if (acceptedWords != null && replacementMap != null)
            return !focusWord.equals(original) &&
                   acceptedWords.contains(original);
        if (acceptedWords == null && replacementMap != null)
            return !focusWord.equals(original);
        if (acceptedWords == null)
            return !focusWord.equals(EMPTY_TOKEN);
        return acceptedWords.contains(focusWord);
    }

    /**
     * A simple map for counting the accuracy of a sample word that is being  
     * tracked with {@code FlyingHermit}.
     */
    private static class AccuracyMap {

        /**
         * A mapping from conflated senses to a list of infered senses.  Each  
         * infered sense has a mapping from original terms to the number of
         * times the instance of that word was mapped to the given sense of this
         * conflated term.
         */
        private Map<String, List<Map<String, Integer>>> wordMap;

        /**
         * A mapping from senses to titles
         */
        private Map<String, String> clusterTitleMap;

        /**
         * Creates an emtpy {@code AccuracyMap}
         */
        public AccuracyMap() {
            wordMap = new HashMap<String, List<Map<String, Integer>>>();
            clusterTitleMap = new HashMap<String, String>();;
        }

        /**
         * Add a new instance of a conflated sense being mapped to a given sense
         * when the original term was {@code originalSense}.
         */
        public void addInstance(String conflatedTerm,
                                Integer senseNum,
                                String originalSense) {
            // Get the list of senses known for this conflated term.
            List<Map<String, Integer>> senseCounts = null;
            synchronized (wordMap) {
                senseCounts = wordMap.get(conflatedTerm);
                if (senseCounts == null) {
                    senseCounts = new ArrayList<Map<String, Integer>>();
                    wordMap.put(conflatedTerm, senseCounts);
                }
            }

            // Get the mapping for the given sense number.
            Map<String, Integer> originalCounts = null;
            synchronized (senseCounts) {
                for (int i = senseCounts.size(); i <= senseNum; ++i)
                    senseCounts.add(new HashMap<String, Integer>());
                originalCounts = senseCounts.get(senseNum);
            }

            // Increment the count of how many times the original sense was
            // mapped to the given sense number of the conflated term.
            synchronized (originalCounts) {
                Integer termCounts = originalCounts.get(originalSense);
                originalCounts.put(
                        originalSense,
                        (termCounts == null) ? 1 : termCounts.intValue() + 1);
            }
        }

        /**
         * Moves the occurances counts of a conflated term's sense to another
         * sense.  This is primarily useful only when senses become merged
         * together as part of a clustering step.
         *
         * @param conflatedTerm the conflated term whose counts should be
         *                      shuffled.
         * @param oldSenseNum counts from this sense will be added to counts for
         *                    newSenseNum
         * @param newSenseNum the new sense which will have the counts found in
         *                    oldSenseNum
         */
        public void moveInstances(String conflatedTerm,
                                  Integer oldSenseNum,
                                  Integer newSenseNum) {
            // Get the list of senses for the conflated term.
            List<Map<String, Integer>> senseCounts = null;
            senseCounts = wordMap.get(conflatedTerm);
            if (senseCounts == null)
                return;

            // Get the two maps for the required senses.
            Map<String, Integer> oldCounts = null;
            Map<String, Integer> newCounts = null;
            oldCounts = senseCounts.get(oldSenseNum);

            // Mapping to a negative sense number corresponds to deleting the
            // sense.
            if (newSenseNum < 0) {
                oldCounts.clear();
                return;
            }

            newCounts = senseCounts.get(newSenseNum);

            // Move the sense counts of the old sense to thew new sense.
            for (Map.Entry<String, Integer> entry : oldCounts.entrySet()) {
                Integer newCount = newCounts.get(entry.getKey());
                Integer oldCount = entry.getValue();
                newCounts.put(entry.getKey(),
                              (newCount == null)
                              ? oldCount
                              : oldCount.intValue() + newCount.intValue());
            }

            // Remove any counts from the old sense.
            oldCounts.clear();
        }

        /**
         * Print out all the occurrance counts stored in this {@code
         * AccuracyMap}.  Output will be in the format of 
         *   conflatedTerm-senseNum-originalTerm|count
         * where conflatedTerm is a conflated term of interest, senseNum is one
         * of the senses infered, originalTerm is the real word encountered for
         * a particular instance of conflatedTerm, and count is the number of
         * times originalTerm occured and was mapped to senseNum.
         */
        public void printCounts() {
            for (Map.Entry<String, List<Map<String, Integer>>> entry :
                    wordMap.entrySet()) {
                String conflated = entry.getKey();
                int i = 0;
                for (Map<String, Integer> originalMap : entry.getValue()) {
                    if (originalMap.isEmpty()) 
                        continue;

                    String title = clusterTitleMap.get(conflated + ":" + i);
                    System.out.println("# " + conflated + " " + i + 
                                       " " + title);

                    for (Map.Entry<String, Integer> e :
                            originalMap.entrySet()) {
                        String original = e.getKey();
                        StringBuilder sb = new StringBuilder();
                        sb.append(conflated).append("-");
                        sb.append(i).append("-");
                        sb.append(original).append("|");
                        sb.append(e.getValue());
                        System.out.println(sb.toString());
                    }
                    ++i;
                }
            }
        }

        /**
         * Determines the name, i.e the original sense, for each cluster stored
         * for {@code term} based on the most frequently occuring original sense
         * in the cluster.
         *
         * @param term The term to label.
         */
        public void setClusterNames(String term) {
            List<Map<String, Integer>> senseCounts = wordMap.get(term);
            if (senseCounts == null)
                return;

            int i = 0;
            for (Map<String, Integer> senseCount : senseCounts) {
                if (senseCount.isEmpty())
                    continue;
                int maxCount = -1;
                String bestTitle = null;
                for (Map.Entry<String, Integer> e : senseCount.entrySet()) {
                    if (e.getValue() > maxCount) {
                        maxCount = e.getValue();
                        bestTitle = e.getKey();
                    }
                }
                clusterTitleMap.put(term + ":" + i, bestTitle);
                ++i;
            }
        }
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
