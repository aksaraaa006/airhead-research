/*
 * Copyright 2009 Keith Stevens 
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

import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;


public class SenseEvalCleaner {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.parseOptions(args);
        if (options.numPositionalArgs() != 2) {
            System.out.println("usage: SenseEvalCleaner [options] " +
                               "<senseval.xml> <out-file>\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        PrintWriter writer = new PrintWriter(options.getPositionalArg(1));

        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        DocumentBuilder db = dbf.newDocumentBuilder();
        Document doc = db.parse(new File(options.getPositionalArg(0)));
        NodeList instances = doc.getElementsByTagName("instance");
        for (int i = 0; i < instances.getLength(); ++i) {
            Element instanceNode = (Element) instances.item(i);
            String instanceId = instanceNode.getAttribute("id").trim();
            String[] wordPosNum = instanceId.split("\\.");

            String prevContext = instanceNode.getFirstChild().getNodeValue();
            prevContext = prevContext.substring(1);
            String nextContext = instanceNode.getLastChild().getNodeValue();
            writer.printf("%s %s %s %s",
                          instanceId, prevContext, wordPosNum[0], nextContext);
        }
        writer.flush();
        writer.close();
    }
}
