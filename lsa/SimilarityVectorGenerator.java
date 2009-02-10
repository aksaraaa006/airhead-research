import java.io.*;
import java.util.*;

public class SimilarityVectorGenerator {

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage <input-dir> <output-dir>");
	    return;
	}
	try {

	    final File inputDir = new File(args[0]);	    
	    final File outputDir = new File(args[1]);
	    System.out.println("input dir: " + inputDir);
	    System.out.println("output dir: " + outputDir);
	    if (!inputDir.isDirectory() || !outputDir.isDirectory())
		throw new Error("provided args must be directories");

	    Map<String,Integer> termToID = new HashMap<String,Integer>();
	    Set<String> sortedTerms = new TreeSet<String>();
	    int termID = 0;
	    for (File termFile : inputDir.listFiles())
		sortedTerms.add(termFile.getName().split("\\.")[0]);
	    
	    for (String term : sortedTerms)
		termToID.put(term, Integer.valueOf(termID++));
	    

	    // now create vectors for all the nearest neighbors
	    
	    int loaded = 0;
	    for (File termFile : inputDir.listFiles()) {
		if (++loaded % 500 == 0)
		    System.out.printf("loaded %d terms%n", loaded);
		String term = termFile.getName().split("\\.")[0];
		double[] values = new double[termToID.size()];
		
		// read in the most-similar terms
		BufferedReader br = 
		    new BufferedReader(new FileReader(termFile));
		for (String line = null; (line = br.readLine()) != null;) {
 		    //System.out.println(line);
		    String[] termAndDist = line.split("\t");
		    //System.out.println("term: " + termAndDist[0]);
		    values[termToID.get(termAndDist[0]).intValue()] =
			Double.parseDouble(termAndDist[1]);
// 		    System.out.printf("%d -> %f%n",
// 				      termToID.get(termAndDist[0]).intValue(),
// 				      Double.parseDouble(termAndDist[1]));
				    
		}

		br.close();

		PrintWriter pw = new PrintWriter(new File(outputDir,
							  term + ".simVector"));
		for (int i = 0; i < values.length; ++i) {
		    if ( + 1 == values.length)
			pw.println(values[i]);
		    else
			pw.print(values[i] + " ");
		}
		pw.close();
	    }

	    System.out.printf("loaded %d terms total%n", loaded);

	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

}