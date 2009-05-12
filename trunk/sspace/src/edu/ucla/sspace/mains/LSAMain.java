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

import edu.ucla.sspace.lsa.LatentSemanticAnalysis;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link LatentSemanticAnalysis} (LSA) from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 *
 * <li><u>Required (at least one of)</u>:
 *   <ul>
 *
 *   <li> {@code -d}, {@code --docFile=FILE[,FILE...]} a file where each line is
 *        a document.  This is the preferred input format for large corpora
 *
 *   <li> {@code -f}, {@code --fileList=FILE[,FILE...]} a list of document files
 *        where each file is specified on its own line.
 *
 *   </ul>
 * 
 * <li><u>Algorithm Options</u>:
 *   <ul>
 *
 *   <li> {@code --dimensions=<int>} how many dimensions to use for the LSA
 *        vectors.  See {@link LatentSemanticAnalysis} for default value
 *
 *   <li> {@code --preprocess=<class name>} specifies an instance of {@link
 *        edu.ucla.sspace.lsa.MatrixTransformer} to use in preprocessing the
 *        word-document matrix compiled by LSA prior to computing the SVD.  See
 *        {@link LatentSemanticAnalysis} for default value
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link SemanticSpaceUtils} for format details.
 *
 *   <li> {@code -t}, {@code --threads=INT} how many threads to use when processing the
 *        documents.  The default is one per core.
 * 
 *   <li> {@code -w}, {@code --overwrite=BOOL} specifies whether to overwrite
 *        the existing output files.  The default is {@code true}.  If set to
 *        {@code false}, a unique integer is inserted into the file name.
 *
 *   <li> {@code -v}, {@code --verbose}  specifies whether to print runtime
 *        information to standard out
 *
 *   </ul>
 *
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * lsa-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code lsa-semantic-space<number>.sspace} will be
 * created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see LatentSemanticAnalysis
 * @see edu.ucla.sspace.lsa.MatrixTransformer MatrixTransformer
 *
 * @author David Jurgens
 */
public class LSAMain extends GenericMain {
    private LSAMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    public void addExtraOptions(ArgOptions options) {
	options.addOption('n', "dimensions", 
			     "the number of dimensions in the semantic space",
			     true, "INT"); 
	options.addOption('p', "preprocess", "a MatrixTransform class to "
			     + "use for preprocessing", true, "CLASSNAME");
    }

    public static void main(String[] args) {
	LSAMain lsa = new LSAMain();
	try {
	    lsa.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    public SemanticSpace getSpace() {
      try {
        return new LatentSemanticAnalysis();
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }

    public Properties setupProperties() {
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();

	if (argOptions.hasOption("dimensions")) {
	    props.setProperty(LatentSemanticAnalysis.LSA_DIMENSIONS_PROPERTY,
			      argOptions.getStringOption("dimensions"));
	}

	if (argOptions.hasOption("preprocess")) {
	    props.setProperty(LatentSemanticAnalysis.MATRIX_TRANSFORM_PROPERTY,
			      argOptions.getStringOption("preprocess"));
	}
    return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
 	    "usage: java LSAMain [options] <output-dir>\n" + 
	    argOptions.prettyPrint());
    }
}
