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

package edu.ucla.sspace.evaluation;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;


/**
 * A test of synonym questions gathered from the Scholastic Aptitude Test (SAT).
 */
public class SATWordChoiceEvaluation implements WordChoiceEvaluation {

    /**
     * The questions for the ESL Test
     */
    private final Collection<MultipleChoiceQuestion> questions;

    /**
     * The name of the data file for this test
     */
    private final String dataFileName;
    
    /**
     * Constructs this evaluation test using the ESL test question file refered
     * to by the provided name.
     */
    public SATWordChoiceEvaluation(String satQuestionsFileName) {
        this(new File(satQuestionsFileName));
    }
    
    /**
     * Constructs this evaluation test using the ESL test question file.
     */
    public SATWordChoiceEvaluation(File satQuestionsFile) {
        questions = parseTestFile(satQuestionsFile);
        dataFileName = satQuestionsFile.getName();
    }

    /**
     * Parses the ESL test file and returns the set of questions contained
     * therein.
     */
    private static Collection<MultipleChoiceQuestion> parseTestFile(File f) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(f));
            Collection<MultipleChoiceQuestion> questions = 
                new LinkedList<MultipleChoiceQuestion>();
            for (String line = null; (line = br.readLine()) != null; ) {

                // skip comments and blank lines
                if (line.startsWith("#") || line.length() == 0) {
                    continue;
                }

                // Ignore the current line since it only explains where the
                // question came from.

                // Read the question.
                line = br.readLine();
                String prompt = line.replaceAll("\\s+", " ");
                System.out.println(prompt);

                // Read the options.
                List<String> options = new LinkedList<String>();
                for (int i = 0; i < 5 && ((line = br.readLine()) != null); ++i)
                    options.add(line.replaceAll("\\s+", " "));

                // Read the letter providing the answer.
                line = br.readLine();
                int answer = line.charAt(0) - 'a';

                questions.add(
                    new SimpleMultipleChoiceQuestion(prompt, options, answer));
            }
            return questions;
        } catch (IOException ioe) {
            // rethrow, as any IOException is fatal to evaluation
            throw new IOError(ioe);
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public Collection<MultipleChoiceQuestion> getQuestions() {
        return questions;
    }

    public String toString() {
        return "SAT-word-choice";
    }
}
