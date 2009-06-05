package edu.ucla.sspace.common;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;

import java.util.logging.Logger;

/**
 * A collection of utility methods for interacting with {@link SemanticSpace
 * instances}.  
 *
 * <p> <a name="format"></a>
 *
 * A semantic space can be written in one of two formats:
 * <ul>
 *
 * <li> <b>{@link SSpaceFormat#TEXT TEXT}</b> The first line contains the number
 * of words in the semantic space followed by a space and the number of
 * dimensions in the space (i.e. how long each word vector will be).  Then each
 * word is printed on a separate line in the format {@code word|vector}, where
 * {@code vector} is a space-separated list of the values, such that {@code
 * val}<sub><i>i</i></sub> corresponds to the vector's value in dimensions
 * <i>i</i>.  Example: <br>
 *
 * <pre>numWords numDimensions
 *word1|valForDim1 valForDim2 valForDim3 ... valForDimN
 *wordn|valForDim1 valForDim2 valForDim3 ... valForDimN
 *</pre>
 * 
 * This format is the default. <br>
 *
 * </li>
 *
 * <li> <b>{@link SSpaceFormat#BINARY BINARY}</b> The binary format has an eight
 * byte header consisting of an {@code int} (four bytes, high byte first) of the
 * number of words in the space an a second {@code int} of the number of
 * dimensions in the space.  Then each word in the space is appended in the
 * following format.  The word is encoded in a Modified UTF-8 (see <a
 * href="http://java.sun.com/javase/6/docs/api/java/io/DataInput.html#modified-utf-8">here</a>
 * for more information on the encoding format).  Each of the values for the
 * vector dimensions is appended as an eight-byte {@code double} (high byte
 * first).
 *
 * <p>
 *
 * The binary format is the preferable format if the {@code .sspace} file is to
 * be loaded in by a different program, as the file is smaller and the I/O
 * overhead will be substantially less.
 *
 * </li>
 *
 * </ul>
 *
 * @see SemanticSpace
 */
public class SemanticSpaceUtils {

    private static final Logger LOGGER =
	Logger.getLogger(SemanticSpaceUtils.class.getName());

    /**
     * The type of formatting to use when writing a semantic space to a file.
     * See <a href="SemantSpaceUtils.html#format">here</a> for file format specifications.
     */
    public enum SSpaceFormat { TEXT, BINARY, SPARSE_TEXT, SPARSE_BINARY }

    /**
     * Uninstantiable
     */
    private SemanticSpaceUtils() { }

    /**
     * Loads and returns the {@link SemanticSpace} stored at the file name in
     * {@link SSpaceFormat#TEXT text} format.
     *
     * @param sspaceFileName the name of a file containing a {@link
     *        SemanticSpace} that has been written to disk
     */
    public static SemanticSpace loadSemanticSpace(String sspaceFileName) {
	return loadSemanticSpace(new File(sspaceFileName), SSpaceFormat.TEXT);
    }

    /**
     * Loads and returns the {@link SemanticSpace} stored at the file in {@link
     * SSpaceFormat#TEXT text} format.
     *
     * @param sspaceFile a file containing a {@link SemanticSpace} that has
     *        been written to disk
     */
    public static SemanticSpace loadSemanticSpace(File sspaceFile) {
	return loadSemanticSpace(sspaceFile, SSpaceFormat.TEXT);
    }
    
