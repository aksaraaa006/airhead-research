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

package edu.ucla.sspace.tools;

import edu.ucla.sspace.text.Stemmer;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOError;
import java.io.IOException;

import java.util.ArrayDeque;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * A {@link Iterator} for the SemEval 2010 training document set.
 *
 * @author Keith Stevens
 */
public class SemEval2010TrainIterator extends SemEvalIterator {

    /**
     * The stemmer to be used in order to determine the focus term.
     */
    private final Stemmer stemmer;

    /**
     * Creates a new {@link SemEval2010TrainIterator}.
     */
    public SemEval2010TrainIterator(InputStream fileStream,
                                    boolean prepareForParse,
                                    String separator,
                                    Stemmer stemmer) {
        super(fileStream, prepareForParse, separator);
        this.stemmer = stemmer;
        advance();
    }

    /**
     * Creates a new {@link SemEval2010TrainIterator}.
     */
    public SemEval2010TrainIterator(List<String> fileNames,
                                    boolean prepareForParse,
                                    String separator,
                                    Stemmer stemmer) {
        super(fileNames, prepareForParse, separator);
        this.stemmer = stemmer;
        advance();
    }

    /**
     * {@inheritDoc}
     */
    protected NodeList getInstances(Document doc) {
        NodeList root = doc.getChildNodes();
        return root.item(0).getChildNodes();
    }

    /**
     * Splits the instance text and finds the instance of the actual word that
     * needs to be represented.  This is done by stemming each word and checking
     * to see which one has the same stem as the instance word.  Each time word
     * is encoutnered that has the instance stem, this outputs a version of the
     * context that has a {@code separator} before the instance word. For some
     * instance text's the word occurs twice, so this should create a context
     * for both instances with a separator at different positions.
     */
    protected String handleElement(Element instanceNode) {
        String instanceId = instanceNode.getNodeName();
        String[] wordPosNum = instanceId.split("\\.");
        String word = stemmer.stem(wordPosNum[0].toLowerCase());

        // Tokenize the text based on white space.
        String text = instanceNode.getTextContent();
        String[] tokens = text.split("\\s+");

        StringBuilder instanceText = new StringBuilder();
        StringBuilder prevContext = new StringBuilder();

        // Make all tokens lowercase.
        for (int k = 0; k < tokens.length; ++k)
            tokens[k] = tokens[k].toLowerCase();

        instanceText.append(instanceId).append("\n");

        // Traverse each token in the cotnext.
        for (int k = 0; k < tokens.length; ++k) {
            // Find where the instance word occurs.
            String stem = stemmer.stem(tokens[k]);
            if (stem.equals(word)) {
                // Generate the context that comes after the instance word.
                StringBuilder nextContext = new StringBuilder();
                for (int j = k + 1; j < tokens.length; ++j)
                    nextContext.append(tokens[j]).append(" ");

                // Output the previous context, a separator, the instance stem,
                // and the next context.
                instanceText.append(prevContext.toString()).append(" ");
                if (prepareForParse) {
                    instanceText.append(instanceId).append(" ");
                } else {
                    instanceText.append(separator).append(" ");
                    instanceText.append(stem).append(" ");
                }
                instanceText.append(nextContext.toString()).append("\n");
            }

            // Add the token to the previous context in case the instance word
            // occurs multiple times.
            prevContext.append(tokens[k]).append(" ");
        }
        return instanceText.toString();
    }
}
