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

import edu.ucla.sspace.common.ArgOptions;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import java.io.InputStream;

import java.util.List;


/**
 * An iterator for traversing the word sense induction test corpus from <a
 * href="http://nlp.cs.swarthmore.edu/semeval/tasks/task02/summary.shtml">
 * SenseEval07 Task 2</a>.  By default this iterator simply extracts the text
 * from the senseEval xml data file, but it can optionaly add a separator token
 * before the instance word.  
 *
 * @author Keith Stevens
 */
public class SenseEval2007Iterator extends SemEvalIterator {

  public SenseEval2007Iterator(InputStream fileStream, 
                               boolean prepareForParse,
                               String separator) {
    super(fileStream, prepareForParse, separator);
    advance();
  }

  public SenseEval2007Iterator(List<String> fileNames, 
                               boolean prepareForParse,
                               String separator) {
    super(fileNames, prepareForParse, separator);
    advance();
  }

  protected NodeList getInstances(Document doc) {
    return doc.getElementsByTagName("instance");
  }

  protected String handleElement(Element instanceNode) {
    // Extract the instance id, and stemmed word.
    String instanceId = instanceNode.getAttribute("id").trim();
    String[] wordPosNum = instanceId.split("\\.");

    String prevContext = instanceNode.getFirstChild().getNodeValue();
    prevContext = prevContext.substring(1);
    String nextContext = instanceNode.getLastChild().getNodeValue();

    if (prepareForParse)
      return String.format(
          "%s\n%s %s %s", instanceId, prevContext, instanceId, nextContext);
    return String.format("%s\n%s %s %s %s", instanceId, prevContext, 
                         separator, wordPosNum[0], nextContext);
  }
}
