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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceUtils;
import edu.ucla.sspace.common.SemanticSpaceUtils.SSpaceFormat;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.text.WordIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.evaluation.WordChoiceEvaluation;
import edu.ucla.sspace.evaluation.WordChoiceEvaluationRunner;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluation;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluationRunner;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.lang.reflect.Constructor;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;


/**
 * Evaluates the performance of {@link SemanticSpace} instances on provided
 * benchmarks.
 */
public class EvaluatorMain {

    private final ArgOptions argOptions;

    private boolean verbose;

    public EvaluatorMain() {
	argOptions = new ArgOptions();
	verbose = false;
	addOptions();
    }

    protected void addOptions() {
	
	// input options
 	argOptions.addOption('c', "wordChoice", "a list of WordChoiceEvaluation " +
			  "class names and their data files", 
 			  true, "CLASS=FILE[,CLASS=FILE...]", 
			  "Required (at least one of)");
 	argOptions.addOption('s', "wordSimlarity", "a list of " + 
			  "WordSimilarityEvaluation class names", 
 			  true, "CLASS=FILE[,CLASS=FILE...]", 
			  "Required (at least one of)");	
 	argOptions.addOption('g', "testConfiguration", "a file containing a list " +
			  "of test configurations to run", 
 			  true, "FILE", "Required (at least one of)");

	
	// program options
	argOptions.addOption('o', "outputFile", "writes the results to this file",
			  true, "FILE", "Program Options");
	argOptions.addOption('t', "threads", "the number of threads to use",
			  true, "INT", "Program Options");
	argOptions.addOption('v', "verbose", "prints verbose output",
			  false, null, "Program Options");
    }

