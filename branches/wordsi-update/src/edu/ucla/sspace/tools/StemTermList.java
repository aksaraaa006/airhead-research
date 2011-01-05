package edu.ucla.sspace.tools;

import edu.ucla.sspace.text.EnglishStemmer;
import edu.ucla.sspace.text.Stemmer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.PrintWriter;

/**
 * A simple utility for stemming a list of terms.  Given an input file that has
 * one word per line, this utility will stem each term and write the stemmed
 * version to the output file.
 *
 * @author Keith Stevens
 */
public class StemTermList {

    public static void main(String args[]) throws Exception {
        if (args.length != 2) {
            System.out.println("Usage: StemTermList <in-term-list> <out-term>");
            System.exit(1);
        }

        Stemmer stemmer = new EnglishStemmer();

        PrintWriter writer = new PrintWriter(args[1]);

        BufferedReader br = new BufferedReader(new FileReader(args[0]));
        for (String line = null; (line=br.readLine()) != null; )
            writer.println(stemmer.stem(line.trim()));
        writer.close();
    }
}
