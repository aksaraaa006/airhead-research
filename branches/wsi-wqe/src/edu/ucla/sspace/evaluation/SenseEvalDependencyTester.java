/*
 * Copyright 2010 Keith Stevens 
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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.dependency.DependencyExtractor;
import edu.ucla.sspace.dependency.DependencyIterator;
import edu.ucla.sspace.dependency.DependencyPath;
import edu.ucla.sspace.dependency.DependencyPathAcceptor;
import edu.ucla.sspace.dependency.DependencyPathWeight;
import edu.ucla.sspace.dependency.DependencyRelation;
import edu.ucla.sspace.dependency.FlatPathWeight;
import edu.ucla.sspace.dependency.UniversalPathAcceptor;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.Misc;
import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


/**
 * A utility class for running <a
 * href="http://ixa2.si.ehu.es/semeval-senseinduction/">SemEval Task 2 for Sense
 * Induction</a>.  This class is currently under-documented in how it should be
 * run and is over-specified for using index vectors.
 *
 * @author Keith Stevens
 */
public class SenseEvalDependencyTester {

    private final DependencyExtractor extractor;
    private final DependencyPathAcceptor acceptor;
    private final DependencyPathWeight weighter;
    private final PrintWriter answers;
    private final SemanticSpace senseInducedSpace;
    private final Map<String, TernaryVector> wordToIndexVector;
    private final PermutationFunction<TernaryVector> permFunc;
    private final Iterator<SenseEvalInstance> iter;
    private final int windowSize;

    @SuppressWarnings("unchecked")
    public SenseEvalDependencyTester(ArgOptions options) throws Exception {
        answers = new PrintWriter(options.getPositionalArg(0));
        windowSize = (options.hasOption("windowSize"))
            ? options.getIntOption("windowSize") 
            : Integer.MAX_VALUE;

        senseInducedSpace = SemanticSpaceIO.load(options.getStringOption('s'));

        wordToIndexVector = 
            SerializableUtil.load(new File(options.getStringOption('m')));

        permFunc = (options.hasOption('p'))
            ? (PermutationFunction<TernaryVector>) SerializableUtil.load(
                    new File(options.getStringOption('p')))
            : null;

        extractor = new DependencyExtractor();

        acceptor = (options.hasOption('a'))
            ? (DependencyPathAcceptor) Misc.getObjectInstance(
                    options.getStringOption('a'))
            : new UniversalPathAcceptor();

        weighter = (options.hasOption('W'))
            ? (DependencyPathWeight) Misc.getObjectInstance(
                    options.getStringOption('W'))
            : new FlatPathWeight();

        iter = new InstanceIterator(options.getStringOption('S'), windowSize);
    }

