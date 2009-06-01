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

package edu.ucla.sspace.ri;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

/**
 * A utility class for loading and saving word-to-{@link IndexVector} mappings.
 * This class is intended to provide the ability to preseve the same mapping
 * between corpora, or between algorithms that both use {@code IndexVector}s.
 * 
 * @see RandomIndexing
 * @see IndexVector
 * @see IndeexVectorGenerator
 */
public class IndexVectorUtil {

    /**
     * Uninstantiable
     */
    private IndexVectorUtil() { }

    /**
     * Saves the mapping from word to {@link IndexVector} to the specified file.
     */
    public static void save(Map<String,IndexVector> wordToIndexVector, 
			    File output) throws IOException {
	
	PrintWriter pw = new PrintWriter(output);

	// On the first output, write out how long the vectors will be
	boolean writtenLength = false;

	for (Map.Entry<String,IndexVector> e : wordToIndexVector.entrySet()) {
	    String s = e.getKey();
	    IndexVector iv = e.getValue();
	    if (!writtenLength) {
		pw.println(iv.length());
		writtenLength = true;
	    }

	    int[] pos = iv.positiveDimensions();
	    int[] neg = iv.negativeDimensions();
	    StringBuffer sb = new StringBuffer(s.length() + 20);
	    sb.append(s).append(" ").append(pos.length);
	    for (int p : pos) {
		sb.append(" ").append(p);
	    }
	    for (int n : neg) {
		sb.append(" ").append(n);
	    }
	    pw.println(sb.toString());
	}
	pw.close();
    }

    /**
     * Loads a mapping from word to {@link IndexVector} from the file
     */
    public static Map<String,IndexVector> load(File indexVectorFile) 
	    throws IOException {
	Map<String,IndexVector> wordToIndexVector 
	    = new HashMap<String,IndexVector>();
	BufferedReader br = new BufferedReader(new FileReader(indexVectorFile));

	// first line is the length of all the vectors
	int vectorLength = Integer.parseInt(br.readLine().trim());

	// loop through each line creating the vector for it
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] arr = line.split(" ");
	    String word = arr[0];

	    int numPos = Integer.parseInt(arr[1]);
	    int[] pos = new int[numPos];

	    // subtract an additional 2 for the word and size indices
	    int numNeg = arr.length - (numPos + 2);
	    int[] neg = new int[numNeg];

	    // index is the position in arr for determining the indices for the
	    // IndexVector
	    int index = 2;
	    for (int i = 0; i < numPos; ++i, ++index) {
		pos[i] = Integer.parseInt(arr[index]);
	    }
	    for (int i = 0; i < numNeg; ++i, ++index) {
		neg[i] = Integer.parseInt(arr[index]);
	    }

	    IndexVector iv = new PresetIndexVector(vectorLength, pos, neg);
	    wordToIndexVector.put(word, iv);
	}
	return wordToIndexVector;
    }
}