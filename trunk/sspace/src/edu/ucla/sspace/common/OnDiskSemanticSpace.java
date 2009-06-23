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

package edu.ucla.sspace.common;

import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A {@link SemanticSpace} where all vector data is kept on disk.  This class is
 * designed for large semantic spaces whose data, even in sparse format, will
 * not fit into memory.<p>
 *
 * The performance of this class is dependent on the format of the backing
 * vector data; {@code .sspace} files in {@link SSpaceFormat#BINARY binary} or
 * {@link SSpaceFormat#SPARSE_BINARY sparse binary} format will be faster for
 * accessing the data.<p>
 *
 * The {@code getWords} method will return words in the order they are stored on
 * disk.  Accessing the words in this order will have to a significant
 * performance improve over random access.  Furtherore, random access to {@link
 * SSpaceFormat#TEXT text} and {@link SSpaceFormat#SPARSE_TEXT sparse text}
 * formatted matrices will have particularly poor performance for large semantic
 * spaces, as the internal cursor to the data will have to restart from the
 * beginning of the file.<p>
 *
 * This class is <i>not</i> thread-safe.
 *
 * @see SemanticSpaceUtils
 * @see FileBasedSemanticSpace
 */
public class OnDiskSemanticSpace implements SemanticSpace {

    private static final Logger LOGGER = 
	Logger.getLogger(OnDiskSemanticSpace.class.getName());

    /**
     * The {@code Matrix} which contains the data read from a finished {@link
     * SemanticSpace}.
     */
    private final Matrix wordSpace;

    /**
     * A mapping of terms to offsets in the file where the word will be found.
     * If the {@code .sspace} is in binary, this will be a byte offset;
     * otherwise it is a line number in the text file.
     */
    private final Map<String,Long> termToOffset;

    /**
     * The name of this semantic space.
     */
    private final String spaceName;

    /**
     * The number of dimensions used in this semantic space.  This value is set
     * when the {@code termToOffset} map is populated and is used for error
     * checking in the files.
     */
    private final int dimensions;

    /**
     * The reader for accessing a text-based {@code .sspace} file, or {@code
     * null} if the {@code .sspace} file is in binary format.
     */
    private final RandomAccessBufferedReader textSSpace;

    /**
     * Byte access for a binary format {@code .sspace} file, or {@code null} if
     * the {@code .sspace} file is in text format.
     */
    private final RandomAccessFile binarySSpace;

    /**
     * The format of the file that backs this space.
     */
    private final SSpaceFormat format;

    /**
     * Creates the {@link OnDiskSemanticSpace} from the file using the {@link
     * SSpaceFormat#TEXT text} format.
     *
     * @param filename filename of the data intended be provided by this
     *   {@link edu.ucla.sspace.common.SemanticSpace}.
     */
    public OnDiskSemanticSpace(String filename) {
	this(new File(filename), SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link OnDiskSemanticSpace} from the provided file in the
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.SemanticSpace}.
     */
    public OnDiskSemanticSpace(File file) {
	this(file, SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link OnDiskSemanticSpace} from the provided file in
     * the specified format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.SemanticSpace}.
     */
    public OnDiskSemanticSpace(File file, SSpaceFormat format) {

	this.format = format;
	spaceName = file.getName();

	// NOTE: Use a LinkedHashMap here because this will ensure that the words
	// are returned in the same row-order as the matrix.  This generates better
	// disk I/O behavior for accessing the matrix since each word is directly
	// after the previous on disk.
	termToOffset = new LinkedHashMap<String,Long>();
	Matrix m = null;
	long start = System.currentTimeMillis();
	int dims = -1;
	RandomAccessFile raf = null;
	RandomAccessBufferedReader lnr = null;
	try {
	    switch (format) {
	    case TEXT:
		lnr = new RandomAccessBufferedReader(file);
		dims = loadTextOffsets(lnr);
		break;
	    case BINARY:
		raf = new RandomAccessFile(file, "r");
		dims = loadBinaryOffsets(raf);
		break;
	    case SPARSE_TEXT:
		lnr = new RandomAccessBufferedReader(file);
		dims = loadSparseTextOffsets(lnr);
		break;
	    case SPARSE_BINARY:
		raf = new RandomAccessFile(file, "r");
		dims = loadSparseBinaryOffsets(raf);
		break;
	    }
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}  
	if (LOGGER.isLoggable(Level.FINE)) {
	    LOGGER.fine("loaded " + format + " .sspace file in " +
			(System.currentTimeMillis() - start) + "ms");
	}
	
	this.dimensions = dims;
	this.binarySSpace = raf;
	this.textSSpace = lnr;
	wordSpace = m;
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToOffset} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private int loadTextOffsets(RandomAccessBufferedReader textSSpace) throws IOException {
	String line = textSSpace.readLine();
	if (line == null)
	    throw new IOError(new Throwable("An empty file has been passed in"));
	String[] dimensionStrs = line.split("\\s");
	int dimensions = Integer.parseInt(dimensionStrs[1]);

	int row = 1;	
	while ((line = textSSpace.readLine()) != null) {
	    String[] termVectorPair = line.split("\\|");
	    termToOffset.put(termVectorPair[0], Long.valueOf(row));
	    row++;
	}

	return dimensions;
    }

    private double[] loadTextVector(String word) throws IOException {
	Long lineNumber = termToOffset.get(word);
	if (lineNumber == null)
	    return null;
	
	// skip to the line where the word's vector is found
	textSSpace.moveToLine(lineNumber.intValue());
	String line = textSSpace.readLine();
	
	double[] row = new double[dimensions];
	String[] termVectorPair = line.split("\\|");
	String[] values = termVectorPair[1].split("\\s");
	
	if (values.length != dimensions) {
	    throw new IOError(
		new Throwable("improperly formated semantic space file"));
	}
	for (int c = 0; c < dimensions; ++c) {
	    double d = Double.parseDouble(values[c]);
	    row[c] = d;
	}
	return row;
    }

    /**
     * Loads the {@link SemanticSpace} from the text formatted file, adding its
     * words to {@link #termToOffset} and returning the {@code Matrix} containing
     * the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#TEXT text} format
     */
    private int loadSparseTextOffsets(RandomAccessBufferedReader textSSpace) 
	    throws IOException {

	String line = textSSpace.readLine();
	if (line == null)
	    throw new IOError(new Throwable("An empty file has been passed in"));

	String[] dimensions = line.split("\\s");
	int columns = Integer.parseInt(dimensions[1]);

	int row = 1;
	
	while ((line = textSSpace.readLine()) != null) {
	    String[] termVectorPair = line.split("\\|");

	    termToOffset.put(termVectorPair[0], Long.valueOf(row));
	    row++;
	}
	return columns;    
    }

    private double[] loadSparseTextVector(String word) throws IOException {
	Long lineNumber = termToOffset.get(word);
	if (lineNumber == null)
	    return null;
	
	// skip to the line where the word's vector is found
	textSSpace.moveToLine(lineNumber.intValue());
	String line = textSSpace.readLine();
	
	double[] row = new double[dimensions];

	String[] termVectorPair = line.split("\\|");
	String[] values = termVectorPair[1].split(",");
	
	// even indicies are columns, odd are the values
	for (int i = 0; i < values.length; i +=2 ) {
	    int col = Integer.parseInt(values[i]);
	    double val = Double.parseDouble(values[i+1]);
	    row[col] = val;
	}
	return row;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToOffset} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private int loadBinaryOffsets(RandomAccessFile binarySSpace) 
	    throws IOException {

	int rows = binarySSpace.readInt();
	int cols = binarySSpace.readInt();

	for (int row = 0; row < rows; ++row) {
	    String word = binarySSpace.readUTF();
	    termToOffset.put(word, binarySSpace.getFilePointer());
	    // read and discard the rest of the vector
	    for (int col = 0; col < cols; ++col) {
		binarySSpace.readDouble();
	    }
	}
	return cols;
    }

    private double[] loadBinaryVector(String word) throws IOException {

	Long byteOffset = termToOffset.get(word);
	if (byteOffset == null)
	    return null;

	binarySSpace.seek(byteOffset);

	double[] vector = new double[dimensions];
	
	for (int col = 0; col < dimensions; ++col) {
	    vector[col] = binarySSpace.readDouble();
	}

	return vector;
    }

    /**
     * Loads the {@link SemanticSpace} from the binary formatted file, adding
     * its words to {@link #termToOffset} and returning the {@code Matrix}
     * containing the space's vectors.
     *
     * @param sspaceFile a file in {@link SSpaceFormat#BINARY binary} format
     */
    private int loadSparseBinaryOffsets(RandomAccessFile binarySSpace) 
	    throws IOException {

	int rows = binarySSpace.readInt();
	int cols = binarySSpace.readInt();

	for (long row = 0; row < rows; ++row) {
	    String word = binarySSpace.readUTF();
	    termToOffset.put(word, binarySSpace.getFilePointer());
	    
	    // read and discard the rest of the vector
	    int nonZero = binarySSpace.readInt();
	    for (int i = 0; i < nonZero; ++i) {
		binarySSpace.readInt();
		binarySSpace.readDouble();
	    }
	}
	return cols;
    }

    private double[] loadSparseBinaryVector(String word) throws IOException {
	Long byteOffset = termToOffset.get(word);
	if (byteOffset == null)
	    return null;

	binarySSpace.seek(byteOffset);
		    
	int nonZero = binarySSpace.readInt();
	double[] vector = new double[dimensions];
	for (int i = 0; i < nonZero; ++i) {
	    int col = binarySSpace.readInt();
	    double val = binarySSpace.readDouble();
	    vector[col] = val;
	}

	return vector;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
	return Collections.unmodifiableSet(termToOffset.keySet());
    }
  
    /**
     * {@inheritDoc}
     */
    public double[] getVectorFor(String word) {
	try {
	    switch (format) {
	    case TEXT:
		return loadTextVector(word);
	    case BINARY:
		return loadBinaryVector(word);
	    case SPARSE_TEXT:
		return loadSparseTextVector(word);
	    case SPARSE_BINARY:
		return loadSparseBinaryVector(word);
	    }
	} catch (IOException ioe) {
	    // rethrow as something catastrophic must have happened to the
	    // underlying .sspace file
	    throw new IOError(ioe);
	}
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
    public void processSpace(Properties props) { }


    private static class RandomAccessBufferedReader {

	private final File backingFile;

	private BufferedReader current;

	private int currentLineNumber;

	public RandomAccessBufferedReader(File f) throws IOException {
	    backingFile = f;
	    reset();
	}

	private void reset() throws IOException {
	    current = new BufferedReader(new FileReader(backingFile));
	    currentLineNumber = 0;
	}

	public void moveToLine(int lineNum) throws IOException {
	    // if we are trying to go backward in the stream, close it and
	    // restart from the beginning
	    if (lineNum < currentLineNumber) {
		reset(); 
	    }
	    for (int i = currentLineNumber; i < lineNum; ++i) {
		current.readLine();
	    }

	    // update to the new line number
	    currentLineNumber = lineNum;
	}
	
	public String readLine() throws IOException {
	    currentLineNumber++;
	    return current.readLine();
	}
    }

}
