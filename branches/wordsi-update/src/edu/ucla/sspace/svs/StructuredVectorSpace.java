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

import edu.ucla.sspace.basis.BasisMapping;
import edu.ucla.sspace.basis.StringBasisMapping;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyExtractorManager;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.DependencyTreeNode;
import edu.ucla.sspace.dependency.FilteredDependencyIterator;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Pair;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseScaledDoubleVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

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

import java.util.logging.Logger;


/**
 * A dependency parsed based approach to statistical semantics that uses a
 * collection of vectors to represent a word.  This implementaiton is based on
 * the following paper:
 *    
 *   <li style="font-family:Garamond, Georgia, serif">Katrin Erk and Sebastian
 *   Sebastian Padó, "A structured vector space model for word meaning in
 *   context," in <i>Annual Meeting of the ACL</i>, Honolulu, Hawaii.
 *   2008.</li>
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
 * <p>
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
 * @author Keith Stevens
 */
public class StructuredVectorSpace implements SemanticSpace, Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * The Semantic Space name for {@link StructuredVectorSpace}
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
        Logger.getLogger(StructuredVectorSpace.class.getName());

    /**
     * A mapping from terms to dimensions in a co-occcurence space.
     */
    private StringBasisMapping termBasis;

    /**
     * A mapping from terms to their lemma co-occurrence vectors.  These vectors
     * simply represent the number of times other words have occurrend with the
     * key word using any relation link with a distance of one relation.
     */
    private Map<String, SelectionalPreference> preferenceVectors;

    /**
     * A mapping for relation tuples (head word, relation, dependent word)
     * couting the number of times this relation has occurred in the corpus.
     * Only tuples where both words are accepted by the filter are stored.  In
     * order to eliminate duplicate  counting, each relation is only counted
     * once per headword observed, i.e. a sentence with cat as a headword of
     * food will create two dependency paths, one rooted at cat and one rooted
     * at food, this only records the data rooted at cat for this single
     * occurrence.
     */
    transient private Map<RelationTuple, SparseDoubleVector> relationVectors;

    /**
     * The {@link DependencyExtractor} being used for parsing corpora.
     */
    transient private final DependencyExtractor parser;

    /**
     * The {@link DependencyPathAcceptor} to use for validating paths.
     */
    transient private final DependencyPathAcceptor acceptor;

    /**
     * An optional set of words that restricts the set of semantic vectors that
     * this instance will retain.
     */
    private Set<String> semanticFilter;

    /**
     * Create a new instance of {@code StructuredVectorSpace}.
     */
    public StructuredVectorSpace(DependencyExtractor extractor,
                                 DependencyPathAcceptor acceptor) {
        this.parser = extractor;
        this.acceptor = acceptor;

        termBasis = new StringBasisMapping();
        preferenceVectors =
            new ConcurrentHashMap<String, SelectionalPreference>();
        relationVectors =
            new ConcurrentHashMap<RelationTuple, SparseDoubleVector>();
        semanticFilter = new HashSet<String>();
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
    public Vector getVector(String term) {
        SelectionalPreference preference = preferenceVectors.get(term);
        return (preference == null) ? null : preference.lemmaVector;
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
        return 0;
    }

    /**
     * {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        // Local maps to record occurrence counts.
        Map<Pair<Integer>,Double> localLemmaCounts = 
            new HashMap<Pair<Integer>,Double>();
        Map<RelationTuple, SparseDoubleVector> localTuples =
            new HashMap<RelationTuple, SparseDoubleVector>();

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
                int focusIndex = termBasis.getDimension(focusWord);

                // Skip any filtered words.
                if (focusWord.equals(EMPTY_STRING))
                    continue;

                // Skip words that are rejected by the semantic filter.
                if (!acceptWord(focusWord))
                    continue;

                // Create the path iterator for all acceptable paths rooted at
                // the focus word in the sentence.
                Iterator<DependencyPath> pathIter = 
                    new FilteredDependencyIterator(nodes[i], acceptor, 1);

                while (pathIter.hasNext()) {
                    DependencyPath path = pathIter.next();

                    // Get the feature index for the co-occurring word.
                    String otherTerm = path.last().word();
                    
                    // Skip any filtered features.
                    if (otherTerm.equals(EMPTY_STRING))
                        continue;

                    int featureIndex = termBasis.getDimension(otherTerm);

                    Pair<Integer> p = new Pair<Integer>(
                            focusIndex, featureIndex);
                    Double curCount = localLemmaCounts.get(p);
                    localLemmaCounts.put(p, (curCount == null)
                            ? 1 : 1 + curCount);

                    // Create a RelationTuple as a local key that records this
                    // relation tuple occurrence.  If there is not a local
                    // relation vector, create it.  Then add an occurrence count
                    // of 1.
                    DependencyRelation relation = path.iterator().next();

                    // Skip relations that do not have the focusWord as the
                    // head word in the relation.  The inverse relation will
                    // eventually be encountered and we'll account for it then.
                    if (!relation.headNode().word().equals(focusWord))
                        continue;

                    RelationTuple relationKey = new RelationTuple(
                            focusIndex, relation.relation().intern());
                    SparseDoubleVector relationVector = localTuples.get(
                            relationKey);
                    if (relationVector == null) {
                        relationVector = new CompactSparseVector();
                        localTuples.put(relationKey, relationVector);
                    }
                    relationVector.add(featureIndex, 1);
                }
            }
        }

        document.close();

        // Once the document has been processed, update the co-occurrence matrix
        // accordingly.
        for (Map.Entry<Pair<Integer>,Double> e : localLemmaCounts.entrySet()){
            // Push the local co-occurrence counts to the larger mapping.
            Pair<Integer> p = e.getKey();

            // Get the prefernce vectors for the current focus word.  If they do
            // not exist, create it in a thread safe manner.
            String focusWord = termBasis.getDimensionDescription(p.x);
            SelectionalPreference preference = preferenceVectors.get(focusWord);
            if (preference == null) {
                synchronized (this) {
                    preference = preferenceVectors.get(focusWord);
                    if (preference == null) {
                        preference = new SelectionalPreference();
                        preferenceVectors.put(focusWord, preference);
                    }
                }
            }
            // Add the local count.
            synchronized (preference) {
                preference.lemmaVector.add(p.y, e.getValue());
            }
        }

        // Push the relation tuple counts to the larger counts.
        for (Map.Entry<RelationTuple, SparseDoubleVector> r : 
                localTuples.entrySet()) {
            // Get the global counts for this relation tuple.  If it does not
            // exist, create a new one in a thread safe manner.
            SparseDoubleVector relationCounts = relationVectors.get(r.getKey());
            if (relationCounts == null) {
                synchronized (this) {
                    relationCounts = relationVectors.get(r.getKey());
                    if (relationCounts == null) {
                        relationCounts = new CompactSparseVector();
                        relationVectors.put(r.getKey(), relationCounts);
                    }
                }
            }

            // Update the counts.
            synchronized (relationCounts) {
                VectorMath.add(relationCounts, r.getValue());
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        for (Map.Entry<RelationTuple, SparseDoubleVector> e :
                relationVectors.entrySet()) {
            RelationTuple relation = e.getKey();
            SparseDoubleVector relationCounts = e.getValue();
            String headWord = termBasis.getDimensionDescription(relation.head);
            String rel = relation.relation;

            SelectionalPreference headPref = preferenceVectors.get(headWord);

            if (headPref == null)
                LOGGER.fine("what the fuck");

            for (int index : relationCounts.getNonZeroIndices()) {
                double frequency = relationCounts.get(index);
                String depWord = termBasis.getDimensionDescription(index);
                SelectionalPreference depPref = preferenceVectors.get(depWord);

                // It's possible that the dependent word is not being
                // represented in this space, so skip missing terms.
                if (depPref == null)
                    continue;

                headPref.addPreference(
                        rel, depPref.lemmaVector, frequency);
                depPref.addInversePreference(
                        rel, headPref.lemmaVector, frequency);
            }

        }

        // Null out all the relation tuple counts so that memory can be
        // freed up.
        relationVectors = null;
    }

    public SparseDoubleVector contextualize(String focusWord, 
                                            String relation,
                                            String secondWord,
                                            boolean isFocusHeadWord) {
        SelectionalPreference focusPref = preferenceVectors.get(focusWord);
        SelectionalPreference secondPref = preferenceVectors.get(secondWord);

        if (focusPref == null)
            return null;
        if (secondPref == null)
            return focusPref.lemmaVector;

        if (isFocusHeadWord)
            return VectorMath.multiplyUnmodified(
                    focusPref.lemmaVector, 
                    secondPref.inversePreference(relation));
        return VectorMath.multiplyUnmodified(focusPref.lemmaVector, 
                                             secondPref.preference(relation));
    }

    /**
     * {@inheritDoc}.
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

    private class RelationTuple {
        public int head;
        public String relation;

        public RelationTuple(int head, String relation) {
            this.head = head;
            this.relation = relation;
        }

        public boolean equals(Object o) {
            if (o == null || !(o instanceof RelationTuple))
                return false;
            RelationTuple r = (RelationTuple) o;
            return this.head == r.head && this.relation == r.relation;
        }

        public int hashCode() {
            return head ^ relation.hashCode();
        }
    }

    private class SelectionalPreference implements Serializable {

        private static final long serialVersionUID = 1L;

        public SparseDoubleVector lemmaVector;
        public Map<String, SparseDoubleVector> selPreferences;
        public Map<String, SparseDoubleVector> inverseSelPreferences;

        public SelectionalPreference() {
            lemmaVector = new CompactSparseVector();
            selPreferences = new HashMap<String, SparseDoubleVector>();
            inverseSelPreferences = new HashMap<String, SparseDoubleVector>();
        }

        public void addPreference(String relation,
                                  SparseDoubleVector vector,
                                  double frequency) {
            add(relation, new SparseScaledDoubleVector(vector, frequency),
                selPreferences);
        }

        public void addInversePreference(String relation, 
                                         SparseDoubleVector vector,
                                         double frequency) {
            add(relation, new SparseScaledDoubleVector(vector, frequency),
                inverseSelPreferences);
        }

        private void add(String relation, 
                         SparseDoubleVector vector,
                         Map<String, SparseDoubleVector> map) {
            SparseDoubleVector preference = map.get(relation);
            if (preference == null) {
                map.put(relation, vector);
                return;
            }

            preference = VectorMath.multiplyUnmodified(preference, vector);
            map.put(relation, preference);
        }

        public SparseDoubleVector preference(String relation) {
            return selPreferences.get(relation);
        }

        public SparseDoubleVector inversePreference(String relation) {
            return inverseSelPreferences.get(relation);
        }
    }
}
