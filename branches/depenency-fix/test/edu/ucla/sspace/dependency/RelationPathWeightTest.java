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

import edu.ucla.sspace.util.Pair;

import java.util.LinkedList;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class RelationPathWeightTest extends AbstractPathTest {

    @Test public void testSubjPath() {
        String[][] pathString = {{"cat", "SBJ"}, {"dog", "OBJ"},
                                 {"meow", "nor a relation"}};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(5, weighter.scorePath(path), .000001);
    }

    @Test public void testObjPath() {
        String[][] pathString = {{"cat", "GEN"}, {"dog", "OBJ"},
                                 {"meow", "nor a relation"}};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(4, weighter.scorePath(path), .000001);
    }

    @Test public void testOblPath() {
        String[][] pathString = {{"cat", "gar"}, {"dog", "OBL"},
                                 {"meow", "GEN"}};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(3, weighter.scorePath(path), .000001);
    }

    @Test public void testGenPath() {
        String[][] pathString = {{"cat", "GEN"}, {"dog", "NMOD"},
                                 {"meow", "nor a relation"}};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(2, weighter.scorePath(path), .000001);
    }

    @Test public void testSimplePath() {
        String[][] pathString = {{"cat", "Rel"}};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(1, weighter.scorePath(path), .000001);
    }

    @Test public void testEmptyPath() {
        String[][] pathString = {};
        List<Pair<String>> path = makePath(pathString);
        DependencyPathWeight weighter = new RelationPathWeight();
        assertEquals(1, weighter.scorePath(path), .000001);
    }
}
