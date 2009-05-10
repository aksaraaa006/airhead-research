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

import edu.ucla.sspace.common.Pair;
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

import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.Date;
//import java.util.TreeMap;
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
    private static final String INDEX_DIR = "/home/chippoc/index/"; 
    private static final String DATA_DIR = "/bigdisk/corpora/textbooks/";//"/bigdisk/corpora/usenet/maui.tapor.ualberta.ca:9000/newscorpus/";
    private static final boolean DO_INDEX = false;

    public LRA() {
        //constructor...will fill in later
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
     * Searches an index given the index directory and the query to search for.  (will make it count phrases later)
     *
     * @param indexDir a String containing the directory where the index will be stored
     * @param query a String containing the word or phrase to look for 
     * @return void
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
        System.out.println("counting phrases " + A + ":" + B);
        QueryParser parser = new QueryParser("contents",new StandardAnalyzer());
        System.out.println("searching for: '\"" + A + " " + B + "\"~"+MAX_PHRASE+"'");
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
        System.out.print("A: ");
        int count = 0;
        for (int i = 0; (i < NUM_SIM && i < A_prime.length); i++) {
            String[] wordForms = A_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(A) != 0) {
                    //Search corpus... A':B
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR, wordForms[j], B));
                    System.out.println("score: " + score); 
                    phrase_frequencies.put(score,new Pair<String>(wordForms[j],B)); 
                    //System.out.print((j > 0 ? ", " : "") +
                    //   wordForms[j]);
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        System.out.print("\n");
        System.out.print("B: ");
        count = 0;
        for (int i = 0; (i < NUM_SIM && i < B_prime.length); i++) {
            String[] wordForms = B_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                if (wordForms[j].compareTo(B) != 0) {
                    //Search corpus... A:B'
                    Float score = new Float(countPhraseFrequencies(INDEX_DIR,A, wordForms[j]));
                    System.out.println("score: " + score); 
                    phrase_frequencies.put(score,new Pair<String>(A,wordForms[j])); 
                    //System.out.print((j > 0 ? ", " : "") +
                    //   wordForms[j]);
                    count++;
                }

                if(count >= NUM_SIM)
                    break;
            }
            if(count >= NUM_SIM)
                break;
        }
        System.out.print("\n");
        
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
            Iterator val_iter = phrase_frequencies.get(curr_key).iterator();
            while (val_iter.hasNext()) {
                System.out.println("key: " + curr_key + ", val: " + val_iter.next());
            }

        }
        System.out.println("filter1: " + filter1 + "\n" + "filter2: " + filter2 + "\n" + "filter3: " + filter3);
        int filter_count = 0;
        Iterator val_iter = phrase_frequencies.get(filter1).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            filtered_phrases.add(val_iter.next().toString());
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter2).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            filtered_phrases.add(val_iter.next().toString());
            filter_count++;
        }
        val_iter = phrase_frequencies.get(filter3).iterator();
        while (val_iter.hasNext() && filter_count < 3) {
            filtered_phrases.add(val_iter.next().toString());
            filter_count++;
        }
        //throw in the original pairs also
        filtered_phrases.add("{"+A+", "+B+"}");
        filtered_phrases.add("{"+B+", "+A+"}");

        System.out.println("filtered phrases: " + filtered_phrases);

        return filtered_phrases;
        
    }

    public static void main(String[] args) {
        //set system property for Wordnet database directory
        Properties sysProps = System.getProperties();
        sysProps.setProperty("wordnet.database.dir","/usr/share/wordnet");

        System.out.println("starting LRA...\n");
        //get input...A B, where we are finding analogies for A:B
        Scanner sc = new Scanner(System.in);
        System.out.print("Input A: ");
        String A = sc.next();
        System.out.print("Input B: ");
        String B = sc.next(); 

        //Index corpus...
        if ( DO_INDEX ) {
            initializeIndex(INDEX_DIR, DATA_DIR);
        } else {
            System.out.println("skipping indexing step...");
        }

        //1. Find alternates for A and B
        Synset[] A_prime = findAlternatives(A);
        Synset[] B_prime = findAlternatives(B);
        
        ArrayList<String> filtered_phrases = filterPhrases(A, B, A_prime, B_prime);

    }
}

