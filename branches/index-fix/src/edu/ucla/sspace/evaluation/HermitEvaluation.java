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
                          "The maximum number of senses for each word",
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

        // Load up the vectors from the control space which has one vector per
        // word, but was built using the semantic space.  We load up each
        // semantic space separately so that everything can fit in memory.
        SemanticSpace controlSpace =
            SemanticSpaceIO.load(options.getStringOption("controlSpace"));
        for (String wordPair : wordList) {
            String[] word1Word2 = wordPair.split("-");
            for (String word : word1Word2)
                controlVectors.put(word, controlSpace.getVectorFor(word));
        }

        // Load up the vectors from the hermit sense and compare them to the
        // control senses to determine which control sense best matches each
        // hermit sense.
        SemanticSpace hermitSpace =
            SemanticSpaceIO.load(options.getStringOption("hermitSpace"));
        Set<String> hermitWords = hermitSpace.getWords();

        int senseCount =  options.getIntOption('n');
        for (String wordPair : wordList) {
            String[] word1Word2 = wordPair.split("-");

            // Retrieve the original vectors from the map.
            double[] word1Vec = controlVectors.get(word1Word2[0]);
            double[] word2Vec = controlVectors.get(word1Word2[1]);

            List<double[]> hermitVectors = new ArrayList<double[]>();

            // Retrieve up to senseCount hermit senses.
            for (int i = 0; i < senseCount; ++i) {
                String senseName = wordPair + "-" + i
                if (hermitWords.contains(senseName))
                    hermitVectors.add(hermitSpace.getVectorFor(senseName));
                else
                    break;
            }

            // Store the index of each word sense with the matches corresponding
            // to the original word word vector.
            List<Integer> word1Matches = new ArrayList<Integer>();
            List<Integer> word2Matches = new ArrayList<Integer>();
            for (int i = 0; i < hermitVectors.length(); ++i) {
                // Compute the similarity of a hermit sense vector to each of
                // the original word vectors.
                double word1Sim = Similarity.cosineSimilarity(
                        word1Vec,hermitVectors.get(i));
                double word2Sim = Similarity.cosineSimilarity(
                        word2Vec,hermitVectors.get(i));

                // Match the hermit sense with the best matching original sense.
                if (word1Sim > word2Sim)
                    word1Matches.add(i);
                else
                    word2Matches.add(i);
            }
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
