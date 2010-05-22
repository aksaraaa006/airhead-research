/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.svs;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.matrix.AtomicGrowingSparseHashMatrix;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Misc;
import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.logging.Logger;


/**
 * @author Keith Stevens
 */
public class StructuredVectorSpace implements SemanticSpace {

    /**
     * The base prefix for all {@code StructuredVectorSpace}
     * properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.dri.StructuredVectorSpace";

    public static final String DEPENDENCY_ACCEPTOR_PROPERTY =
        PROPERTY_PREFIX + ".dependencyAcceptor";
    public static final String DEPENDENCY_PATH_LENGTH_PROPERTY =
        PROPERTY_PREFIX + ".dependencyPathLength";
    public static final String DEPENDENCY_WEIGHT_PROPERTY =
        PROPERTY_PREFIX + ".dependencyWeight";

    public static final int DEFAULT_DEPENDENCY_PATH_LENGTH = 1;

    /**
     * The Semantic Space name for {@link StructuredVectorSpace}
     */
    public static final String SSPACE_NAME = 
        "structured-vector-space";

    public static final String EMPTY_STRING = "";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER =
        Logger.getLogger(StructuredVectorSpace.class.getName());

    private Map<String, Integer> termToRowIndex;

    private Map<String, Integer> termToFeatureIndex;

    private AtomicGrowingSparseHashMatrix cooccurrenceMatrix;

    private final DependencyExtractor parser;
    private final DependencyPathAcceptor acceptor;
    private final DependencyPathWeight weighter;
    private final int pathLength;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private Set<String> semanticFilter;

    /**
     * Creates a new instance of {@code StructuredVectorSpace} that takes
     * ownership of a {@link DependencyExtractor} and uses the System provided
     * properties to specify other class objects.
     */
    public StructuredVectorSpace(DependencyExtractor parser) {
        this(parser, System.getProperties());
    }

