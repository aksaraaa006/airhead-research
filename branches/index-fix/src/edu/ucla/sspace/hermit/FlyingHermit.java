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
 * An implementation of the {@code FlyingHermit} Semantic Space model. This
 * implementation is based on <p style="font-family:Garamond, Georgia,
 * serif">Jones, M. N., Mewhort, D.  J.L. (2007).    Representing Word Meaning
 * and Order Information in a Composite Holographic Lexicon.    <i>Psychological
 * Review</i> <b>114</b>, 1-37.  Available <a
 * href="www.indiana.edu/~clcl/BEAGLE/Jones_Mewhort_PR.pdf">here</a></p>
 *
 * For every word, a unique random index vector is created, where the vector has
 * some large dimension, with each entry in the vector being
 * generated by a {@link IndexGenerator}. The meaning of a word is built up
 * according to the operations specified in a {@link IndexUser}.  These meaning
 * vectors are created for each document processed, and then clustered to infer
 * a set of centroids.  These centroids later become the senses of a word.
 *
 * </p> For Hermit, there are a large number of possiblies regarding how the
 * index vectors are generated, and how they are combined.
 *
 * @see BeagleIndexGenerator
 * @see BeagleIndexUser
 *
 * @see RandomIndexGenerator
 * @see RandomIndexUser
 * @author Keith Stevens
 */
public class FlyingHermit implements BottomUpHermit, SemanticSpace {

    /**
     * The full context size used when scanning the corpus. This is the
     * total number of words considered in the context.
     */
    public static final int CONTEXT_SIZE = 6;

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

    private Map<String, String> replacementMap;

    private ConcurrentMap<String, AtomicInteger> accuracyMap;

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

        accuracyMap = new ConcurrentHashMap<String, AtomicInteger>();

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

        Iterator<String> it =
            IteratorFactory.tokenizeOrdered(document);

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

        // Assume the previous words in the context are empty words. Note that
        // this is not specified in the original paper, but makes computation
        // much easier.
        prevWords.offer("");

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            String replacement = nextReplacements.remove();
            if (it.hasNext())
                addNextWord(it, nextWords, nextReplacements);

            if (!replacement.equals("")) {
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
                int clusterNum = clusterMap.addVector(focusWord, meaning);

                // Count the accuracy of the current cluster assignment for the
                // word if it is a word we are tracking.
                String key = focusWord + "-" + clusterNum + "-" + replacement;
                AtomicInteger clusterCount = accuracyMap.putIfAbsent(
                        key, new AtomicInteger(1));
                if (clusterCount != null)
                  clusterCount.incrementAndGet();
            }
            // Push the focus word into previous word set for the next focus
            // word.
            prevWords.offer(focusWord);
            if (prevWords.size() > prevSize)
                prevWords.remove();
        }
    }
    
    private void addNextWord(Iterator<String> it,
                             Queue<String> nextWords,
                             Queue<String> nextReplacements) {
        String term = it.next();
        String replacement = "";
        if (replacementMap != null) {
            replacement = replacementMap.get(term);
            if (replacement != null) {
                String swap = term;
                term = replacement;
                replacement = swap;
            } else
                replacement = "";
        }
        nextWords.offer(term.intern());
        nextReplacements.offer(replacement);
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        double minPercentage = Double.parseDouble(
            properties.getProperty(BottomUpHermit.DROP_PERCENTAGE, ".25"));

        splitSenses = new ConcurrentHashMap<String, Vector>();
        Set<String> terms = new TreeSet<String>(clusterMap.keySet());

        // Print out the pairwise similarities.
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

        for (String term : terms)
            clusterMap.mergeOrDropClusters(term, minPercentage);

        // Print out the pairwise similarities.
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

        for (Map.Entry<String, AtomicInteger> entry : accuracyMap.entrySet())
            System.out.println(entry.getKey() + "|" + entry.getValue().get());
    }
}
