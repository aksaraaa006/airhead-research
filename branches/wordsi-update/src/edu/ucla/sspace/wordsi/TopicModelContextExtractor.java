package edu.ucla.sspace.wordsi;

import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.SparseDoubleVector;

import java.io.BufferedReader;
import java.io.IOError;
import java.io.IOException;


/**
 * TODO:JAAVAADOOOOC
 * @author Keith Stevens
 */
public class TopicModelContextExtractor implements ContextExtractor {

    private int vectorLength;

    public void processDocument(BufferedReader document, Wordsi wordsi) {
        try {
            String termAndVector;
            if ((termAndVector = document.readLine()) == null)
                return;
            String[] tokens = termAndVector.split("\\s+");
            String[] termSplit = tokens[0].split("\\.");

            if (tokens.length < 10)
                return;

            vectorLength = (tokens.length - 1) / 2;
            SparseDoubleVector vector = new CompactSparseVector(
                  (tokens.length - 1) / 2);
            for (int i = 1; i < tokens.length; i+=2) {
                int index = Integer.parseInt(tokens[i]);
                double value = Double.parseDouble(tokens[i+1]);
                vector.set(index, value);
            }

            wordsi.handleContextVector(termSplit[0], tokens[0], vector);
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    public int getVectorLength() {
        return vectorLength;
    }
}
