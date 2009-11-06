package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.mains.GenericMain;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.vector.DenseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;

import java.util.Collections;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import java.util.concurrent.atomic.AtomicInteger;


public class FrequencySemanticSpaceMain extends GenericMain {

    private FrequencySemanticSpaceMain() {
    }

    public void usage() {
        System.out.println("usage: FrequencySemanitcSpaceMain [options] " +
                           "<output-dir> " + argOptions.prettyPrint());
    }

    public SemanticSpace getSpace() {
        return new FrequencySemanticSpace();
    }

    public static void main(String[] args) {
        FrequencySemanticSpaceMain main = new FrequencySemanticSpaceMain();
        try {
            main.run(args);
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }

    private class FrequencySemanticSpace implements SemanticSpace {

        private ConcurrentMap<String, AtomicInteger> termCounts;

        public FrequencySemanticSpace() {
            termCounts = new ConcurrentHashMap<String, AtomicInteger>();
        }

        public void processDocument(BufferedReader document) {
            Iterator<String> it = IteratorFactory.tokenize(document);

            while (it.hasNext()) {
                String term = it.next().intern();
                AtomicInteger oldCount = termCounts.putIfAbsent(term,
                        new AtomicInteger(1));
                if (oldCount != null)
                    oldCount.incrementAndGet();
            }
        }

        public void processSpace(Properties props) {
        }

        public Set<String> getWords() {
            return Collections.unmodifiableSet(termCounts.keySet());
        }

        public Vector getVector(String term) {
            AtomicInteger count = termCounts.get(term);
            Vector vector = new DenseVector(1);
            vector.set(0, (count == null) ? 0 : count.get());
            return vector;
        }

        public String getSpaceName() {
            return "FrequencySemanticSpace";
        }

        public int getVectorLength() {
            return 1;
        }
    }
}
