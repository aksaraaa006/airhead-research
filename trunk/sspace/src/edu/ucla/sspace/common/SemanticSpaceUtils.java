package edu.ucla.sspace.common;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;

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

    /**
     * The type of formatting to use when writing a semantic space to a file.
     * See <a href="SemantSpaceUtils.html#format">here</a> for file format specifications.
     */
    public enum SSpaceFormat { TEXT, BINARY }

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

	for (String word : words) {
	    dos.writeUTF(word);
	    for (double d : sspace.getVectorFor(word)) {
		dos.writeDouble(d);
	    }
	}
	dos.close();
    }

}