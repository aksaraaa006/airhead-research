package edu.ucla.sspace.mains;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Pattern;

import edu.ucla.sspace.common.ArgOptions;

import edu.ucla.sspace.lsa.LSA;

/**
 *
 */
public class LSAMain {
    
    public static void main(String[] args) {
		
	if (args.length == 0) {
	    usage();
	    return;
	}


	try {
	    run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    private static void run(String[] args) throws Exception {

	LSA lsa = new LSA();
	String input = null;
	
	// process command line args
	ArgOptions options = new ArgOptions(args);

	// REMDINER: really should put in extra checks for conflicting options
	if (options.hasOption("matrixFile")) {
	    String matrixFile = options.getStringOption("matrixFile");
	    input = matrixFile;
	    lsa.loadWordDocumentMatrix(matrixFile);
	}
	else if (options.hasOption("docsFile")) {
	    String docFile = options.getStringOption("docsFile");
	    input = docFile;
	    BufferedReader br = 
		new BufferedReader(new FileReader(docFile));
	    String document = null;
	    int count = 0;
	    while ((document = br.readLine()) != null) {
		System.out.print("parsing document " + (count++) + ": " +
				 document + " ...");
		long startTime = System.currentTimeMillis();
		lsa.parseDocument(document);
		long endTime = System.currentTimeMillis();
		System.out.printf("complete (%.3f seconds)%n",
				  (endTime - startTime) / 1000d);
		
	    }
	}
	else {
	    System.out.println("unrecognized arguments: " + 
			       Arrays.toString(args));
	    usage();
	    return;
	}

    /*
	System.out.printf("Loaded %d words by %d documents%n",
			  lsa.getWordCount(), lsa.getDocCount());
    */
	
	System.out.print("Saving word by document matrix ... ");
	long startTime = System.currentTimeMillis();
	lsa.saveWordDocumentMatrix("wordDocumentMatrix.txt");
	
	long endTime = System.currentTimeMillis();
	System.out.printf("complete (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);
	
	if (options.hasOption("computeTfIdf")) {
	    System.out.print("computing TF-IDF ...");
	    startTime = System.currentTimeMillis();
	    lsa.processSpace();
	    endTime = System.currentTimeMillis();
	    System.out.printf("complete (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);	    	    
	}
	

	lsa.saveSVDresults(input + ".svd.serialized");
	lsa.computeDistances(null, 4);
    }
    
    private static void usage() {
	System.out.println("java LSA <options>\n" +
			   "\t--matrixFile=<file> (or)\n" + 
			   "\t--docsFile=<file>");
    }
}
