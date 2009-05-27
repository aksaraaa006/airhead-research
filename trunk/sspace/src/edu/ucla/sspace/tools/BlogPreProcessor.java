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
import edu.ucla.sspace.common.DocumentPreprocessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.LinkedList;

import java.sql.Timestamp;

/**
 * An informal tool class which extracts the date and content of cleaned xml
 * files.  No xml parsing is done, instead the open and closed tags are searched
 * for in the string, and everything in between is extracted and saved to a
 * file.
 */
public class BlogPreProcessor {
  private DocumentPreprocessor processor;
  private PrintWriter pw;
  private boolean saveTS;
  private String tsLength;

  private BlogPreProcessor(File wordFile, File outFile,
                           boolean saveTimestamp, String tsLength) {
    try {
      pw = new PrintWriter(outFile);
      processor = new DocumentPreprocessor(wordFile);
      saveTS = saveTimestamp;
      this.tsLength = tsLength;
    } catch (FileNotFoundException fnee) {
      fnee.printStackTrace();
      System.exit(1); 
    } catch (IOException ioe) {
      ioe.printStackTrace();
      System.exit(1); 
    }
  }

  /**
   * Given a blog file, read through each line and extract the content and
   * updated date, printing these as one line to the result file.
   */
  public void processFile(File blogFile) throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(blogFile));
    String line = null;
    String date = null;
    String content = null;
    boolean needMoreContent = false;
    while ((line = br.readLine()) != null) {
      if (line.contains("<content>")) {
        // Extract the start of a content node.  If the previous content,
        // updated pair was incomplete, i.e. updated had no value, this will
        // overwrite the previous content value.
        int startIndex = line.indexOf(">")+1;
        int endIndex = line.lastIndexOf("<");
        if (endIndex > startIndex)
          content = line.substring(startIndex, endIndex);
        else {
          content = line.substring(startIndex);
          needMoreContent = true;
        }
      } else if (needMoreContent) {
        // The content node might span several lines, so consider all lines read
        // until the next close bracket to be part of the current content.
        int endIndex = line.lastIndexOf("<");
        if (endIndex > 0) {
          content += line.substring(0, endIndex);
          needMoreContent = false;
        } else
          content += line;
      } else if (saveTS && line.contains("<updated>")) {
        // The updated timestamp only spans one line.
        int startIndex = line.indexOf(">")+1;
        int endIndex = line.lastIndexOf("<");
        date = line.substring(startIndex, endIndex);
        if (date.equals(""))
          date = null;
        else if (tsLength.equals("day")) {
          // Ignore the hours of the day portion of the time stamp.
          date = date.split(" ")[0] + " 00:00:00";
        }
      } else if (content != null && (!saveTS || date != null)) {
        // Cleand and print out the content and date.
        long dateTime = Timestamp.valueOf(date).getTime();
        String cleanedContent = processor.process(content);
        if (!cleanedContent.equals("")) {
          synchronized (pw) {
            pw.format("%d %s\n", dateTime, cleanedContent);
          }
        }
        content = null;
        needMoreContent = false;
        date = null;
      }
    }
  }

  public static ArgOptions setupOptions() {
    ArgOptions opts = new ArgOptions();
    opts.addOption('d', "docFiles", "location of directory containing only blog files", 
                   true, "FILE[,FILE,...]", "Required");
    opts.addOption('w', "wordlist", "Word List for cleaning documents",
                   true, "STRING", "Required");
    opts.addOption('t', "timestamp", "Include timestamps for each document");
    opts.addOption('l', "lengthoftimestamp", "length of the time stamp's duration", 
                   true, "STRING");
    return opts;
  }

  public static void main(String[] args)
      throws IOException, InterruptedException  {
    ArgOptions options = setupOptions();
    options.parseOptions(args);

    if (!options.hasOption("docFiles") || 
        !options.hasOption("wordlist") ||
        options.numPositionalArgs() != 1) {
      System.out.println("usage: java BlogPreProcessor [options] <out_file> \n" +
                         options.prettyPrint());
      System.exit(1);
    }
    String tsLength =
      options.hasOption('l') ? options.getStringOption('l') : "instant";
    File outFile = new File(options.getPositionalArg(0));
    File wordFile = new File(options.getStringOption("wordlist"));
    final BlogPreProcessor blogCleaner =
      new BlogPreProcessor(wordFile, outFile, options.hasOption('t'), tsLength);
    String[] fileNames = options.getStringOption("docFiles").split(",");
    
    List<File> blogFiles = new ArrayList<File>() ;
    for (String fileName : fileNames) {
      blogFiles.add(new File(fileName));
    }

    final Iterator<File> fileIter = blogFiles.iterator();

    // repeatedly try to process documents while some still
    // remain
    while (fileIter.hasNext()) {
      try {
        blogCleaner.processFile(fileIter.next());
      } catch (IOException ioe) {
        ioe.printStackTrace();
      }
    }
  }
}
