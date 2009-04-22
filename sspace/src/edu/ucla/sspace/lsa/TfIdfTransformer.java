package edu.ucla.sspace.lsa;

import java.io.*;
import java.util.*;

public class TfIdfTransformer implements MatrixTransformer {

    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		System.out.println(
		    "usage: java <input matrix file> <output file>");
		return;
	    }
	    
	    new TfIdfTransformer().
		transform(new File(args[0]), new File(args[1]));
	} catch (IOException ioe) {
	    ioe.printStackTrace();
	}
    }

    public File transform(File input) throws IOException {
	// create a temp file for the output
	File output = File.createTempFile(input.getName() + 
					  ".tf-idf-transform", "dat");
	transform(input, output);
	return output;
    }

    public void transform(File input, File output) throws IOException {

	// for each document and for each term in that document how many
	// times did that term appear
	Map<Pair,Integer> docToTermFreq = 
	    new HashMap<Pair,Integer>();
	
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
	BufferedReader br = new BufferedReader(new FileReader(input));
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] termDocCount = line.split("\t");
	    
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
	    
	    docToTermFreq.put(new Pair(term, doc), count);
	}
	br.close();
	
	// the output the new matrix where the count value is replaced by
	// the tf-idf value
	PrintWriter pw = new PrintWriter(output);
	for (Map.Entry<Pair,Integer> e : docToTermFreq.entrySet()) { 
	    Pair p = e.getKey();
	    double count = e.getValue().intValue();
	    double tf = count / docToTermCount.get(p.doc);
	    double idf = Math.log((double)numDocs / 
				  termToDocOccurences.get(p.term));
	    pw.println(p.term + "\t" +
		       p.doc + "\t" +
		       (tf * idf));
	}
	pw.close();
    }


    private static class Pair {
	
	Integer term;
	Integer doc;

	public Pair(Integer term, Integer doc) {
	    this.term = term;
	    this.doc = doc;
	}

	public boolean equals(Object o) {
	    if (o instanceof Pair) {
		Pair p = (Pair)o;
		return p.term.equals(term) && p.term.equals(term);
	    }
	    return false;
	}

	public int hashCode() {
	    return term.hashCode() + doc.hashCode();
	}
	
    }

}



