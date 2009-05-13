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

import java.lang.reflect.Method;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordSimilarityEvaluation} test.
 */ 
public class WordSimilarityEvaluationRunner {

    /**
     *
     */
    public static Report evaluate(SemanticSpace sspace,
				  WordSimilarityEvaluation test,
				  Similarity.SimType vectorComparisonType) {

	return evaluate(sspace, test, 
			Similarity.getMethod(vectorComparisonType));
    }

    /**
     *
     */
    public static Report evaluate(SemanticSpace sspace,
				  WordSimilarityEvaluation test,
				  Method vectorComparisonMethod) {

	Collection<WordSimilarity> wordPairs = test.getPairs();
	int unanswerable = 0;

	// Use lists here to keep track of the judgements for each word pair
	// that the SemanticSpace has vectors for.  This allows us to skip
	// trying to correlate human judgements for pairs that the S-Space
	// cannot handle.
	List<Double> humanJudgements = new ArrayList<Double>(wordPairs.size());
	List<Double> sspaceJudgements = new ArrayList<Double>(wordPairs.size());
	
	double testRange = test.getMostSimilarValue() - 
	    test.getLeastSimilarValue();
	

	question_loop:
	for (WordSimilarity pair : wordPairs) {

	    // get the vector for each word
	    double[] firstVector = sspace.getVectorFor(pair.getFirstWord());
	    double[] secondVector = sspace.getVectorFor(pair.getSecondWord());

	    // check that the s-space had both words
	    if (firstVector == null || secondVector == null) {
		unanswerable++;
		continue;
	    }

	    // use the similarity result and scale it based on the original
	    // answers
	    double similarity = invoke(vectorComparisonMethod, 
				       firstVector, secondVector);
	    double scaled = (similarity * testRange) 
		+ test.getLeastSimilarValue();

	    humanJudgements.add(pair.getSimilarity());
	    sspaceJudgements.add(scaled);
	}
	
	// create arrays to to calculate the correlation
	double[] humanArr = new double[humanJudgements.size()];
	double[] sspaceArr = new double[humanJudgements.size()];

	for(int i = 0; i < humanArr.length; ++i) {
	    humanArr[i] = humanJudgements.get(i);
	    sspaceArr[i] = sspaceJudgements.get(i);
	}

	double correlation = Similarity.correlation(humanArr, sspaceArr);

	return new SimpleReport(wordPairs.size(), correlation, unanswerable);
    }

    /**
     * Invokes the provided method, passing in the two {@code double} arrays as
     * arguments.  This method is provided as an unchecked wrapper around the
     * {@link Method#invoke(Object,Object[])) Method.invoke} call.
     */
    private static double invoke(Method m, double[] d1, double[] d2) {
	try {
	    Double d = (Double)(m.invoke(null, new Object[] {d1, d2}));
	    return d.doubleValue();
	} catch (Exception e) {
	    // generic catch and rethrow
	    throw new Error(e);
	}
    }

    /**
     * A report of the performance of a {@link SemanticSpace} on a particular
     * {@link WordSimilarityEvaluation} test.
     */
    public interface Report {

	/**
	 * Returns the total number of word pairs.
	 */
	int numberOfWordPairs();

	/**
	 * Returns the correlation between the {@link SemanticSpace} similarity
	 * judgements and the provided human similarity judgements.
	 */
	double correlation();

	/**
	 * Returns the number of questions for which the {@link SemanticSpace}
	 * could not give an answer due to missing word vectors.
	 */
	int unanswerableQuestions();

    }

    private static class SimpleReport implements Report {
	
	private final int numWordPairs;

	private final double correlation;

	private final int unanswerable;

	public SimpleReport(int numWordPairs, double correlation, 
			    int unanswerable) {
	    this.numWordPairs = numWordPairs;
	    this.correlation = correlation;
	    this.unanswerable = unanswerable;
	}

	/**
	 * {@inheritDoc}
	 */
	public int numberOfWordPairs() {
	    return numWordPairs;
	}

	/**
	 * {@inheritDoc}
	 */
	public double correlation() {
	    return correlation;
	}

	/**
	 * {@inheritDoc}
	 */
	public int unanswerableQuestions() {
	    return unanswerable;
	}

	public String toString() {
	    return String.format("%.4f correlation; %d/%d unanswered",
				 correlation, unanswerable, numWordPairs);
	}
    }
}