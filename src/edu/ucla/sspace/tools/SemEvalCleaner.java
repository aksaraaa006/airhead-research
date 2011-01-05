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

import edu.ucla.sspace.parser.Parser;
import edu.ucla.sspace.parser.MaltParser;
import edu.ucla.sspace.parser.StanfordParser;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import java.util.logging.Logger;

public class SemEvalCleaner {

    /**
     * The logger used to record all output.
     */
    private static final Logger LOG = 
        Logger.getLogger(SemEvalCleaner.class.getName());

    public enum CorpusType {
      SEMEVAL_2010_TRAIN,
      SEMEVAL_2010_TEST,
      SENSEEVAL_2007,
    }

    public static void main(String[] args) throws Exception {
        // Add and parser command line options.
        ArgOptions options = new ArgOptions();
        options.addOption('p', "parser",
                          "Specifie the parser that should be applied to " +
                          "each context. (Default: None)",
                           true, "malt|stanford", "Optional");
        options.addOption('i', "includeSeparator",
                           "a separator between the previous context and " +
                           "the token of interest",
                           true, "STRING", "Optional");
        options.addOption('T', "threaded",
                          "If set, the cleaner will utilize all available " +
                          "processors and parrallelize the cleaning operations",
                          true, "INT", "Optional");
        options.addOption('t', "corpusType",
                          "Specifies what SemEval format the xml files are in.",
                          true, "CorpusType", "Required");
                          
        options.parseOptions(args);

        // Validate that the expected number of arguments are given.
        if (options.numPositionalArgs() < 2) {
            System.out.println("usage: SemEvalCleaner [options]" +
                               "<out-file> <training-file.xml>+\n" +
                               options.prettyPrint());
            System.exit(1);
        }

        // Get the separator token.
        String separator = options.getStringOption("includeSeparator", "");

        // Set up a stemmer.
        Stemmer stemmer = new EnglishStemmer();

        List<String> fileList = new ArrayList<String>();
        for (int i = 1; i < options.numPositionalArgs(); ++i)
            fileList.add(options.getPositionalArg(i));

        final String parserType = (options.hasOption('p'))
            ? options.getStringOption('p')
            : "";

        CorpusType type = CorpusType.valueOf(
            options.getStringOption('t').toUpperCase());

        final Iterator<String> contextIter;
        switch (type) {
          case SEMEVAL_2010_TRAIN:
            contextIter = new SemEval2010TrainIterator(
                fileList, !parserType.equals(""), separator, stemmer);
            break;
          case SEMEVAL_2010_TEST:
            contextIter = new SemEval2010TestIterator(
                fileList, !parserType.equals(""), separator, stemmer);
            break;
          case SENSEEVAL_2007:
            contextIter = new SenseEval2007Iterator(
                fileList, !parserType.equals(""), separator);
            break;
          default:
            throw new IllegalArgumentException("Invalid corpus type");
        }

        final PrintWriter writer = new PrintWriter(options.getPositionalArg(0));

        Collection<Thread> threads = new LinkedList<Thread>();

        int numThreads = (options.hasOption('T'))
          ? options.getIntOption('T')
          : 1;

        for (int i = 0; i < numThreads; ++i) {
          threads.add(new Thread() {
            public void run() {
              Parser parser = null;
              if (parserType.equals("stanford"))
                parser = new StanfordParser();
              else if (parserType.equals("malt"))
                parser = new MaltParser();

              while (contextIter.hasNext()) {
                String[] lines = contextIter.next().split("\n");
                String header = lines[0];
                LOG.info("Handling: " + header);
                for (int i = 1; i < lines.length; ++i) {
                  String text;
                  if (parser != null) {
                      text = parser.parseText(header, lines[i]).replace(
                          "\t"+header+"\t_\t",
                          "\t"+header.split("\\.")[0]+"\t"+header+"\t");
                  } else {
                      text = String.format("%s %s", header, lines[i]);
                  }
                  synchronized (writer) {
                    writer.println(text);
                  }
                }
              }
            }
          });
        }

        // start all the threads processing
        for (Thread t : threads)
            t.start();

        // wait until all the documents have been parsed
        for (Thread t : threads)
            t.join();
        writer.close();
    }
}

