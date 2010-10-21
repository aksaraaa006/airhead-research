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

package edu.ucla.sspace.matrix;

import java.io.*;
import java.util.*;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class SvdTests {

    
    @Test public void testSvdlibj() throws Exception {
        Matrix m = new YaleSparseMatrix(4,2);
        m.set(0,0,2);
        m.set(0,1,4);
        m.set(1,0,1);
        m.set(1,1,3);
        Matrix[] arr = SVD.svd(m, SVD.Algorithm.SVDLIBJ, 2);



        // Test the U matrix
        Matrix u = arr[0];
        assertEquals(4, u.rows());
        assertEquals(2, u.columns());
        assertEquals(.82, u.get(0,0), .01);
        assertEquals(-.58, u.get(0,1), .01);
        assertEquals(.58, u.get(1,0), .01);
        assertEquals(.82, u.get(1,1), .01);

        // Test the S matrix
        Matrix s = arr[1];
        assertEquals(2, s.rows());
        assertEquals(2, s.columns());
        assertEquals(5.47, s.get(0,0), .01);
        assertEquals(.37, s.get(1,1), .01);


        // Test the V matrix
        Matrix v = arr[2];        
        assertEquals(2, v.rows());
        assertEquals(2, v.columns());
        assertEquals(.40, v.get(0,0), .01);
        assertEquals(.91, v.get(0,1), .01);
        assertEquals(-.91, v.get(1,0), .01);
        assertEquals(.40, v.get(1,1), .01);
    }

}