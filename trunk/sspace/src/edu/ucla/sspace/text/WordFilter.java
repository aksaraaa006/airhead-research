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
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Set;

/**
 * An iterator over all of the tokens present in a {@link BufferedReader} that
 * are separated by any amount of white space, which filters out prespecified
 * words from the output.
 */
public class WordFilter implements Iterator<String> {

    /**
     * The backing iterator that tokenizes the stream
     */
    private Iterator<String> tokenizer;

    /**
     * The set of tokens used to filter the output
     */
    private final Set<String> words;

    /**
     * {@code true} if the returned tokens must not be in the filter set
     */
    private final boolean excludeWords;

    /**
     * The next word to return that has passed through the filter
     */
    private String next;

    /**
     * Constructs an interator over the tokens in the reader that <i>are</i>
     * present in {@code words}.
     */
    public WordFilter(BufferedReader br, Set<String> words) {
	this(new WordIterator(br), words, false);
    }

    /**
     * Constructs an interator over the tokens in the reader that are present in
     * {@code words} if {@code excludeWords} is {@code false} or are <i>not</i>
     * in the {@code words} if {@code excludeWords} is {@code true}.
     *
     * @param br the reader to tokenize 
     * @param words the set of tokens to use in filtering the output
     * @param excludeWords {@code true} if words in {@code words} should be
     *        excluded, {@code false} if only words in {@code words} should
     *        be included
     */
    public WordFilter(BufferedReader br, Set<String> words, 
		      boolean excludeWords) {
	this(new WordIterator(br), words, excludeWords);
    }

    /**
     * Constructs an interator over the tokens in the provided iterator that
     * <i>are</i> present in {@code words}
     *
     * @param iterator a sequence of tokens
     * @param words the set of tokens to use in filtering the output
     */
    public WordFilter(Iterator<String> iterator, Set<String> words) {
	this(iterator, words, false);
    }

    /**
     * Constructs an interator over the tokens in the provided iterator that are
     * present in {@code words} if {@code excludeWords} is {@code false} or are
     * <i>not</i> in the {@code words} if {@code excludeWords} is {@code true}.
     *
     * @param iterator a sequence of tokens
     * @param words the set of tokens to use in filtering the output
     * @param excludeWords {@code true} if words in {@code words} should be
     *        excluded, {@code false} if only words in {@code words} should
     *        be included
     */
    public WordFilter(Iterator<String> iterator, Set<String> words,
		      boolean excludeWords) {

	this.tokenizer = iterator;
	this.words = words;
	this.excludeWords = excludeWords;
	advance();
    }

    /**
     * Advances to the next word in the buffer.
     */
    private void advance() {
	String s = null;
	while (tokenizer.hasNext()) {
	    String nextToken = tokenizer.next();
	    // stop if the word should be returned
	    if (include(nextToken)) {
		s = nextToken;
		break;
	    }
	}
	next = s;
    }

    public boolean hasNext() {
	return next != null;
    }

    private boolean include(String word) {
	return words.contains(word) ^ excludeWords;
    }

    /**
     * Returns the next word from the reader that has passed the filter.
     */
    public String next() {
	if (next == null) {
	    throw new NoSuchElementException();
	}
	String s = next;
	advance();
	return s;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
	throw new UnsupportedOperationException("remove is not supported");
    }

}

