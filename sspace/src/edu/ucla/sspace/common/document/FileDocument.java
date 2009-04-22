package edu.ucla.sspace.common.document;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

/**
 * A {@code Document} implementation backed by a {@code File} whose contents are
 * used for the document text.
 */
public class FileDocument implements Document {
	
    /**
     * The reader for the backing file.
     */
    private final BufferedReader reader;
    
    /**
     * Constructs a {@code Document} based on the contents of the provide file.
     *
     * @param fileName the name of a file whose contents will be used as a
     *        document
     *
     * @throws IOException if any error occurred while reading {@code fileName}.
     */
    public FileDocument(String fileName) throws IOException {
	BufferedReader r = null;
	try {
	    r = new BufferedReader(new FileReader(fileName));
	} catch (Throwable t) {
	    t.printStackTrace();
	}
	reader = r;
    }

    /**
     * {@inheritDoc}
     */
    public BufferedReader reader() {
	return reader;
    }
    
}
