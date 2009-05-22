/*
 * Copyright 2009 Grace Park
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

import edu.ucla.sspace.grefenstette.Grefenstette;

import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link Grefenstette} algorithm from the
 * command line.  This class provides several options:
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -s}, {@code --sentenceFile=FILE[,FILE...]} a file where each
 *        line is a single sentence
 *
 *   </ul>
 * <!--
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   </ul> 
 * -->
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link SemanticSpaceUtils} for format details.
 *   <!--
 *   <li> {@code -t}, {@code --threads=INT} the number of threads to use
 *   -->
 *   <li> {@code -v}, {@code --verbose} prints verbose output
 *
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output
 *   </ul>
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code greffenstette.sspace}.
 * If {@code overwrite} was set to {@code true}, this file will be replaced for
 * each new semantic space.  Otherwise, a new output file of the format {@code
 * greffenstette<number>.sspace} will be created, where {@code
 * <number>} is a unique identifier for that program's invocation.  The output
 * file will be placed in the directory specified on the command line.
 *
 * <p> <!--
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *  -->
 *
 * @see Grefenstette
 * 
 * @author Grace Park
 */
public class GrefenstetteMain extends GenericMain {

    private Properties props;

    private GrefenstetteMain() {

    }

    /**
     * Adds the options for running the {@code Grefenstette} algorithm
     */
    @Override
    protected ArgOptions setupOptions() {
	ArgOptions options = new ArgOptions();
	options.addOption('s', "sentenceFile", 
			  "a file where each line is a sentence", true,
			  "FILE[,FILE...]", "Required (at least one of)");

	options.addOption('o', "outputFormat", "the .sspace format to use",
			  true, "{text|binary}", "Program Options");
	// options.addOption('t', "threads", "the number of threads to use",
	// 			  true, "INT", "Program Options");
	options.addOption('w', "overwrite", "specifies whether to " +
			  "overwrite the existing output", true, "BOOL",
			  "Program Options");
	options.addOption('v', "verbose", "prints verbose output",
			  false, null, "Program Options");
	addExtraOptions(options);
	return options;
    }

    /**
     * Currently adds no extra options
     */
    protected void addExtraOptions(ArgOptions options) {

    }
    
    /**
     * Returns an instance of the {@link Grefenstette} algorithm.
     */
    public SemanticSpace getSpace() {
	return new Grefenstette();
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java Grefenstette [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}