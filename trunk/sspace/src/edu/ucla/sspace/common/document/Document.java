package edu.ucla.sspace.common.document;

import java.io.BufferedReader;

/**
 * An abstraction for a document that allows document processors to access text
 * in a uniform manner.
 */
public interface Document {
    
    /**
     * Returns the {@code BufferedReader} for this document's text
     */
    BufferedReader reader();
    
}
