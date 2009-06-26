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
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * A utility for asserting what tokens are valid and invalid within a stream of
 * tokens.  A filter may be either inclusive or exclusive.<p>
 *
 * An inclusive filter will accept only those tokens with which it was
 * initialized.  For an example, an inclusive filter initialized with all of the
 * words in the english dictionary would exclude all misspellings or foreign
 * words in a token stream.<p>
 *
 * An exclusive filter will aceept only those tokens that are not in set with
 * which it was initialized.  An exclusive filter is often used with a list of
 * common words that should be excluded, which is also known as a "stop
 * list."<p>
 *
 * {@code TokenFilter} instances may be combined into a linear chain of filters.
 * This allows for a highly configurable filter to be made from mulitple rules.
 * Chained filters are created in a linear order and each filter must accept the
 * token for the last filter to return {@code}.  If the any of the earlier
 * filters return {@code false}, then the token is not accepted.<p>
 *
 * This class also provides a static utility function {@link
 * #loadFromSpecification(String) loadFromSpecification} for initializing a
 * chain of filters from a text configuration.  This is intended to facility
 * command-line tools that want to provide easily configurable filters.  Token
 * filter configurations are specified as a comman-separated list of file names,
 * where each file name has an optional string with values: {@code inclusive} or
 * {@code exclusive}, which species whether the token are to be used for an
 * exclusive filter.  <b>The default value is include</b>.  An example
 * configuration might look like:
 * <tt>english-dictionary.txt=include,stop-list.txt=exclude</tt>
 *
 * @see FilteredIterator
 */
public class TokenFilter {

    /**
     * The set of tokens used to filter the output
     */
    private final Set<String> tokens;

    /**
     * {@code true} if the returned tokens must not be in the filter set
     */
    private final boolean excludeTokens;

    /**
     * A filter that is to be applied before this filter when determining
     * whether a token should  be accepted
     */
    private TokenFilter parent;

    /**
     * Constructs a filter that accepts only those tokens present in {@code tokens}.
     */
    public TokenFilter(Set<String> tokens) {
	this(tokens, false, null);
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
    public TokenFilter(Set<String> tokens, boolean excludeTokens) {
	this(tokens, excludeTokens, null);
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
    public TokenFilter(Set<String> tokens, boolean excludeTokens, 
		       TokenFilter parent) {
	this.tokens = tokens;
	this.excludeTokens = excludeTokens;
	this.parent = parent;
    }

    /**
     * Returns {@code true} if the token is valid according to the configuration
     * of this filter.
     *
     * @param token a token to be considered
     *
     * @return {@code true} if this token is valid
     */
    public boolean accept(String token) {
	return (parent == null || parent.accept(token)) &&
		tokens.contains(token) ^ excludeTokens;
    }

    /**
     * Creates a chained filter by accepting the subset of whatever {@code
     * parent} accepts less what tokens this filter rejects.  
     *
     * @param parent a filter to be applied before determining whether a token
     *        is to be accepted
     *
     * @return the previous parent filter or {@code null} if one had not been
     *         assigned
     */
    public TokenFilter combine(TokenFilter parent) {
	TokenFilter oldParent = parent;
	this.parent = parent;
	return oldParent;
    }
    
    /**
     * Loads a series of chained {@code TokenFilter} instances from the
     * specified configuration string.
     * 
     * Token filter configurations are specified as a file name containing a list
     * of tokens, with an optional boolean value specifying whether the file is
     * to be used to exclude all tokens not in the file, or exclude those that
     * are in the file (i.e. a stop list). The format specified as:
     * <tt>file[=boolean][,file...]</tt> where each file to be used is separated
     * by a <b>,</b>, and the boolean value is appended with an <b>=</b> sign.
     *
     * @param configuration a comman-separated list of file names, where each
     *        file name has an optional <tt>=boolean</tt> flag that is {@code
     *        true} if the token are to be used for an exclusive filter
     *
     * @return the chained TokenFilter instance made of all the specification,
     *         or {@code null} if the configuration did not specify any filters
     *
     * @throws IOError if any error occurs when reading the word list files
     */
    public static TokenFilter loadFromSpecification(String configuration) {

	TokenFilter toReturn = null;

	// multiple filter files are specified using a ',' to separate them
	String[] fileAndOptionalFlag = configuration.split(",");

	for (String s : fileAndOptionalFlag) {
	    // If the words in the file are manually specified to be applied in
	    // a specific way, then the string will contain a '='.  Look for the
	    // last index of '=' in case the file name itself contains that
	    // character
	    int eqIndex = s.lastIndexOf('=');
	    String filename = null;
	    boolean exclude = false;
	    if (eqIndex > 0) {
		filename = s.substring(0, eqIndex);
		String flag = s.substring(eqIndex + 1);
		if (flag.equals("include"))
		    exclude = false;
		else if (flag.equals("exclude"))
		    exclude = true;
		else {
		    throw new IllegalArgumentException(
			"unknown filter parameter: " + s);
		}
	    }
	    else {
		filename = s;
	    }
	    
	    // load the words in the file
	    Set<String> words = new HashSet<String>();
	    try {
		BufferedReader br = 
		    new BufferedReader(new FileReader(filename));
		for (String line = null; (line = br.readLine()) != null; ) {
		    for (String token : line.split("\\s+")) {
			words.add(token);
		    }
		}
		br.close();
	    } catch (IOException ioe) {
		// rethrow since filter error is fatal to correct execution
		throw new IOError(ioe);
	    }
	    
	    toReturn = new TokenFilter(words, exclude, toReturn);
	}

	return toReturn;
    }
}

