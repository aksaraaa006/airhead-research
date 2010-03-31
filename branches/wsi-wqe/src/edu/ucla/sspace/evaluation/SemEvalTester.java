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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.IteratorFactory;
import edu.ucla.sspace.text.Stemmer;

import edu.ucla.sspace.util.CombinedIterator;
import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
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
 * @author David Jurgens
 */
public class SemEvalTester {

    private ArgOptions options;

    private final Stemmer stemmer;

    public SemEvalTester() {
        stemmer = new EnglishStemmer();
        options = new ArgOptions();
        options.addOption('s', "sspaceFile",
                          "The SSpace file to test against",
                          true, "FILE", "Required");
        options.addOption('w', "windowSize",
                          "The size of the sliding window on both sides of " +
                          "the focus word",
                          true, "INT", "Optional");
        options.addOption('p', "permutationFunctionFile",
                          "A file specifying a serialized PermutationFunction",
                          true, "FILE", "Optional");
        options.addOption('m', "indexVectorMap",
                          "A file specifying a serialized index vector map",
                          true, "FILE", "Required");
    }

    public static void main(String[] args) {
        try {
            SemEvalTester tester = new SemEvalTester();
            tester.run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
       
    @SuppressWarnings("unchecked")
    private void run(String[] args) throws Exception {
        options.parseOptions(args);

        if (!options.hasOption("sspaceFile") ||
            !options.hasOption("indexVectorMap") || 
            options.numPositionalArgs() < 2) {
            System.out.println(
                    "usage: java SemEvalTester [options] " +
                    "<outfile> <test.xml>+\n" + options.prettyPrint());
            System.exit(1);
        }

        PrintWriter answers = new PrintWriter(options.getPositionalArg(0));

        int windowSize = Integer.MAX_VALUE;
        if (options.hasOption("windowSize"))
            windowSize = options.getIntOption("windowSize");

        SemanticSpace senseInducedSpace =
            SemanticSpaceIO.load(options.getStringOption('s'));
        Map<String, TernaryVector> wordToIndexVector =
            (Map<String, TernaryVector>) SerializableUtil.load(
                    new File(options.getStringOption('m')), Map.class);
        PermutationFunction<TernaryVector> permFunc = null;
        if (options.hasOption('p'))
            permFunc =
                (PermutationFunction<TernaryVector>) SerializableUtil.load(
                        new File(options.getStringOption('p')),
                        PermutationFunction.class);
        
        List<Iterator<SemEvalInstance>> semEvalIters = 
            new LinkedList<Iterator<SemEvalInstance>>();
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            semEvalIters.add(new InstanceIterator(options.getPositionalArg(i)));
        final Iterator<SemEvalInstance> iter =
            new CombinedIterator<SemEvalInstance>(semEvalIters);

        Collection<Thread> threads = new LinkedList<Thread>();
        int numThreads = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < numThreads; ++i) {
            Thread t = addThread(iter, answers, permFunc, windowSize,
                                 senseInducedSpace, wordToIndexVector);
            threads.add(t);
        }

        for (Thread t : threads)
            t.start();

        for (Thread t : threads)
            t.join();

        answers.flush();
        answers.close();
    }

    private Thread addThread(
            final Iterator<SemEvalInstance> iter,
            final PrintWriter answers,
            final PermutationFunction<TernaryVector> permFunc,
            final int windowSize,
            final SemanticSpace senseInducedSpace,
            final Map<String, TernaryVector> wordToIndexVector) {
        return new Thread() {
            public void run() {
                while (iter.hasNext()) {
                    SemEvalInstance instance = iter.next();
                    processInstance(instance, answers, permFunc, windowSize,
                            senseInducedSpace, wordToIndexVector);
                }
            }
        };
    }
    public void processInstance(SemEvalInstance instance,
                                PrintWriter answers,
                                PermutationFunction<TernaryVector> permFunc,
                                int windowSize,
                                SemanticSpace senseInducedSpace,
                                Map<String, TernaryVector> wordToIndexVector) {
        if (instance == null)
            return;

        String word = instance.word;
        String instanceStem = instance.stem;
        String wordPos = instance.wordPos;
        String instanceId = instance.instanceId;

        String[] tokens = instance.instanceText.split("\\s+");
        for (int k = 0; k < tokens.length; ++k)
            tokens[k] = tokens[k].toLowerCase();

        Queue<String> prevWords = new LinkedList<String>();
        Queue<String> nextWords = null;

        double closestSimilarity = -1;
        String clusterName = null;

        for (int k = 0; k < tokens.length; ++k) {
            // Find where the instance word occurs.
            String stem = stemmer.stem(tokens[k]);
            if (stem.equals(instanceStem)) {
                nextWords = new LinkedList<String>();
                for (int j = k + 1; j < tokens.length &&
                                    nextWords.size() < windowSize; ++j)
                    nextWords.offer(tokens[j]);

                // Create a new Vector that will contain the sum of all the
                // index vectors for the context.  This will compared with each
                // sense of the target word.
                DoubleVector contextVector = 
                    new DenseVector(senseInducedSpace.getVectorLength());

                // Extract the TernaryVector and permute in the same way that
                // FlyingHermit would for the words before and after the head
                // token.
                int distance = -1 * prevWords.size();
                for (String term : prevWords) {
                    if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                        TernaryVector termVector = wordToIndexVector.get(term);
                        if (permFunc != null)
                            termVector = permFunc.permute(termVector, distance);
                        add(contextVector, termVector);
                    }
                    ++distance;
                }

                distance = 1;
                for (String term : nextWords) {
                    if (!term.equals(IteratorFactory.EMPTY_TOKEN)) {
                        TernaryVector termVector = wordToIndexVector.get(term);
                        if (permFunc != null)
                            termVector = permFunc.permute(termVector, distance);
                        add(contextVector, termVector);
                    }
                    ++distance;
                }
                for (int sense = 0; sense < Integer.MAX_VALUE; ++sense) {
                    String senseWord = (sense == 0)
                        ? instanceStem
                        : instanceStem+ "-" + sense;

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
                        clusterName = wordPos + ".sense." + sense;
                    }
                }
            } else {
                prevWords.offer(tokens[k]);
                if (prevWords.size() > windowSize)
                    prevWords.remove();
            }
        }

