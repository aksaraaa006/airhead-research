/*
 * Copyright 2009 David Jurgens 
 *
 * This file is part of the S-Space package and is covered under the terms and
 * conditions therein.
 *
 * The S-Space package is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License version 2 as published
 * by the Free Software Foundation and distributed hereunder to you.
 *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND NO REPRESENTATIONS OR WARRANTIES,
 * EXPRESS OR IMPLIED ARE MADE.  BY WAY OF EXAMPLE, BUT NOT LIMITATION, WE MAKE
 * NO REPRESENTATIONS OR WARRANTIES OF MERCHANT- ABILITY OR FITNESS FOR ANY
 * PARTICULAR PURPOSE OR THAT THE USE OF THE LICENSED SOFTWARE OR DOCUMENTATION
 * WILL NOT INFRINGE ANY THIRD PARTY PATENTS, COPYRIGHTS, TRADEMARKS OR OTHER
 * RIGHTS.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package edu.ucla.sspace.common;

import java.util.BitSet;
import java.util.HashMap;
import java.util.Map;

import edu.ucla.sspace.util.IntegerMap;


/**
 * A collection of static methods for statistical analysis.
 */
public class Statistics {

    /**
     * Uninstantiable
     */
    private Statistics() { }

    /**
     * Returns the entropy of the array.
     */
    public static double entropy(int[] a) {
        Map<Integer,Integer> symbolFreq = new IntegerMap<Integer>();
        for (int i : a) {
            Integer freq = symbolFreq.get(i);
            symbolFreq.put(i, (freq == null) ? 1 : 1 + freq);
        }
        
        double entropy = 0;

        int symbols = a.length;
        for (Integer freq : symbolFreq.values()) {
            double symbolProbability = freq / symbols;
            entropy -= symbolProbability * log2(symbolProbability);
        }

        return entropy;
    }

    /**
     * Returns the entropy of the array.
     */
    public static double entropy(double[] a) {
        Map<Double,Integer> symbolFreq = new HashMap<Double,Integer>();
        for (double d : a) {
            Integer freq = symbolFreq.get(d);
            symbolFreq.put(d, (freq == null) ? 1 : 1 + freq);
        }
        
        double entropy = 0;

        int symbols = a.length;
        for (Integer freq : symbolFreq.values()) {
            double symbolProbability = freq / symbols;
            entropy -= symbolProbability * log2(symbolProbability);
        }

        return entropy;
    }

    /**
     * Returns the base-2 logarithm of {@code d}.
     */
    public static double log2(double d) {
        return Math.log(d) / Math.log(2);
    }

    /**
     * Returns the base-2 logarithm of {@code d + 1}.
     * 
     * @see Math#log1p(double)
     */
    public static double log2_1p(double d) {
        return Math.log1p(d) / Math.log(2);
    }

    /**
     * Randomly sets {@code valuesToSet} values to {@code true} for a sequence
     * from [0:{@code range}).
     *
     * @param valuesToSet the number of values that are to be set to {@code
     *        true} in the distribution
     * @param range the total number of values in the sequence.
     */
    public static BitSet randomDistribution(int valuesToSet, int range) {
        if (valuesToSet >= range)
            throw new IllegalArgumentException("too many values for range");
        BitSet values = new BitSet(range);
        // We will be setting fewer than half of the values, so set everything
        // to false, and mark true until the desired number is reached
        if (valuesToSet < (range / 2)) {
            int set = 0;
            while (set < valuesToSet) {
                int i = (int)(Math.random() * range);
                if (!values.get(i)) {
                    values.set(i, true);
                    set++;
                }
            }
        }
        // We will be setting more than half of the values, so set everything to
        // true, and mark false until the desired number is reached
        else {
            values.set(0, range, true);
            int set = range;
            while (set > valuesToSet) {
                int i = (int)(Math.random() * range);
                if (values.get(i)) {
                    values.set(i, false);
                    set--;
                }
            }
        }
        return values;
    }
}
