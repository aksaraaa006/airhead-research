package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * A shared utility for printing vector arrays to files in a uniform manner
 */

public class VectorIO {
    
    /**
     * Uninstantiable
     */
    private VectorIO() { }

    public static double[] readVector(File f) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(f));
	String[] valueStrs = (br.readLine()).trim().split("\t");
	double[] values = new double[valueStrs.length];
	for (int i = 0; i < valueStrs.length; ++i) {
	    values[i] = Double.parseDouble(valueStrs[i]);
	}
	br.close();
	return values;
    }

    public static void writeVector(int[] vector, PrintWriter pw) 
	throws IOException {

	for (int i = 0; i < vector.length - 1; ++i)
	    pw.print(vector[i] + "\t");
	pw.print(vector[vector.length-1]);
	pw.close();
    }
    
    public static void writeVector(int[] vector, File f)
	throws IOException {
	writeVector(vector, new PrintWriter(f));
    }
    
    /**
     * Creates a file using the provided word name in the output directory.  All
     * "/" characters are replaced with <tt>-SLASH-</tt>.
     */
    public static void writeVector(int[] vector, String word, File outputDir) 
	throws IOException {
	if (!outputDir.isDirectory()) {
	    throw new IllegalArgumentException("provided output directory file "
					       + "is not a directory: " + 
					       outputDir);
	}
	word = word.replaceAll("/","-SLASH-");
	File output = new File(outputDir, word + ".vector");
	writeVector(vector, output);
    }
    
}