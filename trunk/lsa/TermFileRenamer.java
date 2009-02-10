import java.io.*;
import java.util.*;

public class TermFileRenamer {

    public static void main(String[] args) {
	if (args.length != 3) {
	    System.out.println("usage: <id-term map> <input dir> <output dir>");
	    return;
	}

	try {
	    Map<Integer,String> idToTerm = new HashMap<Integer,String>();
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    String line = null;
	    while ((line = br.readLine()) != null) {
		String[] idAndTerm = line.split("\t");
		Integer id = Integer.valueOf(idAndTerm[0]);
		idToTerm.put(id,idAndTerm[2]);
	    }
	    System.out.printf("loaded IDs for %d terms%n", idToTerm.size());

	    File inputDir = new File(args[1]);
	    if (!inputDir.isDirectory())
		throw new Error(args[1] + " is not a directory");
	    File outputDir = new File(args[2]);
	    if (!outputDir.isDirectory())
		throw new Error(args[2] + " is not a directory");
	    
	    for (File unconverted : inputDir.listFiles()) {
		System.out.println("converting " + unconverted);
		Integer id = Integer.valueOf(unconverted.
					     getName().split("\\.")[0]);
		String term = idToTerm.get(id);
		if (term == null)
		    throw new Error(id + " has no associated term");
		File output = new File(outputDir, term + ".vector");
		
		FileReader fis = new FileReader(unconverted);
		FileWriter fos = new FileWriter(output);
		int i = -1;
		while ((i = fis.read()) != -1)
		    fos.write(i);

		fis.close();
		fos.close();
	    }

	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }

}