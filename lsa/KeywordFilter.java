import java.io.*;
import java.util.*;

public class KeywordFilter {

    public static void main(String[] args) {
	String inputFile = args[0];
	String stopWords = args[1];
	String outputFile = args[2];

	

	try {
	    BufferedReader stopWordsBr = new BufferedReader(new FileReader(stopWords));
	    String stopWord = null;
	    BitSet stopIndices = new BitSet();
	    while ((stopWord = stopWordsBr.readLine()) != null) {
		String[] line = stopWord.split("\\s+");
		if (line.length != 3)
		    throw new Error("unexpected line: " + stopWord);
		String word = line[2];
		System.out.println("saw stop word: " + word);
		int wordIndex = Integer.parseInt(line[0]);	       
		stopIndices.set(wordIndex);
	    }
	    stopWordsBr.close();

	    BufferedReader br = new BufferedReader(new FileReader(inputFile));
	    PrintWriter filtered = new PrintWriter(outputFile);
	    String line = null;
	    while ((line = br.readLine()) != null) {
		int termIndex = Integer.parseInt(line.split("\\s+")[0]);
		if (!stopIndices.get(termIndex)) {
		    filtered.println(line);
		}
	    }
	    br.close();
	    filtered.close();
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	

    }

}