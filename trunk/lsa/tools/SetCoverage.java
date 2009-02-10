import java.io.*;
import java.util.*;

public class SetCoverage {

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage <word-file> <term-dir>");
	    System.exit(1);
	}
	File termDir = new File(args[1]);
	if (!termDir.isDirectory()) {
	    System.out.println("second arg must be directory");
	    System.exit(1);
	}

	Set<String> words = new HashSet<String>();
	    
	try {
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    for (String line = null; (line = br.readLine()) != null; ) {
		words.add(line.toLowerCase().trim());
	    }
	    
	    for (String termFile : termDir.list()) {
		// strip off the suffix
		String term = termFile.substring(0,termFile.lastIndexOf("."));
		if (words.contains(term))
		    words.remove(term);
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
	
	System.out.println("remaining words: " + words.size());
	System.out.println(words);
    }
}