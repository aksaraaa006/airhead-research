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

import edu.ucla.sspace.clustering.ClusterMap;
import edu.ucla.sspace.clustering.OnlineClusteringGenerator;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.SparseHashIntegerVector;
import edu.ucla.sspace.vector.SparseIntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.VectorMath;

import edu.ucla.sspace.matrix.Matrix;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
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
 * An implementation of the {@code SecondFlyingHermit} Semantic Space model. 
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
 * {@link ClusterMap} for it's functionaltiy.  The {@link
 * IntegerVectorGenerator} provided defines how index vectors are created.  The
 * {@link ClusterMap} defines how contexts are clustered together.
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
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVector} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  
 *
 * @author Keith Stevens
 */
public class SecondFlyingHermit implements SemanticSpace, Filterable {

    /**
     * The base prefix for all {@code SecondFlyingHermit} properties.
     */
    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.hermit.SecondFlyingHermit";

    /**
     * The property for specifying the threshold for merging clusters.
     */
    public static final String MERGE_THRESHOLD_PROPERTY = 
        PROPERTY_PREFIX + ".mergeThreshold";

    /**
     * The property for specifying the number of threads to use when processing
     * the space.
     */
    public static final String THREADS_PROPERTY = 
        PROPERTY_PREFIX + ".numThreads";

    /**
     * An empty token representing a lack of a valid replacement mapping.
     */
    public static final String EMPTY_TOKEN = "";

    /**
     * The Semantic Space name for SecondFlyingHermit
     */
    public static final String FLYING_HERMIT_SSPACE_NAME = 
        "flying-hermit-semantic-space";

    /**
     * The logger used to record all output
     */
    private static final Logger HERMIT_LOGGER =
        Logger.getLogger(SecondFlyingHermit.class.getName());

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
    private ConcurrentMap<String, Vector> splitSenses;

    /**
     * The set of words to produce semantics for.
     */
    private Set<String> acceptedWords; 

    /**
     * An accuracy map to record the frequency statistics for each word cluster.
     * This is used to label the clusters once they have been formed.
     */
    private AssignmentMap assignmentMap;

    /**
     * The type of clustering used for {@code SecondFlyingHermit}.  This specifies how
     * hermit will merge it's context vectors into different senses.
     */
    private ClusterMap<SparseIntegerVector> clusterMap;

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
     * A flag specifying if this {@code SecondFlyingHermit} instance has been
     * compacted, and should enter testing mode.
     */
    private boolean compacted;

