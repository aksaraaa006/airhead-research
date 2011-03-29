
package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.basis.*;

import edu.ucla.sspace.dependency.*;

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
public class PartOfSpeechDependencyContextGeneratorTest {

    public static final String SINGLE_PARSE = 
        "1   Mr. _   NNP NNP _   2   NMOD    _   _\n" +
        "2   Holt    _   NNP NNP _   3   SBJ _   _\n" +
        "3   is  _   VBZ VBZ _   0   ROOT    _   _\n" +
        "4   a   _   DT  DT  _   5   NMOD    _   _\n" +
        "5   columnist   _   NN  NN  _   3   PRD _   _\n" +
        "6   for _   IN  IN  _   5   NMOD    _   _\n" +
        "7   the _   DT  DT  _   9   NMOD    _   _\n" +
        "8   Literary    _   NNP NNP _   9   NMOD    _   _\n" +
        "9   Review  _   NNP NNP _   6   PMOD    _   _\n" +
        "10  in  _   IN  IN  _   9   ADV _   _\n" +
        "11  London  _   NNP NNP _   10  PMOD    _   _\n" +
        "12  .   _   .   .   _   3   P   _   _";

    @Test public void testGenerate() throws Exception {
        DependencyExtractor extractor = new CoNLLDependencyExtractor();
        DependencyTreeNode[] tree = extractor.readNextTree(
                new BufferedReader(new StringReader(SINGLE_PARSE)));
        DependencyContextGenerator generator =
            new PartOfSpeechDependencyContextGenerator(
                    new MockOrderBasis(), 5);
        SparseDoubleVector result = generator.generateContext(tree, 4);
        assertTrue(result.length() >= 9);
        for (int i = 0; i < 9; ++i)
            assertEquals(1, result.get(i), .00001);
    }

    class MockOrderBasis extends AbstractBasisMapping<String, String> {

        private static final long serialVersionUID = 1L;

        public int getDimension(String key) {
            System.out.println(key);
            if (key.equals("a-DT"))
                return 0;
            if (key.equals("is-VBZ"))
                return 1;
            if (key.equals("holt-NNP"))
                return 2;
            if (key.equals("mr.-NNP"))
                return 3;

            if (key.equals("for-IN"))
                return 4;
            if (key.equals("the-DT"))
                return 5;
            if (key.equals("literary-NNP"))
                return 6;
            if (key.equals("review-NNP"))
                return 7;
            if (key.equals("in-IN"))
                return 8;
            return -1;
        }
    }
}
