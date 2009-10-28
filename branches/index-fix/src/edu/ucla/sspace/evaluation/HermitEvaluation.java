package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;


public class HermitEvaluation {
    public static void main(String args[]) throws IOException {
        ArgOptions options = new ArgOptions();
        options.addOption('c', "controlSpace",
                          "The serialized SemanticSpace to test against",
                          true, "FILE", "Test Properties");
        options.addOption('h', "hermitSpace",
                          "The serialized Hermit SemanticSpace to test",
                          true, "FILE", "Test Properties");
        options.addOption('n', "numberOfSenses",
                          "The expected number of senses for each word",
                          true, "INT", "Test Properties");
        options.addOption('w', "wordPairs",
                          "The List of word pairs to be compared in the " +
                          "test space and the hermit space, separated by " +
                          "hyphen",
                          true, "FILE", "Test Properties");

        options.parseOptions(args);
        if (!options.hasOption('c') ||
            !options.hasOption('h') ||
            !options.hasOption('n') ||
            !options.hasOption('w')) {
            System.out.println("error");
            System.exit(1);
        }

        // Read in the set of test words to compare.
        Set<String> wordList = parseWordList(options.getStringOption('w'));


        Map<String, double[]> controlVectors =
            new HashMap<String, double[]>(wordList.size()*2);

        SemanticSpace controlSpace =
            SemanticSpaceIO.load(options.getStringOption("controlSpace"));
        for (String wordPair : wordList) {
            String[] word1Word2 = wordPair.split("-");
            for (String word : word1Word2)
                controlVectors.put(word, controlSpace.getVectorFor(word));
        }

        SemanticSpace hermitSpace =
            SemanticSpaceIO.load(options.getStringOption("hermitSpace"));
        Set<String> hermitWords = hermitSpace.getWords();

        int senseCount =  options.getIntOption('n');
        for (String wordPair : wordList) {
            String[] word1Word2 = wordPair.split("-");
            List<double[]> hermitVectors = new ArrayList<double[]>();
            for (int i = 0; i < senseCount; ++i) {
                if (hermitWords.contains(wordPair+"-"+i))
                    hermitVectors.add(
                            hermitSpace.getVectorFor(wordPair+"-"+i));
            }
            // Evaluate this bizznizz.
        }
    }

    public static Set<String> parseWordList(String filename) {
        Set<String> wordList = new TreeSet<String>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = null;
            while ((line = br.readLine()) != null)
                wordList.add(line);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return wordList; 
    }
}
