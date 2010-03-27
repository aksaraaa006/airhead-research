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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SynchronizedIterator;
import edu.ucla.sspace.util.WorkerThread;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;

import java.util.Collection;
import java.util.Collections;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.Semaphore;

import java.util.concurrent.atomic.AtomicInteger;


/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordSimilarityEvaluation} test.
 *
 * @author Keith Stevens
 */ 
public abstract class AbstractWordPrimingTest implements WordPrimingTest {

    /**
     * A mapping from a word pair to the human association judgement for it
     */
    protected final Set<Pair<String>> primeTargetPairs;

    /** 
     *
     * @param wordPairToHumanJudgement A mapping from a word pair to the human
     *        association judgement for it
     */
    public AbstractWordPrimingTest(
            Set<Pair<String>> primeTargetPairs) {
        this.primeTargetPairs = primeTargetPairs;
    }

    /**
     * Evaluates the performance of a given {@code SemanticSpace} on a given
     * {@code WordSimilarityEvaluation} using the provided similarity metric.
     * Returns a {@link WordSimilarityReport} detailing the performance, with
     * similarity scores scaled by the lowest and highest human based similarity
     * ratings.
     *
     * @param sspace The {@link SemanticSpace} to test against
     *
     * @return A {@link WordSimilarityReport} detailing the performance
     */
    public WordPrimingReport evaluate(final SemanticSpace sspace) {
        Pair<double[]> scores = evaluateRelation(sspace, primeTargetPairs);

        int numItems = scores.x.length;
        double relatedSum = 0;
        double unrelatedSum = 0;
        for (int i = 0; i < numItems; ++i) {
            relatedSum += scores.x[i];
            unrelatedSum += scores.y[i];
        }
        relatedSum /= numItems;
        unrelatedSum /= numItems;
        return new SimpleWordPrimingReport(numItems, relatedSum, unrelatedSum);
    }

    private Pair<double[]> evaluateRelation(
            SemanticSpace sspace, Set<Pair<String>> pairs) {
        final Set<String> sspaceWords = sspace.getWords();

        // Set up the set of prime words in the list.  This will be used later
        // on to compute the average distance between a target and all primes.
        // Only include primes that are in the semantic space.
        final Set<String> primes = new HashSet<String>();
        for (Pair<String> primeTargetPair : pairs) {
            if (sspaceWords.contains(primeTargetPair.x) &&
                sspaceWords.contains(primeTargetPair.y))
                primes.add(primeTargetPair.x);
        }
        int numValidPairs = primes.size();

        // Prepare the arrays to hold the related and unrelated priming scores.
        double[] relatedScores = new double[numValidPairs];
        double[] unrelatedScores = new double[numValidPairs];
        
        // Iterate over each of the pairs.  If a pair is unanswerable, due to
        // either word not being in the space it will be skipped.  For valid
        // pairs, we compute the distance between the two words in the space as
        // the related comparison and use the average distance between the
        // target and all valid primes as the unrelated case.
        int scoreIndex = 0;
        for (final Pair<String> pair: pairs) {
            // Skip pairs where either word is not in the list.
            if (!sspaceWords.contains(pair.x) ||
                !sspaceWords.contains(pair.y))
                continue;

            // Offer a new question to the work queue.
            // Compute the priming relation between the prime and target
            // pair.
            double related = computePriming(sspace, pair.x, pair.y);

            // Compute the average priming relation between all primes
            // and the given target pair.  Start with the initial prime
            // and target pair and then inspect all other valid primes.
            double unrelated = related;
            for (String prime : primes) {
                if (prime.equals(pair.x))
                    continue;
                unrelated += computePriming(sspace, prime, pair.y);
            }
            unrelated /= primes.size();

            // Record the results.
            relatedScores[scoreIndex] = related;
            unrelatedScores[scoreIndex] = unrelated;
            scoreIndex++;
        }
        return new Pair<double[]>(relatedScores, unrelatedScores);
    }

    /**
     * Returns the correlation between the computer generated scores and the
     * human evaluated scores.  Sub-classes can override this if the correlation
     * metric is not suitable for the data set.  Possible alternatives are mean
     * square error or simply the average computer generated score.
     */
    protected double computeScore(double[] humanScores, double[] compScores) {
        return Similarity.correlation(humanScores, compScores);
    }

    /**
     * Returns the association of the two words on a scale of 0 to 1.
     * Subclasses should override this method to provide specific ways of
     * determining the association of two words in the semantic space, but
     * should ensure that the return value falls with the predefined scale.
     *
     * @return the assocation or {@code null} if either {@code word1} or {@code
     *         word2} are not in the semantic space
     */
    protected abstract Double computePriming(SemanticSpace sspace, 
                                             String word1, String word2);

    public class SimpleWordPrimingReport implements WordPrimingReport {

        private int numDataPoints;

        private double relatedScore;

        private double unrelatedScore;

        public SimpleWordPrimingReport(int numDataPoints,
                                       double relatedScore,
                                       double unrelatedScore) {
            this.numDataPoints = numDataPoints;
            this.relatedScore = relatedScore;
            this.unrelatedScore = unrelatedScore;
        }

        /**
         * {@inheritDoc}
         */
        public int numberOfWordPairs() {
            return numDataPoints;
        }

        /**
         * {@inheritDoc}
         */
        public double relatedPriming() {
            return relatedScore;
        }

        /**
         * {@inheritDoc}
         */
        public double unrelatedPriming() {
            return unrelatedScore;
        }

        public double effect() {
            return relatedScore - unrelatedScore;
        }

        public String toString() {
            return String.format("Primed Pairs: %f\nUnrelated Pairs: %f\n",
                                 relatedScore, unrelatedScore);
        }
    }
}
