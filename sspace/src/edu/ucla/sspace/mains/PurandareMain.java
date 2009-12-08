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
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.purandare.PurandareFirstOrder;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;


/**
 * An executable class for running {@link PurandareFirstOrder} from the command
 * line.  See the Purandare and Pedersen <a
 * href="http://code.google.com/p/airhead-research/wiki/PurandareAndPedersen">wiki
 * page</a> for details on running this class from the command line. <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see PurandareFirstOrder
 *
 * @author David Jurgens
 */
public class PurandareMain extends GenericMain {

    private PurandareMain() { }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) { }

    public static void main(String[] args) {
	PurandareMain lsa = new PurandareMain();
	try {
	    lsa.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    protected SemanticSpace getSpace() {
        return new PurandareFirstOrder();
    }

    /**
     * Returns the {@likn SSpaceFormat.BINARY binary} format as the default
     * format of a {@code PurandareFirstOrder} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.SPARSE_BINARY;
    }

    protected Properties setupProperties() {
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();
	return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java PurandareMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint() + "\n" +

            // Token Filter Description
            "Token filter configurations are specified as a comman-separated " +
	    "list of file\nnames, where each file name has an optional string" +
	    " with values:inclusive or\nexclusive, which species whether the" +
	    " token are to be used for an exclusive\nfilter. The default " +
	    "value is include. An example configuration might look like:\n" +
	    "  --tokenFilter=english-dictionary.txt=include," +
	    "stop-list.txt=exclude" +
            
            // Compound Tokens Description
	    "\n\nThe -C, --compoundWords option specifies a file name of " +
	    "multiple tokens that\nshould be counted as a single word, e.g." +
	    " \"white house\".  Each compound\ntoken should be specified on " +
	    "its own line." +

            // S-Space Format
            "\n\nThe output of the program is a semantic space stored in the " +
            "specified format.\nValid options are text, sparse_text, binary, " +
            "and sparse_binary." +

            // Tag
	    "\n\nReport bugs to <s-space-research-dev@googlegroups.com>");
    }
}
