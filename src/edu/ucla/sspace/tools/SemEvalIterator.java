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
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;


/**
 * @author Keith Stevens
 */
public abstract class SemEvalIterator implements Iterator<String> {

  private final Queue<InputStream> xmlFiles;

  private final DocumentBuilder db;

  private NodeList instances;

  private int currentNode;

  private String next;

  protected final String separator;

  protected final boolean prepareForParse;

  public SemEvalIterator(InputStream fileStream,
                         boolean prepareForParse,
                         String separator) {
    this.prepareForParse = prepareForParse;
    this.separator = separator;

    try {
      xmlFiles = new ArrayDeque<InputStream>();
      xmlFiles.add(fileStream);
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    }
  }

  public SemEvalIterator(List<String> fileNames,
                         boolean prepareForParse,
                         String separator) {
    this.prepareForParse = prepareForParse;
    this.separator = separator;

    try {
      xmlFiles = new ArrayDeque<InputStream>();
      for (String filename : fileNames)
        xmlFiles.add(new FileInputStream(filename));

      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      db = dbf.newDocumentBuilder();
    } catch (ParserConfigurationException pce) {
      throw new RuntimeException(pce);
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * {@inheritDoc}
   */
  public synchronized boolean hasNext() {
    return next != null;
  }

  /**
   * {@inheritDoc}
   */
  public synchronized String next() {
    String current = next;
    advance();
    return current;
  }

  /**
   * A  no-op.
   */
  public synchronized void remove() {
  }

  /**
   * Creates a set of cleaned context lines for each instance node in the
   * {@code trainingFile}.  The text for an entire context is written on a
   * single line.  The focus word for each context is preceeded by {@code
   * separator} and the first token in each context is the instance id of the
   * context.  NOTE: for training SemEval2010 files, multiple context can be
   * generated for the same instance id.  This is due to the lack of tags
   * marking where the key word is in the context, so we are conservative and
   * assume that all are possibly the key word.
   */
  protected void advance() {
    if (xmlFiles.size() == 0 && instances == null) {
        next = null;
        return;
    }

    if (instances == null) {
      try {
        Document doc = db.parse(xmlFiles.remove());
        instances = getInstances(doc);
        currentNode = 0;
      } catch (SAXException saxe) {
        throw new RuntimeException(saxe);
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }

    if (currentNode >= instances.getLength()) {
      instances = null;
      advance();
      return;
    }

    next = handleElement((Element) instances.item(currentNode++));
  }

  protected abstract NodeList getInstances(Document doc);

  protected abstract String handleElement(Element instanceNode);
}
