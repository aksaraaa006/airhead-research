/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.isa;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.ri.IndexVector;
import edu.ucla.sspace.ri.IndexVectorGenerator;
import edu.ucla.sspace.ri.PermutationFunction;
import edu.ucla.sspace.ri.RandomIndexVectorGenerator;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.SparseDoubleArray;

import java.io.BufferedReader;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.logging.Logger;

/**
 * An implementation of Incremental Semantic Analysis (ISA).  This
 * implementation is based on the following paper.  <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Baroni, A. Lenci and
 *    L. Onnis. 2007. ISA meets Lara: An incremental word space model for
 *    cognitively plausible simulations of semantic learning. Proceedings of the
 *    ACL 2007 Workshop on Cognitive Aspects of Computational Language
 *    Acquisition, East Stroudsburg PA: ACL. 49-56.  Available <a
 *    href="http://clic.cimec.unitn.it/marco/publications/acl2007/coglearningacl07.pdf">here</a>
 *    </li>
 *
 * </ul>
 *
 * <p> Due to the incremental nature of ISA, instance of this class are
 * <i>not</i> designed to be multi-threaded.  Documents must be processed
 * sequentially to properly compute the something.
 * 
 * @author David Jurgens
 */
public class IncrementalSemanticAnalysis implements SemanticSpace {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
	"edu.ucla.sspace.isa.IncrementalSemanticAnalysis";

    /**
     * The property to specify the decay rate for determing how much the history
     * (semantics) of a word will affect the semantics of co-occurring words.
     */
    public static final String HISTORY_DECAY_RATE_PROPERTY =
        PROPERTY_PREFIX + ".historyDecayRate";

    /**
     * The property to specify the impact rate of word co-occurrence.
     */
    public static final String IMPACT_RATE_PROPERTY =
        PROPERTY_PREFIX + ".impactRate";

    /**
     * The property to specify the {@link IndexVectorGenerator} class to use for
     * generating {@code IndexVector} instances.
     */
    public static final String INDEX_VECTOR_GENERATOR_PROPERTY = 
	PROPERTY_PREFIX + ".indexVectorGenerator";

    /**
     * The property to specify the fully qualified named of a {@link
     * PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
	PROPERTY_PREFIX + ".permutationFunction";

    /**
     * The property to specify whether the index vectors for co-occurrent words
     * should be permuted based on their relative position.
     */
    public static final String USE_PERMUTATIONS_PROPERTY = 
	PROPERTY_PREFIX + ".usePermutations";

    /**
     * The property to specify the number of dimensions to be used by the index
     * and semantic vectors.
     */
    public static final String VECTOR_LENGTH_PROPERTY = 
	PROPERTY_PREFIX + ".vectorLength";

    /**
     * The property to specify the number of words to view before and after each
     * word in focus.
     */
    public static final String WINDOW_SIZE_PROPERTY = 
	PROPERTY_PREFIX + ".windowSize";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space when words do not co-occur with many unique tokens, but
     * requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
	PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default rate at which the history (semantics) decays when affecting
     * other co-occurring word's semantics.
     */
    public static final double DEFAULT_HISTORY_DECAY_RATE = 100;

    /**
     * The default rate at which the co-occurrence of a word affects the
     * semantics.
     */
    public static final double DEFAULT_IMPACT_RATE = 0.003;

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 1800;

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 5; // +5/-5

    /**
     * The rate at which the increased frequency of a word decreases its effect
     * on the semantics of words with which it co-occurs.
     */
    private final double historyDecayRate;

    /**
     * The source of {@link IndexVector} instances.
     */
    private final IndexVectorGenerator indexVectorGenerator;

    /**
     * The degree to which the co-occurrence of a word affects the semantics of
     * a second word.
     */
    private final double impactRate;

    /**
     * If permutations are enabled, what permutation function to use on the
     * index vectors.
     */
    private final PermutationFunction permutationFunc;

    /**
     * Whether the index vectors for co-occurrent words should be permuted based
     * on their relative position.
     */
    private final boolean usePermutations;

    /**
     * A flag for whether this instance should use {@code SparseSemanticVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * How many words to view before and after each word in focus.
     */
    private final int windowSize;

    /**
     * A mapping from each word to its associated index vector
     */
    private final Map<String,IndexVector> wordToIndexVector;

    /**
     * A mapping from each word to the vector the represents its semantics
     */
    private final Map<String,SemanticVector> wordToMeaning;

    /**
     * A mapping from each word to the number of times it has occurred in the
     * corpus at the time of processing.  This mapping is incrementally updated
     * as documents are processed.
     */
    private final Map<String,Integer> wordToOccurrences;

