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
import edu.ucla.sspace.common.SemanticSpaceUtils;

import edu.ucla.sspace.esa.ExplicitSemanticAnalysis;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link ExplicitSemanticAnalysis} (ESA) from
 * the command line.  This class takes in several command line arguments.
 *
 * <ul>
 *
 * <li> {@code --overwrite=<boolean>} specifies whether to overwrite the
 *      existing output files.  The default is {@code true}.  If set to {@code
 *      false}, a unique integer is inserted into the file name.
 *
 * <li> {@code --verbose | -v} specifies whether to print runtime
 *      information to standard out
 *
 * </ul>
 *
 * <p>
 *
 *
 * @see ExplicitSemanticAnalysis
 *
 * @author David Jurgens
 */
public class ESAMain extends GenericMain {
    private ESAMain() {
    }

    /**
     * Addes the {@code verbose} and {@code overwrite} options.
     */
    protected ArgOptions setupOptions() {
	ArgOptions options = new ArgOptions();
	options.addOption('w', "overwrite", "specifies whether to " +
			  "overwrite the existing output", true, "BOOL",
			  "Program Options");

	options.addOption('v', "verbose", "prints verbose output",
			  false, null, "Program Options");
	addExtraOptions(options);
	return options;
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

    public Properties setupProperties() {
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();
	return props;
    }

    public void run(String[] args) throws IOException {
	if (args.length == 0) {
	    usage();
	    System.exit(1);
	}
	argOptions.parseOptions(args);
	
	if (argOptions.numPositionalArgs() != 2) {
	    usage();
	    return;
	}

	File wikipediaSnapshotFile = new File(argOptions.getPositionalArg(0));

	File outputDir = new File(argOptions.getPositionalArg(1));
	if (!outputDir.isDirectory()){
	    throw new IllegalArgumentException(
		"output directory is not a directory: " + outputDir);
	}

	// process command line args
	verbose = argOptions.hasOption('v') || argOptions.hasOption("verbose");

	boolean overwrite = true;
	if (argOptions.hasOption("overwrite")) {
	    overwrite = argOptions.getBooleanOption("overwrite");
	}

	// process the Wikipedia snapshot
	ExplicitSemanticAnalysis esa = new ExplicitSemanticAnalysis();
	esa.processDocument(new BufferedReader(
			        new FileReader(wikipediaSnapshotFile)));

	// perform any remaining processing
	esa.processSpace(setupProperties());

	// determine where the space will go
	File output = (overwrite)
	    ? new File(outputDir, esa.getSpaceName()+ ".sspace")
	    : File.createTempFile(esa.getSpaceName(), 
				  ".sspace", outputDir);

	// print the space
	long startTime = System.currentTimeMillis();
	SemanticSpaceUtils.printSemanticSpace(esa, output);
	long endTime = System.currentTimeMillis();
	verbose("printed space in %.3f seconds%n",
		((endTime - startTime) / 1000d));

    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java ESAMain [options] <wikipedia-snapshot> <output-dir>\n"
	    + argOptions.prettyPrint());
    }
}
