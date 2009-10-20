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
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * An implementation of the FlyingHermit Semantic Space model. This implementation is
 * based on <p style="font-family:Garamond, Georgia, serif">Jones, M. N.,
 * Mewhort, D.  J.L. (2007).    Representing Word Meaning and Order Information
 * in a Composite Holographic Lexicon.    <i>Psychological Review</i>
 * <b>114</b>, 1-37.  Available <a
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
     * The class responsible for creating index vectors, and incorporating them
     * into a semantic vector.
     */
    private final IndexBuilder indexBuilder;

    /**
     * A mapping for terms to their semantic vector representation. A {@code
     * Vector} is used as these representations may be large.
     */
    private final ConcurrentMap<String, List<Vector> > termHolographs;

    /**
     * The size of each index vector, as set when the sspace is created.
     */
    private final int indexVectorSize;

    /**
     * The number of words in the context to save prior to the focus word.
     */
    private int prevSize;

    /**
     * The number of words in the context to save after the focus word.
     */
    private int nextSize;

    public FlyingHermit(IndexBuilder builder, int vectorSize) {
        indexVectorSize = vectorSize;
        indexBuilder = builder;
        prevSize = builder.expectedSizeOfPrevWords();
        nextSize = builder.expectedSizeOfNextWords();
        termHolographs = new ConcurrentHashMap<String, List<Vector> >();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return termHolographs.keySet();
    }

    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String term) {
        Vector finalVector = indexBuilder.getEmtpyVector();
        for (Vector v : termHolographs.get(term))
            Vectors.add(finalVector, v);
        return finalVector.toArray(indexVectorSize);
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
        //long start = System.currentTimeMillis();
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            if (it.hasNext())
                nextWords.offer(it.next().intern());

            // Incorporate the context into the semantic vector for the focus
            // word.  If the focus word has no semantic vector yet, create a new
            // one, as determined by the index builder.
            Vector meaning = documentHolographs.get(focusWord);
            if (meaning == null) {
                meaning = indexBuilder.getEmtpyVector();
                documentHolographs.put(focusWord, meaning);
            }
            indexBuilder.updateMeaningWithTerm(meaning, prevWords, nextWords);

            // Push the focus word into previous word set for the next focus
            // word.
            prevWords.offer(focusWord);
            if (prevWords.size() > prevSize)
                prevWords.remove();
        }
        //long terms = System.currentTimeMillis();

        for (Map.Entry<String, Vector> entry :
                documentHolographs.entrySet()) {
            List<Vector> termVectors = null;
            synchronized (termHolographs) {
                termVectors = termHolographs.get(entry.getKey());
                if (termVectors == null) {
                    termVectors = new ArrayList<Vector>();
                    termHolographs.put(entry.getKey(), termVectors);
                }
            }
            synchronized (termVectors) {
                // Compare the most recent vector to all the saved vectors.  If
                // the vector with the highest similarity has a similarity over
                // a threshold, incorporate this {@code Vector} to that
                // winner.  Otherwise add this {@code Vector} as a new
                // vector for the term.
            }
        }
        //long end = System.currentTimeMillis();
        //System.out.println("time spent per all terms: " + (terms- start));
        //System.out.println("time spent per clustering: " + (end - terms));
        //System.out.println("time spent per doc: " + (end - start));
    }
    
    /**
     * No processing is performed on the holographs.
     */
    public void processSpace(Properties properties) {
    }
}
