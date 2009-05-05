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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.Matrices;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;

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

/**
 * A {@link SemanticSpace} which performs no processing of documents, but simply
 * reads in a text file produced by another semantic space and converts this
 * into a {@link OnDiskMatrix}.  The input format of the file should be that
 * which is produced by {@link edu.ucla.sspace.common.SemanticSpaceUtils}.
 */
public class FileBasedSemanticSpace implements SemanticSpace {

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
     * Creates the {@link FileBasedSemanticSpace} from the file using the {@link
     * SSpace#TEXT TEXT} format.
     *
     * @param filename filename of the data intended be provided by this
     *   {@link edu.ucla.sspace.common.SemanticSpace}.
     */
    public FileBasedSemanticSpace(String filename) {
	this(new File(filename), SSpaceFormat.TEXT);
    }

    /**
     * Creates the {@link FileBasedSemanticSpace} from the provided file using
     * the {@link SSpace#TEXT TEXT} format.
     *
     * @param file a file containing the data intended be provided by this {@link
     *   edu.ucla.sspace.common.SemanticSpace}.
     */
    public FileBasedSemanticSpace(File file) {
	this(file, SSpaceFormat.TEXT);
    }

    public FileBasedSemanticSpace(File file, SSpaceFormat format) {

    
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
	    ioe.printStackTrace();
	    System.exit(1);
	}  
	wordSpace = m;
    }

    private Matrix loadText(File file) throws IOException {
	Matrix matrix = null;

	BufferedReader br = new BufferedReader(new FileReader(file));
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
	    if (values.length != columns)
		throw new IOError(
				  new Throwable("improperly formated semantic space file"));
	    
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

    private Matrix loadBinary(File f) throws IOException {
	DataInputStream dis = new DataInputStream(new FileInputStream(f));
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
     * A noop.
     */
    public void processDocument(BufferedReader document) {
    }

    /**
     * A noop.
     */
    public void processSpace(Properties props) {
    }
}
