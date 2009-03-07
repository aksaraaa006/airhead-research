import java.io.*;
import java.util.*;

/**
 * Transforms a term-document matrix using log and entropy techniques
 */
public class LogEntropyTransformer {

    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		System.out.println("usage: java <input matrix file> "+
				   "<output file>");
		System.exit(0);
	    }

	    Map<Integer,Integer> docToNumTerms = 
		new HashMap<Integer,Integer>();

	    Map<Integer,Integer> termToGlobalCount = 
		new HashMap<Integer,Integer>();

	    System.out.println("calculating document statistics");

	    // calculate how many terms were in each document for the original
	    // term-document matrix
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    for (String line = null; (line = br.readLine()) != null; ) {

		String[] termDocCount = line.split("\t");
		
		Integer term  = Integer.valueOf(termDocCount[0]);
		Integer doc   = Integer.valueOf(termDocCount[1]);
		Integer count = Integer.valueOf(termDocCount[2]);
				
		Integer docTermCount = docToNumTerms.get(doc);
		docToNumTerms.put(doc, (docTermCount == null)
				  ? count
				  : Integer.valueOf(count + docTermCount));

		Integer termGlobalCount = termToGlobalCount.get(term);
		termToGlobalCount.put(term, (termGlobalCount == null)
				      ? count
				      : termGlobalCount + count);
				      
	    }

	    br.close();

	    System.out.println("calculating term entropy");

	    Map<Integer,Double> termToEntropySum = new 
		HashMap<Integer,Double>();

	    // now go through and find the probability that the term appears in
	    // the document given how many terms it has to begin with
	    br = new BufferedReader(new FileReader(args[0]));
	    for (String line = null; (line = br.readLine()) != null; ) {
		String[] termDocCount = line.split("\t");
		
		Integer term  = Integer.valueOf(termDocCount[0]);
		Integer doc   = Integer.valueOf(termDocCount[1]);
		Integer count = Integer.valueOf(termDocCount[2]);

		double numTermsInDoc = docToNumTerms.get(doc).doubleValue();
		double globalTermCount = 
		    termToGlobalCount.get(term).doubleValue();

		double termFreq = count.doubleValue() / numTermsInDoc;
		double probability = count / globalTermCount;
		
		double d = (probability * log(probability)) / 
		    log(docToNumTerms.size());

		// NOTE: keep the entropy sum a positive value
		Double entropySum = termToEntropySum.get(term);
		termToEntropySum.put(term, (entropySum == null)
				     ? d
				     : entropySum + d);
	    }
	    br.close();
	   

	    System.out.println("generating new matrix");
	    	    
	    PrintWriter pw = new PrintWriter(args[1]);

	    // Last, rewrite the original matrix using the log-entropy
	    // transformation describe on page 17 of Landauer et al. "An
	    // Introduction to Latent Semantic Analysis"
	    br = new BufferedReader(new FileReader(args[0]));
	    for (String line = null; (line = br.readLine()) != null; ) {
		String[] termDocCount = line.split("\t");
		
		Integer term  = Integer.valueOf(termDocCount[0]);
		Integer doc   = Integer.valueOf(termDocCount[1]);
		Integer count = Integer.valueOf(termDocCount[2]);

		double log = log(count.doubleValue() + 1);
		
		double entropySum = termToEntropySum.get(term).doubleValue();
		double entropy = 1 - entropySum;
		
		// now print out the noralized values
		pw.println(term + "\t" +
			   doc + "\t" +
			   (log / entropy));	    
	    }
	    br.close();
	    pw.close();
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }

    /**
     * Returns the base-2 logarithm of {@code d}.
     */
    private static double log(double d) {
	return Math.log(d) / Math.log(2);
    }
    
}



