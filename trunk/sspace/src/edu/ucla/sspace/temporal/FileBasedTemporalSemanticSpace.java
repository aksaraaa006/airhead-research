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

package edu.ucla.sspace.temporal;

import edu.ucla.sspace.common.IntegerMap;
import edu.ucla.sspace.common.Matrices;
import edu.ucla.sspace.common.Matrix;

import edu.ucla.sspace.temporal.TemporalSemanticSpaceUtils.SSpaceFormat;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;


/**
 * A {@link TemporalSemanticSpace} which performs no processing of documents, but simply
 * reads in a text file produced by another semantic space and converts this
 * into a {@link OnDiskMatrix}.  The input format of the file should be that
 * which is produced by {@link edu.ucla.sspace.common.TemporalSemanticSpaceUtils}.
 */
public class FileBasedTemporalSemanticSpace implements TemporalSemanticSpace {

    /**
     * A mapping of terms to row indexes.  Also serves as a quick means of
     * retrieving the words known by this {@link TemporalSemanticSpace}.
     */
    private final Map<String,SemanticVector> termToSemantics;
    
    /**
     * The name of this semantic space.
     */
    private final String spaceName;

    /**
     * Creates the {@link FileBasedTemporalSemanticSpace} from the file using the {@link
     * SSpaceFormat#TEXT text} format.
     *
     * @param filename filename of the data intended be provided by this
     *   {@link edu.ucla.sspace.common.TemporalSemanticSpace}.
     */
    public FileBasedTemporalSemanticSpace(String filename) {
	this(new File(filename), SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link FileBasedTemporalSemanticSpace} from the provided file in the
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.TemporalSemanticSpace}.
     */
    public FileBasedTemporalSemanticSpace(File file) {
	this(file, SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link FileBasedTemporalSemanticSpace} from the provided file in
     * the specified format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.TemporalSemanticSpace}.
     */
    public FileBasedTemporalSemanticSpace(File file, SSpaceFormat format) {

	// NOTE: Use a LinkedHashMap here because this will ensure that the words
	// are returned in the same row-order as the matrix.  This generates better
	// disk I/O behavior for accessing the matrix since each word is directly
	// after the previous on disk.
// 	termToIndex = new LinkedHashMap<String, Integer>();
// 	Matrix m = null;
// 	try {
// 	    switch (format) {
// 	    case TEXT:
// 		m = Matrices.synchronizedMatrix(loadText(file));
// 		break;
// 	    case BINARY:
// 		m = Matrices.synchronizedMatrix(loadBinary(file));
// 		break;
// 	    }
// 	} catch (IOException ioe) {
// 	    throw new IOError(ioe);
// 	}  
// 	wordSpace = m;
	termToSemantics = null;
	spaceName = file.getName();
    }

