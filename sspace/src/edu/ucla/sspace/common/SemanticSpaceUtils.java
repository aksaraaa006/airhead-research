package edu.ucla.sspace.common;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;

/**
 * A collection of utility methods for interacting with {@link SemanticSpace
 * instances}.  
 *
 * <p> <a name="format"></a>
 * 
 */
public class SemanticSpaceUtils {

    /**
     * Writes the data contained in the {@link SemanticSpace} to the file with
     * the provided name.  See <a href="#format">here</a> for file format
     * specifications.
     */
    public static void printSemanticSpace(SemanticSpace sspace, 
					  String outputFileName) 
	    throws IOException {
	printSemanticSpace(sspace, new File(outputFileName));
    }

    /**
     * Writes the data contained in the {@link SemanticSpace} to the provided
     * file.  See <a href="#format">here</a> for file format specifications.
     */
    public static void printSemanticSpace(SemanticSpace sspace, File output) 
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

}