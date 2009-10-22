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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.index.IndexBuilder;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
 * some large dimension (by default 512), with each entry in the vector being
 * from a random gaussian distribution. The holographic meaning of a word is
 * updated by first adding the sum of index vectors for all the words in a
 * sliding window centered around the target term. Additionally a sum of
 * convolutions of several n-grams is added to the holographic meaning. The
 * main functionality of this class can be found in the {@link IndexBuilder}
 * class.
 *
 * @author Keith Stevens
 */
public class FlyingHermit implements SemanticSpace {

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
     * The class responsible for creating index vectors, and incorporating them
     * into a semantic vector.
     */
    private final IndexBuilder indexBuilder;

    /**
     * A mapping for terms to their set of semantic vector representations.
     * This will contain the set of senses that {@code Flying Hermit} generates
     * as it's processing the corpus.  This {@code Map} is used prior to {@code
     * processSpace} being called.
     */
    private ConcurrentMap<String, List<Vector>> termVectors;

    /**
     * A mapping from a term sense to it's semantic representation.  This
     * differs from {@code TermHolographs} in that it is index by keys of the
     * form "term-senseNum", and map directly to only one of the term's
     * representations.  This {@code Map} is used after {@code processSpace} is
     * called.
     */
    private ConcurrentMap<String, Vector> splitSenses;

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
     * The threshold to use when combining term {@code Vector}s.  If the
     * similarity between the most recent {@code Vector} and the centroids
     * stored so far.
     */
    private double clusterThreshold;

    public FlyingHermit(IndexBuilder builder,
                        int vectorSize) {
        indexVectorSize = vectorSize;
        indexBuilder = builder;
        clusterThreshold = .75;
        prevSize = builder.expectedSizeOfPrevWords();
        nextSize = builder.expectedSizeOfNextWords();
        termVectors = new ConcurrentHashMap<String, List<Vector> >();
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
    public double[] getVectorFor(String term) {
        Vector sense = splitSenses.get(term);
        return (sense != null) ? sense.toArray(indexVectorSize) : null;
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
    public int getVectorSize() {
        return indexVectorSize;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();

	    HERMIT_LOGGER.info("Processing a new document.");

        Iterator<String> it = IteratorFactory.tokenize(document);

        Map<String, Vector> documentHolographs =
            new HashMap<String, Vector>();

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
            nextWords.offer(it.next().intern());
        // Assume the previous words in the context are empty words. Note that
        // this is not specified in the original paper, but makes computation
        // much easier.
        prevWords.offer("");

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            if (it.hasNext())
                nextWords.offer(it.next().intern());

            // Incorporate the context into the semantic vector for the focus
            // word.  If the focus word has no semantic vector yet, create a new
            // one, as determined by the index builder.
            Vector meaning = documentHolographs.get(focusWord);
            if (meaning == null) {
                meaning = indexBuilder.getEmptyVector();
                documentHolographs.put(focusWord, meaning);
            }
            indexBuilder.updateMeaningWithTerm(meaning, prevWords, nextWords);

            // Push the focus word into previous word set for the next focus
            // word.
            prevWords.offer(focusWord);
            if (prevWords.size() > prevSize)
                prevWords.remove();
        }

        // Compare the most recent vector to all the saved vectors.  If the
        // vector with the highest similarity has a similarity over a threshold,
        // incorporate this {@code Vector} to that winner.  Otherwise add this
        // {@code Vector} as a new vector for the term.
        for (Map.Entry<String, Vector> entry : documentHolographs.entrySet()) {
            // Get the set of term vectors for this word that have been found so
            // far.
            List<Vector> termSenses = null;
            synchronized (termVectors) {
                termSenses = termVectors.get(entry.getKey());
                if (termSenses == null) {
                    termSenses = new ArrayList<Vector>();
                    termVectors.put(entry.getKey(), termSenses);
                }
            }

            // Update the set of centriods.
            synchronized (termVectors) {
                Vector bestMatch = null;
                double bestScore = 0;
                double similarity = 0;
                
                // Find the centriod with the best similarity.
                for (Vector centroid : termSenses) {
                    similarity =
                        Similarity.cosineSimilarity(centroid, entry.getValue());
                    if (similarity > bestScore) {
                        bestScore = similarity;
                        bestMatch = centroid;
                    }
                }

                // Add the current term vector if the similarity is high enough,
                // or set it as a new centroid.
                if (similarity > clusterThreshold)
                    Vectors.add(bestMatch, entry.getValue());
                else
                    termSenses.add(entry.getValue());
            }
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
	    HERMIT_LOGGER.info("Starting with " + termVectors.size() + " terms.");
        splitSenses = new ConcurrentHashMap<String, Vector>();
        for (Map.Entry<String, List<Vector>> entry : termVectors.entrySet()) {
            List<Vector> holographs = entry.getValue();
            for (int i = 0; i < holographs.size(); ++i)
                splitSenses.put(entry.getKey() + "-" + i,
                                    holographs.get(i));
            termVectors.remove(entry.getKey(), entry.getValue());
        }
	    HERMIT_LOGGER.info("Split into " + splitSenses.size() + " terms.");
    }
}
