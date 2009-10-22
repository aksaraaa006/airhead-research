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

package edu.ucla.sspace.tri;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.common.WordComparator;

import edu.ucla.sspace.temporal.TemporalSemanticSpaceTracker;

import edu.ucla.sspace.util.MultiMap;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TimeSpan;
import edu.ucla.sspace.util.TreeMultiMap;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import java.util.logging.Level;
import java.util.logging.Logger;


public class FixedDurationTRITracker implements TemporalSemanticSpaceTracker {

    /**
     * Whether the nearest neighbors for each interesting word should be
     * compared after processing each partition.
     */
    private boolean compareNeighbors;

    /**
     * The word comparator used for computing similarity scores when calculating
     * the semantic shift.
     */
    private WordComparator wordComparator;

    /**
     * How many nearest neightbors of the words in {@code interestingWords} to
     * print for each semantic partition.  If this variable is 0, no neighbors
     * are printed.
     */
    private int interestingWordNeighbors;

    /**
     * The directory in which any serialized .sspace files should be saved.
     */
    private File outputDir;

    /**
     * The logger used for reporting.
     */
    private static final Logger LOGGER = 
        Logger.getLogger(FixedDurationTRITracker.class.getName());

    /**
     * A set of words for which the temporal semantics will be calculated.
     */
    private final Set<String> interestingWords;

    /**
     * A mapping from each word to the vectors that account for its temporal
     * semantics according to the specified time span
     */
    private final Map<String,SortedMap<Long,double[]>> wordToTemporalSemantics;

    public FixedDurationTRITracker(boolean compare, WordComparator comparator,
                                   int interestingNeighbors, File output) {
        interestingWordNeighbors = interestingNeighbors;
        wordComparator = comparator;
        compareNeighbors = compare;
        outputDir = output;
        wordToTemporalSemantics =
            new HashMap<String,SortedMap<Long,double[]>>();
        interestingWords = new HashSet<String>();
    }

    public void addInterestingWord(String term) {
        interestingWords.add(term);
        wordToTemporalSemantics.put(term, new TreeMap<Long,double[]>());
    }

    public int getNumInterestingWords() {
        return interestingWords.size();
    }

    /**
     * Adds the temporal semantics for each interesting word using the provided
     * semantic partition.
     *
     * @param currentSemanticPartitionStartTime the start time of the semantic
     *        partition
     */
    public void updateTemporalSemantics(long currentSemanticPartitionStartTime,
                                        SemanticSpace semanticPartition) {
        // Pre-allocate the zero vector so that if multiple interesting words
        // are not present in the space, they all point to the same zero
        // semantics
        double[] zeroVector = new double[semanticPartition.getVectorSize()];

        for (String word : interestingWords) {
            // update the vectors
            SortedMap<Long,double[]> temporalSemantics = 
                wordToTemporalSemantics.get(word);
            double[] semantics = semanticPartition.getVectorFor(word);
            // If the word was not in the current partition, then give it the
            // zero vector
            if (semantics == null)
                semantics = zeroVector;
            temporalSemantics.put(currentSemanticPartitionStartTime,
                                  semantics);
        }
    }

