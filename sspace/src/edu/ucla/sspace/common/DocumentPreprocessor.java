/*
 * Copyright 2009 David Jurgens and Keith Stevens
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
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * A class for preprocessing all types of documents.  This approach was used by
 * Rohde et al. (2004) for processing USENET articles.
 *
 */
public class DocumentPreprocessor {
    
    private final Set<DocHash> processedDocs;

    private final Set<String> validWords;

    /**
     * Constructs a {@code DocumentPreprocessor} with an empty word list 
     */
    public DocumentPreprocessor() throws IOException {
      processedDocs = new HashSet<DocHash>();
      validWords = new HashSet<String>();
    }

    /**
     * Constructs a {@code DocumentPreprocessor} where the provided file 
     * contains the list all valid words for the output documents.
     *
     * @param wordList a file containing a list of all valid words for
     *        outputting
     */
    public DocumentPreprocessor(File wordList) throws IOException {

	processedDocs = new HashSet<DocHash>();
	validWords = new HashSet<String>();
	WordIterator it = new WordIterator(
	    new BufferedReader(new FileReader(wordList)));
	while (it.hasNext()) {
	    validWords.add(it.next());
	}
    }

    /**
     * A Constructor purely for test purposes.  Pass in an array of words which
     * will serve the roll as a word list.
     *
     */
    public DocumentPreprocessor(String[] wordList) {
	processedDocs = new HashSet<DocHash>();
	validWords = new HashSet<String>();
	for (String word : wordList) {
	    validWords.add(word);
	}
    }

