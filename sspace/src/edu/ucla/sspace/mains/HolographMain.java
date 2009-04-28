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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.document.Document;
import edu.ucla.sspace.common.document.OneLinePerDocumentIterator;
import edu.ucla.sspace.holograph.Holograph;

/**
 * This main is slightly out of date and can probably be ignored
 */
public class HolographMain {
  public static void usage() {
    System.out.println("use --docsFile");
  }

  public static void main(String[] args) {
    Holograph holographBuilder = new Holograph();
	String input = null;
	if (args.length == 0)
	    usage();
	else { 
      if (args[0].startsWith("--docsFile=")) {
        try {
            String docsFile = 
            args[0].substring("--docsFile=".length());
            input = docsFile;
            OneLinePerDocumentIterator docIter = 
              new OneLinePerDocumentIterator(docsFile);
            int count = 0;
            while (docIter.hasNext()) {
              Document doc = docIter.next();
              System.out.print("parsing document " + (count++));
              long startTime = System.currentTimeMillis();
              holographBuilder.processDocument(doc.reader());
              long endTime = System.currentTimeMillis();
              System.out.printf("complete (%.3f seconds)%n",
                        (endTime - startTime) / 1000d);
            }
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
      }
      else {
        System.out.println("unrecognized argument: " + args[0]);
        usage();
        System.exit(0);
      }
      /*
      System.out.println("similarity between Eat and Eaten: " + 
                         holographBuilder.computeSimilarity("eat", "eaten"));
      System.out.println("similarity between Eat and buy: " + 
                         holographBuilder.computeSimilarity("eat", "buy"));
      System.out.println("similarity between Eat and feed: " + 
                         holographBuilder.computeSimilarity("eat", "feed"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "driver"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "boat"));
      System.out.println("similarity between car and driver: " + 
                         holographBuilder.computeSimilarity("car", "truck"));
                         */
      holographBuilder.lutherTest();
    }
  }
}
