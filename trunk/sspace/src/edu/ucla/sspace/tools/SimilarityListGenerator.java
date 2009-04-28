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

package edu.ucla.sspace.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.ucla.sspace.common.BoundedSortedMap;
import edu.ucla.sspace.common.Pair;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.VectorIO;

/**
 * A utility tool for generating lists of most similar word vectors for each
 * word vector in a directory.
 */
public class SimilarityListGenerator {

    // TODO: make the number of threads configurable
    // TODO: make the number of similar items printed be configurable

    public static final int MAX_SIMILAR_ITEMS = 100;

    public static final int NUM_THREADS = 4;

    private SimilarityListGenerator() { }

    public static void main(String[] args) {
	if (args.length != 2) {
	    System.out.println("usage <input-dir> <output-dir>");
	    return;
	}
	
	try {

	    final File inputDir = new File(args[0]);	    
	    final File outputDir = new File(args[1]);
	    if (!inputDir.isDirectory() || !outputDir.isDirectory()) {
		throw new IllegalArgumentException(
		    "must provide directory arguments: input directory" 
		    + inputDir + ", output directory: " + outputDir);
	    }

	    final Map<String,double[]> termToVector = 
		new HashMap<String,double[]>();
	    
	    int loaded = 0;
	    for (File termFile : inputDir.listFiles()) {
		String term = termFile.getName().split("\\.")[0];
		termToVector.put(term, VectorIO.readVector(termFile));
	    }
	    System.out.printf("loaded %d terms total%n", loaded);
	    
	    ThreadPoolExecutor executor = 
		new ScheduledThreadPoolExecutor(NUM_THREADS);

	    
	    for (String term2 : termToVector.keySet()) {
		final String term = term2;
		executor.submit(new Runnable() {
			public void run() {
			    System.out.println(term);
			    
			    double[] vector = termToVector.get(term);

			    SortedMap<Double,String> mostSimilar =
				new BoundedSortedMap<Double,String>(100);

			    new TreeMap<Double,String>();
			    
			    try {
				for (String other : termToVector.keySet()) {

				    if (term.equals(other)) 
					continue;
				    
				    Pair<String> pair = 
					new Pair<String>(term, other);
				    
				    double dist;
				    double[] otherVec = termToVector.get(other);
				    
				    dist = Similarity.
					cosineSimilarity(vector, otherVec);
				    
				    mostSimilar.put(1 - dist,other);
				}
			    }
			    catch (Throwable t) {
				t.printStackTrace();
			    }

			    File outputFile = 
				new File(outputDir, term + ".mostSimilar");
			    
			    try {
				PrintWriter pw = new PrintWriter(outputFile);
				// write similarities to file.
				for (Map.Entry<Double,String> e : 
					 mostSimilar.entrySet()) {
				    String s = e.getValue();
				    Double d = e.getKey();
				    pw.printf("%s\t%f%n", s, 
					      1 - d.doubleValue());
				}
				pw.close();
			    } catch (Throwable t) {
				t.printStackTrace();
			    }
			}
		    });
	    }

	    executor.shutdown();

	    // wait until all the documents have been parsed
	    try {
		executor.awaitTermination(Long.MAX_VALUE, 
					  TimeUnit.MILLISECONDS);
	    } catch (InterruptedException ie) {
		ie.printStackTrace();
	    }
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }



}