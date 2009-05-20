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

import edu.ucla.sspace.common.VectorIO;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;
import java.util.SortedSet;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A collection of utility methods for interacting with {@link
 * TemporalSemanticSpace} instances.
 *
 * <p> <a name="format"></a>
 *
 * A temporal semantic space can be written in one of two formats:
 * <ul>
 *
 * <li> text
 *
 * </li>
 *
 * <li> binary
 *
 * </li>
 *
 * </ul>
 *
 * @see TemporalSemanticSpace
 */
public class TemporalSemanticSpaceUtils {

    private static final Logger LOGGER = 
	Logger.getLogger(TemporalSemanticSpaceUtils.class.getName());
    
    /**
     * The type of formatting to use when writing a semantic space to a file.
     * See <a href="SemantSpaceUtils.html#format">here</a> for file format specifications.
     */
    public enum SSpaceFormat { TEXT, BINARY }

    /**
     * Uninstantiable
     */
    private TemporalSemanticSpaceUtils() { }

    /**
     * Loads and returns the {@link TemporalSemanticSpace} stored at the file name in
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param sspaceFileName the name of a file containing a {@link
     *        TemporalSemanticSpace} that has been written to disk
     */
    public static TemporalSemanticSpace 
	    loadTemporalSemanticSpace(String sspaceFileName) {
	return loadTemporalSemanticSpace(new File(sspaceFileName), SSpaceFormat.TEXT);
    }

    /**
     * Loads and returns the {@link TemporalSemanticSpace} stored at the file in
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param sspaceFile a file containing a {@link TemporalSemanticSpace} that has
     *        been written to disk
     */
    public static TemporalSemanticSpace 
	    loadTemporalSemanticSpace(File sspaceFile) {
	return loadTemporalSemanticSpace(sspaceFile, SSpaceFormat.TEXT);
    }
    
    /**
     * Loads and returns the {@link TemporalSemanticSpace} stored at the file in
     * the specified format.
     *
     * @param sspaceFile a file containing a {@link TemporalSemanticSpace} that
     *        has been written to disk
     * @param format the format of the {@link TemporalSemanticSpace} in the file
     */
    public static TemporalSemanticSpace
	    loadTemporalSemanticSpace(File sspaceFile, SSpaceFormat format) {
	return new FileBasedTemporalSemanticSpace(sspaceFile, format);
    }

    /**
     * Writes the data contained in the {@link TemporalSemanticSpace} to the
     * file with the provided name using the {@link SSpaceFormat#TEXT} format.
     * See <a href="#format">here</a> for file format specifications.
     */
    public static void printTemporalSemanticSpace(TemporalSemanticSpace sspace, 
						  String outputFileName) 
	    throws IOException {
	printTemporalSemanticSpace(sspace, new File(outputFileName), SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link TemporalSemanticSpace} to the
     * provided file using the {@link SSpaceFormat#TEXT} format.  See <a
     * href="#format">here</a> for file format specifications.
     */
    public static void printTemporalSemanticSpace(TemporalSemanticSpace sspace,
						  File output) 
	    throws IOException {

	printTemporalSemanticSpace(sspace, output, SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link TemporalSemanticSpace} to the provided
     * file and format.  See <a href="#format">here</a> for file format
     * specifications.
     */
    public static void printTemporalSemanticSpace(TemporalSemanticSpace sspace,
						  File output, 
						  SSpaceFormat format) 
	    throws IOException {

	switch (format) {
	case TEXT:
	    printText(sspace, output);
	    break;
	case BINARY:
	    printBinary(sspace, output);
	    break;
	default:
	    throw new IllegalArgumentException("Unknown format type: "+ format);
	}
    }

    private static void printText(TemporalSemanticSpace sspace, File output) 
	    throws IOException {

	PrintWriter pw = new PrintWriter(output);
	SortedSet<Long> timeStamps = sspace.getTimeSteps();
	long start = timeStamps.first();
	long end = timeStamps.last();

	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	int size = words.size();
	// print out how many vectors there are and the number of dimensions
	pw.println(size + " " + dimensions + " " + start + " " + end);

	int wordCount = 0;
	for (String word : words) {
	    pw.print(word + "|");
	    if (LOGGER.isLoggable(Level.INFO)) {
		LOGGER.info(String.format("serializing %d/%d: %s",
					  wordCount++, size, word));
	    }
	    for (long i = start; i <= end; ++i) {
		double[] timeSlice = sspace.getVectorBetween(word, i, i);
		if (timeSlice != null) {
		    pw.print(i + " " + VectorIO.toString(timeSlice) + "|");
		}
	    }
	    pw.println("");
	}
	pw.close();
    }

    private static void printBinary(TemporalSemanticSpace sspace, File output) 
	    throws IOException {

	DataOutputStream dos = 
	    new DataOutputStream(new FileOutputStream(output));

	SortedSet<Long> timeStamps = sspace.getTimeSteps();
	long start = timeStamps.first();
	long end = timeStamps.last();


	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	int size = words.size();
	// print out how many vectors there are and the number of dimensions
	dos.writeInt(size);
	dos.writeInt(dimensions);
	dos.writeLong(start);
	dos.writeLong(end);

	int wordCount = 0;
	for (String word : words) {
	    dos.writeUTF(word);
	    if (LOGGER.isLoggable(Level.INFO)) {
		LOGGER.info(String.format("serializing binary %d/%d: %s",
					  wordCount++, size, word));
	    }
	    for (long i = start; i <= end; ++i) {
		double[] timeSlice = sspace.getVectorBetween(word, i, i);
		if (timeSlice != null) {
		    dos.writeLong(i);
		    for (double d : timeSlice) {
			dos.writeDouble(d);
		    }
		}
	    }
	}
	dos.close();
    }
}