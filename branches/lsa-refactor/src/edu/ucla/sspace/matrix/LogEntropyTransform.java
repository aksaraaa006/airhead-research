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

import edu.ucla.sspace.vector.Sparse;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;

import java.util.HashMap;
import java.util.Map;

import java.util.logging.Logger;

import static edu.ucla.sspace.common.Statistics.log2;
import static edu.ucla.sspace.common.Statistics.log2_1p;


/**
 * Transforms a matrix using log-entropy weighting.  The input matrix is assumed
 * to be formatted as rows representing terms and columns representing
 * documents.  Each matrix cell indicates the number of times the row's word
 * occurs within the column's document.  See the following papers for details
 * and analysis:
 *
 * <ul> 
 *
 * <li style="font-family:Garamond, Georgia, serif"> Landauer, T. K., Foltz,
 *      P. W., & Laham, D. (1998).  Introduction to Latent Semantic
 *      Analysis. <i>Discourse Processes</i>, <b>25</b>, 259-284.</li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> S. Dumais, “Enhancing
 *      performance in latent semantic indexing (LSI) retrieval,” Bellcore,
 *      Morristown (now Telcordia Technologies), Tech. Rep. TM-ARH-017527,
 *      1990. </li>
 *
 * <li style="font-family:Garamond, Georgia, serif"> P. Nakov, A. Popova, and
 *      P. Mateev, “Weight functions impact on LSA performance,” in
 *      <i>Proceedings of the EuroConference Recent Advances in Natural Language
 *      Processing, (RANLP’01)</i>, 2001, pp. 187–193. </li>
 *
 * </ul>
 *
 * @author David Jurgens
 *
 * @see TfIdfTransform
 */
public class LogEntropyTransform implements Transform {

    /*
     * Implementation Reminder: This class could be improved through converting
     * it to use the common.Matrix implementations.
     */

    private static final Logger LOGGER = 
	Logger.getLogger(LogEntropyTransform.class.getName());

    /**
     * Creates an instance of {@code LogEntropyTransform}.
     */
    public LogEntropyTransform() { }

    /**
     * Transforms the matrix in the file using the log-entropy transform and
     * returns a temporary file containing the result.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the matrix
     *
     * @return a file with the transformed version of the input.  This file is
     *         marked to be deleted when the JVM exits.
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public File transform(File inputMatrixFile, MatrixIO.Format format) 
             throws IOException {
	// create a temp file for the output
	File output = File.createTempFile(inputMatrixFile.getName() + 
					  ".log-entropy-transform", ".dat");
	transform(inputMatrixFile, format, output);
	return output;
    }

    /**
     * Transforms the input matrix using the log-entropy transform and
     * writes the result to the file for the output matrix.
     *
     * @param inputMatrixFile a file containing a matrix in the specified format
     * @param format the format of the input matrix, and the format in which the
     *        output matrix will be written
     * @param outputMatrixFile the file to which the transformed matrix will be
     *        written
     *
     * @throws IOException if any error occurs while reading the input matrix or
     *         writing the output matrix
     */
    public void transform(File inputMatrixFile, MatrixIO.Format format, 
                          File outputMatrixFile) throws IOException {
        switch (format) {
        case SVDLIBC_SPARSE_BINARY:
            svdlibcSparseBinaryTransform(inputMatrixFile, outputMatrixFile);
            break;
        case MATLAB_SPARSE:
            matlabSparseTransform(inputMatrixFile, outputMatrixFile);
            break;
        default:
            throw new UnsupportedOperationException("Format " + format +
                " is not currently supported for transform.  Email " +
                "s-space-research-dev@googlegroups.com to have it implemented");
        }
    }
    
