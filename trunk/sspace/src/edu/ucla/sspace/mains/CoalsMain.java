package edu.ucla.sspace.mains;

import edu.ucla.sspace.coals.Coals;

class CoalsMain {
  public static void main(String[] args) {
    Coals coals = new Coals();
    String testDocument = "How much wood would a woodchuck chuck , if a woodchuck could chuck wood ? As much wood as a woodchuck would , if a woodchuck could chuck wood .";
    coals.parseDocument(testDocument);
    coals.processSpace();
    coals.printCorrelations();
  }
}
