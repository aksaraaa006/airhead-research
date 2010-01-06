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
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

import edu.ucla.sspace.text.WordIterator;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.HashMultiMap;
import edu.ucla.sspace.util.MultiMap;

import edu.ucla.sspace.evaluation.WordChoiceEvaluation;
import edu.ucla.sspace.evaluation.WordChoiceEvaluationRunner;
import edu.ucla.sspace.evaluation.WordChoiceReport;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluation;
import edu.ucla.sspace.evaluation.WordSimilarityEvaluationRunner;
import edu.ucla.sspace.evaluation.WordSimilarityReport;

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

    /**
     * The options available to this main
     */
    private final ArgOptions argOptions;

    /**
     * If true, this main will emit verbose messages
     */
    private boolean verbose;

    private Collection<WordChoiceEvaluation> wordChoiceTests;

    private Collection<WordSimilarityEvaluation> wordSimilarityTests;

    /**
     * Creates the {@code EvaluatorMain}.
     */
    public EvaluatorMain() {
        argOptions = new ArgOptions();
        verbose = false;
        addOptions();
    }

    /**
     * Adds the options available to this main.
     */
    protected void addOptions() {
        // input options
         argOptions.addOption('c', "wordChoice",
                              "a list of WordChoiceEvaluation " +
                              "class names and their data files", 
                              true, "CLASS=FILE[,CLASS=FILE...]", 
                              "Required (at least one of)");
         argOptions.addOption('s', "wordSimlarity",
                              "a list of WordSimilarityEvaluation class names", 
                              true, "CLASS=FILE[,CLASS=FILE...]", 
                              "Required (at least one of)");        
         argOptions.addOption('g', "testConfiguration",
                              "a file containing a list of test " +
                              "configurations to run", 
                              true, "FILE", "Required (at least one of)");
        
        // program options
        argOptions.addOption('o', "outputFile",
                             "writes the results to this file",
                             true, "FILE", "Program Options");
        argOptions.addOption('t', "threads",
                             "the number of threads to use",
                             true, "INT", "Program Options");
        argOptions.addOption('v', "verbose",
                             "prints verbose output",
                             false, null, "Program Options");
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);

        verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");

        // load in the arguments specificing which tests that will be run 
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
            usage();
            System.out.println("no tests specified");
            System.exit(1);
        }

        // Load the word choice tests.
        wordChoiceTests = (wcTests == null)
            ? new LinkedList<WordChoiceEvaluation>()
            : loadWordChoiceEvaluations(wcTests);

        // Load the word similarity tests.
        wordSimilarityTests = (wsTests == null)
            ? new LinkedList<WordSimilarityEvaluation>()
            : loadWordSimilarityEvaluations(wcTests);

        // Load any Parse the config file for test types.  The configuration
        // file formatted as pairs of evaluations paired with data
        // files with everything separated by spaces.
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
                Constructor<?> c =
                    clazz.getConstructor(new Class[]{String.class});
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

        // Load the semantic spaces one by one, evaluating each one as it's
        // loaded.
        Set<String> loadedSSpaces = new HashSet<String>();
        int spaces = argOptions.numPositionalArgs();
        for (int i = 0; i < spaces; ++i) {
            SemanticSpace sspace = null;
            String[] sspaceConfig = argOptions.getPositionalArg(i).split(",");
            String sspaceFileName = sspaceConfig[0];
            SimType comparisonFunction = SimType.COSINE;
            if (sspaceConfig.length > 1) {
                for (int j = 1; j < sspaceConfig.length; ++j) {
                    String setting = sspaceConfig[j];
                    if (j > 2) {
                        throw new IllegalStateException(
                            "too may .sspace file arguments:" + 
                            argOptions.getPositionalArg(i));
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
            
            // Load and evaluate the .sspace file if it hasn't been loaded
            // already
            if (!loadedSSpaces.contains(sspace)) {
                verbose("Loading semantic space: %s.", sspaceFileName);
                sspace = SemanticSpaceIO.load(sspaceFileName);
                loadedSSpaces.add(sspaceFileName);
                verbose("Done loading.");

                verbose("Evaluating semantic space: %s." , sspaceFileName);
                evaluateSemanticSpace(sspace, comparisonFunction);
                verbose("Done evaluating.");
            }
        }
    }
    
    /**
     * Runs the loaded evaluations on the given {@link SemanticSpace} using the
     * provided {@code SimType}.  Results are printed to the standard out.
     */
    private void evaluateSemanticSpace(SemanticSpace sspace,
                                       SimType similarity) {
        for (WordChoiceEvaluation wordChoice : wordChoiceTests) {
            WordChoiceReport report = WordChoiceEvaluationRunner.evaluate(
                        sspace, wordChoice, similarity);
            System.out.printf("Results for %s:%n%s%n", wordChoice, report);
        }
        for (WordSimilarityEvaluation wordSimilarity : 
                 wordSimilarityTests) {
            WordSimilarityReport report =
                WordSimilarityEvaluationRunner.evaluate(
                        sspace, wordSimilarity, similarity);
            System.out.printf("Results for %s:%n%s%n", wordSimilarity, report);
        }
    }

    /**
     * Prints verbose strings.
     */
    protected void verbose(String msg) {
        if (verbose) {
            System.out.println(msg);
        }
    }

    /**
     * Prints verbose strings with formatting.
     */
    protected void verbose(String format, Object... args) {
        if (verbose) {
            System.out.printf(format, args);
        }
    }

    /**
     * Prints out the usage for the {@code EvaluatorMain}
     */
    public void usage() {
        System.out.println(
            "java EvaluatorMain " +
            argOptions.prettyPrint() +
            "<sspace-file>[,format=SSpaceFormat[,function=SimType]] " +
            "[<sspace-file>...]\n\n" +
            "The .sspace file arguments may have option specifications that " +
            "indicate\n what vector " + 
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

    /**
     * Starts up the evaluation.
     */
    public static void main(String[] args) {
        try {
            new EvaluatorMain().run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamically loads the set of specified {@link WordChoiceEvaluation}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordChoiceEvaluation> loadWordChoiceEvaluations(
            String wcTests) {
        String[] testsAndFiles = wcTests.split(",");
        Collection<WordChoiceEvaluation> wordChoiceTests = 
            new LinkedList<WordChoiceEvaluation>();
        try {
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                if (testAndFile.length != 2) {
                    throw new IllegalArgumentException(
                        "unexpected token: " + wcTests);
                }
                Class<?> clazz = Class.forName(testAndFile[0]);
                Constructor<?> c =
                    clazz.getConstructor(new Class[]{String.class});
                WordChoiceEvaluation eval = (WordChoiceEvaluation)
                (c.newInstance(new Object[]{testAndFile[1]}));
                verbose("Loaded word choice test %s%n", testAndFile[0]);
                wordChoiceTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordChoiceTests;
    }

    /**
     * Dynamically loads the set of specified {@link WordSimilarityEvaluation}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordSimilarityEvaluation> loadWordSimilarityEvaluations(
            String wcTests) {
        String[] testsAndFiles = wcTests.split(",");
        Collection<WordSimilarityEvaluation> wordSimTests = 
            new LinkedList<WordSimilarityEvaluation>();
        try { 
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
                verbose("Loaded word choice test %s%n", testAndFile[0]);
                wordSimTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordSimTests;
    }
}
