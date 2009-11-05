/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.evaluatation;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.temporal.FileBasedTemporalSemanticSpace;
import edu.ucla.sspace.temporal.TemporalSemanticSpace;
import edu.ucla.sspace.temporal.TemporalSemanticSpaceUtils.TSSpaceFormat;

import edu.ucla.sspace.vector.Vector;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;


/**
 * Iterate through a list of {@code TemporaSemanticSpace}s and print out the
 * change in similarity of words in each space.
 *
 * TODO: Determine if this class is still even needed.
 */
public class TemporalShiftEvaluation {
    public static void main(String[] args) throws IOException {
        ArgOptions options = new ArgOptions();
        options.addOption('s', "sspaceFiles",
                          "A list of temporal semantic space files",
                          true, "FILE,FILE[,FILE,...]", "Required");
        options.parseOptions(args);
        if (!options.hasOption("sspaceFiles") ||
            options.numPositionalArgs() != 1) {
            System.out.println("usage: java TemporalShiftEvaluation " +
                               "[options] " + "<out-file> " +
                               options.prettyPrint());
            System.exit(1);
        }

        String[] sspaceFileNames = options.getStringOption('s').split(",");
        if (sspaceFileNames.length < 2) {
            throw new IllegalArgumentException(
                    "Requires at least two semantic spaces");
        }

        TemporalSemanticSpace previousSpace =
            new FileBasedTemporalSemanticSpace(sspaceFileNames[0]);
        PrintWriter writer = new PrintWriter(options.getPositionalArg(0));

        for (int i = 1; i < sspaceFileNames.length; ++i) {
            TemporalSemanticSpace currentSpace =
                new FileBasedTemporalSemanticSpace(sspaceFileNames[i]);
            Set<String> previousWords = previousSpace.getWords();
            Set<String> currentWords = currentSpace.getWords();
            writer.println("difference between : " + (i-1) + " and " + i);
            for (String word : previousWords) {
                if (!currentWords.contains(word))
                    continue;
                Vector currentVector = currentSpace.getVector(word);
                Vector previousVector = previousSpace.getVector(word);
                double similarity =
                    Similarity.cosineSimilarity(previousVector, currentVector);
                writer.println(word + " | " + similarity);
            }
            previousSpace = currentSpace;
        }
        writer.close();
    }
}