    /**
     * Creates a new {@code IncrementalSemanticAnalysis} instance using the
     * current {@code System} properties for configuration.
     */
    public IncrementalSemanticAnalysis() {
        this(System.getProperties());
    }

    /**
     * Creates a new {@code IncrementalSemanticAnalysis} instance using the
     * provided properties for configuration.
     *
     * @param properties the properties that specify the configuration for this
     *        instance
     */
    public IncrementalSemanticAnalysis(Properties properties) {
	String vectorLengthProp = 
	    properties.getProperty(VECTOR_LENGTH_PROPERTY);
	vectorLength = (vectorLengthProp != null)
	    ? Integer.parseInt(vectorLengthProp)
	    : DEFAULT_VECTOR_LENGTH;

	String windowSizeProp = properties.getProperty(WINDOW_SIZE_PROPERTY);
	windowSize = (windowSizeProp != null)
	    ? Integer.parseInt(windowSizeProp)
	    : DEFAULT_WINDOW_SIZE;

	String usePermutationsProp = 
	    properties.getProperty(USE_PERMUTATIONS_PROPERTY);
	usePermutations = (usePermutationsProp != null)
	    ? Boolean.parseBoolean(usePermutationsProp)
	    : false;

	String permutationFuncProp =
	    properties.getProperty(PERMUTATION_FUNCTION_PROPERTY);
	permutationFunc = (permutationFuncProp != null)
	    ? loadPermutationFunction(permutationFuncProp)
	    : null; //new DefaultPermutationFunction;

	String ivgProp = 
	    properties.getProperty(INDEX_VECTOR_GENERATOR_PROPERTY);
	indexVectorGenerator = (ivgProp != null) 
	    ? loadIndexVectorGenerator(ivgProp, properties)
	    : new RandomIndexVectorGenerator(properties);

	String useSparseProp = 
	    properties.getProperty(USE_SPARSE_SEMANTICS_PROPERTY);
	useSparseSemantics = (useSparseProp != null)
	    ? Boolean.parseBoolean(useSparseProp)
	    : true;

        String decayRateProp = 
            properties.getProperty(HISTORY_DECAY_RATE_PROPERTY);
        historyDecayRate = (decayRateProp != null)
            ? Double.parseDouble(decayRateProp)
            : DEFAULT_HISTORY_DECAY_RATE;

        String impactRateProp =
            properties.getProperty(IMPACT_RATE_PROPERTY);
        impactRate = (impactRateProp != null)
            ? Double.parseDouble(impactRateProp)
            : DEFAULT_IMPACT_RATE;
        
	wordToIndexVector = new HashMap<String,IndexVector>();
	wordToMeaning = new HashMap<String,SemanticVector>();
        wordToOccurrences = new HashMap<String,Integer>();
	//semanticFilter = new HashSet<String>();
    }


    /**
     * Returns an instance of the the provided class name, that implements
     * {@code PermutationFunction}.
     *
     * @param className the fully qualified name of a class
     */ 
    private static PermutationFunction 
	    loadPermutationFunction(String className) {
	try {
	    Class clazz = Class.forName(className);
	    return (PermutationFunction)(clazz.newInstance());
	} catch (Exception e) {
	    // catch all of the exception and rethrow them as an error
	    throw new Error(e);
	}
    }

    @SuppressWarnings("unchecked") private static IndexVectorGenerator 
	    loadIndexVectorGenerator(String className, Properties properties) {
	try {
	    Class clazz = Class.forName(className);
	    Constructor c = clazz.getConstructor(Properties.class);
	    IndexVectorGenerator ivg = (IndexVectorGenerator)
		c.newInstance(new Object[] {properties});
	    return ivg;
	} catch (Exception e) {
	    // rethrow
	    throw new Error(e);
	}
    }

    /**
     * Removes all associations between word and semantics while still retaining
     * the word to index vector mapping.  This method can be used to re-use the
     * same mapping on multiple corpora while keeping the same semantic space.
     */
    public void clearSemantics() {
	wordToMeaning.clear();
    }

    /**
     * Returns the index vector for the provided word.
     *
     * @param the index vector for a word
     */
    private IndexVector getIndexVector(String word) {
	
	IndexVector v = wordToIndexVector.get(word);
	if (v == null) {
            v = indexVectorGenerator.create(vectorLength);
            wordToIndexVector.put(word, v);
        }
	return v;
    }  

