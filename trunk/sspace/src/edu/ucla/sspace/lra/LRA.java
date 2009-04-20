//package edu.ucla.sspace.lra;

import edu.ucla.sspace.common.Index;
import edu.smu.tspell.wordnet.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Scanner;

import java.util.regex.Pattern;


public class LRA {
    //constants...should probably be in the constructor
    private static final int NUM_SIM = 10; 
    private static final int MAX_PHASE = 5; 

    public LRA() {
        //constructor...will fill in later
    } 

    /**
     * Returns the synonyms for the specified term.
     * The synonyms will be taken directly from the WordNet database.
     * This is used by LRA to find alternative pairs. Given an input set of A:B.
     * For each A' that is similar to A, make a new pair A':B.  Likewise for B.
     *
     * @param term a String containing a single word
     * @return  an array of all the synonyms 
     */
    public static Synset[] findAlternatives(String term) {
        WordNetDatabase database = WordNetDatabase.getFileInstance();   
        Synset[] all = database.getSynsets(term);
        return all;
    }

    public static void main(String[] args) {
        System.out.println("starting LRA...\n");
        //get input...A B, where we are finding analogies for A:B
        Scanner sc = new Scanner(System.in);
        System.out.print("Input A: ");
        String A = sc.next();
        System.out.print("Input B: ");
        String B = sc.next(); 

        //1. Find alternates for A and B
        Synset[] A_prime = findAlternatives(A);
        Synset[] B_prime = findAlternatives(B);

        System.out.println("Top 10 Similar words:");
        System.out.print("A: ");
        for (int i = 0; (i < NUM_SIM && i < A_prime.length); i++) {
            String[] wordForms = A_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                System.out.print((j > 0 ? ", " : "") +
                    wordForms[j]);
            }
        }
        System.out.print("\n");
        System.out.print("B: ");
        for (int i = 0; (i < NUM_SIM && i < B_prime.length); i++) {
            String[] wordForms = B_prime[i].getWordForms();
            for (int j = 0; j < wordForms.length; j++)
            {
                System.out.print((j > 0 ? ", " : "") +
                    wordForms[j]);
            }
        }
        System.out.print("\n");
    }
}

