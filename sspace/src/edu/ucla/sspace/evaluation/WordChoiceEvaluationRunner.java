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

import java.util.Collection;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;

/**
 * A test-runner for evaluating the performance of a {@link SemanticSpace} on a
 * {@link WordChoiceEvaluation} test.
 */ 
public class WordChoiceEvaluationRunner {

    public Report evaluate(SemanticSpace sspace, WordChoiceEvaluation test,
			   Method vectorComparisonMethod) {

	Collection<MultipleChoiceQuestion> questions = test.getQuestions();
	int correct = 0;
	int unanswerable = 0;
	
	question_loop:
	for (MultipleChoiceQuestion question : questions) {

	    // get the vector for the prompt
	    double[] promptVector = sspace.getVectorFor(question.getPrompt());
	    
	    // check that the s-space had the prompt word
	    if (promptVector == null) {
		unanswerable++;
		continue;
	    }

	    // find the options whose vector has the highest similarity (or
	    // equivalent comparison measure) to the prompt word.  The running
	    // assumption hear is that for the value returned by the comparison
	    // method, a high value implies more similar vectors.
	    int answerIndex = 0;
	    double closestOption = Double.MIN_VALUE;
	    int optionIndex = 0;
	    for (String option : question.getOptions()) {

		double[] optionVector = sspace.getVectorFor(option);

		// check that the s-space had the option word
		if (optionVector == null) {
		    unanswerable++;
		    continue question_loop;
		}
		
		double similarity = invoke(vectorComparisonMethod, 
					   promptVector, optionVector);

		if (similarity > closestOption) {
		    answerIndex = optionIndex;
		    closestOption = similarity;
		}

		optionIndex++;
	    }
	    
	    // see whether our guess matched with the correct index
	    if (answerIndex == question.getCorrectAnswer()) {
		correct++;
	    }
	}

	return new SimpleReport(questions.size(), correct, unanswerable);
    }

    /**
     * Invokes the provided method, passing in the two {@code double} arrays as
     * arguments.  This method is provided as an unchecked wrapper around the
     * {@link Method#invoke(Object,Object[])) Method.invoke} call.
     */
    private double invoke(Method m, double[] d1, double[] d2) {
	try {
	    Double d = (Double)(m.invoke(null, new Object[] {d1, d2}));
	    return d.doubleValue();
	} catch (Exception e) {
	    // generic catch and rethrow
	    throw new Error(e);
	}
    }

    public Report evaluate(SemanticSpace sspace, WordChoiceEvaluation test,
			   Similarity.SimType vectorComparisonType) {
	String methodName = null;
	switch (vectorComparisonType) {
	case COSINE:
	    methodName = "cosineSimilarity";
	    break;
	case CORRELATION:
	    methodName = "correlation";
	    break;
	case EUCLIDEAN:
	    methodName = "euclideanSimilarity";
	    break;
	}
	
	Method m = null;

	try { 
	    m = Similarity.class.getMethod(methodName,
		new Class[] {double[].class, double[].class});
	} catch (NoSuchMethodException nsme) {
	    // rethrow
	    throw new Error(nsme);
	}

	return evaluate(sspace, test, m);
    }

    /**
     * A report of the performance of a {@link SemanticSpace} on a particular
     * {@link MultipleChoiceEvaluation} test.
     */
    public interface Report {

	/**
	 * Returns the total number of questions on the test.
	 */
	int numberOfQuestions();

	/**
	 * Returns the number of questions that were answered correctly.
	 */
	int correctAnswers();

	/**
	 * Returns the number of questions for which the {@link SemanticSpace}
	 * could not give an answer due to missing word vectors in either the
	 * prompt or the options.
	 */
	int unanswerableQuestions();

    }

    private static class SimpleReport implements Report {
	
	private final int numQuestions;

	private final int correct;

	private final int unanswerable;

	public SimpleReport(int numQuestions, int correct, int unanswerable) {
	    this.numQuestions = numQuestions;
	    this.correct = correct;
	    this.unanswerable = unanswerable;
	}

	/**
	 * {@inheritDoc}
	 */
	public int numberOfQuestions() {
	    return numQuestions;
	}

	/**
	 * {@inheritDoc}
	 */
	public int correctAnswers() {
	    return correct;
	}

	/**
	 * {@inheritDoc}
	 */
	public int unanswerableQuestions() {
	    return unanswerable;
	}

	public String toString() {
	    return String.format("%.2f correct; %d/%d unanswered",
				 ((((double)correct) / numQuestions) * 100),
				 unanswerable, numQuestions);
	}
    }

}