package edu.ucla.sspace.common;

import java.io.*;

import java.util.*;

/**
 * A test-only {@link SemanticSpace}, where all the semantic vectors must be
 * manually asssigned.
 */
public class DummySemanticSpace implements SemanticSpace {

    private final Map<String,double[]> wordToVector;
    
    public DummySemanticSpace() {
	wordToVector = new HashMap<String,double[]>();
    }
    
    /**
     * Does nothing
     */
    public void processDocument(BufferedReader document) throws IOException { }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return wordToVector.keySet();
    }

    /**
     * Returns the manually assigned vector for the word
     */
    public double[] getVectorFor(String word) {
	return wordToVector.get(word);
    }

    /**
     * Sets the vector for the word
     */
    public double[] setVectorFor(String word, double[] vector) {
	return wordToVector.put(word, vector);
    }

    /**
     * Does nothing
     */
    public void processSpace(Properties properties) { }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
	return "DummySemanticSpace";
    }

}