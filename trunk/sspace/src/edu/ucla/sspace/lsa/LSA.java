package edu.ucla.sspace.lsa;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.BoundedSortedMap;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.SemanticSpace;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import java.util.regex.Pattern;

import Jama.Matrix;
import Jama.SingularValueDecomposition;

public class LSA implements SemanticSpace {

  private static final int LSA_DIMENSIONS = 50;

  private static final int LINES_TO_SKIP = 40;

  private static final int MAX_LINES = 500;

  private final Map<Index,Integer> wordToDocumentCount;

  private final Map<Index,Double> termFreqInvDocFreqMatrix;

  /**
   * A mapping from a word to the reconstructed vector that best approximates
   * its meaning.  This vector is created after computing the SVD of the
   * word-document matrix and then reconstructing the matrix using a reduced
   * number of dimensions.
   */
  private final Map<String,double[]> wordToApprox;

  public final Set<String> words;

  public final Set<String> documents;

  //private final SpellChecker spellChecker;

  private SingularValueDecomposition svd;

  public LSA() {
    wordToDocumentCount = new HashMap<Index,Integer>();
    termFreqInvDocFreqMatrix = new HashMap<Index,Double>();
    wordToApprox = new HashMap<String,double[]>();
    words = new LinkedHashSet<String>();
    documents = new LinkedHashSet<String>();
    //spellChecker = loadSpellChecker();
  }

  /*
  private static SpellChecker loadSpellChecker() {
    try {
        String DICTIONARY_PATH = "dictionary/english/";
        SpellDictionaryHashMap dictionary = 
        new SpellDictionaryHashMap();

        // load the standard american english dictionaries from file
        dictionary.addDictionary(new File(DICTIONARY_PATH + "eng_com.dic"));
        dictionary.addDictionary(new File(DICTIONARY_PATH + "center.dic"));
        dictionary.addDictionary(new File(DICTIONARY_PATH + "ize.dic"));
        dictionary.addDictionary(new File(DICTIONARY_PATH + "labeled.dic"));
        dictionary.addDictionary(new File(DICTIONARY_PATH + "yze.dic"));
        dictionary.addDictionary(new File(DICTIONARY_PATH + "color.dic"));
        
        return new SpellChecker(dictionary);
    }
    catch (Exception e) {
        e.printStackTrace();
        System.out.println("No spell checker available");
        return null;
    }
  }
  */
  
  public void parseDocument(String filename) throws IOException {
    documents.add(filename);
    BufferedReader br = new BufferedReader(new FileReader(filename));
    String line = null;
    int lineNum = 0;
    while ((line = br.readLine()) != null) {
      if (lineNum++ < LINES_TO_SKIP)
        continue;

      if (lineNum > MAX_LINES)
        break;

      // split the line based on whitespace
      String[] text = line.split("\\s");
      for (String word : text) {
        // clean up each word before entering it into the matrix
        String cleaned = cleanup(word);
        // skip any mispelled or unknown words
        if (!isValid(cleaned))
            continue;
        words.add(cleaned);
        wordToDocumentCount.put(
            new Index(cleaned, filename), 
            Integer.valueOf(1 + getCount(cleaned, filename)));
      }
    }
    br.close();
  }

  public void processSpace() {
    // Compute a how many terms occur in each document.  This allows us to
    // normalize the frequency to prevent biasing towards larger document
    // with more words
    Map<String,Integer> documentToTermCount = new HashMap<String,Integer>();
    for (String document : documents) {
      int terms = 0;
      for (String word :  words) {
        terms += getCount(word, document);
      }
      documentToTermCount.put(document, terms);
    }
  
    // The inverse document frequency is a measure of the general importance
    // of the term (obtained by dividing the number of all documents by the
    // number of documents containing the term, and then taking the
    // logarithm of that quotient).  (from Wikipedia)

    for (String word : words) {
      double docFreq = 0;
      for (String document : documents) {
        if (getCount(word, document) > 0)
          docFreq++;
      }

      double invDocFreq = Math.log(documents.size() / docFreq);
      
      // now divide each term's frequency by the frequency for all the
      // documents
      for (String document : documents) {
        double freq = getCount(word, document);
        if (freq > 0) {
          // normalize the frequency by the number of terms in the
          // document
          double norm = freq / documentToTermCount.get(document);
      
          termFreqInvDocFreqMatrix.put(new Index(word, document), 
                                        norm * invDocFreq);
        }
      }
    }
    wordToDocumentCount.clear();
  }

  /**
   * Returns the number of occurrences of the provided word in the provided
   * document.
   */
  private int getCount(String word, String document) {
    Index index = new Index(word, document);
    Integer occurrence = wordToDocumentCount.get(index);
    return (occurrence == null) ? 0 : occurrence.intValue();
  }

