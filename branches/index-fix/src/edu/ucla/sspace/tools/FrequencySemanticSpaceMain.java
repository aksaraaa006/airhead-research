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


public class FrequencySemanticSpaceMain extends GenericMain {

    private ConcurrentMap<String, AtomicInteger> termCounts;

    private FrequencySemanticSpaceMain() {
        termCounts = new ConcurrentHashMap<String, AtomicInteger>();
    }

    public SemanticSpace getSpace() {
        return null;
    }

    public void usage() {
        System.out.println("usage: FrequencySemanitcSpaceMain [options] " +
                           "<output-dir> " + argOptions.prettyPrint());
    }

    public static void main(String[] args) throws Exception {
        FrequencySemanticSpaceMain main = new FrequencySemanticSpaceMain();
        try {
            main.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    public void run(String[] args) throws Exception {
        if (args.length == 0) {
            usage();
            System.exit(1);
        }
        argOptions.parseOptions(args);

        if (argOptions.numPositionalArgs() != 1) {
            throw new IllegalArgumentException("must specify output directory");
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

        Properties props = setupProperties();

        // Initialize the IteratorFactory to tokenize the documents according to
        // the specified configuration (e.g. filtering, compound words)
        if (argOptions.hasOption("tokenFilter")) {
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              argOptions.getStringOption("tokenFilter"));            
        }

        if (argOptions.hasOption("compoundTokens")) {
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              argOptions.getStringOption("compoundTokens"));
        }
        IteratorFactory.setProperties(props);

        Collection<Thread> threads = new LinkedList<Thread>();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = new Thread() {
                public void run() {
                    while (docIter.hasNext()) {
                        processDocument(docIter.next().reader());
                    }
                }
            };
            threads.add(t);
        }
        for (Thread t : threads)
            t.start();
        for (Thread t : threads)
            t.join();

        TreeMultiMap<Integer, String> sortedFrequencyList =
            new TreeMultiMap<Integer, String>();
        for (Map.Entry<String, AtomicInteger> e : termCounts.entrySet())
            sortedFrequencyList.put(e.getValue().get(), e.getKey());

        PrintWriter outputWriter = new PrintWriter(outputFile);
        for (Map.Entry<Integer, String> e : sortedFrequencyList.entrySet())
            outputWriter.println(e.getValue() + "|" + e.getKey());
        outputWriter.close();
    }

    public void processDocument(BufferedReader document) {
        Iterator<String> it = IteratorFactory.tokenize(document);

        while (it.hasNext()) {
            String term = it.next().intern();
            AtomicInteger oldCount = termCounts.putIfAbsent(
                    term, new AtomicInteger(1));
            if (oldCount != null)
                oldCount.incrementAndGet();
        }
    }
}
