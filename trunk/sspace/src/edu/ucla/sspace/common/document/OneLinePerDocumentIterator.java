package edu.ucla.sspace.common.document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.Iterator;

/**
 * An iterator implementation that returns {@link Document} instances given a
 * file that contains list of files.
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class OneLinePerDocumentIterator implements Iterator<Document> {
    
    /**
     * The reader for accessing the file containing the documents
     */
    private final BufferedReader documentsReader;
    
    /**
     * The next line in the file
     */
    private String nextLine;
    
    /**
     * Constructs an {@code Iterator} for the documents contained in the
     * provided file.
     *
     * @param documentsFile a file that contains one document per line
     *
     * @throws IOException if any error occurs when reading {@code
     *         documentsFile}
     */
    public OneLinePerDocumentIterator(String documentsFile) 
	    throws IOException {
	    
	documentsReader = new BufferedReader(new FileReader(documentsFile));
	nextLine = documentsReader.readLine();
    }
    
    /**
     * Returns {@code true} if there are more documents in the provided file.
     */
    public synchronized boolean hasNext() { 
	return nextLine != null;
    }    

    /**
     * Returns the next document from the file.
     */
    public synchronized Document next() {
	Document next = new StringDocument(nextLine);
	try {
	    nextLine = documentsReader.readLine();
	} catch (Throwable t) {
	    t.printStackTrace();
	    nextLine = null;
	}
	return next;
    }	

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
	throw new UnsupportedOperationException(
	    "removing documents is not supported");
    }
}
