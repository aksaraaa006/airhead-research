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

public class MatlabSparseFileIteratorTests {

    @Test public void testIterator() throws Exception {
        File f = getMatlabFile();
        Iterator<MatrixEntry> it = new MatlabSparseFileIterator(f);
        MatrixEntry me = it.next();
        // Col 0
        assertEquals(0, me.column());
        assertEquals(0, me.row());
        me = it.next();
        assertEquals(0, me.row());
        assertEquals(2, me.column());
        me = it.next();
        // Col 1
        assertEquals(1, me.row());
        assertEquals(1, me.column());
        me = it.next();
        assertEquals(1, me.row());
        assertEquals(2, me.column());
        me = it.next();
        // Col 2
        assertEquals(2, me.row());
        assertEquals(0, me.column());
        me = it.next();
        assertEquals(2, me.row());
        assertEquals(2, me.column());

        assertFalse(it.hasNext());
    }
    
    public static File getMatlabFile() throws Exception {
	File f = File.createTempFile("unit-test",".dat");
	PrintWriter pw = new PrintWriter(f);
	pw.println("1 1 2.3");
	pw.println("1 3 4.2");
	pw.println("2 2 1.3");
	pw.println("2 3 2.2");
	pw.println("3 1 3.8");
	pw.println("3 3 0.5"); 
	pw.close();
	return f;
    }
}
