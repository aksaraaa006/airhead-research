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

package edu.ucla.sspace.sevm;

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.common.SelectionalPreferenceSpace;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyRelationAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;

import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.YaleSparseMatrix;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseScaledDoubleVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;

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
 * A dependency parsed based approach to statistical semantics that uses a
 * collection of vectors to represent a word.  This implementaiton is based on
 * the following paper:
 *    
 * <p>
 *
 * This model requires a dependency parsed corpus.  When processing, three types
 * of vectors: word, which represnts the co-occureences word has with all other
 * tokens via a dependency chain; REL|word, which records the set of tokens that
 * govern the REL relationship with word; and word|REL, which records the set of
 * tokens that are governed by word in the REL relationship.  The first vector
 * is referred to as a lemma vector and the later two are called selectional
 * preference vectors.  In all cases REL is a dependency relationship.
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
 * incremental changes to the semantics as the corpus is processed. 
 *
 * </p>
 * The {@link #processSpace(Properties) processSpace} method does nothing other
 * than print out the feature indexes in the space to standard out.
 *
 * @see DependencyPath
 * @see DependencyPathWeight
 * @see DependencyRelationAcceptor
 *
 * @author Keith Stevens
 */
public class SyntacticallyEnrichedVectorModel
  implements SelectionalPreferenceSpace, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The base prefix for all {@code SyntacticallyEnrichedVectorModel}
     * properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.dri.SyntacticallyEnrichedVectorModel";

    /**
     * The property for setting the {@link DependencyRelationAcceptor}.
     */
    public static final String DEPENDENCY_ACCEPTOR_PROPERTY =
        PROPERTY_PREFIX + ".dependencyAcceptor";

    /**
     * The property for setting the maximal length of any {@link
     * DependencyPath}.
     */
    public static final String DEPENDENCY_PATH_LENGTH_PROPERTY =
        PROPERTY_PREFIX + ".dependencyPathLength";

    /**
     * The property for setting the {@link DependencyPathWeight}.
     */
    public static final String DEPENDENCY_WEIGHT_PROPERTY =
        PROPERTY_PREFIX + ".dependencyWeight";

    /**
     * The default maximal path length.
     */
    public static final int DEFAULT_DEPENDENCY_PATH_LENGTH = 1;

    /**
     * The Semantic Space name for {@link SyntacticallyEnrichedVectorModel}
     */
    public static final String SSPACE_NAME = 
        "structured-vector-space";

    /**
     * A static variable for the empty string.
     */
    public static final String EMPTY_STRING = "";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER =
        Logger.getLogger(SyntacticallyEnrichedVectorModel.class.getName());

    private final BasisMapping<String, String> termBasis;

    private final BasisMapping<String, String> relationBasis;

    private final BasisMapping<String, String> focusBasis;

    /**
     * A Mapping from lemmas to their selectional preference vectors.  The inner
     * maps correspond to the possible relationships for each selectional
     * preference, which are represented as REL- for instances in which the term
     * governs the relationship and -REL for when the term is governed in the
     * relationship
     */
    private Map<String, Map<String, SparseDoubleVector>> selPrefMap;

    /**
     * The matrix that stores the full second order word vectors.  
     */
    private SparseMatrix secondOrderWordSpace;

    private int numFeatures;

    private int numRelations;

    private int numRows;

    private int numColumns;

    /**
     * The {@link DependencyExtractor} being used for parsing corpora.
     */
    private final DependencyExtractor parser;

    /**
     * The {@link DependencyRelationAcceptor} to use for validating paths.
     */
    private final DependencyRelationAcceptor acceptor;

    /**
     * The {@link DependencyPathWeight} to use for scoring paths.
     */
    private final DependencyPathWeight weighter;

    /**
     * The maximum number of relations any path may have.
     */
    private final int pathLength;

    /**
     * Create a new instance of {@code SyntacticallyEnrichedVectorModel}. 
     */
    public SyntacticallyEnrichedVectorModel(
        BasisMapping<String, String> termBasis,
        BasisMapping<String, String> relationBasis,
        DependencyExtractor parser,
        DependencyRelationAcceptor acceptor,
        DependencyPathWeight weighter,
        int pathLength) {
        this.termBasis = termBasis;
        this.relationBasis = relationBasis;
        this.parser = parser;
        this.acceptor = acceptor;
        this.weighter = weighter;
        this.pathLength = pathLength;

        focusBasis = new StringBasisMapping();
        selPrefMap =
            new ConcurrentHashMap<String, Map<String, SparseDoubleVector>>();
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termBasis.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public SparseDoubleVector getVector(String term) {
        int index = focusBasis.getDimension(term);
        return (index < 0) ? null : secondOrderWordSpace.getRowVector(index);
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
        return secondOrderWordSpace.columns();
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        // Iterate over all of the parseable dependency parsed sentences in the
        // document.
        for (DependencyTreeNode[] nodes = null;
                (nodes = parser.readNextTree(document)) != null; ) {

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
                int focusIndex = focusBasis.getDimension(focusWord);
                if (focusIndex < 0)
                    continue;

                // Create the path iterator for all acceptable paths rooted at
                // the focus word in the sentence.
                Iterator<DependencyPath> pathIter = 
                    new DependencyIterator(nodes[i], acceptor, pathLength);

                // Count each co-occurence the focus word has with words that
                // are one relation away.  Since each focus word has several
                // vectors, the word will be stored in the vector corresponding
                // to the term's expectation.  For instance, the path 
                //   [(cat, OBJ, isHead), (play, NULL, false)]
                // Would store the "play" co-occurrence in the "cat|OBJ" vector,
                // which states that "play" is in the OBJ expectation of cat.
                // If cat was not the head word, then the "play" co-occurence
                // would be stored in the "OBJ|cat" vector.
                while (pathIter.hasNext()) {
                    DependencyPath path = pathIter.next();

                    // Get the feature index for the co-occurring word.
                    String otherTerm = path.last().word();
                    
                    // Skip any filtered features.
                    if (otherTerm.equals(EMPTY_STRING))
                        continue;

                    int featureIndex = termBasis.getDimension(otherTerm);
                    if (featureIndex <= 0)
                      continue;

                    // Determine the expectation vector name and retrieve the
                    // row index for that vector.
                    DependencyRelation relation = path.iterator().next();

                    // Check whether the current term is the head node in the 
                    // relation.  If so, the relation will come after.
                    String orderedRelation = 
                        (relation.headNode().word().equals(focusWord))
                        ? relation.relation() + "-"
                        : "_" + relation.relation();

                    // Get the mapping of selectional preferences for the focus
                    // word.
                    Map<String, SparseDoubleVector> preferences =
                      selPrefMap.get(focusWord);
                    if (preferences == null) {
                        synchronized (selPrefMap) {
                            preferences = selPrefMap.get(focusWord);
                            if (preferences == null) {
                              preferences =
                                  new HashMap<String, SparseDoubleVector>();
                              selPrefMap.put(focusWord, preferences);
                            }
                        }
                    }

                    // Get the selection preference vector for the relation that
                    // the focus word has with the feature word.
                    SparseDoubleVector relationPreferenceCounts =
                      preferences.get(orderedRelation);
                    if (relationPreferenceCounts  == null) {
                        synchronized (preferences) {
                            relationPreferenceCounts = preferences.get(
                                orderedRelation);
                            if (relationPreferenceCounts  == null) {
                              relationPreferenceCounts =
                                new CompactSparseVector();
                              preferences.put(
                                  orderedRelation, relationPreferenceCounts);
                            }
                        }
                    }

                    // Set the feature value for the co-occurrence with this
                    // relationship type.
                    double score = weighter.scorePath(path);
                    synchronized (relationPreferenceCounts) {
                        relationPreferenceCounts.add(featureIndex, score);
                    }

                    // Add the ordered relation to the set of known relations.
                    int relIndex = relationBasis.getDimension(orderedRelation);
                }
            }
        }

        document.close();
    }

    /**
     */
    public void processSpace(Properties properties) {
        numRows = focusBasis.numDimensions();
        numRelations = relationBasis.numDimensions();
        numFeatures = termBasis.numDimensions();
        numColumns = numRelations * numRelations * termBasis.numDimensions();
        secondOrderWordSpace = new YaleSparseMatrix(numRows, numColumns);

        System.out.printf("numRows: %d\nnumRelations: %d\nnumFeatures: %d\n",
                          numRows, numRelations, numFeatures);
        // Add in code for the transform here after the new transform changes
        // are in place.
        for (Map.Entry<String, Map<String, SparseDoubleVector>> preferenceEntry :
                selPrefMap.entrySet()) {
            String focusTerm = preferenceEntry.getKey();
            int termIndex = focusBasis.getDimension(focusTerm);
            Map<String, SparseDoubleVector> preferenceMap =
              preferenceEntry.getValue();
            for (Map.Entry<String, SparseDoubleVector> relationPreferences :
                    preferenceMap.entrySet()) {
                // Get the index corresponding to the current relation.
                int relation1Index = relationBasis.getDimension(
                    relationPreferences.getKey());
                SparseDoubleVector preferenceCounts =
                    relationPreferences.getValue();

                // Iterate through all of the non zero values for this relation.
                // Multiply the value for each co-occurring term against the
                // feature values for that co-occurring term, this projects 
                // the single occurrence feature into a space that has the
                // weighted second order values for every co-occuring term.
                for (int index : preferenceCounts.getNonZeroIndices()) {
                    double weight = preferenceCounts.get(index);
                    String occurringTerm =
                      termBasis.getDimensionDescription(index);

                    System.out.printf("%s %d\n", occurringTerm, index);
                    Map<String, SparseDoubleVector> occurringMap =
                      selPrefMap.get(occurringTerm);
                    for (Map.Entry<String, SparseDoubleVector> occurringEntry :
                            occurringMap.entrySet()) {
                        // Get the index corresponding to the indirect relation.
                        int relation2Index = relationBasis.getDimension(
                            occurringEntry.getKey());
                        SparseDoubleVector secondCounts =
                          occurringEntry.getValue();
                        for (int secondIndex : secondCounts.getNonZeroIndices()) {
                            double weight2 = secondCounts.get(secondIndex);
                            int featureIndex =
                              relation1Index * numFeatures * numRelations +
                              relation2Index * numFeatures +
                              secondIndex;
                            double oldValue = secondOrderWordSpace.get(
                                termIndex, featureIndex);
                            secondOrderWordSpace.set(termIndex, featureIndex,
                                weight2 * weight + oldValue);
                        }
                    }
                }
            }
        }
        termBasis.setReadOnly();
        focusBasis.setReadOnly();
        relationBasis.setReadOnly();
    }

    public SparseDoubleVector contextualize(Iterator<DependencyPath> paths) {
      SparseDoubleVector contextVector = new CompactSparseVector(numFeatures);
      while (paths.hasNext()) {
        DependencyPath path = paths.next();

        // Skip any paths longer than 1 relation.
        if (path.length() > 1)
          continue;

        DependencyRelation relation = path.lastRelation();
        String occurringTerm = path.last().word();
        String focusTerm = path.first().word();

        String orderedRelation = (relation.headNode().equals(path.first()))
          ? relation.relation() + "-"
          : "-" + relation.relation(); 

        // Get the index corresponding to the current relation.
        int relation1Index = relationBasis.getDimension(orderedRelation);

        Map<String, SparseDoubleVector> prefs = selPrefMap.get(occurringTerm);
        if (prefs == null)
          continue;

        SparseDoubleVector focusVector = getVector(focusTerm);
        for (Map.Entry<String, SparseDoubleVector> entry : prefs.entrySet()) {
            int relation2Index = relationBasis.getDimension(entry.getKey());
            SparseDoubleVector secondCounts = entry.getValue();
            for (int secondIndex : secondCounts.getNonZeroIndices()) {
                double weight2 = secondCounts.get(secondIndex);
                int featureIndex = secondIndex * numRows * numRelations +
                                   relation2Index * numRelations +
                                   relation1Index;
                contextVector.add(
                    featureIndex, weight2*focusVector.get(featureIndex));
            }
        }
      }
      return contextVector;
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
      for (String term : semanticsToRetain)
        focusBasis.getDimension(term);
      focusBasis.setReadOnly();
    }
}
