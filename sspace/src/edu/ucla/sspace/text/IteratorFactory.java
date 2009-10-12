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
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.StringReader;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

/**
 * A factory class for generating {@code Iterator<String>} tokenizers for
 * streams of tokens such as {@link BufferedReader} instances.  This class
 * manages all of the internal configurations and properties for how to
 * tokenize.  {@link edu.ucla.sspace.common.SemanticSpace SemanticSpace}
 * instances are encouraged to utilize this class for creating iterators over
 * the tokens in the documents rather than creating the iterators themsevles, as
 * this class may contain additional settings to be applied to which the {@link
 * edu.ucla.sspace.common.SemanticSpace SemanticSpace} instance would not have
 * access.
 *
 * <p>
 *
 * This class offers two configurable parameters for controlling the tokenizing
 * of streams.
 *
 * <dl style="margin-left: 1em">
 *
 * <dt> <i>Property:</i> <code><b>{@value #TOKEN_FILTER_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code null}
 *
 * <dd style="padding-top: .5em">This property sets a configuration of a {@link
 *      TokenFilter} that should be applied to all token streams.<p>
 *
 *
 * <dt> <i>Property:</i> <code><b>{@value #COMPOUND_TOKENS_FILE_PROPERTY}
 *      </b></code> <br>
 *      <i>Default:</i> {@code null}
 *
 * <dd style="padding-top: .5em">This property sets the name of a file that
 *      contains all of the recognized compound words (or multi-token tokens)
 *      recognized by any iterators returned by this class.<p>
 *
 * </dl> <p>
 *
 * <p> 
 *
 * Note that tokens will be combined into a compound token prior to filtering.
 * Therefore if filtering is enabled, any compound token should also be
 * permitted by the word filter.<p>
 *
 * Note that this class provides two distinct ways to access the token streams
 * if filtering is enabled.  The {@link #tokenize(BufferedReader) tokenize}
 * method will filter out any tokens without any indication.  This can
 * significantly alter the original ordering of the token stream.  For
 * applications where the original ordering needs to be preserved, the {@link
 * #tokenizeOrdered(BufferedReader) tokenizeOrdered} method should be used
 * instead.  This method will return the {@code IteratorFactor.EMTPY_TOKEN}
 * value to indicate that a token has been removed.  This preserves the original
 * token ordering without requiring applications to do the filtering themselves.
 * Note that If filtering is disabled, the two methods will return the same
 * tokens.<p>
 *
 * This class is thread-safe.
 *
 * @see WordIterator
 * @see TokenFilter
 * @see CompoundWordIterator
 */
public class IteratorFactory {

    /**
     * The signifier that stands in place of a token has been removed from an
     * iterator's token stream by means of a {@link TokenFilter}.  Tokens
     * returned by {@link #tokenizeOrdered(BufferedReader) tokenizeOrdered} may
     * be checked against this value to determine whether a token at that
     * position in the stream would have been returned but was removed.
     */
    public static final String EMPTY_TOKEN = "";

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX =
	"edu.ucla.sspace.text.TokenizerFactory";

    /**
     * Specifies the {@link TokenFilter} to apply to all iterators generated by
     * this factory
     */
    public static final String TOKEN_FILTER_PROPERTY = 
	PROPERTY_PREFIX + ".tokenFilter";

    /**
     * Specifies the name of a file that contains all the recognized compound
     * tokens
     */
    public static final String COMPOUND_TOKENS_FILE_PROPERTY = 
	PROPERTY_PREFIX + ".compoundTokens";
    
    /**
     * An optional {@code TokenFilter} to use to remove tokens from document
     */
    private static TokenFilter filter;
    
    /**
     * A mapping from a thread that is currently processing tokens to the {@link
     * CompoundWordIterator} doing the tokenizing if compound word support is
     * enabled.  This mapping is required for two reasons.  One to reduce the
     * overhead of creating {@code CompoundWordIterators} by calling {@code
     * reset} on them; and two, to provide a way for any updates to the list of
     * compound words to propagate to the threads that process them.
     */
    private static final Map<Thread,CompoundWordIterator> compoundIterators
	= new HashMap<Thread,CompoundWordIterator>();

    /**
     * The set of compound tokens recognized by the system or {@code null} if
     * none are recognized
     */
    private static Set<String> compoundTokens = null;

    /**
     * Uninstantiable
     */
    private IteratorFactory() { }

