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


/**
 * A simple xml parser for the Childes corpus.  Words in each utterance will be
 * extracted from the XML and saved into a specified file.  The resulting
 * document may consist of all uterances in an XML file or a single utterance,
 * where a single xml file generates multiple documents.
 *
 * @author Keith Stevens
 */
public class ChildesParser {

    /**
     * A writer for writing utterances.
     */
    private PrintWriter writer;

    /**
     * Creates the {@code ChildesParser}.   The given file name will be used to
     * write the extracted words.
     */
    public ChildesParser(String outFile) {
        try {
            writer = new PrintWriter(outFile);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Writes strings to the resulting file.
     */
    private synchronized void print(String output) {
        writer.println(output);
    }

    /**
     * Parses a single xml file.  If {@code utterancePerDoc} is true, each
     * utterance will be on a separate line, otherwise they will all be
     * concantanated, and separated by periods, and stored on a single line.
     */
    public void parseFile(File file, boolean utterancePerDoc) {
        try {
            // Build an xml document.
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            DocumentBuilder db = dbf.newDocumentBuilder();
            Document doc = db.parse(file);

            // Extract all utterances.
            NodeList utterances = doc.getElementsByTagName("u");
            StringBuilder fileBuilder = new StringBuilder();
            for (int i = 0; i < utterances.getLength(); ++i) {
                // Extract all words from the utterance
                Element item = (Element) utterances.item(i);
                NodeList words = item.getElementsByTagName("w");
                StringBuilder utteranceBuilder = new StringBuilder();

                // Iterate over the words and get just the word text.
                for (int j = 0; j < words.getLength(); ++j) {
                    utteranceBuilder.append(
                            words.item(j).getFirstChild().getNodeValue());
                    utteranceBuilder.append(" ");
                }

                // Write the utterance if an utterance is a document.
                String utterance = utteranceBuilder.toString();
                if (utterancePerDoc)
                    print(utterance);
                else  // otherwise save the utterance.
                    fileBuilder.append(utterance).append(". ");
            }

            // Write all the utterances if the whole xml file is a document.
            if (!utterancePerDoc)
                print(fileBuilder.toString());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Finalizes the writing of documents.
     */
    public void finalize() {
        writer.flush();
        writer.close();
    } 

    public static void main(String[] args) {

        // Add the options.
        ArgOptions options = new ArgOptions();
        options.addOption('U', "utterancePerDoc",
                          "If set, one utterance is considered a document, " +
                          "otherwise all uterances in a file will be " +
                          "considered a document",
                          false, null, "Optional");
        options.addOption('d', "baseChildesDirectory",
                          "The base childes directory.  XML files will be " +
                          "searched for recursively from this base.  Use of " +
                          "this overrides the fileList option.",
                          true, "DIRECTORY", "Required (At least one of)");
        options.addOption('f', "fileList",
                          "The list of files to process",
                          true, "FILE[,FILE]*", "Required (At least one of)");

        // Process the options and emit errors if any required options are
        // missing.
        options.parseOptions(args);
        if ((!options.hasOption("fileList") &&
             !options.hasOption("baseChildesDirectory")) ||
             options.numPositionalArgs() == 0) {
            System.out.println("usage: java ChildesParser [options] OUTFILE\n" +
                               options.prettyPrint());
            return;
        }

        // The default is to have all utterances from a conversation be in a
        // single document
        boolean utterancePerDoc = false;
        utterancePerDoc = options.hasOption("utterancePerDoc");

        ChildesParser parser = new ChildesParser(options.getPositionalArg(0));

        // Process the given file list, if provided.
        if (options.hasOption("fileList")) {
            String[] files = options.getStringOption("fileList").split(",");
            for (String file : files)
                parser.parseFile(new File(file), utterancePerDoc);
        } else {
            // Otherwise search for xml files to process.
            File baseDir =
                new File(options.getStringOption("baseChildesDirectory"));
            findXmlFiles(parser, utterancePerDoc, baseDir);
        }

        parser.finalize();
    }

    /**
     * Recursively finds any xml documents to parse.
     */
    public static void findXmlFiles(ChildesParser parser,
                                    boolean utterancePerDoc,
                                    File directory) {
        File[] files = directory.listFiles();
        for (File file : files) {
            if (file.isDirectory())
                findXmlFiles(parser, utterancePerDoc, file);
            else if (file.isFile() && file.getPath().endsWith(".xml"))
                parser.parseFile(file, utterancePerDoc);
        }
    }
}
