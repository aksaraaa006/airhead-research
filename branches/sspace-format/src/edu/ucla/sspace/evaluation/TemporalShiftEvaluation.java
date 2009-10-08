package edu.ucla.sspace.evalutation;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.temporal.FileBasedTemporalSemanticSpace;
import edu.ucla.sspace.temporal.TemporalSemanticSpace;
import edu.ucla.sspace.temporal.TemporalSemanticSpaceUtils.TSSpaceFormat;

import java.io.IOException;
import java.io.PrintWriter;

import java.util.Set;

public class TemporalShiftEvaluation {
  public static void main(String[] args) throws IOException {
    ArgOptions options = new ArgOptions();
    options.addOption('s', "sspaceFiles",
                      "A list of temporal semantic space files",
                      true, "FILE,FILE[,FILE,...]", "Required");
    options.addOption('t', "sspaceFormat",
                      "Format of temporal semantic space files",
                      true, "binary|text", "Optional");
    
    options.parseOptions(args);
    if (!options.hasOption("sspaceFiles") ||
        options.numPositionalArgs() != 1) {
      System.out.println("usage: java TemporalShiftEvaluation [options] " +
                         "<out-file> " + options.prettyPrint());
      System.exit(1);
    }

    String[] sspaceFileNames = options.getStringOption('s').split(",");
    if (sspaceFileNames.length < 2) {
      throw new IllegalArgumentException("Requires at least two semantic spaces");
    }

    TSSpaceFormat format = (options.hasOption('t'))
      ? TSSpaceFormat.valueOf(options.getStringOption('t'))
      : TSSpaceFormat.TEXT;

    TemporalSemanticSpace previousSpace =
      new FileBasedTemporalSemanticSpace(sspaceFileNames[0], format);
    PrintWriter writer = new PrintWriter(options.getPositionalArg(0));

    for (int i = 1; i < sspaceFileNames.length; ++i) {
      TemporalSemanticSpace currentSpace =
        new FileBasedTemporalSemanticSpace(sspaceFileNames[i], format);
      Set<String> previousWords = previousSpace.getWords();
      Set<String> currentWords = currentSpace.getWords();
      writer.println("difference between : " + (i-1) + " and " + i);
      for (String word : previousWords) {
        if (!currentWords.contains(word))
          continue;
        double[] currentVector = currentSpace.getVectorFor(word);
        double[] previousVector = previousSpace.getVectorFor(word);
        double similarity =
          Similarity.cosineSimilarity(previousVector, currentVector);
        writer.println(word + " | " + similarity);
      }
      previousSpace = currentSpace;
    }
    writer.close();
  }
}

