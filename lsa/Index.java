class Index {
	
  private final String word;

  private final String document;

  public Index(String word, String document) {
    if (word == null || document == null)
    throw new IllegalArgumentException("arguments cannot be null");
    this.word = word;
    this.document = document;
  }

  public boolean equals(Object o) {
    if (o instanceof Index) {
    Index i = (Index)o;
    return word.equals(i.word) && document.equals(i.document);
    }
    return false;
  }
  
  public int hashCode() {
    return word.hashCode() ^ document.hashCode();
  }

  public String toString() {
    return "(" + word + "," + document + ")";
  }
}
