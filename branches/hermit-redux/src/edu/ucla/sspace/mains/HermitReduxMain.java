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

import edu.ucla.sspace.hermit.HermitRedux;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashSet;
import java.util.Properties;
import java.util.Set;


/**
 * An executable class for running {@link HermitRedux} from the
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
 *
 *   <li> {@code -F}, {@code --tokenFilter=FILE[include|exclude][,FILE...]}
 *        specifies a list of one or more files to use for {@link
 *        edu.ucla.sspace.text.TokenFilter filtering} the documents.  An option
 *        flag may be added to each file to specify how the words in the filter
 *        filter should be used: {@code include} if only the words in the filter
 *        file should be retained in the document; {@code exclude} if only the
 *        words <i>not</i> in the filter file should be retained in the
 *        document.
 *
 *   <li> {@code -S}, {@code --svdAlgorithm}={@link
 *        edu.ucla.sspace.matrix.SVD.Algorithm} species a specific {@code
 *        SVD.Algorithm} method to use when reducing the dimensionality in LSA.
 *        In general, users should not need to specify this option, as the
 *        default setting will choose the fastest algorithm available on the
 *        system.  This is only provided as an advanced option for users who
 *        want to compare the algorithms' performance or any variations between
 *        the SVD results.
 *
 *   </ul>
 *
 * <li><u>Program Options</u>:
 *   <ul>
 *
 *   <li> {@code -o}, {@code --outputFormat=}<tt>text|binary}</tt> Specifies the
 *        output formatting to use when generating the semantic space ({@code
 *        .sspace}) file.  See {@link edu.ucla.sspace.common.SemanticSpaceUtils
 *        SemanticSpaceUtils} for format details.
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
public class HermitReduxMain extends GenericMain {

    private HermitRedux hermit = null;

    /**
     *
     */
    private HermitReduxMain() { }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
	options.addOption('n', "dimensions", 
			  "the number of dimensions in the semantic space",
			  true, "INT", "Algorithm Options"); 
	options.addOption('S', "stopWords", 
			  "a file of stop words",
			  true, "FILE", "Input Options"); 
	options.addOption('D', "contextFileDir", 
			  "a directory to write the context files to",
			  true, "FILE", "Advanced Options"); 
    }

    public static void main(String[] args) {
	HermitReduxMain hermitMain = new HermitReduxMain();
	try {
	    hermitMain.run(args);
	}
	catch (Throwable t) {
	    t.printStackTrace();
	}
    }
    
    protected SemanticSpace getSpace() {
        try {
            hermit = new HermitRedux();
            if (argOptions.hasOption("stopWords")) {
                String stopWordsFile = argOptions.getStringOption("stopWords");
                Set<String> stopWords = new HashSet<String>();
                BufferedReader br = 
                    new BufferedReader(new FileReader(stopWordsFile));
                for (String line = null; (line = br.readLine()) != null; )
                    stopWords.add(line);
                br.close();
                hermit.setExcludedWords(stopWords);
            } 
            return hermit;
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Returns the {@likn SSpaceFormat.BINARY binary} format as the default
     * format of a {@code LatentSemanticAnalysis} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.BINARY;
    }

    protected Properties setupProperties() {
	// use the System properties in case the user specified them as
	// -Dprop=<val> to the JVM directly.
	Properties props = System.getProperties();

	if (argOptions.hasOption("dimensions")) {
	    props.setProperty(HermitRedux.DIMENSIONS_PROPERTY,
			      argOptions.getStringOption("dimensions"));
	}
	return props;
    }

    /**
     * Prints the instructions on how to execute this program to standard out.
     */
    public void usage() {
 	System.out.println(
            "usage: java HermitReduxMain [options] <output-dir>\n" + 
            argOptions.prettyPrint() + "\n" +
            
            // LSA Specifics
            "Note that if this class is being invoked from a .jar" +
            " (e.g. lsa.jar) and JAMA\nis to be used for computing the SVD," +
            " then the path to the JAMA .jar file must\nbe specified using the"+
            " system property \"jama.path\".  To set this on the\ncommand-line,"
            + " use -Djama.path=<.jar location>.\n\n" +
            "Similarly, if COLT is to be used with this class being invoked" +
            " from a .jar,\n then the \"colt.path\" property must be set.\n\n" +
            
            "The --svdAlgorithm provides a way to manually specify which " + 
            "algorithm should\nbe used internally.  This option should not be" +
            " used normally, as LSA will\nselect the fastest algorithm " +
            "available.  However, in the event that it\nis needed, valid" +
            " options are: SVDLIBC, MATLAB, OCTAVE, JAMA and COLT\n\n" +
            
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
