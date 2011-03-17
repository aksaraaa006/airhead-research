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

import edu.ucla.sspace.evaluation.WordPrimingTest;
import edu.ucla.sspace.evaluation.WordPrimingReport;

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

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Evaluates the performance of {@link SemanticSpace} instances on provided
 * benchmarks.
 */
public class PrimingMain {

    /**
     * The logger used to report verbose output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(PrimingMain.class.getName());

    /**
     * The options available to this main
     */
    private final ArgOptions argOptions;
    
    /**
     * The collection of {@link WordPrimingEvaluation} tests that will be run
     * on the {@link SemanticSpace} instances.
     */
    private Collection<WordPrimingTest> wordPrimingTests;

    /**
     * Creates the {@code PrimingMain}.
     */
    public PrimingMain() {
        argOptions = new ArgOptions();
        addOptions();
    }

    /**
     * Adds the options available to this main.
     */
    protected void addOptions() {
        // input options
         argOptions.addOption('s', "wordPriming",
                              "a list of WordPrimingTest class names", 
                              true, "CLASS[=FILE][=FILE2...][,CLASS=FILE...]", 
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

        if (argOptions.hasOption("verbose"))
            LoggerUtil.setLevel(Level.FINE);

        String wpTests = (argOptions.hasOption("wordPriming"))
            ? argOptions.getStringOption("wordPriming")
            : null;

        String configFile = (argOptions.hasOption("testConfiguration"))
            ? argOptions.getStringOption("testConfiguration")
            : null;

        // check that the user provided some input
        if (wpTests == null && configFile == null) {
            usage();
            System.out.println("no tests specified");
            System.exit(1);
        }

        // Load the word similarity tests.
        wordPrimingTests = (wpTests == null)
            ? new LinkedList<WordPrimingTest>()
            : loadWordPrimingTests(wpTests);

        // Load any Parse the config file for test types.  The configuration
        // file formatted as pairs of evaluations paired with data
        // files with everything separated by spaces.
        if (configFile != null) {
            WordIterator it = new WordIterator(new BufferedReader(
                        new FileReader(configFile)));

            while (it.hasNext()) {
                String className = it.next();
                if (!it.hasNext())
                    throw new Error("test is not matched with data file: " + 
                                    className);

                String[] dataFiles = it.next().split(",");
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class<?> clazz = Class.forName(className);
                Class[] constructorArgs = new Class[dataFiles.length];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object o = c.newInstance((Object[])dataFiles);
                
                wordPrimingTests.add((WordPrimingTest)o);
                verbose("Loaded word priming test " + className);
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
                            "too many .sspace file arguments:" + 
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
                verbose("Loading semantic space: " + sspaceFileName);
                sspace = SemanticSpaceIO.load(sspaceFileName);
                loadedSSpaces.add(sspaceFileName);
                verbose("Done loading.");

                verbose("Evaluating semantic space: " + sspaceFileName);
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
        for (WordPrimingTest wordPrimingTest : wordPrimingTests) {
            WordPrimingReport report = wordPrimingTest.evaluate(sspace);
            System.out.printf("Results for %s:%n%s%n", wordPrimingTest, report);
        }
    }

    /**
     * Prints verbose strings.
     */
    protected void verbose(String msg) {
        LOGGER.fine(msg);
    }

    /**
     * Prints verbose strings with formatting.
     */
    protected void verbose(String format, Object... args) {
        LOGGER.fine(String.format(format, args));
    }

    /**
     * Prints out the usage for the {@code PrimingMain}
     */
    public void usage() {
        System.out.println(
            "java PrimingMain " +
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
            "files that contains\nthe test information, comma separated");
    }

    /**
     * Starts up the evaluation.
     */
    public static void main(String[] args) {
        try {
            new PrimingMain().run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Dynamically loads the set of specified {@link WordPrimingEvaluation}s
     * and returns them as a {@link Collection}.
     */
    private Collection<WordPrimingTest> loadWordPrimingTests(String wpTests) {
        String[] testsAndFiles = wpTests.split(",");
        Collection<WordPrimingTest> wordPrimingTests = 
            new LinkedList<WordPrimingTest>();
        try { 
            for (String s : testsAndFiles) {
                String[] testAndFile = s.split("=");
                Class<?> clazz = Class.forName(testAndFile[0]);
                // Base the number of constructor arguments on the number of
                // String parameters specified
                Class[] constructorArgs = new Class[testAndFile.length - 1];
                for (int i = 0; i < constructorArgs.length; ++i)
                    constructorArgs[i] = String.class;
                Constructor<?> c = clazz.getConstructor(constructorArgs);                
                Object[] args = new String[testAndFile.length - 1];
                for (int i = 1; i < testAndFile.length; ++i)
                    args[i - 1] = testAndFile[i];
                WordPrimingTest eval = (WordPrimingTest )(c.newInstance(args));
                verbose("Loaded word priming test " + testAndFile[0]);
                wordPrimingTests.add(eval);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return wordPrimingTests;
    }
}