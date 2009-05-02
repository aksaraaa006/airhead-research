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

package edu.ucla.sspace.hermit;

import edu.ucla.sspace.common.Cluster;
import edu.ucla.sspace.common.IndexBuilder;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.StringUtils;
import edu.ucla.sspace.common.SVD;

import edu.ucla.sspace.lsa.MatrixTransformer;
import edu.ucla.sspace.lsa.LogEntropyTransformer;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerArray;

/** A new Semantic Space model which tries to combine the techniques of LSA
 * {@link edu.ucla.sspace.lsa.LatentSemanticAnalysis} and Beagle {@link
 * edu.ucla.sspace.holograph.Holograph}.  The central goal of this algorithm is
 * to be capable of expanding the results of LSA to take into account multiple
 * senses of a word.  This will be accomplished mainly by building Holograph
 * vectors a la Beagle {@link edu.ucla.sspace.holograph.Holograph} for each term
 * document pair, and then for each term, cluster the relevant holographs to
 * determine how a single row in the LSA term document matrix can be split up
 * into multiple senses.  After splitting up the LSA matrix, the usual
 * processing routine will take place.
 */
public class Hermit implements SemanticSpace {
  private static final int CONTEXT_SIZE = 7;
  public static final String MATRIX_TRANSFORM_PROPERTY =
  "edu.ucla.sspace.lsa.LatentSemanticAnalysis.transform";

  public static final String LSA_DIMENSIONS_PROPERTY =
  "edu.ucla.sspace.lsa.LatentSemanticAnalysis.dimensions";

  /**
   * Beagle based builder for random index vectors.
   */
  private final IndexBuilder indexBuilder;
  /**
   * Sparse matrix map which will contain the raw lsa counts in memory.
   */
  private final Map<Index,Integer> termDocCount;
  /**
   * File writers for each term.  This will be used to temporarily store the
   * holoraphs for each term.
   */
  private final Map<String, File> termFiles;

  /**
   * A mapping from a word to the row index in the that word-document matrix
   * that contains occurrence counts for that word.
   */
  private final Map<String,Integer> termToIndex;
  /**
   * The size which the Beagle based holograph vectors will be.
   */
  private final int indexVectorSize;

  /**
   * Total count of occurances for every term.
   */
  private final AtomicIntegerArray termCountsForAllDocs;    
  /**
   * The counter for recording the current, largest word index in the
   * word-document matrix.
   */
  private final AtomicInteger termIndexCounter;

  /**
   * The counter for recording the current, largest document index in the
   * word-document matrix.
   */
  private final AtomicInteger docIndexCounter;

  /**
   * The word space of the LSA model.  This matrix is only available after the
   * {@link #processSpace(Properties) processSpace} method has been called.
   */
  private Matrix wordSpace;

