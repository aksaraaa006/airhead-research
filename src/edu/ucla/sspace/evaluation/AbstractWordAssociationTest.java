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

package edu.ucla.sspace.evaluation;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;

import edu.ucla.sspace.util.Pair;
import edu.ucla.sspace.util.SynchronizedIterator;

import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.VectorIO;

import java.util.Collection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordSimilarityEvaluation} test.
 *
 * @authro David Jurgens
 */ 
public abstract class AbstractWordAssociationTest 
        implements WordAssociationTest {

    /**
     * A mapping from a word pair to the human association judgement for it
     */
    protected final Map<Pair<String>,Double> wordPairToHumanJudgement;

    /** 
     *
     * @param wordPairToHumanJudgement A mapping from a word pair to the human
     *        association judgement for it
     */
    public AbstractWordAssociationTest(
            Map<Pair<String>,Double> wordPairToHumanJudgement) {
        this.wordPairToHumanJudgement = wordPairToHumanJudgement;
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
    public WordAssociationReport evaluate(final SemanticSpace sspace) {

        Collection<Thread> threads = new LinkedList<Thread>();
        int numThreads = Runtime.getRuntime().availableProcessors();

        final double testRange = getHighestScore() - getLowestScore();
        final int[] unanswered = new int[numThreads];
        final int[] answered = new int[numThreads];
        final double[] totalScore = new double[numThreads];

        final Iterator<Map.Entry<Pair<String>, Double>> questionIter = 
            new SynchronizedIterator<Map.Entry<Pair<String>, Double>>(
                 wordPairToHumanJudgement.entrySet().iterator());

        for (int i = 0; i < numThreads; ++i) {
            final int index = i;
            Thread t = new Thread() {
                public void run() {
                    while (questionIter.hasNext()) {
                        Map.Entry<Pair<String>,Double> e  =
                            questionIter.next();

                        Pair<String> p = e.getKey();
                        Double association =
                            computeAssociation(sspace, p.x, p.y);
                        // Skip questions that cannot be answered with the
                        // provided semantic space
                        if (association == null) {
                            unanswered[index]++;
                            continue;
                        }
                        answered[index]++;

                        // Scale the associated result to within the test's
                        // range of values
                        double score =
                            (association * testRange) + getLowestScore();
                        totalScore[index] += score;
                    }
                }
            };
            threads.add(t);
        }

        for (Thread t : threads)
            t.start();

        try { 
            for (Thread t : threads)
                t.join();
        } catch (InterruptedException ie) {
            throw new Error(ie);
        }

        double meanScore = 0;
        int totalAnswered = 0;
        int totalUnanswered = 0;
        for (int i = 0; i < numThreads; i++) {
            meanScore += totalScore[i];
            totalAnswered += answered[i]; 
            totalUnanswered += unanswered[i];
        }

        meanScore /= totalAnswered;

        return new SimpleWordAssociationReport(
            wordPairToHumanJudgement.size(), meanScore, totalUnanswered);
    }

    /**
     * Returns the lowest score possible for human judgments.  This score is
     * interpreted as the least associated.
     */
    protected abstract double getLowestScore();

    /**
     * Returns the highest score possible for human judgments.  This score is
     * interpreted as the most associated.
     */
    protected abstract double getHighestScore();

    /**
     * Returns the association of the two words on a scale of 0 to 1.
     * Subclasses should override this method to provide specific ways of
     * determining the association of two words in the semantic space, but
     * should ensure that the return value falls with the predefined scale.
     *
     * @return the assocation or {@code null} if either {@code word1} or {@code
     *         word2} are not in the semantic space
     */
    protected abstract Double computeAssociation(SemanticSpace sspace, 
                                                 String word1, String word2);
}
