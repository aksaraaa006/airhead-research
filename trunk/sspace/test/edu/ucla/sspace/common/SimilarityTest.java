/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.common;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;

public class SimilarityTest {

    @Test public void testCosineSimDoubleSame() {
	double[] a = new double[] { 1d, 1d, 1d, 1d };
	double[] b = new double[] { 1d, 1d, 1d, 1d };

	assertEquals(1d, Similarity.cosineSimilarity(a, b), 0);
    }

    @Test public void testCosineSimDoubleOrthogonal() {
	double[] a = new double[] { 1d, 1d, 1d, 1d };
	double[] b = new double[] { 0,  0,  0,  0  };

	assertEquals(0d, Similarity.cosineSimilarity(a, b), 0);
    }

    @Test public void testCosineSimDoubleOpp() {
	double[] a = new double[] { 1d, 1d, 1d, 1d };
	double[] b = new double[] { -1d, -1d, -1d, -1d };

	assertEquals(-1d, Similarity.cosineSimilarity(a, b), 0);
    }

}