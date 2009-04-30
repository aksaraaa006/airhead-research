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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.atomic.AtomicInteger;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.common.document.Document;
import edu.ucla.sspace.common.document.FileListDocumentIterator;
import edu.ucla.sspace.common.document.OneLinePerDocumentIterator;


/**
 * A base class for running {@link SemanticSpace} algorithms.
 *
 * @author David Jurgens
 */
public abstract class GenericMain {

    /**
     * Whether to emit messages to {@code stdout} when the {@code verbose}
     * methods are used.
     */
    protected boolean verbose;

    public GenericMain() {
	verbose = false;
    }

    protected void parseDocumentsSingleThreaded(SemanticSpace sspace,
						Iterator<Document> docIter,
						Properties properties) 
	    throws IOException {

	long processStart = System.currentTimeMillis();
	int count = 0;

	while (docIter.hasNext()) {
	    long startTime = System.currentTimeMillis();
	    Document doc = docIter.next();
	    int docNumber = ++count;
	    int terms = 0;
	    sspace.processDocument(doc.reader());
	    long endTime = System.currentTimeMillis();
	    verbose("processed document #%d in %.3f seconds%n",
		    docNumber, ((endTime - startTime) / 1000d));
	}

	verbose("processed %d document in %.3f total seconds)%n",
		count,
		((System.currentTimeMillis() - processStart) / 1000d));	    
    }

     /**
      *
      */
    protected void parseDocumentsMultiThreaded(final SemanticSpace sspace,
					       final Iterator<Document> docIter,
					       final Properties properties,
					       int numThreads)	
	    throws IOException, InterruptedException {

	Collection<Thread> threads = new LinkedList<Thread>();

	final AtomicInteger count = new AtomicInteger(0);

	
	for (int i = 0; i < numThreads; ++i) {
	    Thread t = new Thread() {
		    public void run() {
			// repeatedly try to process documents while some still
			// remain
			while (docIter.hasNext()) {
			    long startTime = System.currentTimeMillis();
			    Document doc = docIter.next();
			    int docNumber = count.incrementAndGet();
			    int terms = 0;
			    try {
				sspace.processDocument(doc.reader());
			    } catch (Throwable t) {
				t.printStackTrace();
			    }
			    long endTime = System.currentTimeMillis();
			    verbose("parsed document #%d in %.3f seconds%n",
				    docNumber, ((endTime - startTime) / 1000d));
			}
		    }
		};
	    threads.add(t);
	}

	long threadStart = System.currentTimeMillis();
	
	// start all the threads processing
	for (Thread t : threads)
	    t.start();

	verbose("Beginning processing using %d threads", numThreads);

	// wait until all the documents have been parsed
	for (Thread t : threads)
	    t.join();

	verbose("parsed %d document in %.3f total seconds)%n",
		count.get(),
		((System.currentTimeMillis() - threadStart) / 1000d));
    }

    /**
     * Returns a set of terms based on the contents of the provided file.  Each
     * word is expected to be on its own line.
     */
    protected static Set<String> loadValidTermSet(String validTermsFileName) 
	    throws IOException {

	Set<String> validTerms = new HashSet<String>();
	BufferedReader br = new BufferedReader(
		new FileReader(validTermsFileName));
	
	for (String line = null; (line = br.readLine()) != null; ) {
	    validTerms.add(line);
	}
	 
	br.close();

	return validTerms;
    }

    protected void verbose(String msg) {
	if (verbose) {
	    System.out.println(msg);
	}
    }

    protected void verbose(String format, Object... args) {
	if (verbose) {
	    System.out.printf(format, args);
	}
    }


}