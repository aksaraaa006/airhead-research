package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.DocumentPreprocessor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.PrintWriter;

public class TwitterPreProcessor {
  public static void main(String[] args) throws IOException, FileNotFoundException {
    ArgOptions options = new ArgOptions();
    options.addOption('d', "docFiles", "A file where each line is a document",
                      true, "FILE[,FILE..]", "Required");
    options.addOption('w', "wordList", "A file containing valid words, one per line",
                      true, "FILE", "Optional");

    options.parseOptions(args);
    if (options.numPositionalArgs() != 1 || !options.hasOption("docFiles")) {
      System.out.println("usage: java TwitterPreProcessor [options] <out_file> " + 
                         options.prettyPrint());
      System.exit(1);
    }

    DocumentPreprocessor processor;
    if (options.hasOption("wordList")) {
      processor = new DocumentPreprocessor(
          new File(options.getStringOption("wordList")));
    } else {
      processor = new DocumentPreprocessor();
    }

    PrintWriter cleanedDocumentWriter =
      new PrintWriter(options.getPositionalArg(0));

    String[] documentFileNames =
      options.getStringOption("docFiles").split(",");
    for (String documentFileName : documentFileNames) {
      BufferedReader br =
        new BufferedReader(new FileReader(documentFileName));
      String document = null;
      while ((document = br.readLine()) != null) {
        String[] tsAndDocument = document.split(" ", 2);
        String cleanedDocument = processor.process(tsAndDocument[1]);
        if (cleanedDocument.equals(""))
          continue;
        cleanedDocumentWriter.println(
            tsAndDocument[0] + " " + cleanedDocument);
      }
    }
  }
}