    public static void main(String[] args) throws Exception {
        try {
            ArgOptions options = new ArgOptions();
            options.addOption('s', "sspaceFile",
                              "The SSpace file to test against",
                              true, "FILE", "Required");
            options.addOption('w', "windowSize",
                              "The size of the sliding window on both sides " +
                              "of the focus word",
                              true, "INT", "Optional");
            options.addOption('p', "permutationFunctionFile",
                              "A file specifying a serialized " +
                              "PermutationFunction",
                              true, "FILE", "Optional");
            options.addOption('m', "indexVectorMap",
                              "A file specifying a serialized index vector map",
                              true, "FILE", "Required");
            options.addOption('S', "senseEvalFile",
                             "The SenseEvalDependency xml file to test against",
                             true, "FILE", "Required");
            options.addOption('a', "pathAcceptor",
                              "The DependencyPathAcceptor to use",
                              true, "CLASSNAME", "Optional");
            options.addOption('W', "pathWeighter",
                              "The DependencyPathWeight to use",
                              true, "CLASSNAME", "Optional");

            options.parseOptions(args);

            if (!options.hasOption("sspaceFile") ||
                !options.hasOption("indexVectorMap") ||
                !options.hasOption("senseEvalFile") ||
                options.numPositionalArgs() != 1) {
                System.out.println(
                        "usage: java SenseEvalDependencyTester [options] " +
                        "<outfile>\n" +
                        options.prettyPrint());
                System.exit(1);
            }

            SenseEvalDependencyTester tester =
                new SenseEvalDependencyTester(options);
            tester.run();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
       
    @SuppressWarnings("unchecked")
    private void run() throws Exception {
        Collection<Thread> threads = new LinkedList<Thread>();
        int numThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numThreads; ++i) {
            threads.add(new Thread() {
                public void run() {
                    while (iter.hasNext())
                        processInstance(iter.next());
                }
            });
        }

        for (Thread t : threads)
            t.start();

        for (Thread t : threads)
            t.join();

        answers.flush();
        answers.close();
    }

    public void processInstance(SenseEvalInstance instance) {
        // Create a new Vector that will contain the sum of all the
        // index vectors for the context.  This will compared with each
        // sense of the target word.
        DoubleVector contextVector = 
            new DenseVector(senseInducedSpace.getVectorLength());

        Iterator<DependencyPath> pathIter = instance.paths;

        while (pathIter.hasNext()) {
            LinkedList<Pair<String>> path = pathIter.next().path();
            TernaryVector termVector = wordToIndexVector.get(path.peekLast().x);
            int distance = path.size();
            if (permFunc != null)
                termVector = permFunc.permute(termVector, distance);
            add(contextVector, termVector);
        }

        // Once the context vector has been build, determine which sense
        // of the word is most similar to the context
        double closestSimilarity = -1;
        String clusterName = null;

        String word = instance.word;
        String wordPos = instance.wordPos;
        String instanceId = instance.instanceId;

        for (int sense = 0; sense < Integer.MAX_VALUE; ++sense) {
            String senseWord = (sense == 0) ? word : word + "-" + sense;
            Vector semanticVector = 
                senseInducedSpace.getVector(senseWord);
            if (semanticVector == null)
                break;

            double similarity = Similarity.cosineSimilarity(
                semanticVector, contextVector);
            if (similarity > closestSimilarity) {
                closestSimilarity = similarity;
                // Use the Part of Speech as a part of the cluster name
                // to avoid confusing noun and verb clusters in the
                // results.
                clusterName = wordPos + "." + sense;
            }
        }

        if (clusterName != null)
            emitResult(wordPos, instanceId, clusterName);
    }

    private synchronized void emitResult(String wordPos,
                                         String instanceId,
                                         String clusterName) {
        answers.printf("%s %s %s\n", wordPos, instanceId, clusterName);
    }

    private static void add(DoubleVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }

    public class InstanceIterator implements Iterator<SenseEvalInstance> {
        
        private NodeList instances;
        private int index;
        private SenseEvalInstance next;
        private int windowSize;

        public InstanceIterator(String filename, int windowSize) 
                throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            instances = doc.getElementsByTagName("instance");
            index = 0;
            this.windowSize = windowSize;
            next = advance();
        }

        private SenseEvalInstance advance() throws Exception {
            if (index >= instances.getLength())
                return null;
            SenseEvalInstance instance = new SenseEvalInstance(
                    (Element) instances.item(index), windowSize);
            index++;
            return instance;
        }

        public synchronized boolean hasNext() {
            return next != null;
        }

        public synchronized SenseEvalInstance next() {
            try {
                SenseEvalInstance instance = next;
                next = advance();
                return instance;
            } catch (Exception e) {
                e.printStackTrace();
            }
            return null;
        }

        public synchronized void remove() {
        }
    }

    public class SenseEvalInstance {
        Iterator<DependencyPath> paths;
        String instanceId;
        String wordPos;
        String word;

        public SenseEvalInstance(Element instanceNode, int windowSize)
                throws Exception {
            instanceId = instanceNode.getAttribute("id");
            word = instanceNode.getAttribute("name");
            String[] wordPosNum = instanceId.split("\\.");
            word = wordPosNum[0];
            wordPos = word + "." + wordPosNum[1];

            BufferedReader parsedText = new BufferedReader(new StringReader(
                        instanceNode.getFirstChild().getNodeValue()));

            paths = null;
            for (DependencyRelation[] relations = null;
                 (relations = extractor.parse(parsedText)) != null; ) {
                for (int i = 0; i < relations.length; ++i) {
                    if (relations[i].word().equals(word))
                        paths = new DependencyIterator(
                                relations, acceptor, weighter, i, windowSize);
                }
            }
        }
    }
}
