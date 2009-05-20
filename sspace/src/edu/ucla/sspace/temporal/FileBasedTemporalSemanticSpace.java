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
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;

/**
 * A {@link TemporalSemanticSpace} which performs no processing of documents, but simply
 * reads in a text file produced by another semantic space and converts this
 * into a {@link OnDiskMatrix}.  The input format of the file should be that
 * which is produced by {@link edu.ucla.sspace.common.TemporalSemanticSpaceUtils}.
 */
public class FileBasedTemporalSemanticSpace implements TemporalSemanticSpace {

    /**
     * The {@code Matrix} which contains the data read from a finished {@link
     * TemporalSemanticSpace}.
     */
    private final Matrix wordSpace;

    /**
     * A mapping of terms to row indexes.  Also serves as a quick means of
     * retrieving the words known by this {@link TemporalSemanticSpace}.
     */
    private final Map<String, Integer> termToIndex ;

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

	spaceName = file.getName();

	// NOTE: Use a LinkedHashMap here because this will ensure that the words
	// are returned in the same row-order as the matrix.  This generates better
	// disk I/O behavior for accessing the matrix since each word is directly
	// after the previous on disk.
	termToIndex = new LinkedHashMap<String, Integer>();
	Matrix m = null;
	try {
	    switch (format) {
	    case TEXT:
		m = Matrices.synchronizedMatrix(loadText(file));
		break;
	    case BINARY:
		m = Matrices.synchronizedMatrix(loadBinary(file));
		break;
	    }
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}  
	wordSpace = m;
    }

    /**
     * Loads the {@link TemporalSemanticSpace} from the text formatted file,
     * adding its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private Matrix loadText(File sspaceFile) throws IOException {
	Matrix matrix = null;

// 	BufferedReader br = new BufferedReader(new FileReader(sspaceFile));
// 	String line = br.readLine();
// 	if (line == null)
// 	    throw new IOError(new Throwable("An empty file has been passed in"));
// 	String[] dimensions = line.split("\\s");
// 	int rows = Integer.parseInt(dimensions[0]);
// 	int columns = Integer.parseInt(dimensions[1]);
// 	int index = 0;
	
// 	// reusable array for writing rows into the matrix
// 	double[] row = new double[columns];
	
// 	matrix = Matrices.create(rows, columns, true);
// 	while ((line = br.readLine()) != null) {
// 	    String[] termVectorPair = line.split("\\|");
// 	    String[] values = termVectorPair[1].split("\\s");
// 	    termToIndex.put(termVectorPair[0], index);
// 	    if (values.length != columns) {
// 		throw new IOError(
// 		    new Throwable("improperly formated semantic space file"));	    
// 	    }
// 	    for (int c = 0; c < columns; ++c) {
// 		double d = Double.parseDouble(values[c]);
// 		row[c] = d;
// 		// matrix.set(index, c, d);
// 	    }
// 	    matrix.setRow(index, row);
// 	    index++;
// 	}
	return matrix;    
    }

    /**
     * Loads the {@link TemporalSemanticSpace} from the binary formatted file,
     * adding its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private Matrix loadBinary(File sspaceFile) throws IOException {
// 	DataInputStream dis = 
// 	    new DataInputStream(new FileInputStream(sspaceFile));
// 	int rows = dis.readInt();
// 	int cols = dis.readInt();
// 	// create a dense matrix
// 	Matrix m = Matrices.create(rows, cols, true);
// 	double[] d = new double[cols];
// 	for (int row = 0; row < rows; ++row) {
// 	    String word = dis.readUTF();
// 	    termToIndex.put(word, row);
// 	    for (int col = 0; col < cols; ++col) {
// 		d[col] = dis.readDouble();
// 	    }
// 	    m.setRow(row, d);
// 	}
// 	return m;
	return null;
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

    public SortedSet<Long> getTimeSteps() {
	return null;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(termToIndex.keySet());
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
      return null;
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

}
