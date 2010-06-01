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

import org.junit.Ignore;
import org.junit.Test;

import static org.junit.Assert.*;


public class SubjObjPathAcceptorTest {

    @Test public void testArbitraryLink() {
        DependencyPathAcceptor acceptor = new SubjObjPathAcceptor();
        assertFalse(acceptor.acceptLink("sfsdf", "not a relation", "SDF"));
    }

    @Test public void testSubjLink() {
        DependencyPathAcceptor acceptor = new SubjObjPathAcceptor();
        assertTrue(acceptor.acceptLink("N", "SBJ", "V"));
    }

    @Test public void testObjLink() {
        DependencyPathAcceptor acceptor = new SubjObjPathAcceptor();
        assertTrue(acceptor.acceptLink("N", "OBJ", "N"));
    }

    @Test public void testNormalLink() {
        DependencyPathAcceptor acceptor = new SubjObjPathAcceptor();
        assertFalse(acceptor.acceptLink("N", "GEN", "NMOD"));
    }
}

