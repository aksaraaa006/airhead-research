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
 * @author Keith Stevens
 */
public class SemEval2010TestIterator extends SemEvalIterator {

  private final Stemmer stemmer;

  public SemEval2010TestIterator(InputStream fileStream,
                                 boolean prepareForParse,
                                 String separator,
                                 Stemmer stemmer) {
    super(fileStream, prepareForParse, separator);
    this.stemmer = stemmer;
    advance();
  }

  public SemEval2010TestIterator(List<String> fileNames,
                                 boolean prepareForParse,
                                 String separator,
                                 Stemmer stemmer) {
    super(fileNames, prepareForParse, separator);
    this.stemmer = stemmer;
    advance();
  }

  protected NodeList getInstances(Document doc) {
    NodeList root = doc.getChildNodes();
    return root.item(0).getChildNodes();
  }

  /**
   * Writes out a single context for the instance node.  For the test files,
   * each target sentence is embedded withing xml tags with the name
   * "TargetSentence".  This will consider all text before and after this
   * embedded node to be context and only search for the instance term within
   * the "TargetSentence".  The instance term is found by stemming each term
   * in the "TargetSentence" and comparing this to the stem of the known test
   * word, when a match is found, a separator and the stem is printed.  All
   * proceeding text is printed after the instance term.
   */
  protected String handleElement(Element instanceNode) {
    String instanceId = instanceNode.getNodeName();
    String[] wordPosNum = instanceId.split("\\.");
    String word = stemmer.stem(wordPosNum[0].toLowerCase());

    NodeList context = instanceNode.getChildNodes();
    StringBuilder sb = new StringBuilder();

    sb.append(instanceId).append("\n");
    for (int i = 0; i < context.getLength(); ++i) {
      // If we are handling the target sentence, tokenize it based on white
      // space and find the term that has the same stem as the word of
      // interest.  Otherwise just print the entire text of the line.
      if (context.item(i).getNodeName().equals("TargetSentence")) {
        Element target = (Element) context.item(i);
        String text = target.getTextContent();
        for (String token : text.split("\\s+")) {
          String stem = stemmer.stem(token.toLowerCase());
          if (stem.equals(word))
            if (prepareForParse)
              sb.append(instanceId).append(" ");
            else
              sb.append(separator).append(" ").append(stem).append(" ");
          else
            sb.append(token).append(" ");
        }
      } else {
        sb.append(context.item(i).getTextContent()).append(" ");
      }
    }
    return sb.toString();
  }
}
