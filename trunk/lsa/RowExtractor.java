import java.io.*;
import java.util.*;

public class RowExtractor {
    
    public static void main(String[] args) {
	if (args.length != 3) {
	    System.out.println("usage: java <matrix rows> <id-to-term-map> <output dir>");
	    System.exit(0);
	}
	try {
	    File outputDir = new File(args[2]);
	    if (!outputDir.isDirectory())
		throw new Error("output directory must be a directory");

	    final Map<Integer,String> idToTerm = new HashMap<Integer,String>();
	    try {
		BufferedReader br = new BufferedReader(new FileReader(args[1]));
		String line = null;
		while ((line = br.readLine()) != null) {
		    String[] idAndTerm = line.split("\t");
		    Integer id = Integer.valueOf(idAndTerm[0]);
		    idToTerm.put(id,idAndTerm[2]);
		}
		System.out.printf("loaded IDs for %d terms%n", idToTerm.size());
	    } catch (Throwable t) {
		t.printStackTrace();
	    }

	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    int i = 1;
	    for (String line = null; (line = br.readLine()) != null; i++) {
		String term = idToTerm.get(i);
		File output = new File(outputDir, term + ".vector");
		PrintWriter pw = new PrintWriter(output);
		pw.println(line);
		pw.close();
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }

}