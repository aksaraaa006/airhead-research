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

package edu.ucla.sspace.coals;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.ArrayMatrix;

import edu.ucla.sspace.text.StringDocument;

import edu.ucla.sspace.vector.Vector;

import org.junit.*;

import static org.junit.Assert.*;

import java.io.IOException;

import java.util.Properties;
import java.util.Set;


public class CoalsTest {

    public static final String TEST_CORPUS = 
        "how much wood would a woodchuck chuck , if a woodchuck could chuck " +
        "wood ? as much wood as a woodchuck would , if a woodchuck could " +
        "chuck wood .";

    @Test public void testGetWords() throws IOException {
        Coals coals = new Coals();
        StringDocument sDoc = new StringDocument(TEST_CORPUS);
        coals.processDocument(sDoc.reader());
        Properties props = new Properties();
        coals.processSpace(props);
        Set<String> wordSet = coals.getWords();
        assertEquals(13, wordSet.size());
        assertTrue(wordSet.contains("woodchuck"));
        Vector woodchuckMeaning = coals.getVector("woodchuck");
        assertTrue(woodchuckMeaning != null);
        assertEquals(13, woodchuckMeaning.length());
    }

    @Test public void testSVDCoals() throws IOException {
        Coals coals = new Coals();
        StringDocument sDoc = new StringDocument(TEST_CORPUS);
        coals.processDocument(sDoc.reader());
        Properties props = new Properties();
        props.setProperty(Coals.REDUCE_MATRIX_PROPERTY, "");
        props.setProperty(Coals.REDUCE_DIMENSION_PROPERTY, "3");
        coals.processSpace(props);
        Vector woodchuck = coals.getVector("woodchuck");
        assertTrue(woodchuck != null);
        assertEquals(3, woodchuck.length());
    }

    @Test public void testCoalsWithoutNormalization() throws IOException {
        Coals coals = new Coals();
        StringDocument sDoc = new StringDocument(TEST_CORPUS);
        coals.processDocument(sDoc.reader());
        System.setProperty(Coals.DO_NOT_NORMALIZE_PROPERTY, "");
        coals.processSpace(System.getProperties());
        // 13 x 13 matrix.
        double[] testCorrelations = {0, 0, 0,10, 1, 4, 2, 0, 8, 0, 0,10, 5,
                                     0, 0, 0, 0, 0, 3, 2, 0, 0, 0, 4, 1, 0,
                                     0, 0, 0, 0, 5, 3, 2, 0, 0, 3, 6, 1, 0,
                                     10,0, 0, 0, 5, 9, 6, 1,10, 4, 8,18, 9,
                                     1, 0, 5, 5, 4, 2, 1, 0, 0, 7,10, 3, 2,
                                     4, 3, 3, 9, 2, 0, 8, 0, 5, 1, 9,11, 2,
                                     2, 2, 2, 6, 1, 8, 0, 0, 4, 0, 6, 8, 0,
                                     0, 0, 0, 1, 0, 0, 0, 0, 0, 4, 3, 0, 2,
                                     8, 0, 0,10, 0, 5, 4, 0, 0, 0, 0,10, 3,
                                     0, 0, 3, 4, 7, 1, 0, 4, 0, 0,10, 2, 3,
                                     0, 4, 6, 8,10, 9, 6, 3, 0,10, 2, 8, 5,
                                     10,1, 1,18, 3,11, 8, 0,10, 2, 8, 0, 8,
                                     5, 0, 0, 9, 2, 2, 0, 2, 3, 3, 5, 8, 0};
        int[] mappings = {9, 12, 11, 2, 8, 3, 7, 10, 6, 5, 1, 0, 4};
        Matrix testMatrix = new ArrayMatrix(13, 13);
        for (int i = 0; i < 13; ++i) {
            for (int j = 0; j < 13; ++j) {
                testMatrix.set(mappings[i], mappings[j],
                               testCorrelations[i*13 +j]);
            }
        }
    }

    @Test public void testCoalsWithNormalization() throws IOException {
        Coals coals = new Coals();
        StringDocument sDoc = new StringDocument(TEST_CORPUS);
        coals.processDocument(sDoc.reader());
        System.getProperties().remove(Coals.DO_NOT_NORMALIZE_PROPERTY);
        coals.processSpace(System.getProperties());

        // 13 x 13 matrix.
        double[] testFinal = {   0,   0,   0,.291,   0,   0,   0,   0,.372,   0,   0,.291,.246,
                                 0,   0,   0,   0,   0,.297,.263,   0,   0,   0,.333,   0,   0,
                                 0,   0,   0,   0,.365,.175,.151,   0,   0,.268,.317,   0,   0,
                              .291,   0,   0,   0,   0,.120,.093,   0,.291,   0,   0,.310,.262,
                                 0,   0,.365,   0,.175,   0,   0,   0,   0,.364,.320,   0,   0,
                                 0,.297,.175,.120,   0,   0,.306,   0,.146,   0,.177,.220,   0,
                                 0,.263,.151,.093,   0,.306,   0,   0,.182,   0,.149,.221,   0,
                                 0,   0,   0,   0,   0,   0,   0,   0,   0,.438,.265,   0,.263,
                              .372,   0,   0,.291,   0,.146,.182,   0,   0,   0,   0,.291,.076,
                                 0,   0,.268,   0,.364,   0,   0,.438,   0,   0,.358,   0,.136,
                                 0,.333,.317,   0,.320,.177,.149,.265,   0,.358,   0,   0,.034,
                              .291,   0,   0,.310,   0,.220,.221,   0,.291,   0,   0,   0,.221,
                              .246,   0,   0,.262,   0,   0,   0,.263,.076,.136,.034,.221,   0};
        int[] mappings = {9, 12, 11, 2, 8, 3, 7, 10, 6, 5, 1, 0, 4};
        Matrix testMatrix = new ArrayMatrix(13, 13);
        for (int i = 0; i < 13; ++i) {
            for (int j = 0; j < 13; ++j)
                testMatrix.set(i, mappings[j], testFinal[i*13 +j]);
        }

        /**
        Matrix resultCorrel = coals.compareToMatrix(testMatrix);
        assertEquals(13, resultCorrel.rows());
        for (int i = 0; i < resultCorrel.rows(); ++i)
            for (int j = 0; j < resultCorrel.columns(); ++j)
                assertEquals(0, resultCorrel.get(i, j), .001);
                */
    }
  
    @Test public void testMaxSize() throws IOException {
        System.setProperty(Coals.MAX_WORDS_PROPERTY, "3");
        System.setProperty(Coals.MAX_DIMENSIONS_PROPERTY, "2");
        Coals coals = new Coals();
        String testDocument = "word word cat dog dog";
        double[] testCorrelations = { 8, 8, 7, 7, 8, 8};
        StringDocument sDoc = new StringDocument(testDocument);
        coals.processDocument(sDoc.reader());
        System.setProperty(Coals.DO_NOT_NORMALIZE_PROPERTY, "");
        coals.processSpace(System.getProperties());
        Matrix resultCorrel =
          coals.compareToMatrix(new ArrayMatrix(3, 2, testCorrelations));
        for (int i = 0; i < resultCorrel.rows(); ++i)
            for (int j = 0; j < resultCorrel.columns(); ++j)
                assertEquals(0, resultCorrel.get(i, j), .0001);
    }
}