        try {
            if (clusterName != null)
                synchronized (answers) {
                    answers.printf("%s %s %s\n",
                                   wordPos, instanceId, clusterName);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void add(DoubleVector dest, TernaryVector src) {
        for (int p : src.positiveDimensions())
            dest.add(p, 1);
        for (int n : src.negativeDimensions())
            dest.add(n, -1);
    }

    private Queue<String> extractContext(String context) {
        Iterator<String> contextTokens = IteratorFactory.tokenize(
            new BufferedReader(new StringReader(context)));
        Queue<String> words = new ArrayDeque<String>();
        while (contextTokens.hasNext())
            words.offer(contextTokens.next());
        return words;
    }

    public class InstanceIterator implements Iterator<SemEvalInstance> {
        
        private NodeList instances;
        private int index;
        private SemEvalInstance next;

        public InstanceIterator(String filename) 
                throws Exception {
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(new File(filename));
            NodeList root = doc.getChildNodes();
            instances = root.item(0).getChildNodes();
            index = 0;
            next = advance();
        }

        private SemEvalInstance advance() throws Exception {
            if (index >= instances.getLength())
                return null;
            SemEvalInstance instance = new SemEvalInstance(
                    (Element) instances.item(index));
            index++;
            return instance;
        }

        public synchronized boolean hasNext() {
            return next != null;
        }

        public synchronized SemEvalInstance next() {
            try {
                SemEvalInstance instance = next;
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

    public class SemEvalInstance {
        String instanceId;
        String instanceText;
        String wordPos;
        String word;
        String stem;

        public SemEvalInstance(Element instanceNode)
                throws Exception {
            instanceId = instanceNode.getNodeName();
            String[] wordPosNum = instanceId.split("\\.");
            word = wordPosNum[0];
            stem = stemmer.stem(wordPosNum[0].toLowerCase());
            wordPos = word + "." + wordPosNum[1];
            NodeList sent = instanceNode.getElementsByTagName("TargetSentence");
            if (sent.getLength() > 0)
                instanceText = sent.item(0).getTextContent();
            else
                instanceText = instanceNode.getTextContent();
        }
    }
}
