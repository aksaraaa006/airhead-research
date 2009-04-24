package edu.ucla.sspace.common;

/**
 * An interface for classes which will maintain and utilize random indexes.
 * The main purpose of this of this class is to allow any algorithm which makes
 * use of some sort of random index, such as Random Indexing, Beagle, or other
 * varients, can easily swap out the type of random indexing used for
 * experimentation purposes.  Implementations should be thread safe.
 */
public interface IndexBuilder {
  /** 
   * Add a new index vector for this term if one does not already exist.
   */
  public void addTermIfMissing(String term);

  /**
   * Given a current meaning vector, update it using index vectors from a given
   * window of words.
   *
   * @param meaning An existing meaning vector.  After calling this method, the
   * values of meaning will be updated according to some scheme.
   * @param context An array of words, each of which will be combined with
   * meaning.
   */
  public void updateMeaningWithTerm(double[] meaning, String[] context);
}