  /**
   * Returns whether the provided word is valid according to the spell
   * checker, or returns {@code true} if no spell checker has been loaded.
   */
  private boolean isValid(String word) {
    /*
    return (spellChecker == null) 
        ? true
        : spellChecker.isCorrect(word);
      */
    return true;
  }

  private static String cleanup(String word) {
    // remove all non-letter characters
    word = word.replaceAll("\\W", "");
    // make the string lower case
    return word.toLowerCase();
  }

  public void loadWordDocumentMatrix(String inputFile) throws IOException {
    // first clear any old data
    documents.clear();
    words.clear();
    wordToDocumentCount.clear();
    
    BufferedReader br = new BufferedReader(new FileReader(inputFile));
    // the first line must contain the listing of documents in the order in
    // which their data will be presented
    String documentsLine = br.readLine();
    String[] docs = documentsLine.split(" ");
    for (String document : docs)
      documents.add(document);

    String line = null;
    int lineNum = 0;
    while ((line = br.readLine()) != null) {
      lineNum++;
      String[] wordAndOccurrences = line.split(" ");
      // ensure that there are occurrence numbers for each document
      // (subtract 1 to account for the word itself)
      if (wordAndOccurrences.length -1 != documents.size())
        throw new IllegalStateException(
            "Missing occurrence counts on line " + lineNum);
      
      String word = wordAndOccurrences[0];
      Iterator<String> docIter = documents.iterator();	    
      for (int i = 1; i < wordAndOccurrences.length; ++i) {
        Integer occurrence = Integer.valueOf(wordAndOccurrences[i]);
        // only put positive numbers in the backing map to reduce the
        // memory footprint
        if (occurrence.intValue() > 0)
          wordToDocumentCount.put(new Index(word, docIter.next()), occurrence);
      }
    }
  }

  public void saveWordDocumentMatrix(String outputFile) throws IOException {
    PrintWriter pw = new PrintWriter(outputFile);

    // first write out all the documents
    Iterator<String> docIter = documents.iterator();
    StringBuilder sb = new StringBuilder(12 * documents.size());
    while (docIter.hasNext()) {
      sb.append(docIter.next());
      if (docIter.hasNext())
        sb.append(" ");
    }
    pw.println(sb);

    // then for each word, write out the occurences for each document
    for (String word : words) {
      sb = new StringBuilder(3 * documents.size());
      sb.append(word).append(" ");
      docIter = documents.iterator();
      while (docIter.hasNext()) {
        Index index = new Index(word, docIter.next());
        Integer count = wordToDocumentCount.get(index);
        sb.append((count == null) ? "0" : count);
        if (docIter.hasNext())
          sb.append(" ");
      }
      pw.println(sb);
    }
    pw.close();
  }

  private Matrix convertMapToMatrix() {
    Matrix matrix = new Matrix(words.size(), documents.size());

    // Calculate these maps on the fly to avoid having to persist them in
    // memory during the remaining computation
    Map<String,Integer> wordToIndex = new HashMap<String,Integer>();
    int i = 0;
    for (String word : words) 
      wordToIndex.put(word, Integer.valueOf(i++));

    Map<String,Integer> docToIndex = new HashMap<String,Integer>();
    i = 0;
    for (String doc : documents)
      docToIndex.put(doc, Integer.valueOf(i++));

    // if we've already calculated the tf-idf for the words, then use those
    // values for filling the matrix
    if (termFreqInvDocFreqMatrix.size() > 0) {
      for (Map.Entry<Index,Double> e : 
        termFreqInvDocFreqMatrix.entrySet()) {
        Index index = e.getKey();

        matrix.set(wordToIndex.get(index.word).intValue(),
               docToIndex.get(index.document).intValue(),
               e.getValue().doubleValue());
      }
      // last, clear the maps to free the space used by it
      termFreqInvDocFreqMatrix.clear();
      wordToDocumentCount.clear();
    }
    else {
      for (Map.Entry<Index,Integer> e : wordToDocumentCount.entrySet()) {
        Index index = e.getKey();
        
        matrix.set(wordToIndex.get(index.word).intValue(),
               docToIndex.get(index.document).intValue(),
               e.getValue().doubleValue());
      }
      // last, clear the map to free the space used by it
      wordToDocumentCount.clear();
    }
    return matrix;
  }

  public void reduce() {
    System.out.print("computing SVD ...");
    long startTime = System.currentTimeMillis();
    Matrix matrix = convertMapToMatrix();
    System.out.printf("matrix generated (%.3f sec)...",
        (System.currentTimeMillis() - startTime) / 1000d);
    svd = matrix.svd();
    long endTime = System.currentTimeMillis();
    System.out.printf("complete (%.3f seconds)%n",
              (endTime - startTime) / 1000d);	
  }

