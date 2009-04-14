package edu.ucla.sspace.randomindexing;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

import edu.ucla.sspace.common.VectorIO;

/**
 *
 */
public class RandomIndexingMain {


    public static void main(String[] args) {
	try {
	    if (args.length != 2) {
		System.out.println("usage: java RI <doc file> <output dir>");
		return;
	    }
	    
	    RandomIndexing ri = new RandomIndexing();
	    BufferedReader br = new BufferedReader(new FileReader(args[0]));
	    System.out.println("processing documents");
	    int i = 0;
	    for (String line = null; (line = br.readLine()) != null; ){ 
		line = line.replaceAll("\\W", " ").toLowerCase();
		ri.processText(line);
		System.out.println("doc # " + (++i));
	    }

	    File outputDir = new File(args[1]);
	    for (String word : ri.getWords()) {
		VectorIO.writeVector(
		    ri.getSemanticVector(word).getVector(),
		    word, outputDir);
	    }
	} catch (Throwable t) {
	    t.printStackTrace();
	}
    }
}