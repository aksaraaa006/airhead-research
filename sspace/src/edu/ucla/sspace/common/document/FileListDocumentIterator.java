package edu.ucla.sspace.common.document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

import java.util.Iterator;
import java.util.Queue;

import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * An iterator implementation that returns {@link Document} instances given a
 * file that contains list of files.
 *
 * <p>
 *
 * This class is thread-safe.
 */
public class FileListDocumentIterator implements Iterator<Document> {

    /**
     * The files in the list that have yet to be returned as {@code Document}
     * instances
     */
    private final Queue<String> filesToProcess;
    
    /**
     * Creates an {@code Iterator} over the files listed in the provided file.
     *
     * @code fileListName a file containing a list of file names with one per
     *       line
     *
     * @throws IOException if any error occurs when reading {@code fileListName}
     */
    public FileListDocumentIterator(String fileListName) throws IOException {
	
	filesToProcess = new ConcurrentLinkedQueue<String>();
	
	// read in all the files we have to process
	BufferedReader br = new BufferedReader(new FileReader(fileListName));
	for (String line = null; (line = br.readLine()) != null; )
	    filesToProcess.offer(line.trim());	    

	br.close();
    }

    /**
     * Returns {@code true} if there are more documents to return.
     */
    public boolean hasNext() {
	return !filesToProcess.isEmpty();
    }
    
    /**
     * Returns the next document from the list.
     */
    public Document next() {
	String fileName = filesToProcess.poll();
	if (fileName == null) 
	    return null;
	try {
	    return new FileDocument(fileName);
	} catch (IOException ioe) {
	    return null;
	}
    }	
    
    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
	throw new UnsupportedOperationException(
	    "removing documents is not supported");
    }
}
