/*
 * Copyright 2009 David Jurgens
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.matrix;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * A shared utility for printing vectors to files in a uniform manner.
 *
 * @author David Jurgens
 */
public class VectorIO {
    
    /**
     * Uninstantiable
     */
    private VectorIO() { }

    public static double[] readVector(File f) throws IOException {
	BufferedReader br = new BufferedReader(new FileReader(f));
	String[] valueStrs = (br.readLine()).trim().split("\\s+");
	double[] values = new double[valueStrs.length];
	for (int i = 0; i < valueStrs.length; ++i) {
	    values[i] = Double.parseDouble(valueStrs[i]);
	}
	br.close();
	return values;
    }

    public static String toString(double[] vector) {
	StringBuilder sb = new StringBuilder(vector.length * 5);
	
	for (int i = 0; i < vector.length - 1; ++i)
	    sb.append(vector[i]).append(" ");
	sb.append(vector[vector.length-1]);
	
	return sb.toString();
    }

    public static String toString(int[] vector) {
	StringBuilder sb = new StringBuilder(vector.length * 3);
	
	for (int i = 0; i < vector.length - 1; ++i)
	    sb.append(vector[i]).append(" ");
	sb.append(vector[vector.length-1]);
	
	return sb.toString();
    }


    public static void writeVector(int[] vector, PrintWriter pw) 
	throws IOException {
	pw.println(toString(vector));
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