    /**
     * Create a new instance of {@code StructuredVectorSpace} which
     * takes ownership
     */
    public StructuredVectorSpace(DependencyExtractor parser,
                                 Properties properties) {
        this.parser = parser;

        // Load the maximum dependency path length.
        String pathLengthProp =
            properties.getProperty(DEPENDENCY_PATH_LENGTH_PROPERTY);
        pathLength = (pathLengthProp != null)
            ? Integer.parseInt(pathLengthProp)
            : DEFAULT_DEPENDENCY_PATH_LENGTH;

        // Load the path acceptor.
        String acceptorProp = 
            properties.getProperty(DEPENDENCY_ACCEPTOR_PROPERTY);
        acceptor = (acceptorProp != null)
            ? (DependencyPathAcceptor) Misc.getObjectInstance(acceptorProp)
            : new UniversalPathAcceptor();

        // Load the path weight function.
        String weighterProp = 
            properties.getProperty(DEPENDENCY_WEIGHT_PROPERTY);
        weighter = (weighterProp!= null)
            ? (DependencyPathWeight) Misc.getObjectInstance(weighterProp)
            : new FlatPathWeight();

        cooccurrenceMatrix = new AtomicGrowingSparseHashMatrix();
        termToRowIndex = new ConcurrentHashMap<String,Integer>();
        termToFeatureIndex = new ConcurrentHashMap<String,Integer>();
        semanticFilter = new HashSet<String>();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToRowIndex.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String term) {
        Integer termIndex = termToRowIndex.get(term);
        return (termIndex != null)
            ? cooccurrenceMatrix.getRowVectorUnsafe(termIndex)
            : null;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return SSPACE_NAME; 
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return cooccurrenceMatrix.columns();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        Map<Pair<Integer>,Double> matrixEntryToCount = 
            new HashMap<Pair<Integer>,Double>();

        // Iterate over all of the parseable dependency parsed sentences in the
        // document.
        for (DependencyTreeNode[] nodes = null;
                (nodes = parser.parse(document)) != null; ) {

            // Skip empty documents.
            if (nodes.length == 0)
                continue;

            // Examine the paths for each word in the sentence.
            for (int i = 0; i < nodes.length; ++i) {
                String focusWord = nodes[i].word();

                // Skip any filtered words.
                if (focusWord.equals(EMPTY_STRING))
                    continue;

                // Skip words that are rejected by the semantic filter.
                if (!acceptWord(focusWord))
                    continue;

                int focusIndex = getIndexFor(focusWord, termToRowIndex);

                // Create the path iterator for all acceptable paths rooted at
                // the focus word in the sentence.
                Iterator<DependencyPath> pathIter = new DependencyIterator(
                        nodes, acceptor, weighter, i, 1);

                // Count each co-occurence the focus word has with words that
                // are one relation away.  Since each focus word has several
                // vectors, the word will be stored in the vector corresponding
                // to the term's expectation.  For instance, the path 
                //   [(cat, OBJ, isHead), (play, NULL, false)]
                // Would store the "play" co-occurrence in the "cat-OBJ" vector,
                // which states that "play" is in the OBJ expectation of cat.
                // If cat was not the head word, then the "play" co-occurence
                // would be stored in the "OBJ-cat" vector.
                while (pathIter.hasNext()) {
                    DependencyPath path = pathIter.next();

                    // Get the feature index for the co-occurring word.
                    String otherTerm = path.path().peekLast().token();

                    // Skip any filtered features.
                    if (otherTerm.equals(EMPTY_STRING))
                        continue;

                    int featureIndex =
                        getIndexFor(otherTerm, termToFeatureIndex);

                    // Determine the expectation vector name and retrieve the
                    // row index for that vector.
                    DependencyRelation relation = path.path().peek();
                    String termExpectation = (relation.isHeadNode())
                        ? focusWord + "-" + relation.relation()
                        : relation.relation() + "-" + focusWord;
                    int rowIndex = getIndexFor(termExpectation, termToRowIndex);

                    // Increment the score for this co-occurence.
                    incrementCount(matrixEntryToCount, rowIndex, 
                                   featureIndex, 1);
                    incrementCount(matrixEntryToCount, focusIndex, 
                                   featureIndex, path.score());
                }
            }
        }

        // Once the document has been processed, update the co-occurrence matrix
        // accordingly.
        for (Map.Entry<Pair<Integer>,Double> e : matrixEntryToCount.entrySet()){
            Pair<Integer> p = e.getKey();
            cooccurrenceMatrix.addAndGet(p.x, p.y, e.getValue());
        }    
        document.close();
    }

    private void incrementCount(Map<Pair<Integer>, Double> matrixEntryToCount,
                                int rowIndex, int featureIndex,
                                double value) {
        Pair<Integer> p = new Pair<Integer>(rowIndex, featureIndex);
        Double curCount = matrixEntryToCount.get(p);
        matrixEntryToCount.put(p, (curCount == null)
                               ? value : value + curCount);
    }

    /**
     * Returns the index in the co-occurence matrix for this word.  If the word
     * was not previously assigned an index, this method adds one for it and
     * returns that index.
     */
    private final int getIndexFor(String word,
                                  Map<String, Integer> termToIndex) {
        Integer index = termToIndex.get(word);
        if (index == null) {     
            synchronized(this) {
                // recheck to see if the term was added while blocking
                index = termToIndex.get(word);
                // if another thread has not already added this word while the
                // current thread was blocking waiting on the lock, then add it.
                if (index == null) {
                    int i = termToIndex.size();
                    termToIndex.put(word, i);
                    return i; // avoid the auto-boxing to assign i to index
                }
            }
        }
        return index;
    }

    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        System.out.println("# Feature Column_Index");
        for (Map.Entry<String, Integer> featureEntry : 
                termToFeatureIndex.entrySet())
            System.out.printf("%s %d\n",
                              featureEntry.getKey(), featureEntry.getValue());
    }

    /**
     * {@inheritDoc}.
     *
     * </p> Note that all words will still have an index vector assigned to
     * them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    public void setSemanticFilter(Set<String> semanticsToRetain) {
        semanticFilter.clear();
        semanticFilter.addAll(semanticsToRetain);
    }

    /**
     * Returns true if there is no semantic filter list or the word is in the
     * filter list.
     */
    private boolean acceptWord(String word) {
        return semanticFilter.isEmpty() || semanticFilter.contains(word);
    }
}
