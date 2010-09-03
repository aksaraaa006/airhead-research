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

package edu.ucla.sspace.text;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;

import org.apache.hadoop.fs.FileStatus;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;


/**
 * A {@link TokenFilter} designed for running within the Hadoop evironment.  See
 * {@link TokenFilter} for a complete list of behavor and features.
 */
public class HadoopTokenFilter extends TokenFilter {

    /**
     * Constructs a filter that accepts only those tokens present in {@code tokens}.
     */
    public HadoopTokenFilter(Set<String> tokens) {
	super(tokens, false, null);
    }

    /**
     * Constructs a filter using {@code tokens} that if {@code excludeTokens} is
     * {@code false} will accept those in {@code tokens}, or if {@code
     * excludeTokens} is {@code true}, will accept those that are <i>not</i> in
     * {@code tokens}.
     *
     * @param tokens the set of tokens to use in filtering the output
     * @param excludeTokens {@code true} if tokens in {@code tokens} should be
     *        excluded, {@code false} if only tokens in {@code tokens} should
     *        be included
     */
    public HadoopTokenFilter(Set<String> tokens, boolean excludeTokens) {
	super(tokens, excludeTokens, null);
    }

    /**
     * Constructs a chained filter that accepts the subset of what the parent
     * accepts after applying its own filter to any tokens that the parent
     * accepts.  Note that if the parent does not accept a token, then this
     * filter will not either.
     *
     * @param tokens the set of tokens to use in filtering the output
     * @param excludeTokens {@code true} if tokens in {@code tokens} should be
     *        excluded, {@code false} if only tokens in {@code tokens} should
     *        be included
     * @param parent a filter to be applied before determining whether a token
     *        is to be accepted
     */
    public HadoopTokenFilter(Set<String> tokens, boolean excludeTokens, 
		       TokenFilter parent) {
        super(tokens, excludeTokens, parent);
    }
    
    /**
     * Loads a series of chained {@code TokenFilter} instances from the
     * specified configuration string.<p>
     * 
     * A configuration lists sets of files that contain tokens to be included or
     * excluded.  The behavior, {@code include} or {@code exclude} is specified
     * first, followed by one or more file names, each separated by colons.
     * Multiple behaviors may be specified one after the other using a {@code ,}
     * character to separate them.  For example, a typicaly configuration may
     * look like: "include=top-tokens.txt,test-words.txt:exclude=stop-words.txt"
     * <b>Note</b> behaviors are applied in the order they are presented on the
     * command-line.
     *
     * @param configuration a token filter configuration
     *
     * @param hadoopConf the configuration used by the currently running program
     *        within the Hadoop environment.
     * @return the chained TokenFilter instance made of all the specification,
     *         or {@code null} if the configuration did not specify any filters
     *
     * @throws IOError if any error occurs when reading the word list files
     */
    public static TokenFilter loadFromSpecification(Configuration hadoopConf, 
                                                    String configuration) {

	TokenFilter toReturn = null;

	// multiple filter are separated by a ':'
	String[] filters = configuration.split(",");

	for (String s : filters) {
            String[] optionAndFiles = s.split("=");
            if (optionAndFiles.length != 2)
                throw new IllegalArgumentException(
                    "Invalid number of filter parameters: " + s);
            
            String behavior = optionAndFiles[0];
            boolean exclude = behavior.equals("exclude");
            // Sanity check that the behavior was include
            if (!exclude && !behavior.equals("include"))
                throw new IllegalArgumentException(
                    "Invalid filter behavior: " + behavior);
                
            String[] files = optionAndFiles[1].split(":");
            
	    // Load the words in the file(s)
	    Set<String> words = new HashSet<String>();
	    try {
                // Load the hadoop file system for reading in all the resources
                FileSystem fs = FileSystem.get(hadoopConf);

                for (String f : files) {
                    Path p = new Path(f);
                    if (!fs.exists(p)) {
                        throw new IllegalArgumentException(
                            "Cannot create filter using non-existent file: "+f);
                    }

                    BufferedReader br = new BufferedReader(new InputStreamReader(
                        fs.open(p)));
                    for (String line = null; (line = br.readLine()) != null; ) 
                        words.add(line);
                    br.close();
                }
	    } catch (IOException ioe) {
		// rethrow since filter error is fatal to correct execution
		throw new IOError(ioe);
	    }
	    
            // Chain the filters on top of each other
	    toReturn = new HadoopTokenFilter(words, exclude, toReturn);
	}

	return toReturn;
    }
}

