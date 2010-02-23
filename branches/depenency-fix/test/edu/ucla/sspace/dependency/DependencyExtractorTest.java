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

package edu.ucla.sspace.dependency;

import edu.ucla.sspace.text.StringDocument;
import edu.ucla.sspace.text.Document;

import java.util.HashSet;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class DependencyExtractorTest {

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


    /**
     * A simple function that tests the neighbors for a given relation.  The
     * passed in string is expected to contain the relation for each node that
     * is connected to {@code relation}.
     */
    private void evaluateRelations(DependencyRelation relation,
                                   String[] expectedRelations,
                                   int expectedNumRelations) {
        // Check each of the links for "Review".  Add the link id to a set to
        // check that all are accounted for and check the relation each link has
        // with the "Review" node.
        Set<Integer> neighbors = new HashSet<Integer>();
        for (DependencyLink link : relation.neighbors()) {
            assertFalse(expectedRelations[link.neighbor()].equals(""));
            neighbors.add(link.neighbor());
        }

        // Check the number of neighbors.
        assertEquals(expectedNumRelations, neighbors.size());
    }

    @Test public void testSingleExtraction() throws Exception {
        DependencyExtractor extractor = new DependencyExtractor();
        Document doc = new StringDocument(SINGLE_PARSE);
        DependencyRelation[] relations = extractor.parse(doc.reader());

        assertEquals(12, relations.length);

        // Check the basics of the node.
        assertEquals("review", relations[8].word());
        assertEquals("NNP", relations[8].pos());

        // Test expected relation for each of the links for "Review".
        String[] expectedRelations = {"", "", "", "", "",
                                      "PMOD", "NMOD", "NMOD", "", "ADV"};

        evaluateRelations(relations[8], expectedRelations, 4);
    }

    @Test public void testRootNode() throws Exception {
        DependencyExtractor extractor = new DependencyExtractor();
        Document doc = new StringDocument(SINGLE_PARSE);
        DependencyRelation[] relations = extractor.parse(doc.reader());

        assertEquals(12, relations.length);

        // Check the basics of the node.
        assertEquals("is", relations[2].word());
        assertEquals("VBZ", relations[2].pos());

        // Test that the root node does not have a link to itself.
        String[] expectedRelations = {"", "SBJ", "", "", "PRD", "", "", "",
                                      "", "", "", "P"};
        evaluateRelations(relations[2], expectedRelations, 3);
    }
}