    /**
     * Performs the log-entropy transform on the matrix file in the {@link
     * MatrixIO.Format.SVDLIBC_SPARSE_BINARY SVDLIBC_SPARSE_BINARY} format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void svdlibcSparseBinaryTransform(File inputMatrixFile, 
                                              File outputMatrixFile) 
            throws IOException {

        // Open the input matrix as a random access file to allow for us to
        // travel backwards as multiple passes are needed
        RandomAccessFile raf = new RandomAccessFile(inputMatrixFile, "r");

        // Make one pass through the matrix to calculate the global statistics

	int numUniqueWords = raf.readInt();
	int numDocs = raf.readInt(); // equal to the number of rows
        int numMatrixEntries = raf.readInt();

        // Also keep track of how many times a word was seen throughout the
        // entire corpus (i.e. matrix)
        int[] termToGlobalCount = new int[numUniqueWords];
        
        // SVDLIBC sparse binary is organized as column data.  Columns are how
        // many times each word (row) as it appears in that columns's document.
        int entriesSeen = 0;
        int docIndex = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = raf.readInt();

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = raf.readInt();
                // occurrence is specified as a float, rather than an int
                int occurrences = (int)(raf.readFloat());
                termToGlobalCount[termIndex] += occurrences; 
            }
	}

        // Seek back to the start of the data for the next pass
        raf.seek(12); // 3 integers

        // Keep track of the sum of all the entropy measures a term in each
        // document
        double[] termToEntropySum = new double[numUniqueWords];
        
	// Find the probability that the term appears in the document given how
	// many terms it has to begin with.  This probability is used to
	// determine the information (entropy) for a word in the document
	// itself.  These entropy values are summed for the whole corpus.
        docIndex = 0;
        entriesSeen = 0;
        for (; entriesSeen < numMatrixEntries; docIndex++) {
            int numUniqueWordsInDoc = raf.readInt();

            for (int i = 0; i < numUniqueWordsInDoc; ++entriesSeen, ++i) {
                int termIndex = raf.readInt();
                // occurrence is specified as a float, rather than an int
                float occurrences = raf.readFloat();

                double probabilityOfWordInDoc = 
                    occurrences / termToGlobalCount[termIndex];
                double wordEntropyInDoc = probabilityOfWordInDoc * 
                    log2(probabilityOfWordInDoc);
                
                termToEntropySum[termIndex] += wordEntropyInDoc;
            }
	}

        DataOutputStream dos = 
            new DataOutputStream(new FileOutputStream(outputMatrixFile));
        // Write the matrix header
        dos.writeInt(numUniqueWords);
        dos.writeInt(numDocs);
        dos.writeInt(numMatrixEntries);

        // Reset the original once more for the last pass
        raf.seek(12); // 3 ints

	// Last, rewrite the original matrix using the log-entropy
	// transformation describe on page 17 of Landauer et al. "An
	// Introduction to Latent Semantic Analysis"
        docIndex = 0;
        entriesSeen = 0;
        for (; entriesSeen < numMatrixEntries; ++docIndex) {
            int numUniqueWordsInDoc = raf.readInt();
            dos.writeInt(numUniqueWordsInDoc); // unchanged in new matrix

            for (int i = 0; i < numUniqueWordsInDoc; ++i, ++entriesSeen) {
                int termIndex = raf.readInt();
                // occurrence is specified as a float, rather than an int
                float occurrences = raf.readFloat();
                
                double entropySum = termToEntropySum[termIndex];
                double entropy = 1 + (entropySum / log2(numDocs));                
                double log = log2_1p(occurrences);
                dos.writeInt(termIndex);
                dos.writeFloat((float)(log * entropy));
            }
	}

        raf.close();
        dos.close();
    }

    /**
     * Performs the log-entropy transform on the matrix file in the {@link
     * MatrixIO.Format.MATLAB_SPARSE MATLAB_SPARSE} format.
     *
     * @param inputMatrixFile the matrix file to be transformed
     * @param outputMatrixFile the file that will contain the transformed matrix
     */
    private void matlabSparseTransform(File inputMatrixFile, 
                                       File outputMatrixFile) 
            throws IOException {

	Map<Integer,Integer> docToNumTerms = new HashMap<Integer,Integer>();
	int numDocs = 0;
	Map<Integer,Integer> termToGlobalCount = new HashMap<Integer,Integer>();
	int tokensSeenInCorpus = 0;
	
	// calculate how many terms were in each document for the original
	// term-document matrix
	BufferedReader br = new BufferedReader(new FileReader(inputMatrixFile));
	for (String line = null; (line = br.readLine()) != null; ) {

	    String[] termDocCount = line.split("\\s+");
	    
	    Integer term  = Integer.valueOf(termDocCount[0]);
	    int doc   = Integer.parseInt(termDocCount[1]);
	    Integer count = Double.valueOf(termDocCount[2]).intValue();
	    
	    if (doc > numDocs)
		numDocs = doc;
	    
	    Integer termGlobalCount = termToGlobalCount.get(term);
	    termToGlobalCount.put(term, (termGlobalCount == null)
				  ? count
				  : termGlobalCount + count);
	}

	br.close();

	LOGGER.fine("calculating term entropy");

	Map<Integer,Double> termToEntropySum = new HashMap<Integer,Double>();

	// now go through and find the probability that the term appears in the
	// document given how many terms it has to begin with
	br = new BufferedReader(new FileReader(inputMatrixFile));
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] termDocCount = line.split("\\s+");
	    
	    Integer term  = Integer.valueOf(termDocCount[0]);
	    Integer doc   = Integer.valueOf(termDocCount[1]);
	    Integer count = Double.valueOf(termDocCount[2]).intValue();
	    
	    double probability = count.doubleValue() / 
		termToGlobalCount.get(term).doubleValue();
	    
