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

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Statistics;
import edu.ucla.sspace.matrix.Statistics.Dimension;

import edu.ucla.sspace.util.Duple;
import edu.ucla.sspace.util.Pair;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;
import java.io.FileReader;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.List;

public class TimeSeriesAnalysis {
  private String timeSeriesPattern;
  private String neighborPattern;
  private OneLineDocumentSearcher searcher;

  public TimeSeriesAnalysis(String timeSeries, String neighbors,
                            OneLineDocumentSearcher searcher) {
    timeSeriesPattern = timeSeries;
    neighborPattern = neighbors;
    this.searcher = searcher;
  }

  public List<Duple<String, List<String>>> findEvents(String[] words,
                                                      int numIntervals) {
    Matrix dataMatrix = readInData(words, numIntervals);
    List<Pair<Integer>> events = detectEvents(dataMatrix);
    List<Duple<String, List<String>>> eventDocs =
      new ArrayList<Duple<String, List<String>>>();
    for (Pair<Integer> event : events) {
      String word = words[event.x];
      String monthValue = (event.y < 10) ?
        "0" + Integer.toString(event.y) :
        Integer.toString(event.y);
      String nnName =
        neighborPattern.replace("%w", word).replace("%d", monthValue);
      try {
        BufferedReader reader = new BufferedReader(new FileReader(nnName));
        List<String> neighbors = new ArrayList<String>();
        String line = null;
        while ((line = reader.readLine()) != null) {
          if (line.length() == 0 || line.charAt(0) == '#')
            continue;
          neighbors.add(line);
        }
        Duple<String, List<String>> docs =
          new Duple<String, List<String>>(word, neighbors);
        eventDocs.add(docs);
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }
    return eventDocs;
  }

  public Matrix readInData(String[] words, int numIntervals) {
    Matrix dataMatrix = new ArrayMatrix(words.length, numIntervals - 1);
    int j = 0;
    for (String word: words) {
      try {
        BufferedReader reader = new BufferedReader(
            new FileReader(timeSeriesPattern.replace("%w", word)));
        String line = null;
        double[] values = new double[numIntervals];
        int i = 0;
        while ((line = reader.readLine()) != null) {
          if (line.length() == 0 || line.startsWith("#"))
            continue;
          String[] splitLine = line.split("\\s");
          values[i] = Double.parseDouble(splitLine[2]);
          ++i;
        }
        for (i = 1; i < numIntervals - 1; ++i)
          dataMatrix.set(j, i-1, values[i] - values[i-1]);
        ++j;
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }
    return dataMatrix;
  }

  public List<Pair<Integer>> detectEvents(Matrix m) {
    Matrix averages = Statistics.average(m, Dimension.ROW);
    Matrix std = Statistics.std(m, averages, Dimension.ROW);

    List<Pair<Integer>> eventPairs = new ArrayList<Pair<Integer>>();
    for (int r = 0; r < m.rows(); ++r) {
      for (int c = 0; c < m.columns(); ++c) {
        if (Math.abs(m.get(r, c) - averages.get(r, 0)) > 
            2 * std.get(r, 0)) {
          eventPairs.add(new Pair<Integer>(r, c+2));
        }
      }
    }
    return eventPairs;
  }

  public static void main(String[] args) throws Exception {
    ArgOptions opts = new ArgOptions();
    opts.addOption('n', "numDataPoints",
                   "Number of data points for each word",
                   true, "INTEGER", "Required");
    opts.addOption('i', "indexPath", "Directory with indexed blogs",
                   true, "FILE", "Required");
    opts.addOption('S', "simFilePattern", "Pattern for similarity files",
                   true, "PATH/..%w..", "Required");
    opts.addOption('N', "neighborFilePattern", "Pattern for neighbord files",
                   true, "PATH/..%w..", "Required");
    opts.addOption('w', "words", "Words to search through",
                   true, "String[,String..]", "Required");

    opts.parseOptions(args);
    if (opts.numPositionalArgs() != 1 ||
        !opts.hasOption("words") ||
        !opts.hasOption("numDataPoints") ||
        !opts.hasOption("indexPath") ||
        !opts.hasOption("simFilePattern") ||
        !opts.hasOption("neighborFilePattern")) {
      System.out.println("usage: java TimeSeriesAnalysis [OPTIONS] <out-file>" +
                         opts.prettyPrint());
      System.exit(1);
    }

    String[] words = opts.getStringOption("words").split(",");
    int numIntervals = opts.getIntOption("numDataPoints");
    OneLineDocumentSearcher searcher = new OneLineDocumentSearcher(
        opts.getStringOption("indexPath"));
    String simPattern = opts.getStringOption("simFilePattern");
    String neighPattern = opts.getStringOption("neighborFilePattern");
    TimeSeriesAnalysis analyzer =
      new TimeSeriesAnalysis(simPattern, neighPattern, searcher);
    List<Duple<String, List<String>>> eventDocs = analyzer.findEvents(
        words, numIntervals);
    for (Duple<String, List<String>> docs : eventDocs) {
      System.out.println(docs.y.size() +
                         " events have been found for word: " + docs.x);
    }
  }
}
