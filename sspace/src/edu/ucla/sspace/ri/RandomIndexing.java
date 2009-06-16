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
import java.io.FileReader;
import java.io.IOException;
import java.io.IOError;

import java.lang.reflect.Constructor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import edu.ucla.sspace.common.IntegerMap;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Tuple;
import edu.ucla.sspace.common.WordFilter;
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
 * <p>
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
 * This class defines the following configurable properties that may be set
 * using either the System properties or using the {@link
 * RandomIndexing#RandomIndexing(Properties)} constructor.
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
 *      <i>Default:</i> {@link edu.ucla.sspace.ri.DefaultPermutationFunction 
 *      DefaultPermutationFunction} 
 *
 * <dd style="padding-top: .5em">This property specifies the fully qualified
 *      class name of a {@link PermutationFunction} instance that will be used
 *      to permute index vectors.  If the {@value #USE_PERMUTATIONS_PROPERTY} is
 *      set to {@code false}, the value of this property has no effect.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #INDEX_VECTOR_GENERATOR_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@link RandomIndexVectorGenerator} 
 *
 * <dd style="padding-top: .5em">This property specifies the source of {@link
 *       IndexVector} instances.  Users who want to provide more fine-grain
 *       control over the number of and distribution of values in the index
 *       vectors can provide their own {@link IndexVectorGenerator} instance by
 *       setting this value to the fully qualified class name.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #USE_SPARSE_SEMANTICS_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code true} 
 *
 * <dd style="padding-top: .5em">This property specifies whether to use a sparse
 *       encoding for each word's semantics.  Using a sparse encoding can result
 *       in a large saving in memory, while requiring more time to process each
 *       document.<p>
 *
 * <dt> <i>Property:</i> <code><b>{@value #WORD_FILTER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code null} 
 *
 * <dd style="padding-top: .5em">This property specifies the a list of files to
 *      be used with {@link WordFilter} instances.  The value is specified as
 *      <tt>filename[=<i>include</i> (default) |
 *      <i>exclude</i>][,filname...]</tt>, where each <tt>filename</tt> has an
 *      optional parameter to specify whether the words in the file should be
 *      used to filter out words not in the file (include), or to remove those
 *      words that are in the file (exlcude).  Multiple filter files may be
 *      appended using a ',' to separate them.  Note that filters are applied to
 *      the input token stream of {@link #processDocument(BufferedReader)
 *      processDocument} in order they are declared.<p>
 *
 * </dl> <p>
 *
 * This class is thread-safe for concurrent calls of {@link
 * #processDocument(BufferedReader) processDocument}.  At any given point in
 * processing, the {@link #getVectorFor(String) getVectorFor} method may be used
 * to access the current semantics of a word.  This allows callers to track
 * incremental changes to the semantics as the corpus is processed.  <p>
 *
 * The {@link #processSpace(Properties) processSpace} method does nothing for
 * this class and calls to it will not affect the results of {@code
 * getVectorFor}.
 *
 * @see PermutationFunction
 * @see IndexVector
 * @see IndexVectorGenerator
 * 
 * @author David Jurgens
 */
public class RandomIndexing implements SemanticSpace {

    public static final String RI_SSPACE_NAME =
    "random-indexing";

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
	PROPERTY_PREFIX + ".usePermutations";

    /**
     * The property to specify the fully qualified named of a {@link
     * PermutationFunction} if using permutations is enabled.
     */
    public static final String PERMUTATION_FUNCTION_PROPERTY = 
	PROPERTY_PREFIX + ".permutationFunction";

    /**
     * The property to specify the {@link IndexVectorGenerator} class to use for
     * generating {@code IndexVector} instances.
     */
    public static final String INDEX_VECTOR_GENERATOR_PROPERTY = 
	PROPERTY_PREFIX + ".indexVectorGenerator";

    /**
     * Specifies the {@link WordFilter} instances to apply to the tokenized
     * input stream before {@code processDocument} runs.
     */
    public static final String WORD_FILTER_PROPERTY = 
	PROPERTY_PREFIX + ".wordFilter";

    /**
     * Specifies whether to use a sparse encoding for each word's semantics,
     * which saves space but requires more computation.
     */
    public static final String USE_SPARSE_SEMANTICS_PROPERTY = 
	PROPERTY_PREFIX + ".sparseSemantics";

    /**
     * The default number of words to view before and after each word in focus.
     */
    public static final int DEFAULT_WINDOW_SIZE = 2; // +2/-2

    /**
     * The default number of dimensions to be used by the index and semantic
     * vectors.
     */
    public static final int DEFAULT_VECTOR_LENGTH = 4000;
    
    /**
     * A private source of randomization used for creating the index vectors.
     */
    // We use our own source rather than Math.random() to ensure reproduceable
    // behavior when a specific seed is set.
    //
    // NOTE: intentionally package-private to allow other RI-related classes to
    // based their randomness on a this class's seed.
    static final Random RANDOM = new Random();

    /**
     * A mapping from each word to its associated index vector
     */
    private final Map<String,IndexVector> wordToIndexVector;

    /**
     * A mapping from each word to the vector the represents its semantics
     */
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

    /**
     * The source of {@link IndexVector} instances.
     */
    private final IndexVectorGenerator indexVectorGenerator;

    /**
     * A list of files for the {@link WordFilter} instances that will be used to
     * tokenize the input documents.
     */
    private final List<Tuple<Set<String>,Boolean>> filterFiles;

    /**
     * A flag for whether this instance should use {@code SparseSemanticVector}
     * instances for representic a word's semantics, which saves space but
     * requires more computation.
     */
    private final boolean useSparseSemantics;

    /**
     * Creates a new {@code RandomIndexing} instance using the current {@code
     * System} properties for configuration.
     */
    public RandomIndexing() {
	this(System.getProperties());
    }

    /**
     * Creates a new {@code RandomIndexing} instance using the provided
     * properites for configuration.
     */
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

	String ivgProp = 
	    properties.getProperty(INDEX_VECTOR_GENERATOR_PROPERTY);
	indexVectorGenerator = (ivgProp != null) 
	    ? loadIndexVectorGenerator(ivgProp, properties)
	    : new RandomIndexVectorGenerator(properties);

	String filterProp = 
	    properties.getProperty(WORD_FILTER_PROPERTY);
	filterFiles = (filterProp != null)
	    ? loadFilterFiles(filterProp)
	    : new ArrayList<Tuple<Set<String>,Boolean>>(0);

	String useSparseProp = 
	    properties.getProperty(USE_SPARSE_SEMANTICS_PROPERTY);
	useSparseSemantics = (useSparseProp != null)
	    ? Boolean.parseBoolean(useSparseProp)
	    : true;

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
     * Loads words lists from files based on the provide value of the {@value
     * #WORD_FILTER_PROPERTY} property.
     */
    private static List<Tuple<Set<String>,Boolean>> 
	    loadFilterFiles(String property) {

	// multiple filter files are specified using a ',' to separate them
	String[] fileAndOptionalFlag = property.split(",");
	List<Tuple<Set<String>,Boolean>> filterFiles = 
	        new ArrayList<Tuple<Set<String>,Boolean>>(
	        fileAndOptionalFlag.length);
	for (String s : fileAndOptionalFlag) {
	    // If the words in the file are manually specified to be applied
	    // in a specific way, then the string will contain a '='.  Look
	    // for the last index of '=' in case the file name itself
	    // contains that character
	    int eqIndex = s.lastIndexOf('=');
	    String filename = null;
	    boolean exclude = false;
	    if (eqIndex > 0) {
		filename = s.substring(0, eqIndex);
		String flag = s.substring(eqIndex + 1);
		if (flag.equals("include"))
		    exclude = false;
		else if (flag.equals("exclude"))
		    exclude = true;
		else {
		    throw new IllegalArgumentException(
			"unknown filter parameter: " + s);
		}
	    }
	    else {
		filename = s;
	    }
	    
	    // load the words in the file
	    Set<String> words = new HashSet<String>();
	    try {
		BufferedReader br = 
		    new BufferedReader(new FileReader(filename));
		for (String line = null; (line = br.readLine()) != null; ) {
		    for (String token : line.split("\\s+")) {
			words.add(token);
		    }
		}
		br.close();
	    } catch (IOException ioe) {
		// rethrow since filter error is fatal to correct execution
		throw new IOError(ioe);
	    }

	    filterFiles.add(new Tuple<Set<String>,Boolean>(words, exclude));		
	}
	return filterFiles;
    }

    /**
     * Removes all associations between word and semantics while still retaining
     * the word to index vector mapping.  This method can be used to re-use the
     * same instance of a {@code RandomIndexing} on multiple corpora while
     * keeping the same semantic space.
     */
    public void clearSemantics() {
	wordToMeaning.clear();
    }

    /**
     * Returns the index vector for the provided word.
     */
    IndexVector getIndexVector(String word) {
	
	IndexVector v = wordToIndexVector.get(word);
	if (v == null) {
	    // lock on th word in case multiple threads attempt to add it at
	    // once
	    synchronized(word) {
		// recheck in case another thread added it while we were waiting
		// for the lock
		v = wordToIndexVector.get(word);
		if (v == null) {
		    v = indexVectorGenerator.create(vectorLength);
		    wordToIndexVector.put(word, v);
		}
	    }
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
	    synchronized(this) {
		// recheck in case another thread added it while we were waiting
		// for the lock
		v = wordToMeaning.get(word);
		if (v == null) {
		    v = (useSparseSemantics) 
			? new SparseSemanticVector()
			: new DenseSemanticVector();
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
    public String getSpaceName() {
      return RI_SSPACE_NAME + "-" + vectorLength + "v-" + windowSize + "w-" 
	  + ((usePermutations) 
	     ? permutationFunc.toString() 
	     : "noPermutations");
    }

    /**
     * {@inheritDoc}
     */ 
    public Set<String> getWords() {
	return Collections.unmodifiableSet(wordToMeaning.keySet());
    }

    public Map<String,IndexVector> getWordToIndexVector() {
	return Collections.unmodifiableMap(wordToIndexVector);
    }

    public void setWordToIndexVector(Map<String,IndexVector> m) {
	wordToIndexVector.clear();
	wordToIndexVector.putAll(m);
    }
    
    /**
     * Updates the semantic vectors based on the words in the document.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {

	Queue<String> prevWords = new ArrayDeque<String>(windowSize);
	Queue<String> nextWords = new ArrayDeque<String>(windowSize);

	Iterator<String> documentTokens = tokenize(document);
	//WordIterator documentTokens = new WordIterator(document);

	String focusWord = null;

	// prefetch the first windowSize words 
	for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i)
	    nextWords.offer(documentTokens.next());
	
	while (!nextWords.isEmpty()) {
	    
	    focusWord = nextWords.remove();
	    SemanticVector focusMeaning = getSemanticVector(focusWord);

	    // shift over the window to the next word
	    if (documentTokens.hasNext()) {
		// NB: we call .intern() on the string to ensure that we are
		// always dealing with the canonical copy of the word when
		// processing.  This ensures that any locks acquired for the
		// word will be on a single instance.
		String windowEdge = documentTokens.next(); //.intern();
		nextWords.offer(windowEdge);
	    }    

	    // Sum up the index vector for all the surrounding words.  If
	    // permutations are enabled, permute the index vector based on its
	    // relative position to the focus word.
	    int permutations = -(prevWords.size());
	    for (String word : prevWords) {
		IndexVector iv = getIndexVector(word);
		if (usePermutations) {
		    iv = permutationFunc.permute(iv, permutations);
		    ++permutations;
		}
		focusMeaning.add(iv);
	    }

	    // Repeat for the words in the forward window.
	    permutations = 1;
	    for (String word : nextWords) {
		IndexVector iv = getIndexVector(word);
		if (usePermutations) {
		    iv = permutationFunc.permute(iv, permutations);
		    ++permutations;
		}
		focusMeaning.add(iv);
	    }

	    // Last put this focus word in the prev words and shift off the
	    // front of the previous word window if it now contains more words
	    // than the maximum window size
	    prevWords.offer(focusWord);
	    if (prevWords.size() > windowSize) {
		prevWords.remove();
	    }
	}	

	document.close();
    }
    
    /**
     * Returns an iterator over all the tokens in the document, applying any
     * {@link WordFilter} instances as specified for the configuration of this
     * instance.
     */
    private Iterator<String> tokenize(BufferedReader document) {
	Iterator<String> tokens = new WordIterator(document);
	// apply the word filters in the order they were originally specified
	for (Tuple<Set<String>,Boolean> tuple : filterFiles) {
	    tokens = new WordFilter(tokens, tuple.x, tuple.y);
	}
	return tokens;
    }

    /**
     * Does nothing.
     *
     * @param properties {@inheritDoc}
     */
    public void processSpace(Properties properties) {
	
    }

    /**
     * A vector for storing the semantics of a word.
     */
    interface SemanticVector {

	/**
	 * Adds the bits specified for the {@code IndexVector} to this
	 * semantic representation.
	 */
	void add(IndexVector v);

	/**
	 * Returns the full vector representing these semantics.
	 */
	int[] getVector();
    }

    /**
     * A {@code SemanticVector} where all values are held in memory. <p>
     *
     * This class is thread-safe.
     */
    class DenseSemanticVector implements SemanticVector {

	private final int[] vector;

	public DenseSemanticVector() {
	    vector = new int[vectorLength];
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized void add(IndexVector v) {

	    for (int p : v.positiveDimensions())
		vector[p]++;
			
	    for (int n : v.negativeDimensions()) 
		vector[n]--;
	}

	
	/**
	 * {@inheritDoc}
	 */
	public synchronized int[] getVector() {
	    return vector;
	}
    }    

    /**
     * A {@code SemanticVector} instance that keeps only the non-zero values of
     * the semantics in memory, thereby saving space at the expense of time. <p>
     *
     * This class is thread-safe.
     */
    class SparseSemanticVector implements SemanticVector {

	private final Map<Integer,Integer> sparseArray;

	public SparseSemanticVector() {
	    sparseArray = new IntegerMap<Integer>();
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized void add(IndexVector v) {

	    for (int p : v.positiveDimensions()) {
		Integer count = sparseArray.get(p);
		sparseArray.put(p, (count == null) 
				? Integer.valueOf(1) 
				: Integer.valueOf(count.intValue() + 1));
	    }
		
	    for (int n : v.negativeDimensions()) {
		Integer count = sparseArray.get(n);
		sparseArray.put(n, (count == null) 
				? Integer.valueOf(1) 
				: Integer.valueOf(count.intValue + 1));		
	    }		
	}
	
	/**
	 * {@inheritDoc}
	 */
	public synchronized int[] getVector() {
	    int[] vector = new int[vectorLength];
	    for (Map.Entry<Integer,Integer> e : sparseArray.entrySet()) {
		vector[e.getKey()] = e.getValue();
	    }
	    return vector;
	}
    }    
}
