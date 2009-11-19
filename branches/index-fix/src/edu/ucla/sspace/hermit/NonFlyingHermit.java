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

import edu.ucla.sspace.hermit.HierarchicalAgglomerativeClustering.ClusterLinkage;
import edu.ucla.sspace.index.IndexGenerator;
import edu.ucla.sspace.index.IndexUser;
import edu.ucla.sspace.index.SparseRandomIndexVector;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import edu.ucla.sspace.matrix.Matrix;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.ObjectOutputStream;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.TreeSet;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * An implementation of the {@code NonFlyingHermit} Semantic Space model. 
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
public class NonFlyingHermit implements BottomUpHermit, SemanticSpace {

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
     * The Semantic Space name for NonFlyingHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "non-flying-hermit-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger HERMIT_LOGGER =
        Logger.getLogger(NonFlyingHermit.class.getName());

    /**
     * The class responsible for creating index vectors.
     */
    private final IndexGenerator indexGenerator;

    /**
     * The class responsible for combining index vectors.
     */
    private final Class indexUserClazz;

    /**
     * A fixed String describing the {@code IndexUser} that {@code
     * NonFlyingHermit} uses.
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
     * {@code NonFlyingHermit} should generate senses for all non empty tokens.
     * If it is not null, then only tokens which have mappings will have a set
     * of senses generated and stored in the semantic space.
     */
    private Map<String, String> replacementMap;

    /**
     * A simple map containing conflation information and storage of contexts.
     */
    private ConflationMap conflationMap;

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
     * Create a new instance of {@code NonFlyingHermit} which takes ownership
     */
    public NonFlyingHermit(IndexGenerator generator,
                        Class userClazz,
                        Map<String, String> remap,
                        int vectorSize,
                        int prevWordsSize,
                        int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexGenerator = generator;
        indexUserClazz = userClazz;
        replacementMap = remap;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;

        conflationMap = new ConflationMap(remap.values());

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
               "-" + indexUserDescription.toString();
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


                // Count the accuracy of the current cluster assignment for
                // words which have a replacement different from themselves.
                conflationMap.addInstance(replacement, focusWord, meaning);
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
        for (Map.Entry<String, File> entry :
                conflationMap.fileNameMap.entrySet()) {
            Duple<int[], Vector[]> vectorAssignments = 
                HierarchicalAgglomerativeClustering.cluster(
                        entry.getValue(), .13, ClusterLinkage.SINGLE_LINKAGE);
            // Find the number of clusters generated.
            int numClusters = 0;
            for (int clusterNum : vectorAssignments.x)
                if (clusterNum > numClusters)
                    numClusters = clusterNum;

            // Compute the real occurance counts.  For the list of original
            // terms, and the context indexes they map to, count how many times
            // each original term occurs in a particular cluster.
            for (Map.Entry<String, List<Integer>> contextIndexes :
                    conflationMap.wordMap.get(entry.getKey()).entrySet()) {
                int[] occuranceCounts = new int[numClusters];
                for (int contextIndex : contextIndexes.getValue())
                    occuranceCounts[vectorAssignments.x[contextIndex]]++;

                // Output the occurance counts for a particular original sense
                // for each of the infered senses.
                for (int i = 0; i < numClusters; ++i) {
                    StringBuilder sb = new StringBuilder();
                    sb.append(entry.getKey()).append("-");
                    sb.append(i).append("-");
                    sb.append(contextIndexes.getKey()).append("|");
                    sb.append(occuranceCounts[i]);
                    System.out.println(sb.toString());
                }
            }

            // Sum up the instances for each of the cluster to compute the
            // centroid.
            Vector[] centroids = new Vector[numClusters];
            for (int i = 0; i < numClusters; ++i)
                centroids[i] = new SparseRandomIndexVector(indexVectorSize);

            for (int i = 0; i < vectorAssignments.x.length; ++i)
                Vectors.add(centroids[vectorAssignments.x[i]],
                            vectorAssignments.y[i]);

            // Store the centorids as the semantic vectors stored in this
            // semantic space.
            for (int i = 0; i < numClusters; ++i) {
                String key = entry.getKey();
                if (i != 0)
                    key += "-" + i;
                splitSenses.put(key, centroids[i]);
            }
        }
    }

    /**
     * A simple map for counting the accuracy of a sample word that is being  
     * tracked with {@code NonFlyingHermit}.
     */
    private static class ConflationMap {

        /**
         * A mapping from conflated senses to a list of infered senses.  Each
         * infered sense has a mapping from original terms to the number of
         * times the instance of that word was mapped to the given sense of this
         * conflated term.
         */
        private Map<String, Map<String, List<Integer>>> wordMap;

        /**
         * A mapping from conflated senses to an output stream.
         */
        private Map<String, ObjectOutputStream> fileMap;

        /**
         * A mapping from conflated seneses to the temporary file storing it's
         * contexts.
         */
        private Map<String, File> fileNameMap;

        /**
         * A mapping from conflated senses to the number of times the sense has
         * occured.  This allows for giving each context a unique identifier.
         */
        private ConcurrentMap<String, Integer> countMap;

        /**
         * Creates an emtpy {@code ConflationMap}
         */
        public ConflationMap(Collection<String> conflations) {
            countMap = new ConcurrentHashMap<String, Integer>();
            fileMap = new HashMap<String, ObjectOutputStream>();
            fileNameMap = new HashMap<String, File>();
            wordMap = new HashMap<String, Map<String, List<Integer>>>();

            try {
                for (String conflation : conflations) {
                    File f = File.createTempFile(conflation, ".tmp");
                    f.deleteOnExit();
                    ObjectOutputStream stream = new ObjectOutputStream(
                            new BufferedOutputStream(new FileOutputStream(f)));
                    fileMap.put(conflation, stream);
                    fileNameMap.put(conflation, f);
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
        }

        /**
         * Add a new instance of a conflated sense being mapped to a given sense
         * when the original term was {@code originalSense}.
         */
        public void addInstance(String conflatedTerm,
                                String originalSense,
                                Vector conflationVector) {
            HERMIT_LOGGER.fine("Writing conflated sense: " + conflatedTerm);

            ObjectOutputStream conflationStream = fileMap.get(conflatedTerm);
            int vectorIndex = 0;
            synchronized (countMap) {
                Integer conflatedCount = countMap.get(conflatedTerm);
                vectorIndex = (conflatedCount == null) ? 0 : conflatedCount;
                countMap.put(conflatedTerm, vectorIndex + 1);
            }

            Duple<Integer, Vector> v = new Duple<Integer, Vector>(
                    vectorIndex, conflationVector);
            try {
                synchronized (conflationStream) {
                    conflationStream.writeObject(v);
                }
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }

            HERMIT_LOGGER.fine("Writing the sense count: " + conflatedTerm);
            // Get the list of senses known for this conflated term.
            Map<String, List<Integer>> senseCounts = null;
            synchronized (wordMap) {
                senseCounts = wordMap.get(conflatedTerm);
                if (senseCounts == null) {
                    senseCounts = new HashMap<String, List<Integer>>();
                    wordMap.put(conflatedTerm, senseCounts);
                }
            }

            // Get the mapping for the given sense number.
            synchronized (senseCounts) {
                List<Integer> termCounts = senseCounts.get(originalSense);
                if (termCounts == null) {
                    termCounts = new LinkedList<Integer>();
                    senseCounts.put(originalSense, termCounts);
                }
                termCounts.add(vectorIndex);
            }
        }
    }
}
