/*
 * Copyright 2010 Keith Stevens 
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

import edu.ucla.sspace.text.DependencyFileDocumentIterator;
import edu.ucla.sspace.text.Document;

import edu.ucla.sspace.util.CombinedIterator;

import java.io.IOException;

import java.util.Collection;
import java.util.LinkedList;
import java.util.Iterator;


/**
 * This abstract class extends {@link GenericMain} by overing the {@code
 * getDocumentIterator} function such that it generates document iterators for
 * dependency parse trees.
 *
 * @author Keith Stevens 
 */
public abstract class DependencyGenericMain extends GenericMain {

    /**
     * Returns a {@link Document} iterator for all dependency parsed documents
     * specified through the command line.
     *
     * @throws IllegalArgumentException if the {@code --docFile} argument isn't
     *         set.
     */
    protected Iterator<Document> getDocumentIterator() throws IOException {
        Iterator<Document> docIter = null;

        String docFile = (argOptions.hasOption("docFile"))
            ? argOptions.getStringOption("docFile")
            : null;

        if (docFile == null) {
            throw new Error("must specify document sources");
        }

        // Second, determine where the document input sources will be coming
        // from.
        Collection<Iterator<Document>> docIters = 
            new LinkedList<Iterator<Document>>();

        if (docFile != null) {
            String[] fileNames = docFile.split(",");
            // all the documents are listed in one file, with one document per
            // line
            for (String s : fileNames) {
                docIters.add(new DependencyFileDocumentIterator(s));
            }
        }

        // combine all of the document iterators into one iterator.
        docIter = new CombinedIterator<Document>(docIters);
        return docIter;
    }
}
