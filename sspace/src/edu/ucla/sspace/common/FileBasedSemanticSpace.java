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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import edu.ucla.sspace.util.IntegerMap;

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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * An unmodifiable {@link SemanticSpace} whose data is loaded into memory from
 * an {@code .sspace} file.  Instance of this class perform no document
 * processing, and the {@code processDocument} and {@code processSpace} methods
 * do nothing.  The input file format should be one of formats produced by
 * {@link edu.ucla.sspace.common.SemanticSpaceUtils}.<p>
 *
 * In general, users should call {@link
 * edu.ucla.sspace.common.SemanticSpaceUtils#loadSemanticSpace(File)
 * SemanticSpaceUtils.loadSemanticSpace(File)} rather than create an instance of
 * this class directly.<p>
 *
 * This class is thread-safe
 *
 * @see OnDiskSemanticSpace
 * @see SemanticSpaceUtils
 * @see SemanticSpaceUtils.SSpaceFormat
 */
public class FileBasedSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
	Logger.getLogger(FileBasedSemanticSpace.class.getName());

    /**
     * The {@code Matrix} which contains the data read from a finished {@link
     * SemanticSpace}.
     */
    private final Matrix wordSpace;

    /**
     * A mapping of terms to row indexes.  Also serves as a quick means of
     * retrieving the words known by this {@link SemanticSpace}.
     */
    private final Map<String, Integer> termToIndex ;

    /**
     * The name of this semantic space.
     */
    private final String spaceName;

    /**
     * Creates the {@link FileBasedSemanticSpace} from the file using the {@link
     * SSpaceFormat#TEXT text} format.
     *
     * @param filename filename of the data intended be provided by this
     *   {@link edu.ucla.sspace.common.SemanticSpace}.
     */
    public FileBasedSemanticSpace(String filename) {
	this(new File(filename), SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link FileBasedSemanticSpace} from the provided file in the
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.SemanticSpace}.
     */
    public FileBasedSemanticSpace(File file) {
	this(file, SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link FileBasedSemanticSpace} from the provided file in
     * the specified format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.SemanticSpace}.
     */
    public FileBasedSemanticSpace(File file, SSpaceFormat format) {

	spaceName = file.getName();

	// NOTE: Use a LinkedHashMap here because this will ensure that the words
	// are returned in the same row-order as the matrix.  This generates better
	// disk I/O behavior for accessing the matrix since each word is directly
	// after the previous on disk.
	termToIndex = new LinkedHashMap<String, Integer>();
	Matrix m = null;
	long start = System.currentTimeMillis();
	try {
	    switch (format) {
	    case TEXT:
		m = Matrices.synchronizedMatrix(loadText(file));
		break;
	    case BINARY:
		m = Matrices.synchronizedMatrix(loadBinary(file));
		break;
	      
	    // REMINDER: we don't use synchronized here because the current
	    // sparse matrix implementations are thread-safe.  We really should
	    // be aware of this for when the file-based sparse matrix gets
	    // implemented.  -jurgens 05/29/09
	    case SPARSE_TEXT:
		m = loadSparseText(file);
		break;
	    case SPARSE_BINARY:
		m = loadSparseBinary(file);
		break;
	    }
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}  
	if (LOGGER.isLoggable(Level.FINE)) {
	    LOGGER.fine("loaded " + format + " .sspace file in " +
			(System.currentTimeMillis() - start) + "ms");
	}
	
	wordSpace = m;
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToIndex} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private Matrix loadText(File sspaceFile) throws IOException {
	Matrix matrix = null;

	BufferedReader br = new BufferedReader(new FileReader(sspaceFile));
	String line = br.readLine();
	if (line == null)
	    throw new IOError(new Throwable("An empty file has been passed in"));
	String[] dimensions = line.split("\\s");
	int rows = Integer.parseInt(dimensions[0]);
	int columns = Integer.parseInt(dimensions[1]);
	int index = 0;
	
	// reusable array for writing rows into the matrix
	double[] row = new double[columns];
	
	matrix = Matrices.create(rows, columns, true);
	while ((line = br.readLine()) != null) {
	    String[] termVectorPair = line.split("\\|");
	    String[] values = termVectorPair[1].split("\\s");
	    termToIndex.put(termVectorPair[0], index);
	    if (values.length != columns) {
		throw new IOError(
		    new Throwable("improperly formated semantic space file"));	    
	    }
	    for (int c = 0; c < columns; ++c) {
		double d = Double.parseDouble(values[c]);
		row[c] = d;
		// matrix.set(index, c, d);
	    }
	    matrix.setRow(index, row);
	    index++;
	}
	return matrix;    
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToIndex} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private Matrix loadSparseText(File sspaceFile) throws IOException {
	Matrix matrix = null;

	BufferedReader br = new BufferedReader(new FileReader(sspaceFile));
	String line = br.readLine();
	if (line == null)
	    throw new IOError(new Throwable("An empty file has been passed in"));
	String[] dimensions = line.split("\\s");
	int rows = Integer.parseInt(dimensions[0]);
	int columns = Integer.parseInt(dimensions[1]);

	int row = 0;
	
	// create a sparse matrix
	matrix = Matrices.create(rows, columns, false);
	while ((line = br.readLine()) != null) {
	    String[] termVectorPair = line.split("\\|");
	    String[] values = termVectorPair[1].split(",");
	    termToIndex.put(termVectorPair[0], row);

	    // even indicies are columns, odd are the values
	    for (int i = 0; i < values.length; i +=2 ) {
		int col = Integer.parseInt(values[i]);
		double val = Double.parseDouble(values[i+1]);
		matrix.set(row, col, val);
	    }
	    row++;
	}
	return matrix;    
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private Matrix loadBinary(File sspaceFile) throws IOException {
	DataInputStream dis = 
	    new DataInputStream(new FileInputStream(sspaceFile));
	int rows = dis.readInt();
	int cols = dis.readInt();
	// create a dense matrix
	Matrix m = Matrices.create(rows, cols, true);
	double[] d = new double[cols];
	for (int row = 0; row < rows; ++row) {
	    String word = dis.readUTF();
	    termToIndex.put(word, row);
	    for (int col = 0; col < cols; ++col) {
		d[col] = dis.readDouble();
	    }
	    m.setRow(row, d);
	}
	return m;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToIndex} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private Matrix loadSparseBinary(File sspaceFile) throws IOException {
	DataInputStream dis = 
	    new DataInputStream(new FileInputStream(sspaceFile));
	int rows = dis.readInt();
	int cols = dis.readInt();
	// create a sparse matrix
	Matrix m = Matrices.create(rows, cols, false);

	for (int row = 0; row < rows; ++row) {
	    String word = dis.readUTF();
	    termToIndex.put(word, row);
	    
	    int nonZero = dis.readInt();
	    for (int i = 0; i < nonZero; ++i) {
		int col = dis.readInt();
		double val = dis.readDouble();
		m.set(row, col, val);
	    }
	}
	return m;
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
	Integer index = termToIndex.get(term);
	return (index == null) ? null : wordSpace.getRow(index.intValue());
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
      return spaceName;
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorSize() {
      return wordSpace.columns();
    }

    /**
     * A noop.
     */
    public void processDocument(BufferedReader document) { }

    /**
     * A noop.
     */
    public void processSpace(Properties props) { }

}
