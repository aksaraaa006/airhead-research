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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;

public class TfIdfTransform implements Transform {

    
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
            throws IOException {
	// create a temp file for the output
	File output = File.createTempFile(inputMatrixFile.getName() + 
					  ".tf-idf-transform", "dat");
	transform(inputMatrixFile, format, output);
	return output;
    }

    public void transform(File inputMatrixFile, MatrixIO.Format inputFormat, 
                          File outputMatrixFile) throws IOException {

	// for each document and for each term in that document how many
	// times did that term appear
	Map<Pair<Integer>,Integer> docToTermFreq = 
	    new HashMap<Pair<Integer>,Integer>();
	
	// for each term, in how many documents did that term appear?
	Map<Integer,Integer> termToDocOccurences = 
	    new HashMap<Integer,Integer>();
	
	// for each document, how many terms appeared in it
	Map<Integer,Integer> docToTermCount = 
	    new HashMap<Integer,Integer>();
	
	// how many different terms and documents were used in the matrix
	int numTerms = 0;
	int numDocs = 0;	   	    
	
	// calculate all the statistics on the original term-document matrix
	BufferedReader br = new BufferedReader(new FileReader(inputMatrixFile));
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] termDocCount = line.split("\\s+");
	    
	    Integer term  = Integer.valueOf(termDocCount[0]);
	    Integer doc   = Integer.valueOf(termDocCount[1]);
	    Integer count = Integer.valueOf(termDocCount[2]);
	    
	    if (term.intValue() > numTerms)
		numTerms = term.intValue();
	    
	    if (doc.intValue() > numDocs)
		numDocs = doc.intValue();
	    
	    // increase the count for the number of documents in which this
	    // term was seen
	    Integer termOccurences = termToDocOccurences.get(term);
	    termToDocOccurences.put(term, (termOccurences == null) 
				    ? Integer.valueOf(1) 
				    : Integer.valueOf(termOccurences + 1));
	    
	    // increase the total count of terms seen in ths document
	    Integer docTermCount = docToTermCount.get(doc);
	    docToTermCount.put(doc, (docTermCount == null)
			       ? count
			       : Integer.valueOf(count + docTermCount));
	    
	    docToTermFreq.put(new Pair<Integer>(term, doc), count);
	}
	br.close();
	
	// the output the new matrix where the count value is replaced by
	// the tf-idf value
	PrintWriter pw = new PrintWriter(outputMatrixFile);
	for (Map.Entry<Pair<Integer>,Integer> e : docToTermFreq.entrySet()) { 
	    Pair<Integer> termAndDoc = e.getKey();
	    double count = e.getValue().intValue();
	    double tf = count / docToTermCount.get(termAndDoc.y);
	    double idf = Math.log((double)numDocs / 
				  termToDocOccurences.get(termAndDoc.x));
	    pw.println(termAndDoc.x + "\t" +
		       termAndDoc.y + "\t" +
		       (tf * idf));
	}
	pw.close();
    }

    public Matrix transform(Matrix matrix) {
        throw new UnsupportedOperationException();
    }

    public String toString() {
	return "TF-IDF";
    }

}