    /**
     * Reconfigures the type of iterator returned by this factory based on the
     * specified properties.
     */
    public static synchronized void setProperties(Properties props) {
	String filterProp = 
	    props.getProperty(TOKEN_FILTER_PROPERTY);
	filter = (filterProp != null)
	    ? TokenFilter.loadFromSpecification(filterProp)
	    : null;

	String compoundTokensProp = 
	    props.getProperty(COMPOUND_TOKENS_FILE_PROPERTY);
	if (compoundTokensProp != null) {

	    File compoundTokensFile = new File(compoundTokensProp);
	    if (!compoundTokensFile.exists()) {
		throw new IllegalArgumentException(COMPOUND_TOKENS_FILE_PROPERTY
		    + " is set to a non-existant file: " + compoundTokensProp);
	    }
	    
	    // Load the tokens from file
	    compoundTokens = new LinkedHashSet<String>();
	    try {
		BufferedReader br = 
		    new BufferedReader(new FileReader(compoundTokensFile));
		for (String line = null; (line = br.readLine()) != null; ) {
		    compoundTokens.add(line);
		}
		// For any currently processing threads, update their mapped
		// iterator with the new set of tokens
		for (Map.Entry<Thread,CompoundWordIterator> e
			 : compoundIterators.entrySet()) {
		    // Create an empy dummy BufferedReader, which will be
		    // discarded upon the next .reset() call to the iterator
		    BufferedReader dummyBuffer = 
			new BufferedReader(new StringReader(""));
		    e.setValue(
			new CompoundWordIterator(dummyBuffer, compoundTokens));
		}
	    } catch (IOException ioe) {
		// rethrow
		throw new IOError(ioe);
	    }
	}
	// If the user did not specify a set of compound tokens, null out the
	// set, in the event that there was one previously
	else {
	    compoundTokens = null;
	}
    }

    /**
     * Tokenizes the contents of the reader according to the system
     * configuration and returns an iterator over all the tokens, excluding
     * those that were removed by any configured {@link TokenFilter}.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over all of the optionally-filtered tokens in the
     *         reader
     */
    public static Iterator<String> tokenize(BufferedReader reader) {
	Iterator<String> baseIterator = getBaseIterator(reader);

	// If a filter is enabled, wrap the base tokenizer
	return (filter == null) 
	    ? baseIterator : new FilteredIterator(baseIterator, filter);	
    }
    
    /**
     * Tokenizes the contents of the reader according to the system
     * configuration and returns an iterator over all the tokens where any
     * removed tokens have been replaced with the {@code
     * IteratorFactory.EMPTY_TOKEN} value.  Tokens returned by this method may
     * be checked against this value to determine whether a token at that
     * position in the stream would have been returned but was removed.  In
     * doing this, the original order and positioning is retained.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over all of the tokens in the reader where any tokens
     *         removed due to filtering have been replaced with the {@code
     *         IteratorFactory.EMPTY_TOKEN} value
     */
    public static Iterator<String> tokenizeOrdered(BufferedReader reader) {
	Iterator<String> baseIterator = getBaseIterator(reader);

	// If a filter is enabled, wrap the base tokenizer
	return (filter == null) 
	    ? baseIterator
	    : new OrderPreservingFilteredIterator(baseIterator, filter);
    }

    /**
     * Returns an iterator for the basic tokenization of the stream before
     * filtering has been applied to the tokens.
     *
     * @param reader a reader whose contents are to be tokenized
     *
     * @return an iterator over the tokens in the stream
     */
    private static Iterator<String> getBaseIterator(BufferedReader reader) {
	// The base iterator is how the stream will be tokenized prior to any
	// filtering
	Iterator<String> baseIterator = null;

	// If a Set of compound tokens has been set, then create the underlying
	// iterator to recognize those tokens
	if (compoundTokens != null) {

	    // Because the initialization step for a CWI has some overhead, use
	    // the reset to keep the same tokens.  However, multiple threads may
	    // be each using their own CWI, so keep Thread-local storage of what
	    // CWI is being used to avoid resetting another thread's iterator.
	    CompoundWordIterator cwi = 
		compoundIterators.get(Thread.currentThread());
	    if (cwi == null) {
		cwi = new CompoundWordIterator(reader, compoundTokens);
		compoundIterators.put(Thread.currentThread(), cwi);
	    }
	    else {
		// NOTE: if the underlying set of valid compound words is ever
		// changed, the iterator returned from the compoundIterators map
		// will have been updated by the setProperties() call, so this
		// method is guaranteed to pick up the latest set of compound
		// words
		cwi.reset(reader);
	    }
	    baseIterator = cwi;
	}
	// Otherwise, just return a standard iterator over all the tokens with
	// no compounding
	else 
	    baseIterator = new WordIterator(reader);

	return baseIterator;
    }

}