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

package edu.ucla.sspace.ri;

import java.io.BufferedReader;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import edu.ucla.sspace.common.WordIterator;

/**
 * A co-occurrence based approach to statistical semantics.  This implementation
 * is based on three papers:
 * <ul>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, "Vector-based
 *     semantic analysis: Representing word meanings based on random labels," in
 *     <i>Proceedings of the ESSLLI 2001 Workshop on Semantic Knowledge
 *     Acquisition and Categorisation</i>, Helsinki, Finland, 2001.</li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, "An
 *     introduction to random indexing," in <i>Proceedings of the Methods and
 *     Applicatons of Semantic Indexing Workshop at the 7th International
 *     Conference on Terminology and Knowledge Engineering</i>, 2005.</li>
 *
 *   <li style="font-family:Garamond, Georgia, serif">M. Sahlgren, A. Holst, and
 *     P. Kanerva, "Permutations as a means to encode order in word space," in
 *     <i>Proceedings of the 30th Annual Meeting of the Cognitive Science
 *     Society (CogSci’08)</i>, 2008.</li>
 *
 * </ul>
 *
 * Random Indexing (RI) is an efficient way of capturing word co-occurence.  In
 * most co-occurence models, a word-by-word matrix is constructed, where the
 * values denote how many times the columns's word occurred in the context of
 * the row's word.  RI instead represents co-occurrence through index vectors.
 * Each word is assigned a high-dimensional, random vector that is known as its
 * index vector.  These index vectors are very sparse - typically 7 &plusmn; 2
 * non zero bits for a vector of length 2048, which ensures that the the chance
 * of any two arbitrary index vectors having an overlapping meaning (i.e. a
 * cosine similarity that is non-zero) is very low.  Word semantics are
 * calculated for each word by keeping a running sum of all of the index vectors
 * for the words that co-occur.
 *
 * <p>
 *
 * <span style="font-family:Garamond, Georgia, serif">Sahlgren et
 * al. (2008)</span> introduced another variation on RI, where the semantics
 * also capture word order by using a permutation function.  For each occurrence
 * of a word, rather than summing the index vectors of the co-occurring words,
 * the permutation function is used to transform the co-occurring words based on
 * their position.  For example, consider the sentece, "the quick brown fox
 * jumps over the lazy dog."  With a window-size of 2, the semantic vector for
 * "fox" is added with the values &Pi;<sup>-2</sup>(quick<sub>index</sub>) +
 * &Pi;<sup>-1</sup>(brown<sub>index</sub>) +
 * &Pi;<sup>1</sup>(jumps<sub>index</sub>) +
 * &Pi;<sup>2</sup>(over<sub>index</sub>), where &Pi;<sup>{@code k}</sup>
 * denotes the {@code k}<sup>th</sup> permutation of the specified index vector.
 *
 * <p>
 *
 * This class provides four paramaters
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #WINDOW_SIZE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_WINDOW_SIZE}
 *
 * <dd style="padding-top: .5em">This variable sets the number of words before
 *      and after that are counted as co-occurring.  With the default value,
 *      {@code 5} words are counted before and {@code 5} words are counter
 *      after.  This class always uses a symmetric window. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #VECTOR_LENGTH_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@value #DEFAULT_VECTOR_LENGTH}
 *
 * <dd style="padding-top: .5em">This variable sets the number of dimensions to
 *      be used for the index and semantic vectors. <p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #USE_PERMUTATIONS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code false}
 *
 * <dd style="padding-top: .5em">This property specifies whether to enable
 *      permuting the index vectors of co-occurring words.  Enabling this option
 *      will cause the word semantics to include word-ordering information.
 *      However this option is best used with a larger corpus.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #PERMUTATION_FUNCTION_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link DefaultPermutationFunction edu.ucla.sspace.ri.DefaultPermutationFunction} 
 *
 * <dd style="padding-top: .5em">This property specifies the fully qualified
 *      class name of a {@link PermutationFunction} instance that will be used
 *      to permute index vectors.  If the {@value #USE_PERMUTATIONS_PROPERTY} is
 *      set to {@code false}, the value of this property has no effect.<p>
 *
 * </dl> <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  Once {@link
 * #processSpace(Properties) processSpace} has been called, no further calls to
 * {@code processDocument} should be made.
 * 
 * This class <i>does</i> allow calls to {@link #getSemanticVector
 *
 * @see PermutationFunction
 * @see IndexVector
 * 
 * @author David Jurgens
 */
public class RandomIndexing { //implements SemanticSpace {

    /**
     * The prefix for naming public properties.
     */
    private static final String PROPERTY_PREFIX = 
	"edu.ucla.sspace.ri.RandomIndexing";

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
     * The property to specify whether the index vectors for co-occurrent words
     * should be permuted based on their relative position.
     */
    public static final String USE_PERMUTATIONS_PROPERTY = 
	PROPERTY_PREFIX + ".windowSize";

