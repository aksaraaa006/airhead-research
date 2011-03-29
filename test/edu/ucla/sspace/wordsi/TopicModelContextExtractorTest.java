
package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.*;

import edu.ucla.sspace.vector.*;

import java.io.*;

import java.util.HashMap;
import java.util.Queue;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class TopicModelContextExtractorTest {

    private Map <String, SparseDoubleVector> termMap;

    @Test public void testProcessDocument() {
        ContextExtractor extractor = new TopicModelContextExtractor();
        MockWordsi wordsi = new MockWordsi(null, extractor);

        String text = "foxes.n.123 1 1.0 5 2.0 2 4.0 3 4.5 4 0.0 0 0.0";

        extractor.processDocument(
                new BufferedReader(new StringReader(text)), wordsi);
        assertEquals(6, extractor.getVectorLength());
    }

    class MockWordsi extends BaseWordsi {

        public MockWordsi(Set<String> acceptedWords,
                          ContextExtractor extractor) {
            super(acceptedWords, extractor);
        }
                          
        public void processSpace(Properties props) {
        }

        public void handleContextVector(String primaryKey,
                                        String secondaryKey,
                                        SparseDoubleVector v) {
            assertEquals("foxes.n.123", secondaryKey);
            assertEquals("foxes", primaryKey);
            assertEquals(0, v.get(0), .001);
            assertEquals(1.0, v.get(1), .001);
            assertEquals(4.0, v.get(2), .001);
            assertEquals(4.5, v.get(3), .001);
            assertEquals(0, v.get(4), .001);
            assertEquals(2, v.get(5), .001);
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }
}
