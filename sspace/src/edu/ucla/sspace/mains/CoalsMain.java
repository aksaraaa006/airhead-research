package edu.ucla.sspace.mains;

import edu.ucla.sspace.coals.Coals;

import Jama.Matrix;

public class CoalsMain {
  public static void main(String[] args) {
    Coals coals = new Coals();
    String testDocument = "How much wood would a woodchuck chuck , if a woodchuck could chuck wood ? As much wood as a woodchuck would , if a woodchuck could chuck wood .";
    coals.parseDocument(testDocument);
    double[][] testCorrelations = { {0, 0, 0,10, 1, 4, 2, 0, 8, 0, 0,10, 5},
                                    {0, 0, 0, 0, 0, 3, 2, 0, 0, 0, 4, 1, 0},
                                    {0, 0, 0, 0, 5, 3, 2, 0, 0, 3, 6, 1, 0},
                                    {10,0, 0, 0, 5, 9, 6, 1,10, 4, 8,18, 9},
                                    {1, 0, 5, 5, 4, 2, 1, 0, 0, 7,10, 3, 2},
                                    {4, 3, 3, 9, 2, 0, 8, 0, 5, 1, 9,11, 2},
                                    {2, 2, 2, 6, 1, 8, 0, 0, 4, 0, 6, 8, 0},
                                    {0, 0, 0, 1, 0, 0, 0, 0, 0, 4, 3, 0, 2},
                                    {8, 0, 0,10, 0, 5, 4, 0, 0, 0, 0,10, 3},
                                    {0, 0, 3, 4, 7, 1, 0, 4, 0, 0,10, 2, 3},
                                    {0, 4, 6, 8,10, 9, 6, 3, 0,10, 2, 8, 5},
                                    {10,1, 1,18, 3,11, 8, 0,10, 2, 8, 0, 8},
                                    {5, 0, 0, 9, 2, 2, 0, 2, 3, 3, 5, 8, 0}};
    coals.compareToMatrix(new Matrix(testCorrelations)).print(6, 4);
    coals.processSpace();
  }
}