    /**
     * Create a new instance of {@code SecondFlyingHermit} which takes ownership
     */
    public SecondFlyingHermit(
            Map<String, TernaryVector> indexGeneratorMap,
            PermutationFunction<TernaryVector> permFunction,
            OnlineClusteringGenerator<SparseIntegerVector> clusterGenerator,
            Set<String> accepted,
            int vectorSize,
            int prevWordsSize,
            int nextWordsSize) {
        indexVectorSize = vectorSize;
        indexMap = indexGeneratorMap;
        permFunc = permFunction;
        acceptedWords = accepted;
        prevSize = prevWordsSize;
        nextSize = nextWordsSize;
        compacted = false;

        assignmentMap = new AssignmentMap();
        clusterMap = new ClusterMap<SparseIntegerVector>(clusterGenerator);
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

        Iterator<String> it = IteratorFactory.tokenizeOrdered(document);

        // Skip empty documents.
        if (!it.hasNext())
            return;

        // Get the instance id as the first token.
        String instanceId = it.next();

        // Fill up the words after the context so that when the real processing
        // starts, the context is fully prepared.
        for (int i = 0 ; it.hasNext(); ++i) {
            String term = it.next();
            if (term.equals("||||"))
                break;
            prevWords.offer(term.intern());
        }

        String focusWord = it.next().intern();

        while (it.hasNext())
            nextWords.offer(it.next().intern());

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
        clusterNum = clusterMap.addVector(focusWord, meaning);
        assignmentMap.addInstance(focusWord, clusterNum, instanceId);
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
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        Set<String> terms = new TreeSet<String>(clusterMap.keySet());
        if (!compacted) {
            double mergeThreshold = Double.parseDouble(
                properties.getProperty(MERGE_THRESHOLD_PROPERTY, ".25"));

            splitSenses = new ConcurrentHashMap<String, Vector>();

            // Merge the clusters for each of the words being tracked.
            for (String term : terms) {
                HERMIT_LOGGER.info("Mering clusters for : " + term);
                Map<Integer, Integer> mergedMap =
                    clusterMap.finalizeClustering(term);
                for (Map.Entry<Integer, Integer> mapping :
                        mergedMap.entrySet())
                    assignmentMap.moveInstances(
                            term, mapping.getKey(), mapping.getValue());
                assignmentMap.emitResults(term);
            }

            // Extract the list of clusters for each word mapped in the cluster
            // map and save it in the semantic space map.  The first sense will
            // simply be stored as the mapped term and additional senses will
            // have -SENSE_NUM appended to the token.  After this is done, the
            // clusters will be removed from the map to clear up space.
            for (String term : terms) {
                List<List<SparseIntegerVector>> clusters =
                    clusterMap.getClusters(term);
                int i = 0;
                for (List<SparseIntegerVector> cluster : clusters) {
                    SparseIntegerVector sense = null;
                    for (SparseIntegerVector v : cluster) {
                        if (sense == null)
                            sense = (SparseIntegerVector) Vectors.copyOf(v);
                        else
                            VectorMath.add(sense, v);
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
            HERMIT_LOGGER.info("Emitting Accuracy Counts");
            compacted = true;
        }
    }

    public void setSemanticFilter(Set<String> wordsToProcess) {
        if (acceptedWords == null)
            acceptedWords = new HashSet<String>();

        acceptedWords.clear();
        acceptedWords.addAll(wordsToProcess);
    }

    private boolean acceptWord(String focusWord) {
        if (acceptedWords == null)
            return !focusWord.equals(EMPTY_TOKEN);
        return acceptedWords.contains(focusWord);
    }

    private void add(IntegerVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }

    private class AssignmentMap {

        Map<String, List<List<String>>> wordToSenseInstances;

        public AssignmentMap() {
            wordToSenseInstances = new HashMap<String, List<List<String>>>();
        }

        public synchronized void addInstance(String word,
                                             int sense,
                                             String instanceId) {
            List<List<String>> senseInstances = wordToSenseInstances.get(word);
            if (senseInstances == null) {
                senseInstances = new ArrayList<List<String>>();
                wordToSenseInstances.put(word, senseInstances);
            }
            for (int i = senseInstances.size(); i <= sense; ++i)
                senseInstances.add(new ArrayList<String>());
            senseInstances.get(sense).add(instanceId);
        }

        public void moveInstances(String word, int fromSense, int toSense) {
            List<List<String>> senseInstances = wordToSenseInstances.get(word);
            senseInstances.get(toSense).addAll(senseInstances.get(fromSense));
            senseInstances.get(fromSense).clear();
        }

        public void emitResults(String word) {
            List<List<String>> senseInstances = wordToSenseInstances.get(word);
            int senseNum = 0;
            for (List<String> instancesInSense : senseInstances) {
                if (instancesInSense.size() == 0)
                    continue;

                for (String instance : instancesInSense) {
                    String baseInstance = instance.substring(
                            0, instance.lastIndexOf("."));
                    StringBuilder sb = new StringBuilder();
                    sb.append(baseInstance).append(" ");
                    sb.append(instance).append(" ");
                    sb.append(baseInstance).append(".");
                    sb.append(Integer.toString(senseNum));
                    System.out.println(sb.toString());
                }
                senseNum++;
            }
        }
    }
}
