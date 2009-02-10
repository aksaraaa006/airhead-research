import java.io.*;
import java.util.*;

public class TermFrequencyInverseDocumentFrequencyMatrixCreator {

    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		System.out.println("usage: java <input matrix file> <output file>");
		System.exit(0);
	    }

	    // for each document and for each term in that document how many
	    // times did that term appear
// 	    Map<Integer,Map<Integer,Integer>> docToTermFreq = 
// 		new HashMap<Integer,Map<Integer,Integer>>();
	    Map<Pair,Integer> docToTermFreq = new HashMap<Pair,Integer>();

	    // for each term, in how many documents did that term appear?
	    Map<Integer,Integer> termToDocOccurences = 
		new HashMap<Integer,Integer>();
		
	    // for each document, how many terms appeared in it
	    Map<Integer,Integer> docToTermCount = 
		new HashMap<Integer,Integer>();

	    int numTerms = 0;
	    int numDocs = 0;	   	    

	    // calculate all the statistics on the original term-document matrix
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
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
		
		// increase the total term count for the document in which this
		// term occurs
		Integer docTermCount = docToTermCount.get(doc);
		docToTermCount.put(doc, (docTermCount == null)
				   ? count
				   : Integer.valueOf(count + docTermCount));

		// cache the term occurence count for the document for later
		// processing
// 		Map<Integer,Integer> docTerms = docToTermFreq.get(doc);
// 		if (docTerms == null) {
// 		    docTerms = new HashMap<Integer,Integer>();
// 		    docToTermFreq.put(doc, docTerms);
// 		}
// 		docTerms.put(term,count);
		docToTermFreq.put(new Pair(term, doc), count);
	    }
	    br.close();

	    // the output the new matrix where the count value is replaced by
	    // the tf-idf value
	    PrintWriter pw = new PrintWriter(args[1]);
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
	catch (Throwable t) {
	    t.printStackTrace();
	}
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



