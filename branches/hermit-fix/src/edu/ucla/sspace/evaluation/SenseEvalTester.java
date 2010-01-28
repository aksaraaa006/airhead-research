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

import edu.ucla.sspace.index.IntegerVectorGeneratorMap;
import edu.ucla.sspace.index.PermutationFunction;

import edu.ucla.sspace.text.IteratorFactory;

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
import java.util.Iterator;
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
public class SenseEvalTester {

    private ArgOptions options;

    public SenseEvalTester() {
        options = new ArgOptions();
        options.addOption('s', "sspaceFile",
                          "The SSpace file to test against",
                          true, "FILE", "Required");
        options.addOption('p', "permutationFunctionFile",
                          "A file specifying a serialized PermutationFunction",
                          true, "FILE", "Optional");
        options.addOption('m', "indexVectorMap",
                          "A file specifying a serialized index vector map",
                          true, "FILE", "Required");
        options.addOption('S', "senseEvalFile",
                         "The SenseEval xml file to test against",
                         true, "FILE", "Required");
    }

    public static void main(String[] args) {
        try {
            SenseEvalTester tester = new SenseEvalTester();
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
            !options.hasOption("senseEvalFile") ||
            options.numPositionalArgs() != 1) {
            System.out.println(
                    "usage: java SenseEvalTester [options] <outfile>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        PrintWriter answers = new PrintWriter(options.getPositionalArg(0));

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
        System.out.println(permFunc);
        
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(options.getStringOption('S')));
        NodeList instances = doc.getElementsByTagName("instance");
        for (int i = 0; i < instances.getLength(); ++i) {
            Element instanceNode = (Element) instances.item(i);
            String instanceId = instanceNode.getAttribute("id");
            String[] wordPosNum = instanceId.split("\\.");

            Queue<String> prevWords = extractContext(
                    instanceNode.getFirstChild().getNodeValue());
            Queue<String> nextWords = extractContext(
                    instanceNode.getLastChild().getNodeValue());
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

            // Once the context vector has been build, determine which sense
            // of the word is most similar to the context
            double closestSimilarity = -1;
            String clusterName = null;
            String word = wordPosNum[0];
            String wordPos = word + "." + wordPosNum[1];

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

            answers.println(wordPos + " " + instanceId + " " + clusterName);
        }
        answers.flush();
        answers.close();
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
}
