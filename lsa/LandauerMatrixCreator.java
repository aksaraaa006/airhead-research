import java.io.*;
import java.util.*;

public class LandauerMatrixCreator {

    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		System.out.println("usage: java <input matrix file> <output file>");
		System.exit(0);
	    }

	    // for each document and for each term in that document how many
	    // times did that term appear
	    //  Map<Pair,Integer> docToTermFreq = new HashMap<Pair,Integer>();
	    Map<Integer,Map<Integer,Integer>> docToTermFreq = 
		new HashMap<Integer,Map<Integer,Integer>>();

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
 		Map<Integer,Integer> docTerms = docToTermFreq.get(doc);
 		if (docTerms == null) {
 		    docTerms = new HashMap<Integer,Integer>();
 		    docToTermFreq.put(doc, docTerms);
 		}
 		docTerms.put(term,count);
// 		docToTermFreq.put(new Pair(term, doc), count);
	    }
	    br.close();

	    // See page 17 of Landauer et al. "An Introduction to Latent
	    // Semantic Analysis"
	    PrintWriter pw = new PrintWriter(args[1]);
	    for (Map.Entry<Integer,Map<Integer,Integer>> e : 
		     docToTermFreq.entrySet()) { 
		Integer doc = e.getKey();
		int totalTerms = docToTermCount.get(doc).intValue();
		double entropy = 0;
		// calculate the entropy of the document
		for (Map.Entry<Integer,Integer> termCount : e.getValue().entrySet()) {
		    
		    double count = termCount.getValue().doubleValue();
		    double p = count / totalTerms;
		    // convert log to base 2
		    double log = Math.log(p) / Math.log(2);
		    entropy += p * log;
		}

		entropy = -entropy;

		// now print out the noralized values
		for (Map.Entry<Integer,Integer> termCount : e.getValue().entrySet()) {
		    int term = termCount.getKey().intValue();
		    double log = Math.log(termCount.getValue().doubleValue() + 1) / Math.log(2);
		    pw.println(term + "\t" +
			       doc + "\t" +
			       (log / entropy));
		}
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



