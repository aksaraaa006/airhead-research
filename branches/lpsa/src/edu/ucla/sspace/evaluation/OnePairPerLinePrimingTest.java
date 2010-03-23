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

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;


/**
 * @author Keith Stevens
 */ 
public class OnePairPerLinePrimingTest extends AbstractWordPrimingTest {

    /**
     * The name of the data file for this test
     */
    private final String dataFileName;
    
    public OnePairPerLinePrimingTest(String testPairFileName) {
        this(new File(testPairFileName));
    }

    public OnePairPerLinePrimingTest(File testPairFile) {
        super(prepareRelationMap(testPairFile));
        dataFileName = testPairFile.getName();
    }

    public static Set<Pair<String>> prepareRelationMap(File testPairFile) {
        Set<Pair<String>> wordPairSet = new HashSet<Pair<String>>();
        try {
            BufferedReader br = 
                new BufferedReader(new FileReader(testPairFile));
            for (String line = null; (line = br.readLine()) != null; ) {
                if (line.length() == 0 || line.startsWith("#"))
                    continue;
                String[] pair = line.split("\\s+");
                wordPairSet.add(new Pair<String>(pair[0], pair[1]));
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return wordPairSet;
    }

    /**
     * {@inheritDoc}
     */
    protected Double computePriming(SemanticSpace sspace, 
                                    String word1, String word2) {
        return Similarity.cosineSimilarity(
                sspace.getVector(word1), sspace.getVector(word2));
    }

    public String toString() {
        return "Priming Relation: " + dataFileName;
    }
}
