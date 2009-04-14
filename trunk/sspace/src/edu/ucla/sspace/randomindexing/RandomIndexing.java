package edu.ucla.sspace.randomindexing;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.Random;
import java.util.Set;
import java.util.StringTokenizer;


/**
 *
 */
public class RandomIndexing {

    //
    // IMPLEMENTATION NOTES:
    //
    // 1. All word strings should be the canonical copy acquired from
    //    String.intern().  This reduces the memory footprint dramatically by
    //    eliminate duplicate strings in heap.
    //
    // 2. Because we are using the canonical copy of string, we use the
    //    IndentityHashMap implementation of Map, which gives better performance
    //    than HashMap.

    private static final int WINDOW_SIZE = 10; // +5/-5

    private static final int DEFAULT_VECTOR_LENGTH = 2048;

    /**
     * A private source of randomization used for creating the index vectors.
     */
    // We use our own source rather than Math.random() to ensure reproduceable
    // behavior when a specific seed is set.
    private static final Random RANDOM = new Random();

    private final Map<String,IndexVector> wordToIndexVector;

    private final Map<String,SemanticVector> wordToMeaning;

    private final int vectorLength;

    public RandomIndexing() {
	this(DEFAULT_VECTOR_LENGTH);
    }

    public RandomIndexing(int vectorLength) {
	this.vectorLength = vectorLength;
	wordToIndexVector = new IdentityHashMap<String,IndexVector>();
	wordToMeaning = new IdentityHashMap<String,SemanticVector>();
    }

    public Set<String> getWords() {
	return wordToMeaning.keySet();
    }

    public void processText(String document) {
	Queue<String> prevWords = new LinkedList<String>();
	Queue<String> nextWords = new LinkedList<String>();
	StringTokenizer st = new StringTokenizer(document);
	String focusWord = null;

	// prefetch the first (window_size / 2) words 
	for (int i = 0; i < WINDOW_SIZE / 2 && st.hasMoreTokens(); ++i)
	    nextWords.offer(st.nextToken().intern());
	
	while (!nextWords.isEmpty()) {
	    
	    focusWord = nextWords.remove();
	    SemanticVector focusMeaning = getSemanticVector(focusWord);

	    // shift over the window to the next word
	    if (st.hasMoreTokens()) {
		String windowEdge = st.nextToken().intern();
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
	    if (prevWords.size() > WINDOW_SIZE / 2)
		prevWords.remove();
	}	
    }
    

    // package protected
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

    private IndexVector getIndexVector(String word) {
	// ensure we are using the canonical copy of the word by interning it
	// 
	// NOTE: currently disabled.  We have to ensure that only interned
	// strings make it to this point.
	// 
	// word = word.intern();

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

    
    private class IndexVector {

	private static final int BITS_TO_SET = 9; // +/- 3
	private static final int BIT_VARIANCE = 3;

	int[] positive;
	int[] negative;

	private final int length;

	public IndexVector(int length) {
	    HashSet<Integer> pos = new HashSet<Integer>();
	    HashSet<Integer> neg = new HashSet<Integer>();
	    this.length = length;

	    // randomly set bits
	    int bitsToSet = BITS_TO_SET +
		(int)(BIT_VARIANCE * ((RANDOM.nextDouble() > .5) ? 1 : -1));
	    for (int i = 0; i < bitsToSet; ++i) {

		boolean picked = false;
		// loop to ensure we actually pick the full number of bits
		while (!picked) {
		    // pick some random index
		    int index = (int)(RANDOM.nextDouble() * length);
		    
		    // check that we haven't already added this index
		    if (pos.contains(index) || neg.contains(index))
			continue;
		    
		    // decide positive or negative
		    ((RANDOM.nextDouble() > .5) ? pos : neg).add(index);
		    picked = true;
		}
	    }
	    
	    positive = new int[pos.size()];
	    negative = new int[neg.size()];

	    Iterator<Integer> it = pos.iterator();
	    for (int i = 0; i < positive.length; ++i) 
		positive[i] = it.next();

	    it = neg.iterator();
	    for (int i = 0; i < negative.length; ++i) 
		negative[i] = it.next();		

	    // sort so we can use a binary search in getValue()
	    Arrays.sort(positive);
	    Arrays.sort(negative);
	}

	public int getValue(int index) {
	    if (Arrays.binarySearch(positive,index) >= 0)
		return 1;
	    else return (Arrays.binarySearch(negative,index) >= 0) ? -1 : 0;
	}
	
	public int length() {
	    return length;
	}

	public int[] negativeDimensions() {
	    return negative;
	}

	public int[] positiveDimensions() {
	    return positive;
	}
    }
}