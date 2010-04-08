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

import edu.ucla.sspace.clustering.Assignment;
import edu.ucla.sspace.clustering.Clustering;
import edu.ucla.sspace.clustering.GapStatistic;

import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.Misc;

import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;
import edu.ucla.sspace.vector.CompactSparseVector;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileReader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;


/**
 * An experimental tool for clustering a list of Twitter List tweets.  Each
 * tweet will be represented with a vector.  Once all tweets have been
 * processed, they are clustered using a specific {@link Clustering} algorithm
 * to find if there are any similarities.  Ideally, at least one of the
 * resulting clusters will correspond to the list's topic.  Currently, this tool
 * reports the number of clusters found and the 10 most frequently occuring
 * tokens in each cluster.
 *
 * @author Keith Stevens
 */
public class TweetClustering {

    /**
     * The clustering algorithm used for finding similarities between tweets.
     */
    private final Clustering clusteringAlgorithm;

    /**
     * The {@link Transform} instance that should be used for transforming
     * values in the tweet-term matrix.
     */
    private final Transform transform;

    /**
     * The list of tweet {@link Vector}s.
     */
    private final List<SparseDoubleVector> tweets;

    /**
     * A mapping from {@link String} to vector index.
     */
    private final Map<String, Integer> wordToIndexMap;

    /**
     * The number of frequent words to report from each cluster.
     */
    private final int numFrequentWords;

    /**
     * Creates a new {@link TweetClustering} instance that will use a specified
     * {@link Clustering} algorithm.
     */
    public TweetClustering(int numFrequentWords,
                           Clustering clusteringAlgorithm,
                           Transform transform) {
        this.numFrequentWords = numFrequentWords;
        this.clusteringAlgorithm = clusteringAlgorithm;
        this.transform = transform;
        wordToIndexMap = new ConcurrentHashMap<String, Integer>();
        tweets = new LinkedList<SparseDoubleVector>();
    }

    /**
     * Generates a vector representation for the tokens in the given {@code
     * tweet}.  The vector records how many times each unique token occurred in
     * the tweet.
     */
    public void processTweet(String tweet) {
        Iterator<String> tokens = IteratorFactory.tokenize(tweet);
        SparseDoubleVector tweetVector = new CompactSparseVector();
        while (tokens.hasNext()) {
            String token = tokens.next().toLowerCase();
            Integer wordIndex = wordToIndexMap.get(token);
            if (wordIndex == null) {
                wordIndex = wordToIndexMap.size();
                wordToIndexMap.put(token, wordIndex);
            }

            tweetVector.add(wordIndex, 1);
        }
        tweets.add(tweetVector);
    }

    /**
     * Clusters the list of tweet {@link Vector}s and outputs the 10 most
     * frequently occurring words in each cluster.
     */
    public void cluster() {
        // Get the total number of unique words.
        int numWords = wordToIndexMap.size();

        // Transform each unbounded vector into a smaller hash vector that has a
        // fixed length.
        List<SparseDoubleVector> shortTweets =
            new LinkedList<SparseDoubleVector>();
        for (SparseDoubleVector tweet : tweets) {
            SparseDoubleVector newTweet = new SparseHashDoubleVector(numWords);
            int[] nonZeros = tweet.getNonZeroIndices();
            for (int index : nonZeros)
                newTweet.set(index, tweet.get(index));
            shortTweets.add(newTweet);
        }

        // Transform the tweet-term matrix, if a transform is specified.
        Matrix transformedTweets = (transform != null)
            ? transform.transform(Matrices.asSparseMatrix(shortTweets))
            : Matrices.asSparseMatrix(shortTweets);

        // Cluster the list of tweet vectors.
        Assignment[] assignments = clusteringAlgorithm.cluster(
                transformedTweets, 15, System.getProperties());

        // Compute the centroid of each vector by summing the tweet vectors in
        // each cluster.
        List<SparseDoubleVector> centroids =
            new ArrayList<SparseDoubleVector>();
        int i = 0;
        for (SparseDoubleVector tweet : shortTweets) {
            Assignment clusterAssignment = assignments[i++];
            if (clusterAssignment.assignments().length == 0)
                continue;
            int assignment = clusterAssignment.assignments()[0];
            for (int l = centroids.size(); l <= assignment; ++l)
                centroids.add(new SparseHashDoubleVector(numWords));
            add(centroids.get(assignment), tweet);
        }

        // Comput the inverse mapping from vector indices to words.
        String[] indexToWord = new String[numWords];
        for (Map.Entry<String, Integer> entry : wordToIndexMap.entrySet())
            indexToWord[entry.getValue()] = entry.getKey();

        // For each centroid, generate the n most frequent terms in each
        // centroid.
        i = 0;
        for (SparseDoubleVector centroid : centroids) {
            MultiMap<Double,String> topWords =
                new BoundedSortedMultiMap<Double, String>(numFrequentWords);
            int[] nonZeros = centroid.getNonZeroIndices();
            for (int index : nonZeros)
                topWords.put(centroid.get(index), indexToWord[index]);

            // Print out the top N words.
            System.out.printf("Printing top words for centroid %d\n", i++);
            for (Map.Entry<Double, String> entry : topWords.entrySet())
                System.out.printf("Word: %s, weight: %f\n",
                                  entry.getValue(), entry.getKey());
        }
    }