    /**
     * Loads the {@link TemporalSemanticSpace} from the text formatted file,
     * adding its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private static Map<String,SemanticVector> loadText(File sspaceFile) 
	    throws IOException {
	
	BufferedReader br = new BufferedReader(new FileReader(sspaceFile));
	String[] header = br.readLine().split("\\s+");
	int words = Integer.parseInt(header[0]);
	int dimensions = Integer.parseInt(header[1]);
	
	Map<String,SemanticVector> wordToSemantics = 
	    new HashMap<String,SemanticVector>(words, 2f);
	
	// read in each word
	for (String line = null; (line = br.readLine()) != null; ) {

	    String[] wordAndSemantics = line.split("\\|");
	    String word = wordAndSemantics[0];
	    SemanticVector semantics = new SemanticVector(dimensions);

	    for (int i = 1; i < wordAndSemantics.length; ++i) {

		String[] timeStepAndValues = wordAndSemantics[i].split(" ");
		long timeStep = Long.parseLong(timeStepAndValues[0]);

		// Load that time step's vector.  Note that we make the assumption
		// here that even though the T-Space is serialized in a dense
		// format, that the vector data is actually sparse, and so it will
		// be more efficient to store it as such.
		Map<Integer,Double> sparseArray = new IntegerMap<Double>();

		for (int j = 1; j < timeStepAndValues.length; ++j) {
		    sparseArray.put(Integer.valueOf(j-1), 
				    Double.valueOf(timeStepAndValues[j]));
		}
		semantics.setSemantics(timeStep, sparseArray);
	    }
	    wordToSemantics.put(word,semantics);
	}
	
	return wordToSemantics;
    }

    private static Map<String,SemanticVector> loadSparseText(File sspaceFile) 
	    throws IOException {

	
	return null;
    }


    /**
     * Loads the {@link TemporalSemanticSpace} from the binary formatted file,
     * adding its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private static Map<String,SemanticVector> loadBinary(File sspaceFile) 
	    throws IOException {
 	DataInputStream dis = 
 	    new DataInputStream(new FileInputStream(sspaceFile));
 	int words = dis.readInt();
 	int dimensions = dis.readInt();

	// initialize to the number of words, but keep the loading factor high
	// to reduce empty table space
	Map<String,SemanticVector> wordToSemantics = 
	    new HashMap<String,SemanticVector>(words, 2f);

 	for (int wordIndex = 0; wordIndex < words; ++wordIndex) {

 	    String word = dis.readUTF();
	    int timeSteps = dis.readInt();
	    SemanticVector vector = new SemanticVector(dimensions);
	    wordToSemantics.put(word, vector);

	    // Load that time step's vector.  Note that we make the assumption
	    // here that even though the T-Space is serialized in a dense
	    // format, that the vector data is actually sparse, and so it will
	    // be more efficient to store it as such.
	    Map<Integer,Double> semantics = new IntegerMap<Double>();

	    // read in each time step
 	    for (int tsIndex = 0; tsIndex < timeSteps; ++tsIndex) {
		long timeStep = dis.readLong();

		// load that time step's vector
		for (int i = 0; i < dimensions; ++i) {
		    int index = dis.readInt();
		    double val = dis.readDouble();
		    semantics.put(Integer.valueOf(index), Double.valueOf(val));
		}
		// associate the time step with the semantics
		vector.setSemantics(timeStep, semantics);
	    }
	}
	return wordToSemantics;
    }
    
    private static Map<String,SemanticVector> loadSparseBinary(File sspaceFile) 
	    throws IOException {
	
 	DataInputStream dis = 
 	    new DataInputStream(new FileInputStream(sspaceFile));
 	int words = dis.readInt();
 	int dimensions = dis.readInt();

	// initialize to the number of words, but keep the loading factor high
	// to reduce empty table space
	Map<String,SemanticVector> wordToSemantics = 
	    new HashMap<String,SemanticVector>(words, 2f);

 	for (int wordIndex = 0; wordIndex < words; ++wordIndex) {

 	    String word = dis.readUTF();
	    int timeSteps = dis.readInt();
	    SemanticVector vector = new SemanticVector(dimensions);
	    wordToSemantics.put(word, vector);
	    
	    // read in each time step
 	    for (int tsIndex = 0; tsIndex < timeSteps; ++tsIndex) {
		long timeStep = dis.readLong();
		int nonZero = dis.readInt();

		// load that time step's vector
		Map<Integer,Double> semantics = new IntegerMap<Double>();
		for (int i = 0; i < nonZero; ++i) {
		    int index = dis.readInt();
		    double val = dis.readDouble();
		    semantics.put(Integer.valueOf(index), Double.valueOf(val));
		}
		// associate the time step with the semantics
		vector.setSemantics(timeStep, semantics);
	    }
	}
	
	return wordToSemantics;
    }
    
    public double[] getVectorAfter(String word, long startTime) {
	return null;
    }

    public double[] getVectorBefore(String word, long endTime) {
	return null;
    }

    public double[] getVectorBetween(String word, long start, long endTime) {
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public SortedSet<Long> getTimeSteps() {
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public SortedSet<Long> getTimeSteps(String word) {
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(termToSemantics.keySet());
    }
  
    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String term) {
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
      return spaceName;
    }

    /**
     * A noop.
     */
    public void processDocument(BufferedReader document) { }

