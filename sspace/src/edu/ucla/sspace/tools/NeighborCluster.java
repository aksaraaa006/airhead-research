package edu.ucla.sspace.tools;

import edu.ucla.sspace.matrix.ArrayMatrix;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.common.ArgOptions;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NeighborCluster {
  public static List<List<String>> findNeighborClusters(
      File neighborFile,
      double threshold) throws IOException {
    BufferedReader reader = new BufferedReader(new FileReader(neighborFile));
    String line = null;
    line = reader.readLine();
    if (line == null)
      return null;
    String[] terms = line.split("\\s");
    Map<String, Integer> termMap = new HashMap<String, Integer>();
    int r = 0;
    for (String term : terms) {
      termMap.put(term, r);
      r++;
    }
    Matrix comparisons = new ArrayMatrix(terms.length, terms.length);
    List<List<String>> clusters = new ArrayList<List<String>>();
    r = 0;
    while ((line = reader.readLine()) != null) {
      String[] termComps = line.split("\\s");
      for (int c = 0; c < terms.length; ++c)
        comparisons.set(r, c, Double.parseDouble(termComps[c+1]));
      List<String> bestCluster = null;
      double bestSim = 0;
      for (List<String> cluster : clusters) {
        for (String other : cluster) {
          int otherIndex = termMap.get(other).intValue();
          double sim = comparisons.get(otherIndex, r);
          if (sim > bestSim) {
            bestSim = sim;
            bestCluster = cluster;
          }
        }
      }

      if (bestCluster == null || bestSim < threshold) {
        bestCluster = new ArrayList<String>();
        clusters.add(bestCluster);
      }
      bestCluster.add(termComps[0]);
      r++;
    }

    return clusters;
  }

  public static void main(String[] args) throws Exception {
    ArgOptions opts = new ArgOptions();
    opts.addOption('t', "threshold", "Threshold for clustering",
                   true, "DOUBLE", "Required");
    opts.addOption('d', "docFile", "Document file with neighbors",
                   true, "FILE", "Required");
    opts.parseOptions(args);

    if (!opts.hasOption("threshold") ||
        !opts.hasOption("docFile")) {
      System.out.println("usage: java edu.ucla.sspace.tools.NeighborClusters" +
                         " [options] \n" + opts.prettyPrint());
      System.exit(1);
    }
    File neighborFile = new File(opts.getStringOption("docFile"));
    double threshold = opts.getDoubleOption("threshold");
    List<List<String>> clusters =
      findNeighborClusters(neighborFile, threshold);
    for (List<String> cluster : clusters) {
      System.out.println("New Cluster");
      for (String term : cluster) 
        System.out.println(term);
    }
  }
}