    public void run(String[] args) throws Exception {
	if (args.length == 0) {
	    usage();
	    System.exit(1);
	}
	argOptions.parseOptions(args);

	Collection<WordChoiceEvaluation> wordChoiceTests = 
	    new LinkedList<WordChoiceEvaluation>();

	Collection<WordSimilarityEvaluation> wordSimilarityTests = 
	    new LinkedList<WordSimilarityEvaluation>();

	verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");

	// load in the tests that we will be running
	String wcTests = (argOptions.hasOption("wordChoice"))
	    ? argOptions.getStringOption("wordChoice")
	    : null;

	String wsTests = (argOptions.hasOption("wordSimilarity"))
	    ? argOptions.getStringOption("wordSimilarity")
	    : null;

	String configFile = (argOptions.hasOption("testConfiguration"))
	    ? argOptions.getStringOption("testConfiguration")
	    : null;

	// check that the user provided some input
	if (wcTests == null && wsTests == null && configFile == null) {
	    System.out.println("no tests specified");
	    System.exit(1);
	}

	// load the word choice tests
	if (wcTests != null) {
	    String[] testsAndFiles = wcTests.split(",");
	    for (String s : testsAndFiles) {
		String[] testAndFile = s.split("=");
		if (testAndFile.length != 2) {
		    throw new IllegalArgumentException(
			"unexpected token: " + wcTests);
		}
		Class<?> clazz = Class.forName(testAndFile[0]);
		Constructor<?> c = clazz.getConstructor(new Class[]{String.class});
		WordChoiceEvaluation eval = (WordChoiceEvaluation)
		    (c.newInstance(new Object[]{testAndFile[1]}));
		verbose("Loaded word choice test %s%n", testAndFile[0]);
		wordChoiceTests.add(eval);
	    }
	}

	// load the word similarity tests
	if (wsTests != null) {
	    String[] testsAndFiles = wsTests.split(",");
	    for (String s : testsAndFiles) {
		String[] testAndFile = s.split("=");
		if (testAndFile.length != 2) {
		    throw new IllegalArgumentException(
			"unexpected token: " + wcTests);
		}
		Class<?> clazz = Class.forName(testAndFile[0]);
		Constructor<?> c = 
		    clazz.getConstructor(new Class[]{String.class});
		WordSimilarityEvaluation eval = (WordSimilarityEvaluation)
		    (c.newInstance(new Object[]{testAndFile[1]}));
		verbose("Loaded word similarity test %s%n", testAndFile[0]);
		wordSimilarityTests.add(eval);
	    }
	}

	// last, parse the config file for test types
	if (configFile != null) {
	    WordIterator it = new WordIterator(new BufferedReader(
					           new FileReader(configFile)));
	    while (it.hasNext()) {
		String className = it.next();
		if (!it.hasNext()) {
		    throw new Error("test is not matched with data file: " + 
				    className);
		}
		String dataFile = it.next();
		Class<?> clazz = Class.forName(className);
		Constructor<?> c = clazz.getConstructor(new Class[]{String.class});
		Object o = c.newInstance(new Object[] {dataFile});
		
		// once the test has been created, determine what kind it is
		if (o instanceof WordChoiceEvaluation) {
		    wordChoiceTests.add((WordChoiceEvaluation)o);
		    verbose("Loaded word choice test %s%n", className);
		}
		else if (o instanceof WordSimilarityEvaluation) {
		    wordSimilarityTests.add((WordSimilarityEvaluation)o);
		    verbose("Loaded word similarity test %s%n", className);
		}
		else {
		    throw new IllegalStateException(
			"provided class is not an known Evaluation class type: "
			+ className);
		}
	    }
	}

	// Once all the evaluations have been loaded, load the semantic spaces.
	// Use a mapping from file name to the semantic space to avoid loading
	// the same semantic space more than once
	Map<String,SemanticSpace> fileToSSpace = 
	    new HashMap<String,SemanticSpace>();

	MultiMap<SemanticSpace,SimType> sspaceToVectorComparator = 
	    new HashMultiMap<SemanticSpace,SimType>();
	
	int spaces = argOptions.numPositionalArgs();
	for (int i = 0; i < spaces; ++i) {
	    String[] sspaceConfig = argOptions.getPositionalArg(i).split(",");
	    String sspaceFileName = sspaceConfig[0];
	    SSpaceFormat format = SSpaceFormat.TEXT;
	    SimType comparisonFunction = SimType.COSINE;
	    if (sspaceConfig.length > 1) {
		for (int j = 1; j < sspaceConfig.length; ++j) {
		    String setting = sspaceConfig[j];
		    if (j > 3) {
			throw new IllegalStateException(
			    "too may .sspace file arguments:" + 
			    argOptions.getPositionalArg(i));
		    }
		    if (setting.startsWith("format")) {
			format = SSpaceFormat.valueOf(setting.substring(8));
		    }
		    else if (setting.startsWith("function")) {
			comparisonFunction = 
			    SimType.valueOf(setting.substring(10));
		    }
		    else {
			throw new IllegalArgumentException(
			    "unknown sspace parameter: " + setting);
		    }
		}
	    }
	    
	    // load the .sspace file if it hasn't been loaded already
	    SemanticSpace sspace = fileToSSpace.get(sspaceFileName);
	    if (sspace == null) {
		verbose("loading semantic space %s...", sspaceFileName);
		sspace = SemanticSpaceUtils.
		    loadSemanticSpace(new File(sspaceFileName), format);
		fileToSSpace.put(sspaceFileName, sspace);
		verbose("done");
	    }
	    sspaceToVectorComparator.put(sspace, comparisonFunction);
	}

	// Once all the SemanticSpaces have been loaded, run each one with each
	// vector comparison function on each test
	for (Map.Entry<SemanticSpace,SimType> e :
		 sspaceToVectorComparator.entrySet()) {
	    
	    SemanticSpace sspace = e.getKey();
	    SimType vectorComparisonType = e.getValue();

	    // run each of the word choice test
	    for (WordChoiceEvaluation wordChoice : wordChoiceTests) {
		WordChoiceEvaluationRunner.Report report =
		    WordChoiceEvaluationRunner.
		    evaluate(sspace, wordChoice, vectorComparisonType);
		System.out.printf("Results for %s:%n%s%n", 
				  wordChoice, report);
	    }

	    // run each of the word similarity test
	    for (WordSimilarityEvaluation wordSimilarity : 
		     wordSimilarityTests) {
		WordSimilarityEvaluationRunner.Report report = 
		    WordSimilarityEvaluationRunner.
		    evaluate(sspace, wordSimilarity, vectorComparisonType);
		System.out.printf("Results for %s:%n%s%n", 
				  wordSimilarity, report);
	    }
	}     
    }
    
    protected void verbose(String msg) {
	if (verbose) {
	    System.out.println(msg);
	}
    }

    protected void verbose(String format, Object... args) {
	if (verbose) {
	    System.out.printf(format, args);
	}
    }

    public void usage() {
	System.out.println(
	    "java EvaluatorMain " +
	    argOptions.prettyPrint() +
	    "<sspace-file>[,format=SSpaceFormat[,function=SimType]] " +
	    "[<sspace-file>...]\n\n" +
	    "The .sspace file arguments may have option specifications that " +
	    "indicate\n" +
	    "what format the file is (default TEXT), and what vector " + 
	    "comparison method\n" + 
	    "should be used (default COSINE).  Users should specify the name " +
	    "of a\n" +
	    "Similarity.SimType.  A single .sspace can be evaluated with " + 
	    "multiple\n" +
	    "comparison functions by specifying the file multiple times on " + 
	    "the command\n" +
	    "line.  The .sspace file will be loaded only once.\n\n" +
	    "A test configuration file is a series of fully qualified class " +
	    "names of evaluations\nthat should be run followed by the data" +
	    "file that contains\nthe test information");
    }

    public static void main(String[] args) {
	try {
	    new EvaluatorMain().run(args);
	} catch (Exception e) {
	    e.printStackTrace();
	}
    }

}