    /**
     * A noop.
     */
    public void processDocument(BufferedReader document, long time) { }

    /**
     * A noop.
     */
    public void processSpace(Properties props) { }

    /**
     * A class that contains the vectors that constitute the temporal semantic
     * for a word.
     */
    private static class SemanticVector {

	private final NavigableMap<Long,Map<Integer,Double>> 
	    timeStampToSemantics;

	private final int dimensions;

	public SemanticVector(int dimensions) {

	    this.dimensions = dimensions;
	    timeStampToSemantics = new TreeMap<Long,Map<Integer,Double>>();
	}

	/**
	 * Set the provided {@code double} array as the semantics of this word
	 * at the specified time step.
	 *
	 * @param v the index vector of a word that co-occurred
	 * @param timestamp the time at which the co-occurrence happened
	 */
	public void setSemantics(long timestamp, double[] semantics) {
	    Long t = Long.valueOf(timestamp);
	    Map<Integer,Double> sparseArr = new IntegerMap<Double>();
	    for (int i = 0; i < semantics.length; ++i) {
		double d = semantics[i];
		if (d != 0d) {
		    sparseArr.put(i, Double.valueOf(d));
		}
	    }
	    timeStampToSemantics.put(t, sparseArr);
	}

	/**
	 * Set the provided sparse encoding of an array (mapping from index to
	 * value) as the semantics of this word at the specified time step.
	 *
	 * @param v the index vector of a word that co-occurred
	 * @param timestamp the time at which the co-occurrence happened
	 */
	public void setSemantics(long timestamp, Map<Integer,Double> semantics) {
	    timeStampToSemantics.put(Long.valueOf(timestamp), semantics);
	}

	/**
	 * Returns the summed array of all the {@link IndexVector} instances in
	 * the provided {@code Map}.
	 */
	private double[] computeSemantics(
	        Map<Long,Map<Integer,Double>> timespan) {

	    double[] semantics = new double[dimensions];
 	    for (Map<Integer,Double> vectors : timespan.values()) {
		
		// add up all of the semantics for this time step's semantics
		for (Map.Entry<Integer,Double> e : vectors.entrySet()) {
		    semantics[e.getKey().intValue()] += e.getValue().intValue();
		}
	    }
	    return semantics;
	}

	/**
	 * Returns the time of the last co-occcurrence for the semantics of
	 * this word.
	 */
	public long getEndTime() {
	    return timeStampToSemantics.lastKey();
	}

	/**
	 * Returns the time of the first co-occcurrence for the semantics of
	 * this word.
	 */
	public long getStartTime() {
	    return timeStampToSemantics.firstKey();
	}

	/**
	 * Returns the time steps at which this word occurred.
	 */
	public SortedSet<Long> getTimeSteps() {
	    return Collections.unmodifiableSortedSet(
		timeStampToSemantics.navigableKeySet());
	}

	/**
	 * Returns the semantics for this vector for all co-occurrence instances
	 * regardless of when they happened.
	 */
	public double[] getVector() {
	    return computeSemantics(timeStampToSemantics);
	}

	/**
	 * Returns the semantics of the vector for all co-occurrences that
	 * happened after the timestamp.
	 */
	public double[] getVectorAfter(long start) {
	    SortedMap<Long,Map<Integer,Double>> timespan = 
		timeStampToSemantics.tailMap(start);	    
	    return computeSemantics(timespan);
	}

	/**
	 * Returns the semantics of the vector for all co-occurrences that
	 * happened before the timestamp.
	 */
	public double[] getVectorBefore(long end) {
	    SortedMap<Long,Map<Integer,Double>> timespan = 
		timeStampToSemantics.headMap(end);	    
	    return computeSemantics(timespan);
	}

	/**
	 * Returns the semantics of the vector for all co-occurrences that
	 * happened on or after the start timestamp but before the ending
	 * timestamp.
	 */
	public double[] getVectorBetween(long start, long end) {
	    SortedMap<Long,Map<Integer,Double>> timespan = 
		timeStampToSemantics.subMap(start, end);
	    return computeSemantics(timespan);
	}
    }


}
