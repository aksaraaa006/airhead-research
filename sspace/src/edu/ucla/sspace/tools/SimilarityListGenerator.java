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

import java.lang.reflect.Method;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.BoundedSortedMap;
import edu.ucla.sspace.common.Pair;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.VectorIO;

/**
 * A utility tool for generating lists of most similar words for each word in a
 * {@link SemanticSpace}.
 */
public class SimilarityListGenerator {

    public static final int DEFAULT_SIMILAR_ITEMS = 10;

    private boolean verbose;

    private final ArgOptions argOptions;
    
    public SimilarityListGenerator() { 
	argOptions = new ArgOptions();
	verbose = false;
	addOptions();
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    private void addOptions() {

	argOptions.addOption('p', "printSimilarity", "whether to print the " +
			     "similarity score (default: false)", true, "int", 
			     "Program Options");
	argOptions.addOption('s', "similarityFunction", "name of a similarity " +
			     "function (default: cosine)", true, "String", 
			     "Program Options");
	argOptions.addOption('n', "numSimilar", "the number of similar words " +
			     "to print (default: 10)", true, "String", 
			     "Program Options");


	argOptions.addOption('t', "threads", "the number of threads to use" +
			     " (default: #procesors)", true, "int", 
			     "Program Options");
	argOptions.addOption('w', "overwrite", "specifies whether to " +
			     "overwrite the existing output (default: true)",
			     true, "boolean", "Program Options");
	argOptions.addOption('v', "verbose", "prints verbose output "+ 
			     "(default: false)", false, null, 
			     "Program Options");
    }


    private void usage() {
	System.out.println("usage: java SimilarityListGenerator [options] " +
			   "<sspace-file> <output-dir>\n"  + 
			   argOptions.prettyPrint());
    }

    public static void main(String[] args) {
	try {
	    SimilarityListGenerator generator = new SimilarityListGenerator();
	    if (args.length == 0) {
		generator.usage();
		return;
	    }

	    generator.run(args);

	} catch (Exception e) {
	    e.printStackTrace();
	}
    }	

    public void run(String[] args) throws Exception {

	// process command line args
	argOptions.parseOptions(args);

	if (argOptions.numPositionalArgs() < 2) {
	    throw new IllegalArgumentException("must specify input and output");
	}

	final File sspaceFile = new File(argOptions.getPositionalArg(0));
	final File outputDir = new File(argOptions.getPositionalArg(1));

	if (!outputDir.isDirectory()) {
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}

	final SemanticSpace sspace = new FileBasedSemanticSpace(sspaceFile);
	    	
	// Load the program-specific options next.
	int numThreads = Runtime.getRuntime().availableProcessors();
	if (argOptions.hasOption("threads")) {
	    numThreads = argOptions.getIntOption("threads");
	}

	boolean overwrite = true;
	if (argOptions.hasOption("overwrite")) {
	    overwrite = argOptions.getBooleanOption("overwrite");
	}

	verbose = argOptions.hasOption("v") || argOptions.hasOption("verbose");

	// load the behavior options
	argOptions.addOption('p', "printSimilarity", "whether to print the " +
			     "similarity score (default: false)", true, "int", 
			     "Program Options");
	argOptions.addOption('s', "similarityFunction", "name of a similarity " +
			     "function (default: cosine)", true, "String", 
			     "Program Options");
	argOptions.addOption('n', "numSimilar", "the number of similar words " +
			     "to print (default: 10)", true, "String", 
			     "Program Options");

	final boolean printSimilarity = (argOptions.hasOption('p'))
	    ? argOptions.getBooleanOption('p') : false;

	String similarityName = (argOptions.hasOption('s'))
	    ? argOptions.getStringOption('s') : "cosineSimilarity";

	// refecltively load whatever similarity measure was desired
	final Method similarityMethod = 
	    Similarity.class.getMethod(similarityName, 
	        new Class[] {double[].class, double[].class });

	final int numSimilar = (argOptions.hasOption('n'))
	    ? argOptions.getIntOption('n') : 10;

	File output = (overwrite)
	    ? new File(outputDir, sspaceFile.getName() + ".similarityList")
	    : File.createTempFile(sspaceFile.getName(), "similarityList",
				  outputDir);

	final PrintWriter outputWriter = new PrintWriter(output);


	// Start the execution
	ThreadPoolExecutor executor = 
	    new ScheduledThreadPoolExecutor(numThreads);
	    
	final Set<String> words = sspace.getWords();

	for (String word : words) {

	    final String term = word;
	    executor.submit(new Runnable() {
		    public void run() {
			verbose("processing: " + term);
			
			double[] vector = sspace.getVectorFor(term);

			// the most-similar set will automatically retainy only
			// a fixed number of elements
			SortedMap<Double,String> mostSimilar =
			    new BoundedSortedMap<Double,String>(numSimilar);
			
			// loop through all the other words computing their
			// similarity
			try {
			    for (String other : words) {
				
				// skip if it is ourselves
				if (term.equals(other)) 
					continue;
				
				Pair<String> pair = new Pair<String>(term, other);
				
				double[] otherVec = sspace.getVectorFor(other);
				
				double similarity = -1;
				if (false) {
				    similarity = Similarity.
					cosineSimilarity(vector, otherVec);
				} 
				else {
				    similarity = (Double)(similarityMethod.
					invoke(vector, otherVec));
				}
				    
				mostSimilar.put(similarity, other);
			    }
			}
			catch (Throwable t) {
			    t.printStackTrace();
			}


			// once processing has finished write the k most-similar
			// elemnts to the output file.
			StringBuilder sb = new StringBuilder(256);
			sb.append(term).append("|");
			for (Map.Entry<Double,String> e : 
				     mostSimilar.entrySet()) {
			    String s = e.getValue();
			    Double d = e.getKey();
			    sb.append(s);
			    if (printSimilarity) {
				sb.append(" ").append(d);
			    }
			    sb.append("|");
			}
			synchronized(outputWriter) {
			    outputWriter.println(sb.toString());
			}
		    }
		});
	    }
    
	    executor.shutdown();

	    // wait until all the documents have been parsed
	    executor.awaitTermination(Long.MAX_VALUE, TimeUnit.MILLISECONDS);

	    verbose("Done.");
    }

    private void verbose(String msg) {
	if (verbose) {
	    System.out.println(msg);
	}
    }

    private void verbose(String format, Object... args) {
	if (verbose) {
	    System.out.printf(format, args);
	}
    }
}