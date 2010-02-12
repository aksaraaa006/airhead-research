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

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.mains.GenericMain;

import edu.ucla.sspace.text.Document;
import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.TreeMultiMap;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.PrintWriter;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * A simple derived main of {@code GenericMain} which gathers frequency counts
 * of all words in a corpus, using the same tokenizing scheme a {@link
 * SemanticSpace} will utilize.  This class uses the same options available in
 * {@link GenericMain}, and expects the a single positional which specifies
 * where to store term frequencies.
 *
 * @see GenericMain
 *
 * @author Keith Stevens
 */
public class FrequencySemanticSpaceMain extends GenericMain {

    /**
     * A concurrent map of frequency counts for each token in a corpus.
     */
    private ConcurrentMap<String, AtomicInteger> termCounts;

    /**
     * Creates a private instance of this main.
     */
    private FrequencySemanticSpaceMain() {
        termCounts = new ConcurrentHashMap<String, AtomicInteger>();
    }

    public SemanticSpace getSpace() {
        return null;
    }

    /**
     * Emits the expected usage of this main.
     */
    public void usage() {
        System.out.println("usage: FrequencySemanitcSpaceMain [options] " +
                           "<output-file> \n" + argOptions.prettyPrint());
    }

    /**
     * Starts the processing of this main.
     */
    public static void main(String[] args) throws Exception {
        FrequencySemanticSpaceMain main = new FrequencySemanticSpaceMain();
        try {
            main.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * An overloaded implementation of the {@code run} method from {@code
     * GenericMain} which does not create a {@link SemanticSpace}.  Instead,
     * this method sets up any tokenizing and document iterators, and then
     * simply processes each document to compute the term frequencies.  When all
     * documents have been processed, the term frequencies will be written to
     * the output file in the order of words with increasing frequencies.
     */
    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);

        if (argOptions.numPositionalArgs() != 1) {
            throw new IllegalArgumentException("must specify output file");
        }

        File outputFile = new File(argOptions.getPositionalArg(0));

        // all the documents are listed in one file, with one document per line
        final Iterator<Document> docIter = getDocumentIterator();

        // Check whether this class supports mutlithreading when deciding how
        // many threads to use by default
        int numThreads = Runtime.getRuntime().availableProcessors();
        if (argOptions.hasOption("threads")) {
            numThreads = argOptions.getIntOption("threads");
        }

        verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");
        // If verbose output is enabled, update all the loggers in the S-Space
        // package logging tree to output at Level.FINE (normally, it is
        // Level.INFO).  This provides a more detailed view of how the execution
        // flow is proceeding.
        if (verbose) {
            Logger appRooLogger = Logger.getLogger("edu.ucla.sspace");
            Handler verboseHandler = new ConsoleHandler();
            verboseHandler.setLevel(Level.FINE);
            appRooLogger.addHandler(verboseHandler);
            appRooLogger.setLevel(Level.FINE);
            appRooLogger.setUseParentHandlers(false);
        }

        Properties props = setupProperties();

        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (argOptions.hasOption("tokenFilter")) {
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              argOptions.getStringOption("tokenFilter"));
        }

        if (argOptions.hasOption("compoundWords")) {
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundWords"));
        }
        IteratorFactory.setProperties(props);

        Collection<Thread> threads = new LinkedList<Thread>();
        final AtomicInteger count = new AtomicInteger(0);

        // Process each document to compute the term frequencies.
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new Thread() {
                public void run() {
                    while (docIter.hasNext()) {
                        long startTime = System.currentTimeMillis();
                        processDocument(docIter.next().reader());
                        long endTime = System.currentTimeMillis();
                        int docNumber = count.incrementAndGet();
                        verbose("processed document #%d in %.3f seconds",
                                docNumber, ((endTime - startTime) / 1000d));
                    }
                }
            };
            threads.add(t);
        }

        for (Thread t : threads)
            t.start();
        for (Thread t : threads)
            t.join();

        // Sort the term frequencies in ascending order.
        TreeMultiMap<Integer, String> sortedFrequencyList =
            new TreeMultiMap<Integer, String>();
        for (Map.Entry<String, AtomicInteger> e : termCounts.entrySet())
            sortedFrequencyList.put(e.getValue().get(), e.getKey());

        // Write the term frequencies to the output file.
        PrintWriter outputWriter = new PrintWriter(outputFile);
        for (Map.Entry<Integer, String> e : sortedFrequencyList.entrySet())
            outputWriter.println(e.getValue() + "|" + e.getKey());
        outputWriter.close();
    }

    /**
     * Counts the number of times each token occurs in a given document.
     */
    public void processDocument(BufferedReader document) {
        Iterator<String> it = IteratorFactory.tokenize(document);

        while (it.hasNext()) {
            String term = it.next();
            AtomicInteger oldCount = termCounts.putIfAbsent(
                    term, new AtomicInteger(1));
            if (oldCount != null)
                oldCount.incrementAndGet();
        }
    }
}
