package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.DocumentPreprocessor;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class OneLineDocumentCleaner {
  public static void main(String[] args) {
    try {
      File wordList = new File(args[1]);
      DocumentPreprocessor processor = new DocumentPreprocessor(wordList);
      BufferedReader br = new BufferedReader(new FileReader(args[2]));
      BufferedWriter bw = new BufferedWriter(new FileWriter(args[3]));
      for (String line = null; (line = br.readLine()) != null;) {
        String cleaned = processor.process(line);
        bw.write(cleaned);
      }
      bw.close();
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