  public void saveSVDresults(String filename) throws IOException {
    ObjectOutputStream oos = new ObjectOutputStream(
        new FileOutputStream(filename));
    oos.writeObject(svd);
    oos.close();
  }

  public void loadSVDresults(String filename) throws IOException,
                                                     ClassNotFoundException {

    ObjectInputStream ois = new ObjectInputStream(
        new FileInputStream(filename));
    svd = (SingularValueDecomposition)(ois.readObject());
    ois.close();
  }

  public void reduceDimensionality() {
    System.out.println("recalculating approximated matrix");
    long startTime = System.currentTimeMillis();
    // now X = U*S*V'
    //
    // next, remove all factors we don't want to keep (i.e. reduce the
    // dimensionality)
    Matrix U = svd.getU();
    Matrix S = svd.getS();
    Matrix V = svd.getV();

    System.out.printf("U: %d x %d; S: %d x %d; V: %d x %d%n",
              U.getRowDimension(),
              U.getColumnDimension(),
              S.getRowDimension(),
              S.getColumnDimension(),
              V.getRowDimension(),
              V.getColumnDimension());

    int dimension = Math.min(LSA_DIMENSIONS, U.getColumnDimension());
    System.out.printf("  reducing from %d to to %d dimensions%n",
              U.getColumnDimension(), dimension);
    Matrix Uprime = new Matrix(U.getArrayCopy(), U.getRowDimension(), 
                   dimension);
    Matrix Sprime = new Matrix(S.getArrayCopy(), dimension, dimension);
    Matrix Vprime = new Matrix(V.getArrayCopy(), dimension, dimension);
    
    System.out.printf("U: %d x %d; S: %d x %d; V: %d x %d%n",
            Uprime.getRowDimension(),
            Uprime.getColumnDimension(),
            Sprime.getRowDimension(),
            Sprime.getColumnDimension(),
            Vprime.getRowDimension(),
            Vprime.getColumnDimension());


    //Matrix Xhat = Uprime.times(Sprime).times(Vprime.transpose());
    //Matrix Xhat = Vprime.transpose().times(Sprime).times(Uprime);
    Matrix Xhat = Uprime.times(Sprime.times(Vprime.transpose()));

    long endTime = System.currentTimeMillis();
    System.out.printf("...complete (%.3f seconds)%n",
              (endTime - startTime) / 1000d);
    System.out.printf("X hat's dimensions: %d rows, %d columns %n",
              Xhat.getRowDimension(), Xhat.getColumnDimension());

    // each row is associated with a word, so associate the array with the
    // string for the word
    double[][] xHatArray = Xhat.getArray(); // REMDINER: use a copy?
    int i = 0;
    for (String word : words) 
      wordToApprox.put(word, xHatArray[i++]);
  }

  private static double[][] resize(double[][] orig, int rows, int cols) {
    double[][] resized = new double[rows][cols];
    for (int r = 0; r < rows; ++r) {
    }
    return resized;
  }

  public void computeDistances(String filename, int similarCount) {
    System.out.print("computing word similarities ...");
    long startTime = System.currentTimeMillis();
    Map<String,BoundedSortedMap<Double,String>> wordToMostSimilar = 
        new HashMap<String,BoundedSortedMap<Double,String>>();

    for (Map.Entry<String,double[]> e : wordToApprox.entrySet()) {
      String word = e.getKey();
      double[] vec = e.getValue();
      BoundedSortedMap<Double,String> mostSimilar = 
      new BoundedSortedMap<Double,String>(similarCount);
      wordToMostSimilar.put(word, mostSimilar);

      for (Map.Entry<String,double[]> f : wordToApprox.entrySet()) {
        String other = f.getKey();
      if (word.equals(other))
        continue;
      double[] vec2 = f.getValue();
      Double cosDist = Double.valueOf(Similarity.cosineSimilarity(vec, vec2));
      mostSimilar.put(cosDist,other);		
      }
    }

    PrintWriter pw = null;
    if (filename != null) {
      try {
        pw = new PrintWriter(filename);
      } catch (IOException ioe) {
        pw = null;
        ioe.printStackTrace();
      }
    }

    for (Map.Entry<String,BoundedSortedMap<Double,String>> e :
        wordToMostSimilar.entrySet()) {

      StringBuilder sb = new StringBuilder((1 + similarCount) * 8);
      sb.append(e.getKey()).append(":\n");
      for (String s : e.getValue().values())
        sb.append("  ").append(s);

      if (pw == null) 
        System.out.println(sb.toString());
      else
        pw.println(sb.toString());
    }
    if (pw != null) {
        pw.close();
    }
    long endTime = System.currentTimeMillis();
    System.out.printf("...complete (%.3f seconds)%n",
              (endTime - startTime) / 1000d);
  }
}
