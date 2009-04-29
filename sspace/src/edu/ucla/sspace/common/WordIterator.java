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

package edu.ucla.sspace.common;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * An iterator over all of the words present in a {@link BufferedReader} that
 * are separated by any amount of white space.
 */
public class WordIterator implements Iterator<String> {

    private final BufferedReader br;
    
    private String next;

    private int curIndex;

    private String[] curLine;

    /**
     * Constructs an iterator for all the words contained in text of the
     * provided reader.
     */
    public WordIterator(BufferedReader br) {
	this.br = br;
	advance();
    }

    /**
     * Advances to the next word in the buffer.
     */
    private void advance() {
	try {
	    // loop until we find a word in the reader, or there are no more
	    // words
	    while (true) {
		// if we haven't looked at any lines yet, or if the index into
		// the current line is already at the end 
		if (curLine == null || curIndex == curLine.length) {
		    String line = br.readLine();
		    
		    // if there aren't any more lines in the reader, then mark
		    // next as null to indicate that there are no more words
		    if (line == null) {
			next = null;
			return;
		    }
		    // skip empty lines
		    else if (line.length() == 0) {
			continue;
		    }

		    curLine = line.split("\\s+");
		    curIndex = 0;

		    // if the current line did not contain any words, move to
		    // the next line
		    if (curLine.length == 0) {
			continue;
		    }
		}

		// the index points to somewhere in the middle of the line
		next = curLine[curIndex++];
		break;
	    }
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
    }

    /**
     * Returns {@code true} if there is another word to return.
     */
    public boolean hasNext() {
	return next != null;
    }

    /**
     * Returns the next word from the reader.
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