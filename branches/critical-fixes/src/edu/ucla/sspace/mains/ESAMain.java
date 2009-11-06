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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;

import edu.ucla.sspace.esa.ExplicitSemanticAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link ExplicitSemanticAnalysis} (ESA) from
 * the command line.  This class takes in the standard command line arguments
 * found in {@code GenericMain}.
 *
 * @see ExplicitSemanticAnalysis
 *
 * @author David Jurgens
 */
public class ESAMain extends GenericMain {

    private ESAMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
    }

    public static void main(String[] args) {
        ESAMain esa = new ESAMain();
        try {
            esa.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }

    /**
     * {@inheritDoc} 
     */
    public SemanticSpace getSpace() {
        return new ExplicitSemanticAnalysis();
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
         System.out.println(
             "usage: java ESAMain [options] <output-dir>\n"
            + argOptions.prettyPrint());
    }
}
