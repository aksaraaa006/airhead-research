/*
 * Copyright 2009 Keith Stevens
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

import java.util.Iterator;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * An iterator decorator that returns a limited number of string tokens.
 *
 * @author Keith Stevens
 */
public class LimitedWordIterator implements Iterator<String> {

    /**
     * The base iterator to decorate.
     */
    private final Iterator<String> iter;

    /**
     * The current number of words returned by this iterator so far.
     */
    private final AtomicInteger wordCount;

    /**
     * The maximum number of words to return by this iterator.
     */
    private final int maxWords;

    /**
     * Constructs an iterator for the first {@code maxWords} tokens contained in
     * given iterator. 
     */
    public LimitedWordIterator(Iterator<String> iter, int maxWords) {
        this.iter = iter;
        this.maxWords = maxWords;
        wordCount = new AtomicInteger();
    }

    /**
     * Returns {@code true} if there is another word to return.
     */
    public boolean hasNext() {
        return wordCount.get() < maxWords && iter.hasNext();
    }

    /**
     * Returns the next word from the reader.
     */
    public String next() {
        wordCount.incrementAndGet();
        return iter.next();
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */ 
    public void remove() {
        throw new UnsupportedOperationException("remove is not supported");
    }
}
