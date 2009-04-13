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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import java.util.regex.Pattern;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

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

	LSA lsa = new LSA();
	String input = null;
	
	// process command line args
	    if (args[0].startsWith("--matrixFile=")) {
		try {
		    String matrixFile = 
			args[0].substring("--matrixFile=".length());
		    input = matrixFile;
		    lsa.loadWordDocumentMatrix(matrixFile);
		}
		catch (Throwable t) {
		    t.printStackTrace();
		}
	    } else if (args[0].startsWith("--docsFile=")) {
		try {
		    String docsFile = 
			args[0].substring("--docsFile=".length());
		    input = docsFile;
		    BufferedReader br = 
			new BufferedReader(new FileReader(docsFile));
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
		} catch (Throwable t) {
		    t.printStackTrace();
		}
	    } else {
		System.out.println("unrecognized argument: " + args[0]);
		usage();
		System.exit(0);
	    }

	    System.out.printf("Loaded %d words by %d documents%n",
			      lsa.words.size(),
			      lsa.documents.size());
	    
	    System.out.print("Saving word by document matrix ... ");
	    long startTime = System.currentTimeMillis();
	    try {
		lsa.saveWordDocumentMatrix("wordDocumentMatrix.txt");
	    } catch (IOException ioe) {		
		ioe.printStackTrace();
	    }
	    long endTime = System.currentTimeMillis();
	    System.out.printf("complete (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);
	    
	    System.out.print("computing TF-IDF ...");
	    startTime = System.currentTimeMillis();
	    lsa.processSpace();
	    endTime = System.currentTimeMillis();
	    System.out.printf("complete (%.3f seconds)%n",
			      (endTime - startTime) / 1000d);	    
	    
	    lsa.reduce();
	    try {
		lsa.saveSVDresults(input + ".svd.serialized");
	    } catch (IOException ioe) {
		ioe.printStackTrace();
	    }
	    lsa.computeDistances(null, 4);
	}
    }
    
    private static void usage() {
	System.out.println("java LSA <options>\n" +
			   "\t--matrixFile=<file> (or)\n" + 
			   "\t--docsFile=<file>");
    }
}
