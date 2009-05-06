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

package edu.ucla.sspace.esa;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A utility class for buffering Wikipedia articles for processing.  This
 * allows concurrent reading of the Wikipedia snapshot file with ESA
 * processing.
 */
public class ArticleIterator implements Iterator<WikiArticle> {
	    
    private static final int TITLE_HTML_LENGTH = "    <title>".length();
    
    /**
     *
     */
    private final BufferedReader wikipediaSnapshot;
    
    /**
     *
     */
    private WikiArticle next;   

    /**
     *
     */
    public ArticleIterator(BufferedReader wikipediaSnapshot) {
	try {
	    this.wikipediaSnapshot = wikipediaSnapshot;
	    next = null;
	    advance();
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
    }
	
    /**
     * Advances {@link #next} to the next document in the Wikipedia snapshot.
     */ 
    private void advance() throws IOException {

	StringBuilder sb = new StringBuilder();
	String articleTitle = null;

	for (String line = null; 
	         (line = wikipediaSnapshot.readLine()) != null; ) {
	    
	    // This end tag denotes the end of the Wikipedia snapshot.
	    if (line.startsWith("</mediawiki>")) {
		next = null;
		return;
	    }
	    
	    // Look for the start of an article
	    if (line.startsWith("  <page>")) {

		// title immediately follows page declaration
		String titleLine = wikipediaSnapshot.readLine();
		// titles start with '    <title>'		    
		String rem = titleLine.substring(TITLE_HTML_LENGTH);
		int index = rem.indexOf("<");
		if (index < 0)
		    throw new Error("Malformed title: " + line);
		articleTitle = rem.substring(0, index);
		// System.out.println("cached: " + articleTitle);
		
		// read in the rest of the page until we see the end tag
		while ((line = wikipediaSnapshot.readLine()) != null && 
		       !line.startsWith("  </page>")) {
		    sb.append(line);
		}
		break;
	    }
	}	
	
	next = new WikiArticle(articleTitle, sb.toString());
    }

    /**
     * Returns {@code true} if there is still another article from the snapshot
     * left to return.
     */
    public boolean hasNext() {
	return next != null;
    }

    /**
     * Returns the next Wikipedia article from the snapshot.
     *
     * @throws {@inheritDoc}
     */ 
    public WikiArticle next() {
	if (next == null) {
	    throw new NoSuchElementException();
	}
	WikiArticle d = next;
	try {
	    advance();
	} catch (IOException ioe) {
	    throw new IOError(ioe);
	}
	return d;
    }

    /**
     * Throws a {@code UnsupportedOperationException} if called.
     */
    public void remove() {
	throw new UnsupportedOperationException();
    }

}