    private void add(DoubleVector centroid, SparseDoubleVector tweet) {
        int[] nonZeros = tweet.getNonZeroIndices();
        for (int index : nonZeros)
            centroid.add(index, tweet.get(index));
    }

    public static void main(String[] args) throws IOException {
        // Set up the options for the main.
        ArgOptions options = new ArgOptions();
        options.addOption('F', "tokenFilter", "filters to apply to the input " +
                          "token stream", true, "FILTER_SPEC", 
                          "Tokenizing Options");
        options.addOption('C', "compoundWords", "a file where each line is a " +
                          "recognized compound word", true, "FILE", 
                          "Tokenizing Options");
        options.addOption('c', "clusteringAlgorithm",
                          "A clustering algorithm to use for the " +
                          "clustering of tweets.",
                          true, "CLASSNAME", "Clusteirng Options");
        options.addOption('n', "numFrequentWords",
                          "The number of frequent words in a cluster that " +
                          "should be used to describe it",
                          true, "INT", "Process Properties");
        options.addOption('t', "transform",
                          "The Transform instance to use for the tweet-doc " +
                          "matirx",
                          true, "CLASSNAME", "Process Properties");

        options.parseOptions(args);

        // Check that the required arguments are given.  If they are not, report
        // an error and print the usage.
        if (options.numPositionalArgs() != 1) {
            System.out.println(
                    "usage: java TweetClustering [options] <tweets>\n" +
                    options.prettyPrint());
            System.exit(1);
        }

        // Set up the the iterator factor.
        Properties props = System.getProperties();
        if (options.hasOption("tokenFilter"))
            props.setProperty(IteratorFactory.TOKEN_FILTER_PROPERTY,
                              options.getStringOption("tokenFilter"));            
        if (options.hasOption("compoundWords"))
            props.setProperty(IteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY,
                              options.getStringOption("compoundWords"));

        IteratorFactory.setProperties(props);

        // Set up the clustering algorithm.
        Clustering clusteringAlgorithm;
        if (options.hasOption("clusteringAlgorithm"))
            clusteringAlgorithm = (Clustering) Misc.getObjectInstance(
                    options.getStringOption("clusteringAlgorithm"));
        else
            clusteringAlgorithm = new GapStatistic();

        // Set up the number of words to report from each cluster.
        int numFrequentWords = (options.hasOption("numFrequentWords"))
            ? options.getIntOption("numFrequentWords")
            : 10;

        Transform transform = null;
        if (options.hasOption("transform"))
            transform = (Transform) Misc.getObjectInstance(
                    options.getStringOption("transform"));

        // Set up the tweet clusteirng instance.
        String tweetFile = options.getPositionalArg(0);
        TweetClustering tweetCluster = new TweetClustering(
                numFrequentWords, clusteringAlgorithm, transform);

        // Process each tweet.
        BufferedReader br = new BufferedReader(new FileReader(tweetFile));
        String line = null;
        while ((line = br.readLine()) != null)
            tweetCluster.processTweet(line);

        // Cluster the tweets.
        tweetCluster.cluster();
    }
}
