package edu.ucla.sspace.common.document;

import java.io.BufferedReader;
import java.io.StringReader;

/**
 * A {@code Document} implementation backed by a {@code String} whose contents
 * are used for the document text.
 */
public class StringDocument implements Document {

    /**
     * A reader to the text of the document
     */
    private final BufferedReader reader;
    
    /**
     * Constructs a {@code Document} using the provided string as the document
     * text
     *
     * @param docText the document text
     */
    public StringDocument(String docText) {
	reader = new BufferedReader(new StringReader(docText));
    }
    
    /**
     * {@inheritDoc}
     */
    public BufferedReader reader() {
	return reader;
    }

}