    /**
     * Prints the semantic shifts for all the words in the {@link
     * #wordToTemporalSemantics} map, using the {code dateString} for naming the
     * output file with the date of the last semantic partition.
     *
     * @param dateString the date of the last semantic partition.
     */
    public void printSemanticShifts(String dateString) {
        // Once we have all the vectors for each word in each sspace,
        // calculate how much the vector has changed.
        for (Map.Entry<String,SortedMap<Long,double[]>> e : 
                 wordToTemporalSemantics.entrySet()) {
            String word = e.getKey();
            SortedMap<Long,double[]> timeStampToSemantics = e.getValue();
            Iterator<Map.Entry<Long,double[]>> it = 
                timeStampToSemantics.entrySet().iterator();
            
            try {
                PrintWriter pw = new PrintWriter(new File(outputDir,
                            word + "." + dateString + ".temporal-changes.txt"));

                // Write the header so we can keep track of what all the columns
                // mean
                pw.println("#time\ttime-delay\tcosineSim" +
                           "\tcosineAngle\tEuclidean"+
                           "\tchange-in-magnitde\tmagnitde\tprev-magnitude");
                Map.Entry<Long,double[]> last = null;
                while (it.hasNext()) {
                    Map.Entry<Long,double[]> cur = it.next();
                    if (last != null) {
                        long timeDelay = cur.getKey() - last.getKey();
                        double euclideanDist =
                            Similarity.euclideanDistance(cur.getValue(),
                                                         last.getValue());
                        double cosineSim = Similarity.
                            cosineSimilarity(cur.getValue(), last.getValue());
                        double cosineAngle = Math.acos(cosineSim);
                        
                        double oldMag = getMagnitude(last.getValue());
                        double newMag = getMagnitude(cur.getValue());
                        
                        pw.println(cur.getKey() + "\t" + timeDelay + "\t" + 
                                   cosineSim + "\t" + cosineAngle + "\t" + 
                                   euclideanDist + "\t" + (newMag - oldMag) +
                                   "\t" + newMag + "\t" + oldMag);
                    }
                    last = cur;
                }
                pw.close();
            } catch (IOException ioe) {
                ioe.printStackTrace();
            }

        }
    }

