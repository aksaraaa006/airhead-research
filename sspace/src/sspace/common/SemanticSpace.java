package sspace.common;

import java.io.IOException;

// A SemanticSpace implementation should implement each of the following
// methods in such a way that it conforms to the expected format of document
// input, and the stage of method calls.  For each SemanticSpace, the expected
// sequence of calls will be:
//   1) at least 1 call to parseDocument()
//   2) processSpace()
//   3) reduce()
//   4) at least 1 call to computeDistances()
public interface SemanticSpace {
  // Implementation should read from the given filename and process all of the
  // words in the document.  Models may consider filename to be a unique string
  // identifying the document if this is needed.  Implementations may throw an
  // IOException if needed.  
  // This function may be called several times, such as for LSA, or it might be
  // called once with one file containing all the relevant text, as might be
  // done for COALS.
  public void parseDocument(String filename) throws IOException;

  // Implementation should read from the given filename and compute the
  // similarity between each of the word pairs read.  This function needs a
  // little more discussion.
  public void computeDistances(String filename, int similarCount);

  // If the Semantic Space model requires some method of reduction, most notably
  // the use of SVD, it should be executed when this method is called.  It is
  // assumed that processSpace will be called prior to reduce.
  public void reduce();

  // Do any processing of the Semantic Space model once all the documents have
  // been processed.  Likely implementations of this would be to compute TFID,
  // normalize values, or any other non-dimensionality reducing post-processing
  // techniques.
  public void processSpace();
}