	    double d = (probability * log2(probability));
	    
	    // NOTE: keep the entropy sum a positive value
	    Double entropySum = termToEntropySum.get(term);
	    termToEntropySum.put(term, (entropySum == null)
				 ? d : entropySum + d);
	}
	br.close();
	   

	LOGGER.fine("generating new matrix");
	    	    
	PrintWriter pw = new PrintWriter(outputMatrixFile);

	// Last, rewrite the original matrix using the log-entropy
	// transformation describe on page 17 of Landauer et al. "An
	// Introduction to Latent Semantic Analysis"
	br = new BufferedReader(new FileReader(inputMatrixFile));
	for (String line = null; (line = br.readLine()) != null; ) {
	    String[] termDocCount = line.split("\\s+");
	    
	    Integer term  = Integer.valueOf(termDocCount[0]);
	    Integer doc   = Integer.valueOf(termDocCount[1]);
	    Integer count = Double.valueOf(termDocCount[2]).intValue();
	    
	    double log = log2_1p(count);
	    
	    double entropySum = termToEntropySum.get(term).doubleValue();
	    double entropy = 1 + (entropySum / log2(numDocs));
	    
	    // now print out the noralized values
	    pw.println(term + "\t" +
		       doc + "\t" +
		       (log * entropy));	    
	}
	br.close();
	pw.close();
    }      

    /**
     * Returns the log-entropy transformm of the input matrix.
     *
     * @param matrix the matrix to be transformed
     *
     * @return the transformed version of the input matrix
     */
    public Matrix transform(Matrix matrix) {
        // NOTE: as of 0.9.9, there is no good way to create a new matrix of the
        // same type unless you already know the type or use reflection.  In
        // addition, there's no way to access the Matrix.Type for a given
        // instance, further obfuscating what class should be instantiated.
        // Therefore, we just make a guess.  This is definitely a case for
        // concern in the API.  -jurgens
        Matrix transformed = Matrices.create(matrix.rows(), matrix.columns(), 
                                             Matrix.Type.DENSE_IN_MEMORY);
        int rows = matrix.rows();
        int cols = matrix.columns();
        
        // Count how many total words are in each document.  We need this value
        // when calculating the entropy of each word in the next step.
        int[] docToNumWords = new int[cols];
        for (int doc = 0; doc < cols; ++doc) {
            // sum how many time each term appears the doc
            for (int term = 0; term < rows; ++term)
                docToNumWords[doc] += matrix.get(term, doc);
        }

        for (int row = 0; row < rows; ++row) {
            // Each row is a word
            Vector rowVec = matrix.getVector(row);
            
            // Get the total number of times the word occurs
            double occurrencesInCorpus = 0;

            // Special case for sparse vectors
            if (rowVec instanceof Sparse) {
                Sparse sv = (Sparse)rowVec;
                for (int nonZeroCol : sv.getNonZeroIndices())
                    occurrencesInCorpus += rowVec.get(nonZeroCol);
            }
            else {
                for (int col = 0; col < cols; ++col)
                    occurrencesInCorpus += rowVec.get(col);
            }

            double wordEntropy = 0;

            // Then calculate the entropy (information gain) for the occurrence
            // of the word in each document
            if (rowVec instanceof Sparse) {
                Sparse sv = (Sparse)rowVec;
                for (int doc : sv.getNonZeroIndices()) {                    
                    double occurrences = rowVec.get(doc);
                    double probabilityOfWord = 
                        occurrences / docToNumWords[doc];
                    wordEntropy += probabilityOfWord * log2(probabilityOfWord);
                }
            }
            else {
                for (int doc = 0; doc < cols; ++doc) {
                    double occurrences = rowVec.get(doc);
                    double probabilityOfWord = 
                        occurrences / docToNumWords[doc];
                    wordEntropy += probabilityOfWord * log2(probabilityOfWord);

                }
            }

            // log2(cols) = log of the number of documents
            double entropy = 1 + (wordEntropy / log2(cols));

            // Last, take the log value of each occurrence and multiply by the
            // entropy to get the new value in the transformed matrix
            if (rowVec instanceof Sparse) {
                Sparse sv = (Sparse)rowVec;
                for (int doc : sv.getNonZeroIndices()) {                    
                    double occurrences = rowVec.get(doc);
                    transformed.set(row, doc, log2_1p(occurrences) * entropy);
                }
            }
            else {
                for (int doc = 0; doc < cols; ++doc) {
                    double occurrences = rowVec.get(doc);
                    transformed.set(row, doc, log2_1p(occurrences) * entropy);
                }
            }
        }

        return transformed;
    }

    /**
     * Returns the name of this transform.
     */
    public String toString() {
	return "log-entropy";
    }
}