    /**
     * Returns the current semantic vector for the provided word, or if the word
     * is not currently in the semantic space, a vector is added for it and
     * returned.
     *
     * @param word a word
     *
     * @return the {@code SemanticVector} for the provide word.
     */
    private SemanticVector getSemanticVector(String word) {

	SemanticVector v = wordToMeaning.get(word);
	if (v == null) {
            v = (useSparseSemantics) 
                ? new SparseSemanticVector()
                : new DenseSemanticVector();
            wordToMeaning.put(word, v);
        }
	return v;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "IncrementSemanticAnalysis-"
            + "-" + vectorLength + "v-" + windowSize + "w-" 
            + ((usePermutations) 
               ? permutationFunc.toString() 
               : "noPermutations");
    }
 
    /**
     * {@inheritDoc}
     */ 
    public double[] getVectorFor(String word) {
	SemanticVector v = wordToMeaning.get(word);
	if (v == null) {
	    return null;
	}
	double[] vec = v.toArray();
	return vec;
    }

    /**
     * {@inheritDoc}
     */ 
    public int getVectorSize() {
        return vectorLength;
    }

    /**
     * Returns an unmodifiable view on the token to {@link IndexVector} mapping
     * used by this instance.  Any further changes made by this instance to its
     * token to {@code IndexVector} mapping will be reflected in the return map.
     *
     * @return a mapping from the current set of tokens to the index vector used
     *         to represent them
     */
    public Map<String,IndexVector> getWordToIndexVector() {
	return Collections.unmodifiableMap(wordToIndexVector);
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(wordToMeaning.keySet());
    }
    
    /**
     * {@inheritDoc}  Note that this method is <i>not</i> thread safe.
     */
    public void processDocument(BufferedReader document) throws IOException {

	Queue<String> prevWords = new ArrayDeque<String>(windowSize);
	Queue<String> nextWords = new ArrayDeque<String>(windowSize);

	Iterator<String> documentTokens = 
	    IteratorFactory.tokenizeOrdered(document);

	String focusWord = null;

	// Prefetch the first windowSize words.  As soon as a word enters the
	// nextWords buffer increase its occurrence count.
	for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i) 
	    nextWords.offer(documentTokens.next());        
	
