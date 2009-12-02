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

package edu.ucla.sspace.beagle;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.index.BeagleIndexGenerator;
import edu.ucla.sspace.index.BeagleIndexUser;
import edu.ucla.sspace.index.IndexGenerator;
import edu.ucla.sspace.index.IndexUser;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;


/**
 * An implementation of the Beagle Semantic Space model. This implementation is
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
public class Beagle implements SemanticSpace {

    /**
     * The full context size used when scanning the corpus. This is the
     * total number of words considered in the context.
     */
    public static final int CONTEXT_SIZE = 6;

    /**
     * The Semantic Space name for Beagle
     */
    public static final String BEAGLE_SSPACE_NAME = 
        "beagle-semantic-space";

    /**
     * The class responsible for creating index vectors, and incorporating them
     * into a semantic vector.
     */
    private final IndexGenerator indexGenerator;

    /**
     * A mapping for terms to their semantic vector representation. A {@code
     * Vector} is used as these representations may be large.
     */
    private final ConcurrentMap<String, Vector> termHolographs;

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

    public Beagle(int vectorSize) {
        System.setProperty(IndexGenerator.INDEX_VECTOR_LENGTH_PROPERTY,
                           Integer.toString(vectorSize));
        System.setProperty(IndexUser.INDEX_VECTOR_LENGTH_PROPERTY,
                           Integer.toString(vectorSize));
        indexVectorSize = vectorSize;
        indexGenerator = new BeagleIndexGenerator();
        prevSize = 1;
        nextSize = 5;
        termHolographs = new ConcurrentHashMap<String, Vector>();
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
    public Vector getVector(String term) {
        return Vectors.immutableVector(termHolographs.get(term));
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return BEAGLE_SSPACE_NAME + "-" + indexVectorSize;
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
        Queue<String> nextWords = new ArrayDeque<String>();
        IndexUser indexUser = new BeagleIndexUser();

        Iterator<String> it = IteratorFactory.tokenize(document);
        Map<String, Vector> documentVectors = new HashMap<String, Vector>();

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; i < nextSize && it.hasNext(); ++i)
            nextWords.offer(it.next().intern());

        String focusWord = null;
        while (!nextWords.isEmpty()) {
            focusWord = nextWords.remove();
            if (it.hasNext())
                nextWords.offer(it.next().intern());

            // Incorporate the context into the semantic vector for the focus
            // word.  If the focus word has no semantic vector yet, create a new
            // one, as determined by the index builder.
            Vector meaning = termHolographs.get(focusWord);
            if (meaning == null) {
                meaning = new DenseVector(indexVectorSize);
                documentVectors.put(focusWord, meaning);
            }

            Vector focusVector = indexGenerator.getIndexVector(focusWord);

            for (String term : nextWords) {
                Vector addedMeaning = indexUser.generateMeaning(
                        focusVector,
                        indexGenerator.getIndexVector(term),
                        0);
                Vectors.add(meaning, addedMeaning);
            }
        }

        for (Map.Entry<String, Vector> entry : documentVectors.entrySet()) {
            synchronized (entry.getKey()) {
                Vector existingVector = termHolographs.get(entry.getKey());
                if (existingVector == null)
                    termHolographs.put(entry.getKey(), entry.getValue());
                else
                    Vectors.add(existingVector, entry.getValue());
            }
        }
    }
    
    /**
     * No processing is performed on the holographs.
     */
    public void processSpace(Properties properties) {
    }
}
