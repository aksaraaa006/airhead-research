/*
 * Copyright 2009 Sky Lin
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

package edu.ucla.sspace.lra;

import edu.ucla.sspace.common.BoundedSortedMap;
import edu.ucla.sspace.common.Pair;
import edu.ucla.sspace.common.Matrix;
import edu.ucla.sspace.common.MatrixIO;
import edu.ucla.sspace.common.Matrices;
import static edu.ucla.sspace.common.Similarity.cosineSimilarity;
import edu.ucla.sspace.common.SVD;

import edu.ucla.sspace.lsa.LogEntropyTransformer;
import edu.ucla.sspace.lsa.MatrixTransformer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.lang.Float;
import java.lang.Integer;
import java.lang.Math;

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Scanner;
import java.util.SortedMap;

import edu.ucla.sspace.common.Index;
import edu.ucla.sspace.common.HashMultiMap;

import edu.smu.tspell.wordnet.*;

import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.WhitespaceAnalyzer;
import org.apache.lucene.document.DateTools;
import org.apache.lucene.document.Document; 
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Store;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryParser.QueryParser;
import org.apache.lucene.search.HitCollector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.PhraseQuery;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.Similarity;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

public class LRA {
    //constants...should probably be in the constructor
    private static final int NUM_SIM = 10; 
    private static final int MAX_PHRASE = 5; 
    private static final int NUM_FILTER = 3;
    private static final int MAX_INTER = 3;
    private static final int MIN_INTER = 1;
    private static final int NUM_PATTERNS = 4000;


    //private static final String INDEX_DIR = "/home/chippoc/index/"; 
    private static final String INDEX_DIR = "/argos/lra/index_textbooks/"; 
    private static final String DATA_DIR = "/bigdisk/corpora/textbooks/";
    //private static final String DATA_DIR = "/bigdisk/corpora/usenet/maui.tapor.ualberta.ca:9000/newscorpus/";
    private static final boolean DO_INDEX = false;

    public LRA() {
    } 

    /**
     * Returns the synonyms for the specified term.
     * The synonyms will be taken directly from the WordNet database.
     * This is used by LRA to find alternative pairs. Given an input set of A:B.
     * For each A' that is similar to A, make a new pair A':B.  Likewise for B.
     *
     * @param term a String containing a single word
     * @return  an array of all the synonyms 
     */
    public static Synset[] findAlternatives(String term) {
        WordNetDatabase database = WordNetDatabase.getFileInstance();   
        Synset[] all = database.getSynsets(term);
        return all;
    }

    /**
     * Initializes an index given the index directory and data directory.
     *
     * @param indexDir a String containing the directory where the index will be stored
     * @param dataDir a String containing the directory where the data is found
     * @return void
     */
    public static void initializeIndex(String indexDir, String dataDir) {
        File indexDir_f = new File(indexDir);
        File dataDir_f = new File(dataDir);

        long start = new Date().getTime();
        try {
            int numIndexed = index(indexDir_f, dataDir_f);
            long end = new Date().getTime();

            System.out.println("Indexing " + numIndexed + " files took " + (end -start) + " milliseconds");
        } catch (IOException e) {
            System.out.println("Unable to index "+indexDir_f+": "+e.getMessage());
        }
    }

    //creates the index files
    private static int index(File indexDir, File dataDir) 
        throws IOException {
        if (!dataDir.exists() || !dataDir.isDirectory()) {
            throw new IOException(dataDir
                    + " does not exist or is not a directory");
        }

        IndexWriter writer = new IndexWriter(indexDir,
                    new StandardAnalyzer(), true, IndexWriter.MaxFieldLength.UNLIMITED);
        writer.setUseCompoundFile(false);

        indexDirectory(writer, dataDir);

        int numIndexed = writer.numDocs();
        writer.optimize();
        writer.close();
        return numIndexed;
    }

    //recursive method that finds interleving patterns between A and B in all files
    //within a given directory
    private static HashSet<String> searchDirectoryForPattern(File dir,String A, String B) 
        throws Exception {
        
        File[] files = dir.listFiles();

        HashSet<String> pattern_set = new HashSet<String>();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                pattern_set.addAll(searchDirectoryForPattern(f, A, B));
            } else if (f.getName().endsWith(".txt")) {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()) {
                    if (A.equals(sc.next())) {
                        String pattern = "";
                        int count = 0;
                        while (count <= MAX_INTER && sc.hasNext()) { 
                            String curr = sc.next();
                            if (count >= MIN_INTER && B.equals(curr)) {
                                //add the String onto a Set of Strings containing the patterns
                                System.out.println("adding pattern: " + pattern);
                                pattern_set.add(pattern);
                                break;
                                /*
                                for (int j = 0; j < count; j++) {
                                    System.out.print(pattern[j] + " ");
                                }
                                    System.out.print("\n");
                                */
                            } else {
                                if (count > 0) {
                                    pattern += " ";
                                }
                                pattern += curr;
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return pattern_set;
    }

    //recursive method that calls itself when it finds a directory 
    private static void indexDirectory(IndexWriter writer, File dir)
        throws IOException {
        
        File[] files = dir.listFiles();

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                indexDirectory(writer, f);
            } else if (f.getName().endsWith(".txt")) {
                indexFile(writer, f);
            }
        }
    }
   
   //method to actually index a file using Lucene, adds a document
   //onto the index writer
   private static void indexFile(IndexWriter writer, File f)
        throws IOException {

    if (f.isHidden() || !f.exists() || !f.canRead()) {
        System.out.println("not writing "+f.getName());
        return;
    }

    System.out.println("Indexing " + f.getCanonicalPath());

    Document doc = new Document();

    doc.add(new Field("path", f.getCanonicalPath(), Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("modified",DateTools.timeToString(f.lastModified(), DateTools.Resolution.MINUTE),Field.Store.YES, Field.Index.NOT_ANALYZED));
    doc.add(new Field("contents", new FileReader(f)));
                
    writer.addDocument(doc);
   }

    /**
     * Searches an index given the index directory and counts up the frequncy of the two words used in a phrase.
     *
     * @param indexDir a String containing the directory where the index is stored
     * @param A a String containing the first word of the phrase
     * @param B a String containing the last word of the phrase
     * @return float 
     */
    public static float countPhraseFrequencies(String indexDir, String A, String B) {
        File indexDir_f = new File(indexDir);

        if (!indexDir_f.exists() || !indexDir_f.isDirectory()) {
            System.out.println("Search failed: index directory does not exist");
        } else {
            try {
                return searchPhrase(indexDir_f, A, B);
            } catch (Exception e) {
                System.out.println("Unable to search "+indexDir);
                return 0;
            }
        }
        return 0;
    }

    //method that actually does the searching
    private static float searchPhrase(File indexDir, String A, String B) 
        throws Exception {
        Directory fsDir = FSDirectory.getDirectory(indexDir);
        IndexSearcher searcher = new IndexSearcher(fsDir);

        long start = new Date().getTime();
        QueryParser parser = new QueryParser("contents",new StandardAnalyzer());
        //System.out.println("searching for: '\"" + A + " " + B + "\"~"+MAX_PHRASE+"'");
        parser.setPhraseSlop(MAX_PHRASE);
        String my_phrase = "\"" + A + " " + B + "\"";
        Query query = parser.parse(my_phrase);
        //System.out.println("total hits: " + results.totalHits);

        //set similarity to use only the frequencies
        //score is based on frequency of phrase only
        searcher.setSimilarity(
                new Similarity() {
                   public static final long serialVersionUID = 1L;
                   public float coord(int overlap, int maxOverlap) {
                      return 1;
                   } 
                   public float queryNorm(float sumOfSquaredWeights) {
                      return 1;
                   } 
                   public float tf(float freq) {
                      return freq;
                   } 
                   public float idf(int docFreq, int numDocs) {
                      return 1;
                   } 
                   public float lengthNorm(String fieldName, int numTokens) {
                      return 1;
                   } 
                   public float sloppyFreq(int distance) {
                       return 1;
                   }
        });
        TopDocs results = searcher.search(query,10);

        ScoreDoc[] hits = results.scoreDocs;
        float total_score = 0;
        //add up the scores
        for (ScoreDoc hit : hits) {
            Document doc = searcher.doc(hit.doc);
            //System.out.printf("%5.3f %sn\n",
             //   hit.score, doc.get("contents"));
            total_score += hit.score;
        }

        long end = new Date().getTime();
        searcher.close();

        return total_score;
    }


    /**
     * Returns an ArrayList of phrases with the greatest frequencies in the corpus.
     * For each alternate pair, send a phrase query to the Lucene search engine
     * containing the corpus.  The phrase query will find the frequencies of phrases
     * that begin with one member of the pair and end with the other.  The phrases
     * cannot have more than MAX_PHRASE words.
     * Select the top NUM_FILTER (current NUM_FILTER=3) most frequent phrases and 
     * return them along with the original pairs.
     *
     * @param A a String containing the first member in the original pair 
     * @param B a String containing the second member in the original pair 
     * @param A_prime a Synset array containing the alternates for A 
     * @param B_prime a Synset array containing the alternates for B 
     * @return  an ArrayList of Strings with the top NUM_FILTER pairs along with the original pairs 
     */
    public static ArrayList<String> filterPhrases (String A, String B, Synset[] A_prime, Synset[] B_prime) {
        HashMultiMap<Float,Pair<String>> phrase_frequencies  = new HashMultiMap<Float,Pair<String>>();
        //Search corpus... A:B
        //phrase_frequencies.put(new Float(countPhraseFrequencies(INDEX_DIR, A, B)),new Pair<String>(A,B)); 
        //System.out.println("Top 10 Similar words:");
        int count = 0;
        for (int i = 0; (i < NUM_SIM && i < A_prime.length); i++) {
            String[] wordForms = A_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(A) != 0) {
                    //Search corpus... A':B
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR, wordForms[j], B));
                    phrase_frequencies.put(score,new Pair<String>(wordForms[j],B)); 
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        count = 0;
        for (int i = 0; (i < NUM_SIM && i < B_prime.length); i++) {
            String[] wordForms = B_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(B) != 0) {
                    //Search corpus... A:B'
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR,A, wordForms[j]));
                    phrase_frequencies.put(score,new Pair<String>(A,wordForms[j])); 
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        
        // filter out the phrases and add the top 3 to the ArrayList, and return it
        Iterator iter = phrase_frequencies.keySet().iterator();
        //TODO: make number of filters dynamic
        //create Array with size = num filters
        ArrayList<String> filtered_phrases = new ArrayList<String>();
        Float filter1 = new Float(0.0);
        Float filter2 = new Float(0.0); 
        Float filter3 = new Float(0.0);
        while (iter.hasNext()) {
            Float curr_key = (Float)iter.next();
            //this will bump the filters up each time a greater value comes along
            //so that filter1 will be the greatest key and filter3 the 3rd greatest
            if (curr_key > filter1) {
                filter3 = filter2;
                filter2 = filter1; 
                filter1 = curr_key;
            } else if (curr_key > filter2) {
                filter3 = filter2;
                filter2 = curr_key;
            } else if (curr_key > filter3) {
                filter3 = curr_key;
            }
        }
        int filter_count = 0;
        Iterator val_iter = phrase_frequencies.get(filter1).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter2).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter3).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            String alternative_pair = val_iter.next().toString();
            String pair_arr[] = parsePair(alternative_pair);
            filtered_phrases.add(pair_arr[0]+":"+pair_arr[1]);
            filter_count++;
        }
        //throw in the original pair also
        filtered_phrases.add(A+":"+B);

        return filtered_phrases;
    }

    private static String combinatorialPatternMaker(String[] str, int str_size, int c) {
        String comb_pattern = "";
        int curr_comb = 1;
        for (int i = 0; i < str_size; i++) {
            if ((c & curr_comb) != 0) {
                comb_pattern += str[i] + "\\s";
            } else {
                comb_pattern += "[\\w]+\\s";
            }
            curr_comb = curr_comb << 1;
        }
        System.out.println(comb_pattern);
        return comb_pattern;
    }

    private static int countWildcardPhraseFrequencies(File dir, String pattern)
        throws Exception {
        
        File[] files = dir.listFiles();

        int total = 0;

        for (int i = 0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                total += countWildcardPhraseFrequencies(f, pattern);
            } else if (f.getName().endsWith(".txt")) {
                Scanner sc = new Scanner(f);
                while (sc.hasNext()) {
                    String line = sc.nextLine();
                    if (line.matches(pattern)) {
                        total++;
                    }
                }
            }
        }
        return total;
    }

    // parses a pair in the form {A, B}
    private static String[] parsePair (String pair) {
        String[] tmp = new String[2];
        int indexOfA = pair.indexOf('{')+1;
        int indexOfB = pair.indexOf(',');
        tmp[0] = pair.substring(indexOfA,indexOfB);
        tmp[1] = pair.substring(indexOfB+2,pair.length()-1);

        return tmp;
    }
    
    //      1. get intervening terms from filtered phrases
    //      2. use grep with different combinations from intervening terms
    //      3. let grep count the number of times a pattern appears
    public static BoundedSortedMap<InterveningWordsPattern, Integer> findPatterns (ArrayList<String> phrases) 
        throws Exception {
        BoundedSortedMap<InterveningWordsPattern, Integer> pattern_database = new BoundedSortedMap<InterveningWordsPattern, Integer>(NUM_PATTERNS);
        //TODO: make String[] -> String and use split
        HashSet<String> patterns = new HashSet<String>();
        for (String phrase : phrases) {
            String phrase_arr[] = phrase.split(":");
            String A = phrase_arr[0];
            String B = phrase_arr[1];
            System.out.println(A + ": " + B);

            patterns.addAll(searchDirectoryForPattern(new File(DATA_DIR), A, B));
        }
        Iterator iter = patterns.iterator();
        while (iter.hasNext()) {
            String curr_pattern_str = (String)iter.next();
            String[] curr_pattern = curr_pattern_str.split("\\s");
            int curr_length = curr_pattern.length;
            System.out.println("length of pattern: " + curr_length);
            //do a for loop with all combinatorials of wildcard patterns
            //for each iteration do a wildcard search
            for (int comb = 0; comb < (int)Math.pow(2.0,(double)curr_length); comb++) {
                String comb_pattern = "\\s" + combinatorialPatternMaker(curr_pattern, curr_length, comb);
                try {
                    int score = countWildcardPhraseFrequencies(new File(DATA_DIR), ".*" + comb_pattern + ".*");
                    InterveningWordsPattern db_pattern = new InterveningWordsPattern(comb_pattern);
                    db_pattern.setOccurrences(score);
                    pattern_database.put(db_pattern, score); //insert the pattern into database (only if it has a high enough score)

                    System.out.println(comb_pattern + ": " + score);
                } catch (Exception e) {
                    System.err.println("could not perform wildcard search");
                }
            }
        }
        return pattern_database;
    }

    /**
     * Maps a list of patterns to the columns of the sparse matrix.
     * Takes the results of findPattern() and maps it to the column indeces of a sparse matrix.
     *
     * @param patterns a BoundedSortedMap containing the top NUM_PATTERN patterns
     * @return  a HashMap of Integers mapped to Strings 
     */
    public static HashMap<Integer, InterveningWordsPattern> mapColumns(BoundedSortedMap<InterveningWordsPattern, Integer> patterns) {
            HashMap<Integer,InterveningWordsPattern> matrix_column_map = new HashMap<Integer, InterveningWordsPattern>();
            System.out.print("Patterns found: ");
            System.out.println(patterns.size());
        
            int index = 0;
            //NOTE: occurrences can be used as Sigma X<k,j> when calculating Entropy
            for (InterveningWordsPattern a_pattern : patterns.keySet()) {
                //int val = a_pattern.getOccurrences();
                //System.out.println(a_pattern.getPattern() + " " + val);
                matrix_column_map.put(new Integer(index), a_pattern);
                index++;
                InterveningWordsPattern b_pattern = new InterveningWordsPattern(a_pattern.getPattern());
                b_pattern.setOccurrences(a_pattern.getOccurrences());
                b_pattern.setReverse(true);
                matrix_column_map.put(new Integer(index), b_pattern);
                index++;
            }

            return matrix_column_map; 
    }
    
    /**
     * Maps a list of phrases to the rows of the sparse matrix.
     * Takes an ArrayList containing the filtered phrases (originals and alternates) and maps them to the sparse matrix.
     *
     * @param phrases an ArrayList containing the filtered phrases 
     * @return  a HashMap of Integers mapped to Strings 
     */
    public static HashMap<Integer, String> mapRows(ArrayList<String> phrases) {
            HashMap<Integer,String> matrix_row_map = new HashMap<Integer, String>();
        
            int index = 0;
            for (String a_phrase : phrases) {
                String[] curr = a_phrase.split(":");
                String A = curr[0];
                String B = curr[1];
                matrix_row_map.put(new Integer(index), A + ":" + B);
                index++;
                //add reverse pair as well
                matrix_row_map.put(new Integer(index), B + ":" + A);
                index++;
            }

            return matrix_row_map; 
    }

    public static Matrix createSparseMatrix(HashMap<Integer, String> row_data, HashMap<Integer, InterveningWordsPattern> col_data) {

        Matrix m = Matrices.create(row_data.size(), col_data.size(), false);
        for (int row_num = 0; row_num < row_data.size(); row_num++) { // for each pattern
            String p = row_data.get(new Integer(row_num));
            String[] p_sp = p.split(":");
            String a = p_sp[0];
            String b = p_sp[1];
            for (int col_num = 0; col_num < col_data.size(); col_num++) { // for each phrase
                InterveningWordsPattern col_pattern = col_data.get(new Integer(col_num));
                String pattern = col_pattern.getPattern();
                String comb_patterns;
                if (col_pattern.getReverse()) { //if the column is a reverse pattern...word2 P word1
                    comb_patterns = ".*\\s" + b + pattern + a + "\\s.*";
                } else {
                    comb_patterns = ".*\\s" + a + pattern + b + "\\s.*";
                }
                try {
                    m.set(row_num, col_num, (double)countWildcardPhraseFrequencies(new File(DATA_DIR), comb_patterns));
                } catch (Exception e) {
                    System.err.println("could not perform wildcard search");
                }
            }
        }
        System.out.println("\nCompleted matrix generation.");
        System.out.println("Number of rows: " + m.rows());
        System.out.println("Number of cols: " + m.columns());
        return m;
    }

    public static Matrix calculateEntropy(int m, int n, Matrix mat) {
        for (int col_num = 0; col_num < n; col_num++) {
            double col_total = 0.0;
            for (int row_num = 0; row_num < m; row_num++) {
                col_total += mat.get(row_num,col_num);
            }
            //System.out.println("coltotal: " + col_total);
            if (col_total == 0.0) 
                continue;

            double entropy = 0.0;
            for (int row_num = 0; row_num < m; row_num++) {
                double p = mat.get(row_num,col_num)/col_total;
                //System.out.print(p + " ");
                if (p==0.0)
                    continue;
                entropy += p * Math.log10(p);
            }
            //System.out.println("entropy: " + entropy);
            entropy *= -1;
            double w = 1 - entropy/Math.log10(m);
            //System.out.println("w: " + w);
            for (int row_num = 0; row_num < m; row_num++) {
                mat.set(row_num, col_num, w*Math.log10(mat.get(row_num, col_num) + 1.0));
            }
        }
        return mat;
    }

    private static int getIndexOfPair(String value, HashMap<Integer, String> row_data) {
        for(Integer i : row_data.keySet()) {
            if(row_data.get(i).equals(value)) {
                return i.intValue();
            }
        } 
        return -1;
    }
    
    //analogy is of the form A:B::C:D
    public static double computeCosineSimilarity(String analogy, HashMap<String,ArrayList<String>> originals, HashMap<Integer, String> row_data, Matrix m) {

        double cosineVals = 0.0;
        int totalVals = 0;
        if (!isAnalogyFormat(analogy, true)) {
            System.out.println("Analogy: \"" + analogy + "\" not in proper format");
            return 0.0;
        }
        String pairs[] = analogy.split("::");
        String pair1 = pairs[0];
        String pair2 = pairs[1];
        if (!isAnalogyFormat(pair1) || !isAnalogyFormat(pair2)) {
            System.out.println("Analogy: \"" + analogy + "\" not in proper format");
            return 0.0;
        }

        if(!originals.containsKey(pair1) || !originals.containsKey(pair2)) {
            System.out.println("Analogy: \"" + analogy + "\" not included in original pairs");
            return 0.0;
        }
        double original_cosineVal = cosineSimilarity(m.getRow(getIndexOfPair(pair1, row_data)), m.getRow(getIndexOfPair(pair2, row_data)));
        cosineVals += original_cosineVal;
        totalVals++;
        System.out.println("orig cos: " + cosineVals);
        ArrayList<String> alternates1 = originals.get(pair1);
        ArrayList<String> alternates2 = originals.get(pair2);
        for (String a : alternates1) {
            for (String b : alternates2) {
                int a_index = getIndexOfPair(a, row_data);
                int b_index = getIndexOfPair(b, row_data);
                if(a_index != -1 && b_index != -1) {
                    double alternative_cosineVal = cosineSimilarity(m.getRow(a_index),m.getRow(b_index));
                    System.out.println("adding cos: " + alternative_cosineVal);
                    if (alternative_cosineVal >= original_cosineVal) {
                        cosineVals += alternative_cosineVal;
                        totalVals++;
                    }
                }
            }
        }

        if (totalVals > 0) {
            return cosineVals/totalVals;
        } else {
            return 0.0;
        }
    }


    public static void printMatrix(int rows, int cols, Matrix m) {
        for(int col_num = 0; col_num < cols; col_num++) {
            for (int row_num = 0; row_num < rows; row_num++) {
                System.out.print(m.get(row_num,col_num) + " ");
            }
            System.out.print("\n");
        }
        System.out.print("\n");
    }

    public static boolean isAnalogyFormat(String analogy) {
        return isAnalogyFormat(analogy,false);
    }

    public static boolean isAnalogyFormat(String analogy, boolean pair) {
        if (pair) {
            return analogy.matches("[\\w]+:[\\w]+::[\\w]+:[\\w]+");
        } else {
            return analogy.matches("[\\w]+:[\\w]+");    
        }
    }

    public static void main(String[] args) {
        //set system property for Wordnet database directory
        Properties sysProps = System.getProperties();
        sysProps.setProperty("wordnet.database.dir","/usr/share/wordnet");

        System.out.println("starting LRA...\n");

        //Index corpus...
        if ( DO_INDEX ) {
            initializeIndex(INDEX_DIR, DATA_DIR);
        } else {
            System.out.println("skipping indexing step...");
            System.out.println("getting input set...");

            ArrayList<String> original_pairs = new ArrayList<String>();
            ArrayList<String> filtered_phrases = new ArrayList<String>(); //TODO: get rid of this variable -> use original_to_alternates
            HashMap<String,ArrayList<String>> original_to_alternates = new HashMap<String, ArrayList<String>>();

            Scanner sc = new Scanner(System.in);
            while (true) {
                System.out.print("Input A: ");
                String A = sc.next();
                System.out.print("Input B: ");
                String B = sc.next(); 

                if (A.equals("EXIT") || B.equals("EXIT"))
                    break;

                //1. Find alternates for A and B
                Synset[] A_prime = findAlternatives(A);
                Synset[] B_prime = findAlternatives(B);
                
                //2. Filter phrases
                ArrayList<String> tmp = new ArrayList<String>(filterPhrases(A,B,A_prime,B_prime));
                filtered_phrases.addAll(tmp);
                original_to_alternates.put(A+":"+B, tmp);
            }

            try {
                //3. Get patterns 4. Filter top NUM_PATTERNS
                BoundedSortedMap<InterveningWordsPattern, Integer> pattern_list = findPatterns(filtered_phrases);

                //5. Map phrases to rows 
                HashMap<Integer,String> matrix_row_map = mapRows(filtered_phrases);
                //6. Map patterns to columns 
                HashMap<Integer,InterveningWordsPattern> matrix_col_map = mapColumns(pattern_list);

                File rawTermDocMatrix = 
                    File.createTempFile("lra-term-document-matrix", ".dat");

                //7. Create sparse matrix 
                Matrix sparse_matrix = createSparseMatrix(matrix_row_map, matrix_col_map);

                //8. Calculate entropy

                sparse_matrix = calculateEntropy(sparse_matrix.rows(), sparse_matrix.columns(), sparse_matrix);

                //printMatrix(sparse_matrix.rows(), sparse_matrix.columns(), sparse_matrix);
                
                MatrixIO.writeMatrix(sparse_matrix, rawTermDocMatrix, MatrixIO.Format.SVDLIBC_SPARSE_TEXT); 

                //Matrix tmp_matrix = MatrixIO.readMatrix(rawTermDocMatrix, MatrixIO.Format.SVDLIBC_SPARSE_TEXT,Matrix.Type.SPARSE_IN_MEMORY); 
                //printMatrix(tmp_matrix.rows(), tmp_matrix.columns(), tmp_matrix);

                //9. Compute SVD on the pre-processed matrix.
                int dimensions = 300;
                Matrix[] usv = SVD.svd(rawTermDocMatrix, SVD.Algorithm.SVDLIBC, MatrixIO.Format.SVDLIBC_SPARSE_TEXT, dimensions);

                if (usv[1].rows() < usv[0].columns()) { //can't do projection, if the dimensions don't match up...redo SVD with updated dimensions
                    dimensions = usv[1].rows();
                    System.out.println("Default dimensions too big...redoing SVD with new dimensions, k" + "=" + dimensions + " ...");
                    usv = SVD.svd(rawTermDocMatrix, SVD.Algorithm.SVDLIBC, MatrixIO.Format.SVDLIBC_SPARSE_TEXT, dimensions);
                }

                Matrix projection = Matrices.multiply(usv[0],usv[1]);

                printMatrix(projection.rows(), projection.columns(), projection);

                System.out.println("Completed LRA...\n");

                //11. Get analogy input and Evaulte Alternatives
                while (true) {
                    System.out.print("Input Analogy (EXIT to quit): ");
                    String analogy = sc.next();

                    if (analogy.equals("EXIT"))
                        break;

                    double cosineVal = computeCosineSimilarity(analogy, original_to_alternates, matrix_row_map, projection);
                    System.out.println("cosine value: " + cosineVal);
                }

            } catch (Exception e) {
                System.out.println("FAILURE");
            } 
        }
    }
}