    /**
     * Computes the ranking of which words underwent the most dramatic shifts in
     *  the most recent partition and then prints the ranking list of a file.
     *
     * @param dateString the string to use when indiciation which partition is
     *        having its ranking lists printed.  This string becomes a part of
     *        the file name.
     */
    public void printShiftRankings(String dateString, 
                                    long startOfMostRecentPartition,
                                    TimeSpan partitionDuration) {
        SortedMultiMap<Double,String> shiftToWord = 
            new TreeMultiMap<Double,String>();

        // Create a second time span than is twice the duration.  We will use
        // this to check whether two partition's vectors were adjacent in the
        // slice by seeing wether the timestamps fall within this duration
        TimeSpan twoPartitions = new TimeSpan(partitionDuration.getYears() * 2,
                                              partitionDuration.getMonths() * 2,
                                              partitionDuration.getWeeks() * 2,
                                              partitionDuration.getDays() * 2,
                                              partitionDuration.getHours() * 2);
        
        // Once we have all the vectors for each word in each sspace,
        // calculate how much the vector has changed.
        for (Map.Entry<String,SortedMap<Long,double[]>> e : 
                 wordToTemporalSemantics.entrySet()) {
            String word = e.getKey();
            SortedMap<Long,double[]> m = e.getValue();
            
            // Skip computing shifts for words without enough partitions
            if (m.size() < 2)
                continue;

            // Get the timestamps as a navigable map so we can identify the last
            // two keys in it more easly.
            NavigableMap<Long,double[]> timestampToVector = 
                (e instanceof NavigableMap) 
                ? (NavigableMap<Long,double[]>)m
                : new TreeMap<Long,double[]>(m);            

            Map.Entry<Long,double[]> mostRecent = timestampToVector.lastEntry();
            // Skip calculating the shift for words who most recent partition
            // was not the same as the most recent partition for TRI
            if (!mostRecent.getKey().equals(startOfMostRecentPartition))
                continue;
            
            Map.Entry<Long,double[]> secondMostRecent = 
                timestampToVector.lowerEntry(mostRecent.getKey());
            // Skip calculating the shift for words where the two most recent
            // partitoins aren't contiguous.  Check for this using the custom
            // time span that covers two partitions
            if (!twoPartitions.insideRange(
                        secondMostRecent.getKey(), mostRecent.getKey()))
                continue;

            
            // Compute the semantic shift of the two partitions
            shiftToWord.put(Similarity.cosineSimilarity(
                            secondMostRecent.getValue(),
                            mostRecent.getValue()), word);
        }

        try {
            PrintWriter pw = new PrintWriter(new File(outputDir,
                "shift-ranks-for." + dateString + ".txt"));
            for (Map.Entry<Double,String> e : shiftToWord.entrySet())
                pw.println(e.getKey() + "\t" + e.getValue());
            pw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    /**
     * Using the {@link wordToTemporalSemantics} set and input parameters,
     * calculates the shift in each word's semantic vector per recorded time
     * period and also prints out the nearest neighbors to each word for each
     * time period.
     *
     * @param dateString the string that encodes the date of the semantic
     *                   partition.  This will be used as a part of the file
     *                   name to indicate when the shifts occurred.
     * @param semanticPartition the current semantic that will be used to
     *                          identify the neighbors of each interesting word
     */
    public void printWordNeighbors(String dateString,
                                    SemanticSpace semanticPartition) {
        LOGGER.info("printing the most similar words for the semantic partition"
                    + "starting at: " + dateString);

        // generate the similarity lists
        for (String toExamine : interestingWords) {
            SortedMultiMap<Double,String> mostSimilar = 
            wordComparator.getMostSimilar(toExamine, semanticPartition,
                                          interestingWordNeighbors,
                                          Similarity.SimType.COSINE);

            if (mostSimilar != null) {
                File neighborComparisonFile = null;

                try {
                    File neighborFile = 
                        new File(outputDir, toExamine + "-" + dateString);
                    // Create the new file iff it doesn't already exist
                    neighborFile.createNewFile();
                    
                    neighborComparisonFile = new File(outputDir,
                        toExamine + "_neighbor-comparisons_" + dateString);
                    // see above comment
                    neighborComparisonFile.createNewFile();

                    PrintWriter pw = new PrintWriter(neighborFile);
                    for (String similar : mostSimilar.values())
                        pw.println(similar);
                    pw.close();
                } catch (IOException ioe) {
                    ioe.printStackTrace();
                }

                if (compareNeighbors) {                        
                    // Print an N x N comparison between all of the most similar
                    // words.  This gives an indication of whether any of the
                    // words might be outliers.
                    writeNeighborComparison(neighborComparisonFile, 
                                            mostSimilar, semanticPartition);
                }
            }
        }
    }

    /**
     * Write to the {@code neighborFile} the previously computed results of an N
     * x N similarity comparison of all the neighbors for word.
     *
     * @param neighborFile the file to which the results of the comparison
     *        should be written
     * @param mostSimilar a mapping from the similarity value to the neighbors
     *        of an interesting word that have the value.
     * @param sspace the semantic
     */
    public void writeNeighborComparison(File neighborFile, 
                                        MultiMap<Double,String> mostSimilar, 
                                        SemanticSpace sspace) {    
        try {
            PrintWriter pw = new PrintWriter(neighborFile);
        
            // print out the header so we know the comparison order
            StringBuffer sb = new StringBuffer(mostSimilar.size() * 10);
            for (Iterator<String> it = mostSimilar.values().iterator();
                    it.hasNext();) {
                sb.append(it.next());
                if (it.hasNext()) 
                    sb.append(" ");
            }
            pw.println(sb.toString());
        
            // create an N x N table of how similar all the words are to each
            // other.
            for (String word : mostSimilar.values()) {
                sb = new StringBuffer(mostSimilar.size() * 10);
                sb.append(word).append(" ");
                
                // loop through all of the words
                for (String other : mostSimilar.values()) {
                    // determine how similar the two words are
                    double similarity =
                        Similarity.cosineSimilarity(sspace.getVectorFor(word),
                                                    sspace.getVectorFor(other));
                    sb.append(similarity).append(" ");
                }
                pw.println(sb.toString());
            }
            
            pw.close();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    private static double getMagnitude(double[] arr) {
        double mag = 0d;
        for (double d : arr) {
            if (d != 0)
                mag += d*d;
        }
        return Math.sqrt(mag);
    }
}