	while (!nextWords.isEmpty()) {
	    
	    focusWord = nextWords.remove();

	    // shift over the window to the next word
	    if (documentTokens.hasNext()) {
		String windowEdge = documentTokens.next(); 
		nextWords.offer(windowEdge);
	    }    
            
            // Don't bother calculating the semantics for empty tokens
            // (i.e. words that were filtered out)
	    if (!focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {

		SemanticVector focusMeaning = getSemanticVector(focusWord);

		// Sum up the index vector for all the surrounding words.  If
		// permutations are enabled, permute the index vector based on
		// its relative position to the focus word.
		int permutations = -(prevWords.size());		
		for (String word : prevWords) {
		    
		    // Skip the addition of any words that are excluded from the
		    // filter set.  Note that by doing the exclusion here, we
		    // ensure that the token stream maintains its existing
		    // ordering, which is necessary when permutations are taken
		    // into account.
		    if (focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {
			++permutations;
			continue;
		    }
		    
		    IndexVector iv = getIndexVector(word);
		    if (usePermutations) {
			iv = permutationFunc.permute(iv, permutations);
			++permutations;
		    }
                    
                    updateSemantics(focusMeaning, word, iv);
		}
		
		// Repeat for the words in the forward window.
		permutations = 1;
		for (String word : nextWords) {
		    
		    // Skip the addition of any words that are excluded from the
		    // filter set.  Note that by doing the exclusion here, we
		    // ensure that the token stream maintains its existing
		    // ordering, which is necessary when permutations are taken
		    // into account.
		    if (focusWord.equals(IteratorFactory.EMPTY_TOKEN)) {
			++permutations;
			continue;
		    }
		    
		    IndexVector iv = getIndexVector(word);
		    if (usePermutations) {
			iv = permutationFunc.permute(iv, permutations);
			++permutations;
		    }
		    
                    updateSemantics(focusMeaning, word, iv);
		}
	    }

	    // Last put this focus word in the prev words and shift off the
	    // front of the previous word window if it now contains more words
	    // than the maximum window size
	    prevWords.offer(focusWord);

            // Increment the frequency count for the word now that it has been
            // seen and processed.
            Integer count = wordToOccurrences.get(focusWord);
            wordToOccurrences.put(focusWord, (count == null) ? 1 : count + 1);

	    if (prevWords.size() > windowSize) {
		prevWords.remove();
	    }
	}	

	document.close();
    }
    
    /**
     * Does nothing, as ISA in an incremental algorithm.
     *
     * @properties {@inheritDoc}
     */
    public void processSpace(Properties properties) { }

    /**
     * Assigns the token to {@link IndexVector} mapping to be used by this
     * instance.  The contents of the map are copied, so any additions of new
     * index words by this instance will not be reflected in the parameter's
     * mapping.
     *
     * @param m a mapping from token to the {@code IndexVector} that should be
     *        used represent it when calculating other word's semantics
     */
    public void setWordToIndexVector(Map<String,IndexVector> m) {
	wordToIndexVector.clear();
	wordToIndexVector.putAll(m);
    }

    /**
     * {@inheritDoc} Note that all words will still have an index vector
     * assigned to them, which is necessary to properly compute the semantics.
     *
     * @param semanticsToRetain the set of words for which semantics should be
     *        computed.
     */
    /*
    public void setSemanticFilter(Set<String> semanticsToRetain) {
	semanticFilter.clear();
	semanticFilter.addAll(semanticsToRetain);
    }
    */

    /**
     * Update the semantics using the weighed combination of the semantics of
     * the co-occurring word and the provided index vector.  Note that the index
     * vector is provided so that the caller can permute it as necessary.
     *
     * @param toUpdate the semantics to be updated
     * @param cooccurringWord the word that is co-occurring 
     * @param iv the index vector for the co-occurring word, which has be
     *        permuted as necessary
     */    
    private void updateSemantics(SemanticVector toUpdate, String cooccurringWord,
                                 IndexVector iv) {

        SemanticVector prevWordSemantics = getSemanticVector(cooccurringWord);
        
        Integer occurrences = wordToOccurrences.get(cooccurringWord);
        if (occurrences == null)
            occurrences = 0;
        double semanticWeight = 
            1d / (Math.exp(occurrences / historyDecayRate));
                    
        // The meaning is updated as a combination of the index vector and the
        // semantics, which is weighted by how many times the co-occurring word
        // has been seen.  The semantics of frequently co-occurring words
        // receive less weight, i.e. the index vector is weighted more.
        toUpdate.add(iv, impactRate * (1 - semanticWeight));
        toUpdate.add(prevWordSemantics, impactRate * semanticWeight);
    }

    /**
     * An abstract for the vector that stores the semantics of a word.
     */
    interface SemanticVector {

	/**
	 * Adds the bits specified for the {@code IndexVector} to this
	 * semantic representation.
	 */
	void add(IndexVector v, double percentage);

	/**
	 * Adds the bits specified for the {@code SemanticVector} to this
	 * semantic representation.
	 */
	void add(SemanticVector v, double percentage);

	/**
	 * Returns the full vector representing these semantics.
	 */
	double[] toArray();
    }
    
    class SparseSemanticVector implements SemanticVector {
        
        private final SparseDoubleArray semantics;

        public SparseSemanticVector() {
            semantics = new SparseDoubleArray(vectorLength);
        }

        /**
	 * {@inheritDoc}
	 */
	public void add(IndexVector v, double percentage) {

	    for (int p : v.positiveDimensions())
		semantics.set(p, semantics.getPrimitive(p) + percentage);
			
	    for (int n : v.negativeDimensions()) 
                semantics.set(n, semantics.getPrimitive(n) - percentage);
        }

        /**
	 * {@inheritDoc}
	 */
	public void add(SemanticVector v, double percentage) {

            double[] vec = v.toArray();
            for (int i = 0; i < vec.length; ++i) 
                semantics.
                    set(i, semantics.getPrimitive(i) + percentage * vec[i]);
        }

        /**
         * {@inheritDoc}
         */
        public double[] toArray() {
            return semantics.toPrimitiveArray(new double[vectorLength]);
        }
    }
    
    class DenseSemanticVector implements SemanticVector {

        private final double[] semantics;

        public DenseSemanticVector() {
            semantics = new double[vectorLength];
        }

        /**
	 * {@inheritDoc}
	 */
	public void add(IndexVector v, double percentage) {

	    for (int p : v.positiveDimensions())
		semantics[p] += percentage;
			
	    for (int n : v.negativeDimensions()) 
                semantics[n] -= percentage;
        }

        /**
	 * {@inheritDoc}
	 */
	public void add(SemanticVector v, double percentage) {

            double[] vec = v.toArray();
            for (int i = 0; i < vec.length; ++i) 
                semantics[i] += percentage * vec[i];
        }

        /**
         * {@inheritDoc}
         */
        public double[] toArray() {
            return semantics;
        }
    }
}