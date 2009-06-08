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
import edu.ucla.sspace.common.SVD;
import edu.ucla.sspace.common.WordIterator;

import edu.ucla.sspace.common.matrix.GrowingSparseMatrix;

import edu.ucla.sspace.lsa.MatrixTransformer;
import edu.ucla.sspace.lsa.LogEntropyTransformer;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;

/** A new Semantic Space model which tries to combine the techniques of LSA
 * {@link edu.ucla.sspace.lsa.Hermit} and Beagle {@link
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
  "edu.ucla.sspace.lsa.Hermit.transform";

  public static final String LSA_DIMENSIONS_PROPERTY =
  "edu.ucla.sspace.lsa.Hermit.dimensions";

  public static final String NUM_THREADS_PROPERTY = 
  "edu.ucla.sspace.hermit.Hermit.threads";

  public static final String HERMIT_SSPACE_NAME =
  "hermit-semantic-space";

  private static final Logger LOGGER = 
  Logger.getLogger(Hermit.class.getName());

  /**
   * A mapping from a word to the row index in the that word-document matrix
   * that contains occurrence counts for that word.
   */
  private final ConcurrentMap<String,Triplet> termToIndex;

  private final ConcurrentMap<Integer, String> indexToTerm;
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
   * A file in {@link edu.ucla.sspace.common.MatrixIO.Format#MATLAB_SPARSE
   * MATLAB_SPARSE} format.
   */
  private final File rawTermDocMatrix;

  /**
   * The writer to the {@code rawTermDocMatrix}.
   */
  private final PrintWriter rawTermDocMatrixWriter;

  /**
   * The word space of the LSA model.  This matrix is only available after the
   * {@link #processSpace(Properties) processSpace} method has been called.
   */
  private Matrix wordSpace;

  private IndexBuilder indexBuilder;

  private int reducedDims;
  private int indexVectorSize;

  private int prevSize;
  private int nextSize;
  private int contextSize;

  /**
   * Constructs the {@code Hermit}.
   *
   * @throws IOException if this instance encounters any errors when creatng
   *         the backing array files required for processing
   */
  public Hermit(IndexBuilder builder, int vectorSize) throws IOException {
    termToIndex = new ConcurrentHashMap<String, Triplet>();
    indexToTerm = new ConcurrentHashMap<Integer, String>();
    termIndexCounter = new AtomicInteger(0);
    docIndexCounter = new AtomicInteger(0);

    rawTermDocMatrix = 
        File.createTempFile("hermit-term-document-matrix", "dat");
    rawTermDocMatrixWriter = new PrintWriter(rawTermDocMatrix);

    indexBuilder = builder;
    indexVectorSize = vectorSize;
    prevSize = builder.expectedSizeOfPrevWords();
    nextSize = builder.expectedSizeOfNextWords();
    contextSize = prevSize + nextSize;
    indexToTerm.put(0, "");
    termToIndex.put("", new Triplet(0, ""));
    wordSpace = null;
  }

  /**
   * Parses the document.
   *
   * @param document {@inheritDoc}
   */
  public void processDocument(BufferedReader document) throws IOException {
    Map<String,Integer> termCounts = 
        new LinkedHashMap<String,Integer>(1 << 10, 16f);  
    Queue<String> prevWords = new ArrayDeque<String>();
    Queue<String> nextWords = new ArrayDeque<String>();
    Map<String, List<int[]>> wordContexts =
      new HashMap<String, List<int[]>>();

    WordIterator it = new WordIterator(document);

    String focusWord = null;
    prevWords.add("");
    for (int i = 0; i < nextSize && it.hasNext(); ++i) {
      focusWord = it.next().intern();
      nextWords.offer(focusWord);

      addTerm(focusWord);

      Integer termCount = termCounts.get(focusWord);
      termCounts.put(focusWord, (termCount == null) 
                 ? Integer.valueOf(1)
                 : Integer.valueOf(1 + termCount.intValue()));
    }

    while (!nextWords.isEmpty()) {
      // Shift to a new focus word.
      focusWord = nextWords.remove();
      if (it.hasNext()) {
        String newWord = it.next().intern();

        // Add the term to the total list of terms to ensure it has a
        // proper index.  If the term was already added, this method is
        // a no-op
        addTerm(newWord);
        nextWords.offer(newWord);
      }

      // Build an integer array storing the context, where each integer
      // corresponds a unique id for the strings in the context.
      int[] currContext = makeCompactContext(prevWords, nextWords);

      List<int[]> contextList = wordContexts.get(focusWord);
      if (contextList == null) {
        contextList = new ArrayList<int[]>();
        wordContexts.put(focusWord, contextList);
      }
      contextList.add(currContext);

      // Shift the window over by one word.
      prevWords.offer(focusWord);
      if (prevWords.size() > prevSize)
        prevWords.remove();
      Integer termCount = termCounts.get(focusWord);

      // update the term count
      termCounts.put(focusWord, (termCount == null) 
                 ? Integer.valueOf(1)
                 : Integer.valueOf(1 + termCount.intValue()));
    }

    // check that we actually loaded in some terms before we increase the
    // documentIndex.  This could possibly save some dimensions in the final
    // array for documents that were essentially blank.  If we didn't see
    // any terms, just return 0
    if (termCounts.isEmpty())
        return;

    int documentIndex = docIndexCounter.incrementAndGet();

    for (Map.Entry<String, List<int[]>> e : wordContexts.entrySet()) {
      Triplet index = termToIndex.get(e.getKey());
      index.addContext(documentIndex, e.getValue());
    }

    for (Map.Entry<String, Integer> e : termCounts.entrySet()) {
      Triplet index = termToIndex.get(e.getKey());
      index.addToWordCount(e.getValue());
    }

    // Once the document has been fully parsed, output all of the sparse
    // data points using the writer.  Synchronize on the writer to prevent
    // any interleaving of output by other threads
    synchronized(rawTermDocMatrixWriter) {
      for (Map.Entry<String,Integer> e : termCounts.entrySet()) {
      String term = e.getKey();
      int count = e.getValue().intValue();
      StringBuffer sb = new StringBuffer(32);
      sb.append(termToIndex.get(term).wordId).append(" ").
          append(documentIndex).append(" ").append(count);
      rawTermDocMatrixWriter.println(sb.toString());
      }
      
      rawTermDocMatrixWriter.flush();
    }
  }
    
  private int[] makeCompactContext(Queue<String> prevWords,
                                   Queue<String> nextWords) {
    int[] context = new int[contextSize];
    int index = 0;
    for (String word : prevWords) {
      context[index] = termToIndex.get(word).wordId;
      index++;
    }
    for (String word : nextWords) {
      context[index] = termToIndex.get(word).wordId;
      index++;
    }
    return context;
  }

  /**
   * Adds the term to the list of terms and gives it an index, or if the term
   * has already been added, does nothing.
   */
  private void addTerm(String term) {
    // ensure that we are using the canonical version of this term so that
    // we can properly lock on it.
    term = term.intern();
    Triplet index = termToIndex.get(term);
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
          index = new Triplet(termIndexCounter.incrementAndGet(), term);
          termToIndex.put(term, index);
          indexToTerm.put(index.wordId, term);
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  public Set<String> getWords() {
    return Collections.unmodifiableSet(termToIndex.keySet());
  }

  /**
   * {@inheritDoc}
   */
  public double[] getVectorFor(String word) {
    // determine the index for the word
    Triplet index = termToIndex.get(word);
    
    // NB: substract 1 from the index value because our output starts at
    // index 1 (not 0), but the wordSpace Matrix starts indexing at 0.
    return (index == null || word.equals(""))
        ? null
        : wordSpace.getRow(index.wordId - 1);
  }

  /**
   * {@inheritDoc}
   */
  public String getSpaceName() {
    return HERMIT_SSPACE_NAME + "-" + indexVectorSize + "-" + reducedDims;
  }

  /**
   * {@inheritDoc}
   *
   * <p>
   *
   * This method is thread-safe and may be called in parallel with separate
   * documents to speed up overall processing time.
   *
   * @param properties {@inheritDoc} See this class's {@link
   *        Hermit javadoc} for the full list of supported
   *        properties.
   */
  public void processSpace(Properties properties) {
    try {
      // first ensure that we are no longer writing to the matrix
      synchronized(rawTermDocMatrix) {
        rawTermDocMatrixWriter.close();
      }

      // Next write all contexts to disk to free up memory.
      for (Triplet e : termToIndex.values()) {
        e.dumpContexts();
      }

      final Matrix termDocMatrix = loadLSAMatrix();

      int numThreads =
        Integer.parseInt(properties.getProperty(NUM_THREADS_PROPERTY, "1"));
      List<Thread> threads = new ArrayList<Thread>();

      final Iterator<Map.Entry<String, Triplet>> tripletIter =
        termToIndex.entrySet().iterator();

      for (int i = 0; i < numThreads; ++i) {
        Thread t = new Thread() {
          public void run() {
            while (tripletIter.hasNext()) {
              Map.Entry<String, Triplet> entry = null;
              synchronized (tripletIter) {
                if (tripletIter.hasNext())
                  entry = tripletIter.next();
                if (entry.getKey().equals(""))
                  continue;
              }
              System.out.println("clustering: " + entry.getKey() + " id: " + entry.getValue().wordId);
              int[] reassignments = clusterSemanticVectors(entry.getValue());
              int[] docIds = new int[reassignments.length];
              splitMatrix(termDocMatrix, entry.getKey(),
                          entry.getValue(), reassignments);
            }
          }
        };
        threads.add(t);
      }

      try {
        for (Thread t : threads)
          t.start();
        for (Thread t : threads)
          t.join();
      } catch (InterruptedException ie) {
        throw new IOError(ie);
      }

      termToIndex.remove("");
      indexToTerm.remove(0);

      // After doing the hermit splitting, do the regular transformations and
      // dimensionality reduction.
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

      File processedTermDocMatrix = dumpLSAMatrix(termDocMatrix);

      // Convert the split term counts using the specified transform
      File processedTermDocumentMatrix =
        transform.transform(processedTermDocMatrix);
        
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
      //rethrow as Error
      throw new IOError(ioe);
    }
  }

  /**                                                                                                                                          
   * Print out the lsa term document matrix to a temp file in                                                                                  
   * in {@link edu.ucla.sspace.common.MatrixIO.Format#MATLAB_SPARSE                                                                            
   * MATLAB_SPARSE} format.                                                                                                                    
   */                                                                                                                                          
  private File dumpLSAMatrix(Matrix lsaMatrix) throws IOException {                                                                                            
    File lsaFile = File.createTempFile("hermit-term-doc-matrix", ".tmp");                                                                      
    PrintWriter lsaWriter = new PrintWriter(lsaFile);                                                                                          
    for (int row = 0; row < lsaMatrix.rows(); ++row) {
      double[] currRow = lsaMatrix.getRow(row);
      for (int col = 0; col < currRow.length; ++col) {
        if (currRow[col] != 0d) {
          StringBuffer sb = new StringBuffer(32);
          sb.append(row).append("\t").
              append(col).append("\t").append(new Double(currRow[col]).intValue());
          lsaWriter.println(sb.toString());
        }
      }
    }
    lsaWriter.flush();                                                                                                                         
    lsaWriter.close();                                                                                                                         
    return lsaFile;                                                                                                                            
  }

  private Matrix loadLSAMatrix() throws IOException {
    BufferedReader br = new BufferedReader(new FileReader(rawTermDocMatrix));
    String line = null;
    Matrix lsaMatrix = new GrowingSparseMatrix();
    while ((line = br.readLine()) != null) {
      String[] rowColCount = line.split(" ");
      int row = Integer.parseInt(rowColCount[0]);
      int col = Integer.parseInt(rowColCount[1]);
      int count = Integer.parseInt(rowColCount[2]);
      lsaMatrix.set(row, col, count);
    }
    return lsaMatrix;
  }

  private int[] clusterSemanticVectors(Triplet triplet) {
    // First read in all contexts for this triplet.
    // Lock on file reading to keep the disk from thrashing to heavily.
    synchronized (this) {
      triplet.loadContexts();
    }

    // Create a semantic vector for each document.
    List<double[]> semanticVectors = new ArrayList<double[]>();
    int assignmentIndex = 0;
    for (Map.Entry<Integer, List<int[]>> e :
        triplet.docToContextMap.entrySet()) {
      double[] meaning = new double[indexVectorSize];
      for (int[] context : e.getValue()) {
        // Create the queues needed to represent the current context.
        Queue<String> prevWords = new ArrayDeque<String>();
        Queue<String> nextWords = new ArrayDeque<String>();
        for (int i = 0; i < prevSize; ++i)
          prevWords.add(indexToTerm.get(context[i]));
        for (int i = prevSize; i < context.length; ++i)
          nextWords.add(indexToTerm.get(context[i]));

        // Update meaning with the context.
        indexBuilder.updateMeaningWithTerm(meaning, prevWords, nextWords);
      }
      semanticVectors.add(meaning);
    }

    // Clear the map again to clear out memory.
    triplet.docToContextMap.clear();

    double oldPotential = 0;
    double potential = 0;
    int[] bestAssignments = null;
    int[] assignments = null;
    int k = 1;

    // Cluster the semantic vectors with a larger number of clusters until the
    // kMeansPotential reaches a relative maximum. This will determine the
    // number of senses produced for a word.
    do {
      double[][] kClusters =
        Cluster.kMeansCluster(semanticVectors, k, indexVectorSize);
      oldPotential = potential;
      bestAssignments = assignments;
      potential = Cluster.kMeansPotential(semanticVectors, kClusters);
      assignments = Cluster.kMeansClusterAssignments(semanticVectors,
                                                     kClusters);
      k++;
    } while (potential > oldPotential && k < 7);

    return (bestAssignments != null) ? bestAssignments : new int[semanticVectors.size()];
  }

  private synchronized void splitMatrix(Matrix m, String word, Triplet triplet,
                                        int[] assignments) {
    int i = 0;
    for (Integer docId : triplet.docToContextMap.keySet()) {
      if (assignments[i] == 0)
        continue;
      String currentTerm = word + ":" + assignments[i];
      if (!termToIndex.containsKey(currentTerm))
        addTerm(currentTerm);
      int newIndex = termToIndex.get(currentTerm).wordId;

      double splitValue = m.get(triplet.wordId, docId);
      m.set(newIndex, docId, splitValue);
      m.set(triplet.wordId, docId, 0);
      i++;
    }
  }

  private class Triplet {
    public static final int CONTEXT_LIMIT = 100000;

    int wordId;
    AtomicInteger wordCount;
    public final ConcurrentNavigableMap<Integer, List<int[]>>
      docToContextMap;
    AtomicInteger contextCount;
    File contextDump;

    public Triplet(int id, String word) {
      wordId = id;
      wordCount = new AtomicInteger();
      contextCount = new AtomicInteger();
      contextDump = null;
      docToContextMap =
        new ConcurrentSkipListMap<Integer, List<int[]>>();
      try {
        contextDump = File.createTempFile("hermit-dump-" + word, "cxt");
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }

    public void addContext(int docId, List<int[]> context) {
      docToContextMap.put(docId, context);
      contextCount.addAndGet(context.size());

      if (contextCount.get() >= CONTEXT_LIMIT) {
        synchronized (this) {
          dumpContexts();
        }
      }
    }

    public void dumpContexts() {
      try {
        DataOutputStream writer =
          new DataOutputStream(new FileOutputStream(contextDump, true));
        for (Map.Entry<Integer, List<int[]>> e : docToContextMap.entrySet()) {
          for (int[] contextList : e.getValue()) {
            writer.writeInt(e.getKey());
            for (int value : contextList) {
              writer.writeInt(value);
            }
          }
        }
        contextCount.set(0);
        docToContextMap.clear();
        writer.close();
      } catch (IOException ioe) {
        throw new IOError(ioe);
      }
    }

    public void loadContexts() {
      int i = 0;
      int j = 0;
      long numContexts = 0;
      try {
        DataInputStream reader = 
          new DataInputStream(new FileInputStream(contextDump));
        long fileSize = contextDump.length();
        numContexts = fileSize / (4 * (1 + contextSize));
        for (; i < numContexts; ++i) {
          int docId = reader.readInt();
          int context[] = new int[contextSize];
          for (; j < contextSize; ++j) {
            context[j] = reader.readInt();
          }
          List<int[]> docContexts = docToContextMap.get(docId);
          if (docContexts == null) {
            docContexts = new ArrayList<int[]>();
            docToContextMap.put(docId, docContexts);
          }
          docContexts.add(context);
        }
        reader.close();
      } catch (IOException ioe) {
        System.out.println(contextDump.getPath());
        System.out.println("line " + i + ", value " + j);
        System.out.println("numContexts " + numContexts);
        throw new IOError(ioe);
      }
    }

    public void addToWordCount(int delta) {
      wordCount.addAndGet(delta);
    }
  }
}