    /**
     * Processes the provided document and returns the cleaned version of the
     * document.
     *
     * @param document a document to process
     *
     * @return a cleaned version of the document
     */
    public String process(String document) {
	
	// Step 1: Removing images, non-ascii codes, and HTML tags.
	StringTokenizer st = new StringTokenizer(document);
	StringBuilder htmlFree = new StringBuilder(document.length());
	while (st.hasMoreTokens()) {
	    String tok = st.nextToken();
	    if (tok.length() > 2 && 
		(tok.startsWith("<") || tok.startsWith("</"))) {
		// check if there are actually two tags with a single word
		// between, e.g. "<i>wow</i>"
		int pos = tok.indexOf("<", 1);
		if (pos > 0) {
		    // get the middle text
		    int start = tok.indexOf(">");
		    // protect against malformed tags or other wierdness
		    if (start < pos) {
			String text = tok.substring(start+1, pos);
			htmlFree.append(text).append(" ");
		    }		
		}
	    } else {
		// if it wasn't a tag, just append it
		htmlFree.append(tok).append(" ");
	    }
	}
	document = htmlFree.toString();
	
	// Step 2: Removing all non-standard punctuation and separating other
	//         punctuation from adjacent words.

	// Step 7: Replacing URLs, email addresses, IP addresses, numbers
	//         greater than 9, and emoticons with special word markers, such
	//         as <URL>.
	st = new StringTokenizer(document);
	StringBuilder urlized = new StringBuilder(document.length());
	while (st.hasMoreTokens()) {
	    String tok = st.nextToken();
        if (tok.endsWith("?")) {
          urlized.append(tok.substring(0, tok.length() - 1)).append(" ?");
        } else if (tok.endsWith(",")) {
          urlized.append(tok.substring(0, tok.length() - 1)).append(" ,");
        } else if (tok.endsWith(".")) {
          urlized.append(tok.substring(0, tok.length() - 1)).append(" .");
        } else if (tok.equals("&amp;")) {
          urlized.append("&");
        } else if (tok.equals("&lt;")) {
          urlized.append("<");
        } else if (tok.equals("&gt;")) {
          urlized.append(">");
        } else if (tok.equals("&quot;")) {
          urlized.append("\"");
        } else if (tok.equals("&#39;")) {
          urlized.append("'");
        } else if (tok.contains("@") &&
		           tok.contains(".")) {
          // assume it's an email address
          urlized.append("<URL>");
	    } else if (tok.startsWith("http") ||
                  tok.startsWith("ftp")) {
          urlized.append("<URL>");
        } else if (tok.matches("[0-9]+")) {
          urlized.append("<NUM>");
        } else if (tok.equals("/")) {
          urlized.append("<slash>");
        }
	    // basic emotions
	    else if ((tok.length() == 2 || tok.length() == 3) &&
		     (tok.equals(":)") ||
		      tok.equals(":(") ||
		      tok.equals(":/") ||
		      tok.equals(":\\") ||
		      tok.equals(":|") ||
		      tok.equals(":[") ||
		      tok.equals(":]") ||
		      tok.equals(":X") ||
		      tok.equals(":|") ||
		      tok.equals(":[") ||
		      tok.equals(":]") ||
		      tok.equals(":X") ||
		      tok.equals(":D"))) {
		urlized.append("<EMOTE>");
	    }		     
	    else {
		urlized.append(tok);
	    }
        urlized.append(" ");
	}
	document = urlized.toString().trim();
	
	// Step 4: Splitting words joined by certain punctuation marks and
	//         removing other punctuation from within words.



	// Separate all punctionation from words that it might touch.  This
	// effectively turns the punction into a separate token for 
	// co-occurrence counting purposes.
	document = document.replaceAll("'", " ' ");
	document = document.replaceAll("!", " ! ");
	document = document.replaceAll("\\.", " . ");
	document = document.replaceAll("\\?", " ? ");
	document = document.replaceAll(";", " ; ");
	document = document.replaceAll(",", " , ");
	document = document.replaceAll("\\(", " ( ");
	document = document.replaceAll("\\)", " ) ");
	document = document.replaceAll("\\[", " [ ");
	document = document.replaceAll("\\]", " ] ");
	document = document.replaceAll("/", " / ");
	document = document.replaceAll(":", " : ");
	document = document.replaceAll("\"", " \" ");

	// Step 3: Removing words over 20 characters in length.
	st = new StringTokenizer(document);
	StringBuilder shortWords = new StringBuilder(document.length());
	while (st.hasMoreTokens()) {
	    String tok = st.nextToken();
	    if (tok.length() <= 20) {
		shortWords.append(tok).append(" ");
	    }
	}
	document = shortWords.toString().trim();
	       
	// Step 5: Converting to lower case.
	document = document.toLowerCase();

	// Step 6: Converting $5 to 5 DOLLARS.
	st = new StringTokenizer(document);
	StringBuilder dollarized = new StringBuilder(document.length());
	while (st.hasMoreTokens()) {
	    String tok = st.nextToken();
	    if (tok.startsWith("$")) {
		String s = tok.substring(1);
		// if the rest of it was a number, then do the dollar
		// substitution.  Otherwise, exclude.
		// 
		// NOTE: I still haven't found a better way of checking whether
		// a string is actually a number  --jurgens
		try {
		    int i = Integer.parseInt(s);
		    dollarized.append(i).append(" dollars ");
		} catch (NumberFormatException nfe) {
		    // ignore, wasn't a number, so drop the token
		}
	    }
	    else {
		dollarized.append(tok).append(" ");
	    }
	}
	document = dollarized.toString().trim();
	
	// Step 8: Discarding articles with fewer than 80% real words, based on
	//         a large English word list. This has the effect of ﬁltering
	//         out foreign text and articles that primarily contain computer
	//         code.
    if (validWords.size() > 0) {
      int totalTokens = 0;
      int actualWords = 0;
      st = new StringTokenizer(document);
      StringBuilder wordCounter = new StringBuilder(document.length());
      while (st.hasMoreTokens()) {
          String tok = st.nextToken();
          totalTokens++;
          if (validWords.contains(tok))
          actualWords++;
      }
      if (actualWords / (double)(totalTokens) < .8) {
          // discard the document
          return "";
      }
	}
	
	// Step 9: Discarding duplicate articles. This was done by computing a
	//         128-bit hash of the contents of each article. Articles with
	//         identical hash values were assumed to be duplicates.
	DocHash hash = new DocHash(document);
	if (processedDocs.contains(hash)) {
	    // discard the document
	    return "";
	}
	else {
	    processedDocs.add(hash);
	}

	// Step 10: Performing automatic spelling correction.
	
	/* -- SKIP -- */
	
	// Step 11: Splitting the hyphenated or concatenated words that do not
	//          have their own entries in a large dictionary but whose
	//          components do.
	
	/* -- SKIP -- */

	document = document.replaceAll("[^\\w\\s;'?\",\\.<>]","");
	return document;
    }

    /**
     * A class that represents the hashed contents of a document.  This class
     * allows long documents to be quickly compared.  This class uses the MD5
     * algorithm to compute the hash.
     */
    private static class DocHash {

	private final byte[] hash;

	private final int hashCode;

	public DocHash(String article) {
	    hash = hash(article);
	    hashCode = hash[3] << 24 | hash[2] << 16 | hash[1] << 8 | hash[0];
	}

	public boolean equals(Object o) {
	    return o != null &&
               o instanceof DocHash &&
               Arrays.equals(hash, ((DocHash)o).hash);
	}

	private static byte[] hash(String article) {	    
	    try {
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		return md5.digest(article.getBytes());
	    } catch (NoSuchAlgorithmException nsae) {
		// rethrow
		throw new Error(nsae);
	    }
	}

	public int hashCode() {
	    return hashCode;
	}
    }


}
