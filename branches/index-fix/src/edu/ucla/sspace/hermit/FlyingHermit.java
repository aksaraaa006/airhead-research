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

import edu.ucla.sspace.cluster.BottomUpVectorClusterMap;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.index.IndexGenerator;
import edu.ucla.sspace.index.IndexUser;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.matrix.Matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Collections;
import java.util.HashMap;
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

import java.util.logging.Level;
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
 * This implementation relies heavily on a {@link IndexGenerator}, a {@link
 * IndexUser}, and a {@link ClusterMap} for it's functionaltiy.  The {@link
 * IndexGenerator} provided defines how index vectors are created.  The {@link
 * IndexUser} defines how index vectors are combined together to represent the
 * context.  The {@link ClusterMap} defines how contexts are clustered together.
 *
 * @see BeagleIndexGenerator
 * @see BeagleIndexUser
 *
 * @see RandomIndexGenerator
 * @see RandomIndexUser
 *
 * @author Keith Stevens
 */
public class FlyingHermit implements BottomUpHermit, SemanticSpace {

    /**
     * The full context size used when scanning the corpus. This is the
     * total number of words considered in the context.
     */
    public static final int CONTEXT_SIZE = 6;

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
     * The class responsible for creating index vectors.
     */
    private final IndexGenerator indexGenerator;

    /**
     * The class responsible for combining index vectors.
     */
    private final Class indexUserClazz;

    /**
     * A fixed String describing the {@code IndexUser} that {@code FlyingHermit}
     * uses.
     */
    private final String indexUserDescription;

    /**
     * A mapping from a term sense to it's semantic representation.  This
     * differs from {@code TermHolographs} in that it is index by keys of the
     * form "term-senseNum", and map directly to only one of the term's
     * representations.  This {@code Map} is used after {@code processSpace} is
     * called.
     */
    private ConcurrentMap<String, Vector> splitSenses;

    /**
     * A mapping from tokens to conflated terms.  If this is null, it is assumed
     * {@code FlyingHermit} should generate senses for all non empty tokens.  If
     * it is not null, then only tokens which have mappings will have a set of
     * senses generated and stored in the semantic space.
     */
    private Map<String, String> replacementMap;

    //private ConcurrentMap<String, AtomicInteger> accuracyMap;
    private AccuracyMap accuracyMap;

    /**
     * The type of clustering used for {@code FlyingHermit}.  This specifies how
     * hermit will merge it's context vectors into different senses.
     */
    private BottomUpVectorClusterMap clusterMap;

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
     * Create a new instance of {@code FlyingHermit} which takes ownership
     */
    public FlyingHermit(IndexGenerator generator,
                        Class userClazz,
                        BottomUpVectorClusterMap cluster,
                        Map<String, String> remap,
                        int vectorSize,
                        int prevWordsSize,
                        int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexGenerator = generator;
        indexUserClazz = userClazz;
        clusterMap = cluster;
        replacementMap = remap;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;

        accuracyMap = new AccuracyMap();

        try {
            IndexUser indexUser = (IndexUser) indexUserClazz.newInstance();
            indexUserDescription = indexUser.toString();
        } catch (Exception ie) {
            throw new Error(ie);
        }
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
        return Vectors.immutableVector(splitSenses.get(term));
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return FLYING_HERMIT_SSPACE_NAME + "-" + indexVectorSize + 
               "-w" + prevSize + "_" + nextSize +
               "-" + indexUserDescription.toString() +
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
        Queue<String> nextReplacements = new ArrayDeque<String>();

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        IndexUser indexUser = null;
        try {
            indexUser = (IndexUser) indexUserClazz.newInstance();
        } catch (Exception ie) {
            throw new Error(ie);
        }

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
            addNextWord(it, nextWords, nextReplacements);

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            String replacement = nextReplacements.remove();

            // Ensure that the index vectors exists for all interesting words in
            // the corpus.
            indexGenerator.getIndexVector(focusWord);

            if (it.hasNext())
                addNextWord(it, nextWords, nextReplacements);

            if (!replacement.equals(EMPTY_TOKEN)) {
                // Incorporate the context into the semantic vector for the
                // focus word.  If the focus word has no semantic vector yet,
                // create a new one, as determined by the index builder.
                Vector meaning = indexUser.getEmptyVector();

                // Process the previous words, specifying their distance from
                // the focus word.
                int distance = -1 * prevWords.size();
                for (String term : prevWords) {
                    if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                        Vector termVector = indexGenerator.getIndexVector(term);
                        indexUser.generateMeaning(meaning, termVector,
                                                  distance);
                    }
                    ++distance;
                }

                distance = 1;

                // Process the next words, specifying their distance from the
                // focus word.
                for (String term : nextWords) {
                    if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                        Vector termVector = indexGenerator.getIndexVector(term);
                        indexUser.generateMeaning(meaning, termVector,
                                                  distance);
                    }
                    ++distance;
                }


