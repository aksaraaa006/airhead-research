/*
 * Copyright 2009 David Jurgens
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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.BoundedSortedMultiMap;
import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SortedMultiMap;

import edu.ucla.sspace.vector.DoubleVector;

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A utility class for finding the {@code k} most-similar rows to a provided
 * row in a {@link Matrix}.  The comparisons required for generating the
 * list maybe be run in parallel by configuring an instance of this class to use
 * multiple threads. Note that this comparator assumes that the similarity
 * method used is symmetric.<p>
 *
 * All instances of this class are thread-safe.
 * 
 * @author David Jurgens
 */
public class RowComparator {

    /**
     * The queue from which worker threads run word-word comparisons
     */
    private final BlockingQueue<Runnable> workQueue;
    
    /**
     * Creates this {@code RowComparator} with as many threads as processors.
     */
    public RowComparator() {
        this(Runtime.getRuntime().availableProcessors());
    }
    
    /**
     * Creates this {@code RowComparator} with the specified number of threads.
     */
    public RowComparator(int numThreads) {
        workQueue = new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < numThreads; ++i) {
            new WorkerThread(workQueue).start();            
        }
    }

    /**
     * Compares the provided row to all other rows in the provided {@link
     * Matrix} and populates the list of most similar rows to this row, and to
     * other rows that are compared.  This method only compares the current row
     * to rows that come after the current row.
     */
    public void getMostSimilar(
            final int row,
            final Matrix matrix,
            final ArrayList<SortedMultiMap<Double, Integer>> mostSimilarPerRow,
            Similarity.SimType similarityType) {
        int startRow = row + 1;

        // The semaphore used to block until all the rows have been compared.
        // Set the number of computations to be the number of rows that come
        // after the current row.  The negative ensures that the release() must
        // happen before the main thread's acquire() will return.
        int numRowsToCompare = 0 - (matrix.rows() - startRow);
        final Semaphore comparisons = new Semaphore(numRowsToCompare);

        // loop through all rows after the current row to compute their
        // similarity.
        for (int i = row+1; i < matrix.rows(); ++i) {
             workQueue.offer(new Comparison(
                         comparisons, matrix, row, 
                         i, similarityType, mostSimilarPerRow));
        }
        
        try {
            comparisons.acquire();
        } catch (InterruptedException ie) {
            // check whether we were interrupted while still waiting for the
            // comparisons to finish
            if (comparisons.availablePermits() < 1) {
                throw new IllegalStateException(
                    "interrupted while waiting for word comparisons to finish", 
                    ie);
            }
        }
    }

    /**
     * A comparison task that compares the vector for the other word and updates
     * the mapping from similarity to word.
     */
    private static class Comparison implements Runnable {
        
        private final Semaphore semaphore;

        Matrix matrix;
        int row;
        int other;
        Similarity.SimType similarityMeasure;
        ArrayList<SortedMultiMap<Double,Integer>> mostSimilarPerRow;

        public Comparison(
                Semaphore semaphore,
                Matrix matrix,
                int row,
                int other,
                Similarity.SimType similarityMeasure,
                ArrayList<SortedMultiMap<Double, Integer>> mostSimilarPerRow) {
            this.semaphore = semaphore;
            this.matrix = matrix;
            this.row = row;
            this.other = other;
            this.similarityMeasure = similarityMeasure;
            this.mostSimilarPerRow = mostSimilarPerRow;
        }

        public void run() {
            try {            
                DoubleVector otherV = matrix.getRowVector(other);
                DoubleVector rowV = matrix.getRowVector(row);
                Double similarity = Similarity.getSimilarity(
                    similarityMeasure, rowV, otherV);

                addToMostSimilar(row, other, similarity.doubleValue());
                addToMostSimilar(other, row, similarity.doubleValue());
            } catch (Exception e) {
                // Rethrow any reflection-related exception, as this situation
                // should not normally occur since the Method being invoked
                // comes directly from the Similarity class.
                throw new Error(e);
            } finally {
                // notify that the word has been processed regardless of whether
                // an error occurred
                semaphore.release();
            }
        }

        private void addToMostSimilar(int row, int other, double similarity) {
            // Get the multi map for the row we that is being comapred against.
            SortedMultiMap<Double, Integer> mostSimilar =
                mostSimilarPerRow.get(row);
            // lock on the Map, as it is not thread-safe
            synchronized(mostSimilar) {
                mostSimilar.put(similarity, other);
            }
        }
    }

    /**
     * A daemon thread that continuously dequeues {@code Runnable} instances
     * from a queue and executes them.
     */
    protected static final class WorkerThread extends Thread {

        static int threadInstanceCount;

        private final BlockingQueue<Runnable> workQueue;

        public WorkerThread(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            setDaemon(true);
            setName("RowComparator-WorkerThread-" + (threadInstanceCount++));
        }

        public void run() {
            while (true) {
                try {
                    Runnable r = workQueue.take();
                    r.run();
                } catch (InterruptedException ie) {
                    throw new Error(ie);
                }
            }
        }
    }
}
