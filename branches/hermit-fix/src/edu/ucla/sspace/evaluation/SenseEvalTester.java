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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorMath;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.PrintWriter;
import java.io.StringReader;

import java.util.Iterator;
import java.util.Map;


/**
 * A utility class for running <a
 * href="http://ixa2.si.ehu.es/semeval-senseinduction/">SemEval Task 2 for Sense
 * Induction</a>.  This class is currently under-documented in how it should be
 * run and is over-specified for using index vectors.
 *
 * @author David Jurgens
 */
public class SenseEvalTester {

    public static void main(String[] args) {
        if (args.length != 4) {
            System.out.println("usage: java SenseEvalTester <.sspace> " +
                               "<index-vectors> <senseEval.xml> <output>");
            return;
        }
        try {
            run(args);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
       
    static void run(String[] args) throws Exception {

        SemanticSpace senseInducedSpace = SemanticSpaceIO.load(args[0]);
        ObjectInputStream ois 
            = new ObjectInputStream(new FileInputStream(args[1]));
        @SuppressWarnings("unchecked")
        Map<String,Vector> wordToIndexVector = 
            (Map<String,Vector>)(ois.readObject());
        ois.close();            
        
        BufferedReader br = new BufferedReader(new FileReader(args[2]));
        PrintWriter answers = new PrintWriter(args[3]);
        
        for (String line = null; (line = br.readLine()) != null; ) {
            
            if (line.startsWith("    <instance")) {
                // Find the ID for this word instance
                int start = 18;
                int end = line.indexOf("\"", start);
                // Get the full name of this instance of the word,
                // e.g. "explain.v.46"
                String instance = line.substring(start, end);
                int firstPeriod = instance.indexOf(".");
                // Get the raw form of the word, which we will use with the
                // semantic space, e.g. "explain"
                String word = line.substring(0, firstPeriod);
                // Identify the word plus its part of speech.  This is not
                // used with the semantic space, but is necessary for
                // reporting, e.g. "explain.v"
                String wordPlusPos = line.substring(0, firstPeriod +2);

                // The next line will contain the exact phrase that
                // disambiguates the sense
                String context = br.readLine();
                
                // REMINDER: if we wanted to support compound tokens in the
                // context-building process, we need to provide an option
                Iterator<String> contextTokens = IteratorFactory.tokenize(
                    new BufferedReader(new StringReader(context)));

                // Create a new Vector that will contain the sum of all the
                // index vectors for the context.  This will compared with each
                // sense of the target word.
                Vector contextVector = 
                    new DenseVector(senseInducedSpace.getVectorLength());
                
                while (contextTokens.hasNext()) {
                    String token = contextTokens.next();
                    // The context contains two XML tags to indicate where the
                    // sense word is found.  Since we ignore any positional
                    // information, remove these tags.  Note that the token in
                    // the middle is the word we want to disambiguate, so in
                    // order to remove any bias from adding in the index vector
                    // for it, we remove that token as well
                    if (token.equals("<head>")) {
                        contextTokens.next();
                        contextTokens.next();
                        continue;
                    }

                    Vector indexVector = wordToIndexVector.get(token);
                    if (indexVector != null)
                        VectorMath.add(contextVector, indexVector);
                }                

                // Once the context vector has been build, determine which sense
                // of the word is most similar to the context
                double closestSimilarity = -1;
                String clusterName = null;

                for (int sense = 0; sense < Integer.MAX_VALUE; ++sense) {
                    String senseWord = (sense == 0) ? word : word + "-" + sense;
                    Vector semanticVector = 
                        senseInducedSpace.getVector(senseWord);
                    double similarity = Similarity.cosineSimilarity(
                        semanticVector, contextVector);
                    if (similarity > closestSimilarity) {
                        closestSimilarity = similarity;
                        // Use the Part of Speech as a part of the cluster name
                        // to avoid confusing noun and verb clusters in the
                        // results.
                        clusterName = wordPlusPos + "." + sense;
                    }
                }

                answers.println(wordPlusPos + " " + instance + " "+clusterName);
            }
        }        
    }
}