    /**
     * Loads and returns the {@link SemanticSpace} stored at the file in the
     * specified format.
     *
     * @param sspaceFile a file containing a {@link SemanticSpace} that has
     *        been written to disk
     * @param format the format of the {@link SemanticSpace} in the file
     */
    public static SemanticSpace loadSemanticSpace(File sspaceFile, 
						  SSpaceFormat format) {
	
	long freeMemory = Runtime.getRuntime().freeMemory();
	long fileSize = sspaceFile.length();
	// Guess whether the file contents will fit into memory
	//
	// REMINDER: update these heuristics with some better empirically
	// determined values
	boolean inMemory = false;
	switch (format) {
	case TEXT:
	    // assume the character versions are much bigger than the actual
	    // space
	    inMemory = fileSize < freeMemory;
	    break;
	case BINARY:
	    // factor of 1.5 for additional data structures
	    inMemory = fileSize * 1.5 < freeMemory;
	    break;
	case SPARSE_TEXT:
	    // this may actually require slightly more memory for the sparse
	    // data structures than the equivalent size TEXT version
	    inMemory = fileSize < freeMemory;
	    break;
	case SPARSE_BINARY:
	    // factor of 2 for additional data structures
	    inMemory = fileSize * 2 < freeMemory;
	    break;
	default:
	    throw new IllegalArgumentException("Unknown format type: "+ format);
	}

	if (inMemory) {
	    LOGGER.fine(format + "-formatted .sspace file will fit into memory"
			+ "; creating FileBasedSemanticSpace");
	    return new FileBasedSemanticSpace(sspaceFile, format);
	}
	else {
	    LOGGER.fine(format + "-formatted .sspace file will not fit into"
			+ "memory; creating OnDiskSemanticSpace");
	    return new OnDiskSemanticSpace(sspaceFile, format);
	}
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the file with
     * the provided name using the {@link SSpaceFormat#TEXT} format.  See <a
     * href="#format">here</a> for file format specifications.
     */
    public static void printSemanticSpace(SemanticSpace sspace, 
					  String outputFileName) 
	    throws IOException {
	printSemanticSpace(sspace, new File(outputFileName), SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the provided
     * file using the {@link SSpaceFormat#TEXT} format.  See <a
     * href="#format">here</a> for file format specifications.
     */
    public static void printSemanticSpace(SemanticSpace sspace, File output) 
	    throws IOException {

	printSemanticSpace(sspace, output, SSpaceFormat.TEXT);
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the provided
     * file and format.  See <a href="#format">here</a> for file format
     * specifications.
     */
    public static void printSemanticSpace(SemanticSpace sspace, File output, 
					  SSpaceFormat format) 
	    throws IOException {

	switch (format) {
	case TEXT:
	    printText(sspace, output);
	    break;
	case BINARY:
	    printBinary(sspace, output);
	    break;
	case SPARSE_TEXT:
	    printSparseText(sspace, output);
	    break;
	case SPARSE_BINARY:
	    printSparseBinary(sspace, output);
	    break;
	default:
	    throw new IllegalArgumentException("Unknown format type: "+ format);
	}
    }

    private static void printText(SemanticSpace sspace, File output) 
	    throws IOException {

	PrintWriter pw = new PrintWriter(output);
	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	// print out how many vectors there are and the number of dimensions
	pw.println(words.size() + " " + dimensions);
	LOGGER.fine("saving text S-Space with " + words.size() + 
		    " words with " + dimensions + "-dimensional vectors");

	for (String word : words) {
	    pw.println(word + "|" + 
		       VectorIO.toString(sspace.getVectorFor(word)));
	}
	pw.close();
    }

    private static void printBinary(SemanticSpace sspace, File output) 
	    throws IOException {

	DataOutputStream dos = 
	    new DataOutputStream(new FileOutputStream(output));
	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	// print out how many vectors there are and the number of dimensions
	dos.writeInt(words.size());
	dos.writeInt(dimensions);
	LOGGER.fine("saving binary S-Space with " + words.size() + 
		    " words with " + dimensions + "-dimensional vectors");

	for (String word : words) {
	    dos.writeUTF(word);
	    for (double d : sspace.getVectorFor(word)) {
		dos.writeDouble(d);
	    }
	}
	dos.close();
    }

    private static void printSparseText(SemanticSpace sspace, File output) 
	    throws IOException {

	PrintWriter pw = new PrintWriter(output);
	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	// print out how many vectors there are and the number of dimensions
	pw.println(words.size() + " " + dimensions);

	LOGGER.fine("saving sparse-text S-Space with " + words.size() + 
		    " words with " + dimensions + "-dimensional vectors");

	for (String word : words) {
	    pw.print(word + "|");
	    // for each vector, print all the non-zero elements and their indices
	    double[] vector = sspace.getVectorFor(word);
	    boolean first = true;
	    StringBuilder sb = new StringBuilder(dimensions * 4);
	    for (int i = 0; i < vector.length; ++i) {
		double d = vector[i];
		if (d != 0d) {
		    if (first) {
			sb.append(i).append(",").append(d);
			first = false;
		    }
		    else {
			sb.append(",").append(i).append(",").append(d);
		    }
		}
	    }
	    pw.println(sb.toString());
	}
	pw.close();
    }


    private static void printSparseBinary(SemanticSpace sspace, File output) 
	    throws IOException {

	DataOutputStream dos = 
	    new DataOutputStream(new FileOutputStream(output));
	Set<String> words = sspace.getWords();
	// determine how many dimensions are used by the vectors
	int dimensions = 0;
	if (words.size() > 0) {
	    dimensions = (sspace.getVectorFor(words.iterator().next())).length;
	}

	// print out how many vectors there are and the number of dimensions
	dos.writeInt(words.size());
	dos.writeInt(dimensions);

	LOGGER.fine("saving sparse-binary S-Space with " + words.size() + 
		    " words with " + dimensions + "-dimensional vectors");

	for (String word : words) {
	    dos.writeUTF(word);
	    double[] vector = sspace.getVectorFor(word);
	    // count how many are non-zero
	    int nonZero = 0;
	    for (double d : vector) {
		if (d != 0d)
		    nonZero++;
	    }
	    dos.writeInt(nonZero);
	    for (int i = 0; i < vector.length; ++i) {
		double d = vector[i];
		if (d != 0d) {
		    dos.writeInt(i);
		    dos.writeDouble(d);
		}
	    }
	}
	dos.close();
    }

}