                // Compare the most recent vector to all the saved vectors.  If
                // the vector with the highest similarity has a similarity over
                // a threshold, incorporate this {@code Vector} to that winner.
                // Otherwise add this {@code Vector} as a new vector for the
                // term.
                int clusterNum = clusterMap.addVector(replacement, meaning);

                // Count the accuracy of the current cluster assignment for
                // words which have a replacement different from themselves.
                if (!focusWord.equals(replacement)) {
                    accuracyMap.addInstance(replacement, clusterNum, focusWord);
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
     * Extracts the next word from a token iterator and add the token, and it's
     * replacement to the two provided queues.
     * If the {@code replacementMap} exists, and the map contains
     * a replacement mapping for the next token, it will store that token in
     * {@code nextReplacements}.  If the map exists and no replacement is found
     * the empty token will be stored in {@code nextReplacements}.  Lastly, if
     * the map does not exist, the current token will also be stored in {@code
     * nextReplacements}
     *
     * @param it The token iterator to extract a token from.
     * @param nextWords The {@code Queue} which contains the set of words to the
     *                  right of the focus word.
     * @param nextReplacements The {@code Queue} which contains replacement
     *                         tokens for the set of words to the right of 
     *                         the focus word.
     */
    private void addNextWord(Iterator<String> it,
                             Queue<String> nextWords,
                             Queue<String> nextReplacements) {
        String term = it.next();
        String replacement = term;
        if (replacementMap != null) {
            replacement = replacementMap.get(term);
            replacement = (replacement != null) ? replacement : EMPTY_TOKEN;
        } 

        nextWords.offer(term.intern());
        nextReplacements.offer(replacement.intern());
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        double minPercentage = Double.parseDouble(
            properties.getProperty(BottomUpHermit.DROP_PERCENTAGE, ".25"));

        splitSenses = new ConcurrentHashMap<String, Vector>();
        Set<String> terms = new TreeSet<String>(clusterMap.keySet());

        printPairWiseSimilarities(terms);

        // Merge the clusters for each of the words being tracked.
        for (String term : terms) {
            Map<Integer, Integer> mergedMap =
                clusterMap.mergeOrDropClusters(term, minPercentage);
            for (Map.Entry<Integer, Integer> mapping : mergedMap.entrySet()) {
                accuracyMap.moveInstances(term, 
                                          mapping.getKey(),
                                          mapping.getValue());
            }
        }

        printPairWiseSimilarities(terms);

        // Extract the list of clusters for each word mapped in the cluster map
        // and save it in the semantic space map.  The first sense will simply
        // be stored as the mapped term and additional senses will have
        // -SENSE_NUM appended to the token.  After this is done, the clusters
        // will be removed from the map to clear up space.
        for (String term : terms) {
            List<List<Vector>> clusters = clusterMap.getClusters(term);
            int i = 0;
            for (List<Vector> cluster : clusters) {
                Vector sense = null;
                for (Vector v : cluster) {
                    if (sense == null)
                        sense = Vectors.copyOf(v);
                    else
                        Vectors.add(sense, v);
                }
                String senseName = term;
                if (i != 0)
                    senseName += "-" + i;
                splitSenses.put(senseName, sense);
                HERMIT_LOGGER.info("There are " + cluster.size() +
                                   " instances for sense: " + i + 
                                   " of word " + term + 
                                   " stored as: " + senseName);
                ++i;
            }
            clusterMap.removeClusters(term);
        }

	    HERMIT_LOGGER.info("Split into " + splitSenses.size() + " terms.");

        accuracyMap.printCounts();
    }

    private void printPairWiseSimilarities(Set<String> terms) {
        for (String term : terms) {
            Matrix m = clusterMap.pairWiseSimilarity(term);
            StringBuilder sb = new StringBuilder();
            sb.append(term);
            sb.append("term cluster similarity matrix\n");
            for (int r = 0; r < m.rows(); ++r) {
                sb.append(r);
                sb.append(":");
                for (int c = 0; c < m.columns(); ++c)
                    sb.append(String.format("%3f ", m.get(r, c)));
                sb.append("\n");
            }
            HERMIT_LOGGER.fine(sb.toString());
        }
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
         * Creates an emtpy {@code AccuracyMap}
         */
        public AccuracyMap() {
            wordMap = new HashMap<String, List<Map<String, Integer>>>();
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
                if (senseNum >= senseCounts.size()) {
                    for (int i = 0; i <= senseNum; ++i)
                        senseCounts.add(new HashMap<String, Integer>());
                }
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
            if (senseCounts == null) {
                senseCounts = new ArrayList<Map<String, Integer>>();
                wordMap.put(conflatedTerm, senseCounts);
            }

            // Get the two maps for the required senses.
            Map<String, Integer> oldCounts = null;
            Map<String, Integer> newCounts = null;
            oldCounts = senseCounts.get(oldSenseNum);
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
    }
}