    /**
     * The property to specify the fully qualified named of a {@link
     * PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
	PROPERTY_PREFIX + ".permutationFunction";

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 5; // +5/-5

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 2048;
    
    /**
     * A private source of randomization used for creating the index vectors.
     */
    // We use our own source rather than Math.random() to ensure reproduceable
    // behavior when a specific seed is set.
    //
    // NOTE: intentionally package-private to allow other RI-related classes to
    // based their randomness on a this class's seed.
    static final Random RANDOM = new Random();

    private final Map<String,IndexVector> wordToIndexVector;

    private final Map<String,SemanticVector> wordToMeaning;

    /**
     * The number of dimensions for the semantic and index vectors.
     */
    private final int vectorLength;

    /**
     * How many words to view before and after each word in focus.
     */
    private final int windowSize;

    /**
     * Whether the index vectors for co-occurrent words should be permuted based
     * on their relative position.
     */
    private final boolean usePermutations;

    /**
     * If permutations are enabled, what permutation function to use on the
     * index vectors.
     */
    private final PermutationFunction permutationFunc;

    public RandomIndexing(Properties properties) {

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

	wordToIndexVector = new ConcurrentHashMap<String,IndexVector>();
	wordToMeaning = new ConcurrentHashMap<String,SemanticVector>();
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
	} catch (Exception ex) {
	    // catch all of the exception and rethrow them as an error
	    throw new Error(ex);
	}
    }

    /**
     * Returns the index vector for the provided word.
     */
    private IndexVector getIndexVector(String word) {

	IndexVector v = wordToIndexVector.get(word);
	if (v == null) {
	    // lock on th word in case multiple threads attempt to add it at
	    // once
	    synchronized(word) {
		// recheck in case another thread added it while we were waiting
		// for the lock
		v = wordToIndexVector.get(word);
		if (v == null) {
		    v = new IndexVector(vectorLength);
		    wordToIndexVector.put(word, v);
		}
	    }
	}
	return v;
    }  

    /**
     * {@inheritDoc}
     */
    public SemanticVector getSemanticVector(String word) {
	// ensure we are using the canonical copy of the word by interning it
	// 
	// NOTE: currently disabled.  We have to ensure that only interned
	// strings make it to this point.
	// 
	// word = word.intern();

	SemanticVector v = wordToMeaning.get(word);
	if (v == null) {
	    // lock on the word in case multiple threads attempt to add it at
	    // once
	    synchronized(word) {
		// recheck in case another thread added it while we were waiting
		// for the lock
		v = wordToMeaning.get(word);
		if (v == null) {
		    v = new SemanticVector(vectorLength);
		    wordToMeaning.put(word, v);
		}
	    }
	}
	return v;
    }

    /**
     * {@inheritDoc}
     */ 
    public double[] getVectorFor(String word) {
	SemanticVector v = wordToMeaning.get(word);
	if (v == null) {
	    return null;
	}
	int[] vec = v.getVector();
	double[] dVec = new double[vec.length];
	for (int i = 0; i < vec.length; ++i) {
	    dVec[i] = vec[i];
	}
	return dVec;
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
	return Collections.unmodifiableSet(wordToIndexVector.keySet());
    }

    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) {

	Queue<String> prevWords = new LinkedList<String>();
	Queue<String> nextWords = new LinkedList<String>();

	WordIterator it = new WordIterator(document);

	String focusWord = null;

	// prefetch the first windowSize words 
	for (int i = 0; i < windowSize && it.hasNext(); ++i)
	    nextWords.offer(it.next());
	
	while (!nextWords.isEmpty()) {
	    
	    focusWord = nextWords.remove();
	    SemanticVector focusMeaning = getSemanticVector(focusWord);

	    // shift over the window to the next word
	    if (it.hasNext()) {
		String windowEdge = it.next();
		nextWords.offer(windowEdge);
	    }    

	    // Sum up the index vector for all the surrounding words.  Note that
	    // these strings are necessarily interned when they are added to the
	    // nextWords Queue.
	    for (String word : prevWords) 
		focusMeaning.add(getIndexVector(word));

	    for (String word : nextWords) 
		focusMeaning.add(getIndexVector(word));

	    // last put, this focus word in the prev words and shift off the
	    // front if it is larger than the window
	    prevWords.offer(focusWord);
	    if (prevWords.size() > windowSize)
		prevWords.remove();
	}	
    }
    
    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
	
    }

    public class SemanticVector {

	private int[] vector;

	public SemanticVector(int length) {
	    vector = new int[length];
	}
	
	public void add(IndexVector v) {

	    for (int p : v.positiveDimensions()) 
		vector[p]++;
		
	    for (int n : v.negativeDimensions()) 
		vector[n]--;
	}

	
	public int[] getVector() {
	    return vector;
	}
    }    
}