  /**
   * Construct the Hermit Semantic Space
   */
  public Hermit(IndexBuilder builder, int vectorSize) {
	termIndexCounter = new AtomicInteger(0);
	docIndexCounter = new AtomicInteger(0);
	termCountsForAllDocs = new AtomicIntegerArray(1 << 25);

	termDocCount = new ConcurrentHashMap<Index,Integer>();
    termFiles = new ConcurrentHashMap<String, File>();
	termToIndex = new ConcurrentHashMap<String,Integer>();
    indexBuilder = builder;
    indexVectorSize = vectorSize;
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return termToIndex.keySet();
  }

  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String word) {
    Integer index = termToIndex.get(word);
    if (index == null)
      return null;
    return wordSpace.getRow(index.intValue() - 1);
  }

  /**
   * Parses the document.
   *
   * @param document {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    // split the line based on whitespace
    HashMap<String, double[]> termDocHolographs = new HashMap<String, double[]>();
    LinkedList<String> words = new LinkedList<String>();
    words.add("");
	Map<String,Integer> termCounts = 
        new LinkedHashMap<String,Integer>(1 << 10, 16f);	
    for (String line = null; (line = document.readLine()) != null;) {
      String[] text = line.split("\\s");
      for (String word : text) {
        // clean up each word before entering it into the matrix
        String cleaned = StringUtils.cleanup(word);
        // skip any mispelled or unknown words
        if (!StringUtils.isValid(cleaned))
          continue;
        words.add(cleaned);
        // update the holographs, first adding new index vectors if needed.
        indexBuilder.addTermIfMissing(cleaned);
        updateHolograph(words, termDocHolographs);

        // Update the lsa based information.
		addTerm(cleaned);
		Integer termCount = termCounts.get(cleaned);

		// update the term count
		termCounts.put(cleaned, (termCount == null) 
			       ? Integer.valueOf(1)
			       : Integer.valueOf(1 + termCount.intValue()));
      }
    }
    finishUpdate(words, termDocHolographs);
    document.close();
	// check that we actually loaded in some terms before we increase the
	// documentIndex.  This could possibly save some dimensions in the final
	// array for documents that were essentially blank.  If we didn't see
	// any terms, just return 0
	if (termCounts.isEmpty())
	    return;

	int documentIndex = docIndexCounter.incrementAndGet();
    for (String key : termCounts.keySet()) {
      synchronized(key) {
        File writer = termFiles.get(key);
        if (writer == null)
          termFiles.put(
              key, File.createTempFile("holograph-" + key, ".txt"));
      }
    }

    dumpHolographsToFile(termDocHolographs, documentIndex);

	// Once the document has been fully parsed, output all of the space values
    // into the main concurrent map. 
    for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
      String term = e.getKey();
      Index index = new Index(termToIndex.get(term), documentIndex);
      int count = e.getValue().intValue();
      termDocCount.put(index, count);
      termCountsForAllDocs.addAndGet(
          termToIndex.get(e.getKey()).intValue(),
          e.getValue().intValue());
    }
  }

  /**
   * Adds the term to the list of terms and gives it an index, or if the term
   * has already been added, does nothing.
   */
  private Integer addTerm(String term) {
    // ensure that we are using the canonical version of this term so that
    // we can properly lock on it.
    term = term.intern();
    Integer index = termToIndex.get(term);
    if (index == null) {
      // lock on the term itself so that only two threads trying to add
      // the same term will block on each other
      synchronized(term) {
        // recheck to see if the term was added while blocking
        index = termToIndex.get(term);
        // if some other thread has not already added this term while
        // the current thread was blocking waiting on the lock, then add
        // it.
        if (index == null) {
          index = Integer.valueOf(termIndexCounter.incrementAndGet());
          termToIndex.put(term, index);
        }
      }
    }
    return index;
  }

  /**
   * Print all of the holographs to the appropriate temporary file.
   *
   * @param termDocHolographs A mapping of terms to their meaning vectors for a
   * particular document.
   * @param document The current document number.
   */
  private void dumpHolographsToFile(HashMap<String, double[]> termDocHolographs, int document) {
    try {
      for (Map.Entry<String, double[]> entry : termDocHolographs.entrySet()) {
        File termFile = termFiles.get(entry.getKey());
        synchronized (termFile) {
          BufferedWriter writer =
            new BufferedWriter(new FileWriter(termFile, true));
          StringBuffer sb = new StringBuffer();
          sb.append(document).append(" ");
          double[] value = entry.getValue();
          for (int i = 0; i < indexVectorSize; ++i)
            sb.append(value[i]).append(" ");
          writer.write(sb.toString());
          writer.newLine();
          writer.close();
        }
      }
    } catch (IOException e) {
    }
  }

  /**
   * Read Holograph vectors from a file for a particular word
   *
   * @param term Term for which the saved holograph vectors should be retrieved
   * @return An ArrayList of {@link DocHolographPair}s.
   */
  private ArrayList<DocHolographPair> uploadTermMeaning(String term) {
    try {
      File termFile = termFiles.get(term);
      ArrayList<DocHolographPair> termVectors =
        new ArrayList<DocHolographPair>();
      synchronized  (termFile) {
        BufferedReader reader = new BufferedReader(new FileReader(termFile));
        String newLine = null;
        int count = 0;
        while ((newLine = reader.readLine()) != null) {
          count++;
          String[] splitLine = newLine.split(" ");
          if (splitLine.length != (indexVectorSize + 1)) {
            continue;
          }
          double[] holograph = new double[indexVectorSize];
          int docId = Integer.valueOf(splitLine[0]);
          for (int i = 1; i < splitLine.length; ++i)
            holograph[i-1] = Double.valueOf(splitLine[i]);
          termVectors.add(new DocHolographPair(docId, holograph));
        }
        reader.close();
      }
      return termVectors;
    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }


  /**
   * {@inheritDoc}
   *
   * <p>
   *
   * @param properties {@inheritDoc} See this class's {@link
   *        Hermit javadoc} for the full list of supported
   *        properties.
   */
  public void processSpace(Properties properties) {
    for (String key : termFiles.keySet()) {
      ArrayList<DocHolographPair> termVectors = uploadTermMeaning(key);
      // If there are 0, or 1 term vectors then there is no way to split them
      // up.
      if (termVectors.size() < 2)
        continue;
      ArrayList<double[]> termMeanings =
        new ArrayList<double[]>(termVectors.size());
      for (DocHolographPair docPair : termVectors)
        termMeanings.add(docPair.holograph);
      double oldPotential = 0;
      double potential = 0;
      int[] bestAssignments = null;
      int[] assignments = null;
      int k = 1;
      do {
        double[][] kClusters =
          Cluster.kMeansCluster(termMeanings, k, indexVectorSize);
        oldPotential = potential;
        bestAssignments = assignments;
        potential = Cluster.kMeansPotential(termMeanings, kClusters) / k;
        assignments = Cluster.kMeansClusterAssignments(termMeanings, kClusters);
        k++;
      } while (potential > oldPotential);
      if (k == 1)
        continue;
      for (int i = 0; i < termVectors.size(); ++i) {
        if (assignments[i] == 0)
          continue;
        Index oldIndex =
          new Index(termToIndex.get(key), termVectors.get(i).docId);
        String newTerm = key + "^" + assignments[i];
        Integer newTermIndex = termToIndex.get(newTerm);
        if (newTermIndex == null)
          newTermIndex = addTerm(newTerm);
        Index newIndex = new Index(newTermIndex.intValue(),
                                   termVectors.get(i).docId);
        termDocCount.put(newIndex, termDocCount.get(oldIndex));
        termDocCount.remove(oldIndex);
      }
    }
    try {
      File rawTermDocMatrix = dumpLSAMatrix();
      MatrixTransformer transform = new LogEntropyTransformer();

      String transformClass = 
      properties.getProperty(MATRIX_TRANSFORM_PROPERTY);
      if (transformClass != null) {
        try {
          Class clazz = Class.forName(transformClass);
          transform = (MatrixTransformer)(clazz.newInstance());
        } 
        // perform a general catch here due to the number of possible
        // things that could go wrong.  Rethrow all exceptions as an
        // error.
        catch (Exception e) {
          throw new Error(e);
        } 
      }

      // Convert the raw term counts using the specified transform
      File processedTermDocumentMatrix = 
      transform.transform(rawTermDocMatrix);
      
      int dimensions = 300; // default
      String userSpecfiedDims = 
      properties.getProperty(LSA_DIMENSIONS_PROPERTY);
      if (userSpecfiedDims != null) {
        try {
          dimensions = Integer.parseInt(userSpecfiedDims);
        } catch (NumberFormatException nfe) {
          throw new IllegalArgumentException(
            LSA_DIMENSIONS_PROPERTY + " is not an integer: " +
          userSpecfiedDims);
        }
      }
      // Compute SVD on the pre-processed matrix.
      Matrix[] usv = SVD.svd(processedTermDocumentMatrix, dimensions);
      
      // Load the left factor matrix, which is the word semantic space
      wordSpace = usv[0];

    } catch (IOException ioe) {
      throw new IOError(ioe);
    }
  }

  /**
   * Print out the lsa term document matrix to a temp file in 
   * in {@link edu.ucla.sspace.common.MatrixIO.Format#MATLAB_SPARSE
   * MATLAB_SPARSE} format.
   */
  private File dumpLSAMatrix() throws IOException {
    HashSet<Integer> tempSet = new HashSet<Integer>();
    File lsaFile = File.createTempFile("hermit-term-doc-matrix", ".tmp");
    PrintWriter lsaWriter = new PrintWriter(lsaFile);
    for (Map.Entry<Index,Integer> e : termDocCount.entrySet()) {
      tempSet.add(e.getKey().termId);
      Index index = e.getKey();
      int count = e.getValue().intValue();
      StringBuffer sb = new StringBuffer(32);
      sb.append(index.termId).append("\t").
          append(index.docId).append("\t").append(count);
      lsaWriter.println(sb.toString());
    }
    termDocCount.clear();
    lsaWriter.flush();
    lsaWriter.close();
    return lsaFile;
  }

  private void finishUpdate(LinkedList<String> words,
                            HashMap<String, double[]> termDocHolographs) {
    int size = words.size();
    for (int i = 0; i < size; ++i) {
      words.add("");
      updateHolograph(words, termDocHolographs);
    }
  }

  /**
   * Update the meaning of the main word using words as the context.
   * @param words A LinkedList of Strings functioning as a sliding window of
   * words.
   * @param termDocHolographs a mapping of terms to holograph vectors which will
   * get updated with the most recent meaning of the first word in words.
   */
  private void updateHolograph(LinkedList<String> words,
                               HashMap<String, double[]> termDocHolographs) {
    if (words.size() < CONTEXT_SIZE) {
      return;
    }
    String[] context = words.toArray(new String[0]);
    String mainWord = context[1];
    context[1] = "";
    double[] meaning = termDocHolographs.get(mainWord);
    if (meaning == null) {
      meaning = new double[indexVectorSize];
      termDocHolographs.put(mainWord, meaning);
    }
    indexBuilder.updateMeaningWithTerm(meaning, context);
    words.removeFirst();
  }

  /**
   * A simple wrapper class containing the document name and holograph relevant
   * to some particular word.
   */
  private class DocHolographPair {
    int docId;
    double[] holograph;

    public DocHolographPair(int d, double[] h) {
      docId = d;
      holograph = h;
    }
  }

  /**
   * A simple index class for representing an index in some sparse matrix.
   */
  private class Index {
    public int docId;
    public int termId;

    public Index(int t, int d) {
      docId = d;
      termId = t;
    }
    public boolean equals(Object o) {
      if (o instanceof Index) {
        Index i = (Index)o;
        return docId == i.docId && termId == i.termId;
      }
      return false;
    }
    public int hashCode() {
      return new Integer(docId).hashCode() ^ (new Integer(termId).hashCode());
    }
  }
}
