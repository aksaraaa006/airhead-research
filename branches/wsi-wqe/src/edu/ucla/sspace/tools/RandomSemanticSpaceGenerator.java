package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.StaticSemanticSpace;

import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.DefaultPermutationFunction;

import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.IOException;

import java.util.Collections;
import java.util.ArrayList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;

public class RandomSemanticSpaceGenerator {

    public static void main(String[] args) throws IOException {
        ArgOptions options = new ArgOptions();
        options.addOption('s', "sspaceFileName",
                          "The input semantic space to randomize",
                          true, "FILE", "Required");
        options.parseOptions(args);

        Random random = new Random();
        SemanticSpace sspace =
            new StaticSemanticSpace(options.getStringOption("sspaceFileName"));
        PermutationFunction<Vector> permFunc =
            new DefaultPermutationFunction();
        Set<String> words = sspace.getWords();
        RandomSemanticSpace rSpace =
            new RandomSemanticSpace(words.size(), sspace.getVectorLength());
        for (String word : words) {
            Vector permuted = permFunc.permute(
                    sspace.getVector(word), random.nextInt(words.size() / 100));
            rSpace.addVector(word, permuted);
        }
        SemanticSpaceIO.save(rSpace, options.getPositionalArg(0));
    }

    private static class RandomSemanticSpace implements SemanticSpace {

        private Map<String, Vector> vectors;

        private int vectorLength;

        public RandomSemanticSpace(int size, int vectorLength) {
            this.vectorLength = vectorLength;
            vectors = new HashMap<String, Vector>(size);
        }

        public void processDocument(BufferedReader reader) {
        }

        public void processSpace(Properties props) {
        }
        
        public void addVector(String word, Vector v) {
            vectors.put(word, v);
        }

        public String getSpaceName() {
            return "RandomSemanticSpace";
        }

        public void shuffleMappings() {
            Set<String> words = vectors.keySet();
            List<Vector> values = new ArrayList<Vector>(vectors.values());
            Collections.shuffle(values);
            Map<String, Vector> newValues = new HashMap<String, Vector>();
            int i = 0;
            for (String word : words)
                newValues.put(word, values.get(i++));
            vectors = newValues;
        }

        public Set<String> getWords() {
            return vectors.keySet();
        }

        public Vector getVector(String word) {
            return vectors.get(word);
        }

        public int getVectorLength() {
            return vectorLength;
        }
    }
}
