

package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.common.*;

import edu.ucla.sspace.vector.*;

import java.io.*;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 * @author Keith Stevens
 */
public class BaseWordsiTest {

    @Test public void testSetAcceptWord() {
        Set<String> words = new HashSet<String>();
        words.add("cat");
        Wordsi mock = new MockWordsi(words, new MockExtractor());

        assertFalse(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testEmptyAcceptWord() {
        Set<String> words = new HashSet<String>();
        Wordsi mock = new MockWordsi(words, new MockExtractor());

        assertTrue(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testNullAcceptWord() {
        Wordsi mock = new MockWordsi(null, new MockExtractor());

        assertTrue(mock.acceptWord("dog"));
        assertTrue(mock.acceptWord("cat"));
    }

    @Test public void testVectorLength() {
        ContextExtractor extractor = new MockExtractor();
        SemanticSpace mock = new MockWordsi(null, extractor);
        assertEquals(extractor.getVectorLength(), mock.getVectorLength());
    }

    @Test public void testProcessDocument() throws Exception {
        MockExtractor extractor = new MockExtractor();
        SemanticSpace mock = new MockWordsi(null, extractor);
        mock.processDocument(null);
        assertTrue(extractor.calledProcessDocument);
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
        }

        public Vector getVector(String word) {
            return null;
        }

        public Set<String> getWords() {
            return null;
        }
    }

    class MockExtractor implements ContextExtractor {

        public boolean calledProcessDocument;

        public void processDocument(BufferedReader br, Wordsi wordsi) {
            calledProcessDocument = true;
        }

        public int getVectorLength() {
            return 10;
        }
    }
}
