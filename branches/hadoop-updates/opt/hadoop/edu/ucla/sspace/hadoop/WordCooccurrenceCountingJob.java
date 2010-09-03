/*
 * Copyright 2010 David Jurgens
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

package edu.ucla.sspace.hadoop;

import edu.ucla.sspace.common.Filterable;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.index.IntegerVectorGenerator;
import edu.ucla.sspace.index.PermutationFunction;
import edu.ucla.sspace.index.RandomIndexVectorGenerator;
import edu.ucla.sspace.index.TernaryPermutationFunction;

import edu.ucla.sspace.text.HadoopIteratorFactory;

import edu.ucla.sspace.util.Duple;

import edu.ucla.sspace.vector.CompactSparseIntegerVector;
import edu.ucla.sspace.vector.DenseIntVector;
import edu.ucla.sspace.vector.IntegerVector;
import edu.ucla.sspace.vector.TernaryVector;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.Vectors;

import java.io.BufferedReader;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.File;
import java.io.FileReader;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;

import java.util.Arrays;
import java.util.ArrayDeque;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import java.util.Queue;
import java.util.Random;
import java.util.Set;

import java.util.logging.Logger;

import org.apache.hadoop.fs.*;
import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.io.*;
import org.apache.hadoop.mapreduce.*;
import org.apache.hadoop.mapreduce.lib.input.*;
import org.apache.hadoop.mapreduce.lib.output.*;
import org.apache.hadoop.util.*;


/**
 * A hadoop utility that spawns a job to count all the co-occurrences for a
 * directory of files within the Hadoop File System.  This class serves as the
 * underlying co-occurrence counting logic for other semantic space algorithms
 * that use the oc-occurrences to build their final representations.
 *
 *
 *
 *
 * <p> This class is not thread safe.
 *
 * @author David Jurgens
 */
public class WordCooccurrenceCountingJob {
   
    /**
     * The internal property used to configure the {@code Mapper} instances'
     * window size.  This property is not public because the API exposes this
     * configurability with a method parameter.
     */
    private static final String WINDOW_SIZE_PROPERTY =
        "edu.ucla.sspace.hadoop.WordCooccurrenceCountingJob.windowSize";
    /**
     * A list of all the factory properties that need to be initialized by each
     * {@code Mapper} instances upon {@code setup}.  These properties are
     * statically stored so the Job and Mapper classes can reuse the properties.
     */
    private static final Set<String> ITERATOR_FACTORY_PROPERTIES =
        new HashSet<String>();
    
    // Static block for setting the properties
    static {
        ITERATOR_FACTORY_PROPERTIES.add(
                HadoopIteratorFactory.TOKEN_FILTER_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                HadoopIteratorFactory.STEMMER_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                HadoopIteratorFactory.COMPOUND_TOKENS_FILE_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                HadoopIteratorFactory.TOKEN_REPLACEMENT_FILE_PROPERTY);
        ITERATOR_FACTORY_PROPERTIES.add(
                HadoopIteratorFactory.TOKEN_COUNT_LIMIT_PROPERTY);
    }
    
    /**
     * The configuration used by the Mapper and Reducer instances for running.
     * The parameters are set at Job creation time and then customized based on
     * the execution's input parameters.
     */
    private final Configuration conf;

    /**
     * Creates a {@code WordCooccurrenceCountingJob} using the System properties
     * for configuring the parameters.
     */
    public WordCooccurrenceCountingJob() {
        this(System.getProperties());
    }

    /**
     * Creates a {@code WordCooccurrenceCountingJob} using the provided
     * properties for configuring the parameters.
     */
    public WordCooccurrenceCountingJob(Properties props) {
        conf = new Configuration();
        for (String prop : ITERATOR_FACTORY_PROPERTIES) {
            String value = props.getProperty(prop);
            if (value == null)
                conf.set(prop, value);
        }
    }

    /**
     * Exceutes the word co-occurrence counting job on the corpus files in the
     * input directory using the current Hadoop instance, returning an iterator
     * over all the occurrences frequences found in the corpus.
     *
     * @param inputDir a directory on the Hadoop distributed file system
     *        containing all the corpus files that will be processed
     * @param windowSize the size of the co-occurrence window
     *
     * @return an iterator over the unique {@link WordCoOccurrence} counts found
     *         in the corpus.  Note that if two words co-occur the same distance
     *         apart multiple times, only one {@code WordCoOccurrence} is
     *         returned, where the number of co-occurrences is reflected by the
     *         the {@link WordCoOccurrence#getCount() getCount()} method.
     *
     * @throws Exception if Hadoop throws an {@code Exception} during its
     *         execution or if the resulting output files cannot be read.
     */
    public Iterator<WordCoOccurrence> execute(String inputDir, int windowSize)
            throws Exception {

        // Load any of the iterator factory properties that were specified by
        // the parameters so that the Mapper instances can acces them.
        conf.setInt(WINDOW_SIZE_PROPERTY, windowSize);

        // Create a mostly unique file name for the output directory.
        String outputDir = "output-" + System.currentTimeMillis();
        //conf.setBoolean("mapred.task.profile", true);

        Job job = new Job(conf, "Word Co-occurrence Counting");
	
        job.setJarByClass(WordCooccurrenceCountingJob.class);
        job.setMapperClass(CooccurrenceMapper.class);
        job.setReducerClass(CooccurrenceReducer.class);
	
        job.setMapOutputKeyClass(Text.class);
        job.setMapOutputValueClass(TextIntWritable.class);
        job.setOutputKeyClass(WordCoOccurrenceWritable.class);
        job.setOutputValueClass(IntWritable.class);

        FileInputFormat.addInputPath(job, new Path(inputDir));
        Path outputDirPath = new Path(outputDir);
        FileOutputFormat.setOutputPath(job, outputDirPath);
	
        job.waitForCompletion(true);

        // From the output directory, collect all the results files and return
        // an iterator over them that extracts the word occurrences
        FileSystem fs = FileSystem.get(conf);
        FileStatus[] outputFiles = 
            fs.listStatus(outputDirPath, new OutputFilePathFilter());
        Collection<Path> paths = new LinkedList<Path>();
        for (FileStatus status : outputFiles) {
            paths.add(status.getPath());
        }

        return new WordCoOccurrenceIterator(fs, paths.iterator());
    }

    /**
     * A private {@link PathFilter} implementation designed to only accept
     * output files from the reducer.
     */
    private static class OutputFilePathFilter implements PathFilter {
        
        /**
         * Returns {@code true} if the path begins the prefix for output files
         * from the reducer.
         */
        public boolean accept(Path p) {
            return p.getName().startsWith("part-r-");
        }
    }
    
    /**
     * A {@link Mapper} implementation that processes a sequence of text, where
     * term keys are then mapped to the words they co-occur with and the
     * distance, represented as a {@link TextIntWritable}.
     */
    public static class CooccurrenceMapper 
        extends Mapper<LongWritable,Text,Text,TextIntWritable> {

        /**
         * The default window size if none is specified.  Note that this value
         * is never used, but is provided as the default value when calling
         * {@link Configuration#getInt(String, int)} to get the actual window
         * size.
         */
        private static final int DEFAULT_WINDOW_SIZE = 2;

        /**
         * The window size used by the mapper for determining which words count
         * as a co-occurrence.
         */
        private int windowSize = 2;

        /**
         * The set of terms that should have the co-occurences counted for them.
         * This set acts as an inclusive filter, removing terms from the mapper
         * output if not present in the set.  If the set is empty, all terms are
         * accepted as valid.
         */
        private Set<String> semanticFilter;        
        
        /**
         * Creates an unconfigured {@code CooccurrenceMapper}.
         */
        public CooccurrenceMapper() {
            semanticFilter = new HashSet<String>();
        }

        /**
         * Initializes all the properties for this particular mapper.  This
         * process includes setting up the window size and configuring how the
         * input documents will be tokenized.
         */
        protected void setup(Mapper.Context context) {
            Configuration conf = context.getConfiguration();
            windowSize = conf.getInt(WINDOW_SIZE_PROPERTY,
                                     DEFAULT_WINDOW_SIZE);

            // Set up the HadoopIteratorFactory properties           
            Properties props = new Properties();
            for (String property : ITERATOR_FACTORY_PROPERTIES) {
                props.setProperty(property, conf.get(property));
            }
            HadoopIteratorFactory.setProperties(conf, props);
        }

        /**
         * Takes the {@code document} and produces a set of tuples mapping a
         * word to the other words it cooccurs with and the relative position of
         * those cooccurrences.
         *
         * @param key the byte offset of the document in the input corpus
         * @param document the document that will be segmented into tokens and
         *        mapped to cooccurrences
         * @param context the context in which this mapper is executing
         */
        public void map(LongWritable key, Text document, Context context)
                throws IOException, InterruptedException {
 	
            Queue<String> prevWords = new ArrayDeque<String>(windowSize);
            Queue<String> nextWords = new ArrayDeque<String>(windowSize);
            Iterator<String> documentTokens = 
                HadoopIteratorFactory.tokenizeOrdered(document.toString());

            String focusWord = null;
            Text focusWordWritable = new Text();

            // Prefetch the first windowSize words 
            for (int i = 0; i < windowSize && documentTokens.hasNext(); ++i)
                nextWords.offer(documentTokens.next());
            
            Map<WordCoOccurrenceWritable,Integer> occurrenceToCount = 
                new HashMap<WordCoOccurrenceWritable,Integer>();

            while (!nextWords.isEmpty()) {
                focusWord = nextWords.remove();
                focusWordWritable.set(focusWord);

                // Shift over the window to the next word
                if (documentTokens.hasNext()) {
                    String windowEdge = documentTokens.next(); 
                    nextWords.offer(windowEdge);
                }    
                
                // If we are filtering the semantic vectors, check whether this
                // word should have its semantics calculated.  In addition, if
                // there is a filter and it would have excluded the word, do not
                // keep its semantics around
                boolean calculateSemantics =
                    semanticFilter.isEmpty() 
                    || semanticFilter.contains(focusWord)
                    && !focusWord.equals(HadoopIteratorFactory.EMPTY_TOKEN);
                
                if (calculateSemantics) {
                    
                    int pos = -prevWords.size();
                    for (String word : prevWords) {

                        if (focusWord.equals(
                                HadoopIteratorFactory.EMPTY_TOKEN)) {
                            ++pos;
                            continue;
                        }
                        context.write(focusWordWritable,
                                      new TextIntWritable(word, pos));
                        ++pos;
                    }
                    
                    // Repeat for the words in the forward window.
                    pos = 1;
                    for (String word : nextWords) {
                        // Skip the addition of any words that are excluded from
                        // the filter set.  Note that by doing the exclusion
                        // here, we ensure that the token stream maintains its
                        // existing ordering, which is necessary when
                        // permutations are taken into account.
                        if (focusWord.equals(
                                HadoopIteratorFactory.EMPTY_TOKEN)) {
                            ++pos;
                            continue;
                        }
                        context.write(focusWordWritable,
                                      new TextIntWritable(word, pos));
                        ++pos;
                    }
                }
                
                // Last put this focus word in the prev words and shift off the
                // front of the previous word window if it now contains more
                // words than the maximum window size
                prevWords.offer(focusWord);
                if (prevWords.size() > windowSize) {
                    prevWords.remove();
                }
            }    
        }
    }

    /**
     * A special-purpose tuple {@link Writable} for storing text and int values
     * together in one object.  This class is designed to record the
     * co-occurrence a term and a relative offset indicating the distance from
     * the focus term.
     */
    private static class TextIntWritable 
        implements WritableComparable<TextIntWritable> {

        /**
         * The term that co-occurred with the focus term
         */
        Text t;

        /**
         * The relative position of the co-occurring term from the focus term.
         * Note that if this value is negative, the co-occurring term appeared
         * <i>before</i> the focus term.
         */
        int position;

        /**
         * Creates an empty {@code TextIntWritable} with no text and no position.
         */
        public TextIntWritable() {
            t = new Text();
            position = 0;
        }

        public TextIntWritable(String s, int position) {
            this.t = new Text(s);
            this.position = position;
        }

        public TextIntWritable(Text t, int position) {
            this.t = t;
            this.position = position;
        }

        public static TextIntWritable read(DataInput in) throws IOException {
            TextIntWritable tiw = new TextIntWritable();
            tiw.t.readFields(in);
            tiw.position = in.readInt();
            return tiw;
        }

        public int compareTo(TextIntWritable o) {
            int c = t.compareTo(o.t);
            if (c != 0)
                return c;
            return position - o.position;
        } 

        public int hashCode() {
            return t.hashCode() 
                ^ position;
        }

        public boolean equals(Object o) {
            if (o instanceof TextIntWritable) {
                TextIntWritable tiw = (TextIntWritable)o;
                return t.equals(tiw.t) &&
                    position == tiw.position;
            }
            return false;
        }
        
        public void readFields(DataInput in) throws IOException {
            t.readFields(in);
            position = in.readInt();
        }

        public void write(DataOutput out) throws IOException {
            t.write(out);
            out.writeInt(position);
        }

        public String toString() {
            return t + "\t" + position;
        }
    }

    /**
     * A {@link Writable} that represents the occurrence of a two words a
     * certain distance apart.
     */
    private static class WordCoOccurrenceWritable 
            implements WritableComparable<WordCoOccurrenceWritable> {

        /**
         * The first word that occurred.
         */
        private Text w1;

        /**
         * The co-occurring word.
         */
        private Text w2;

        /**
         * The distance between {@code w1} and {@code w2}.  If {@code w2} occurs
         * before, this distance is negative.
         */
        private int distance;

        public WordCoOccurrenceWritable() { 
            w1 = new Text();
            w2 = new Text();
            distance = 0;
        }

        public WordCoOccurrenceWritable(String word1, String word2, 
                                      int distance) {
            w1 = new Text(word1);
            w2 = new Text(word2);
            this.distance = distance;
        }

        public WordCoOccurrenceWritable(Text word1, Text word2, 
                                      int distance) {
            w1 = word1;
            w2 = word2;
            this.distance = distance;
        }        

        public static WordCoOccurrenceWritable read(DataInput in) 
                throws IOException {
            WordCoOccurrenceWritable wow = new WordCoOccurrenceWritable();
            wow.w1.readFields(in);
            wow.w2.readFields(in);
            wow.distance = in.readInt();
            return wow;
        }

        public int compareTo(WordCoOccurrenceWritable o) {
            int c = w1.compareTo(o.w1);
            if (c != 0)
                return c;
            c = w2.compareTo(o.w2);
            if (c != 0)
                return c;
            return distance - o.distance;
        } 
        public int hashCode() {
            return w1.hashCode() 
                ^ w2.hashCode()
                ^ distance;
        }

        public boolean equals(Object o) {
            if (o instanceof WordCoOccurrenceWritable) {
                WordCoOccurrenceWritable wow = (WordCoOccurrenceWritable)o;
                return w1.equals(wow.w1) &&
                    w2.equals(wow.w2) &&
                    distance == wow.distance;
            }
            return false;
        }
        
        public void readFields(DataInput in) throws IOException {
            w1.readFields(in);
            w2.readFields(in);
            distance = in.readInt();
        }

        public void write(DataOutput out) throws IOException {
            w1.write(out);
            w2.write(out);
            out.writeInt(distance);
        }

        public String toString() {
            return w1 + "\t" + w2 + "\t" + distance;
        }
    }    

    /**
     * A {@link Reducer} that transforms the co-occurrence of they input key
     * with another word at a certan position to a count of how many times that
     * co-occurrence took place.
     */
    public static class CooccurrenceReducer
        extends Reducer<Text,TextIntWritable,
                        WordCoOccurrenceWritable,IntWritable> {

        public CooccurrenceReducer() {
        }

        public void reduce(Text occurrence,
                           Iterable<TextIntWritable> values, Context context)
                throws IOException, InterruptedException {

            Map<Duple<String,Integer>,Integer> cooccurrenceToCount = 
                new HashMap<Duple<String,Integer>,Integer>();
            for (TextIntWritable cooccurrence : values) {
                Duple<String,Integer> tuple = new Duple<String,Integer>(
                    cooccurrence.t.toString(),
                    cooccurrence.position);
                Integer count = cooccurrenceToCount.get(tuple);
                cooccurrenceToCount.put(tuple, 
                                        (count == null) ? 1 : count + 1);
            }

            for (Map.Entry<Duple<String,Integer>,Integer> e : 
                     cooccurrenceToCount.entrySet()) {
                Duple<String,Integer> d = e.getKey();
                context.write(new WordCoOccurrenceWritable(
                              occurrence, new Text(d.x), d.y),
                              new IntWritable(e.getValue()));
            }
        }
    }
    
    /**
     * An iterator over the output files from the {@link CooccurrenceReducer}
     * that returns the set of {@link WordCoOccurrence} instances extracted from
     * the corpus.
     */
    private static class WordCoOccurrenceIterator 
            implements Iterator<WordCoOccurrence> {
        
        /**
         * The files containing results that have not yet been returned
         */
        private final Iterator<Path> files;

        /**
         * The current file being processed by this iterator.  The actual file
         * processing is delegated to a special purpose iterator.
         */
        private FileIterator curFile;

        /**
         * The next word occurrence to return or {@code null} if there are no
         * further instances to return.
         */
        private WordCoOccurrence next;

        /**
         * The file system currently being used by this Hadoop instance.
         */
        private FileSystem fileSystem;

        /**
         * Creates a {@code WordCoOccurrenceIterator} that returns all the
         * occurrences contained in the provided files.
         *
         * @param fileSystem the file system used to access the paths in {@code
         *        files}
         * @param files the series of input files to be read by this iterator
         *        and returned as {@link WordCoOccurrence} instances
         */
        public WordCoOccurrenceIterator(FileSystem fileSystem, 
                                      Iterator<Path> files) throws IOException {
            this.fileSystem = fileSystem;
            this.files = files;
            advance();
        }

        private void advance() throws IOException {
            if (curFile != null && curFile.hasNext()) {
                next = curFile.next();
            }
            else if (files.hasNext()) {
                curFile = new FileIterator(
                    new BufferedReader(
                        new InputStreamReader(fileSystem.open(files.next()))));
                next = curFile.next();
            }
            else {
                next = null;
            }
        }

        /**
         * Returns true if the iterator has more occurrences
         */
        public boolean hasNext() {
            return next != null;
        }
        
        /**
         * Returns the next instance
         */
        public WordCoOccurrence next() {
            if (next == null) {
                throw new NoSuchElementException("No further word occurrences");
            }
            WordCoOccurrence n = next;
            try {
                advance();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            return n;
        }

        /**
         * Throws an {@link UnsupportedOperatonException} if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * An iterator that transforms the file output of the reduce step into
     * {@link WordCoOccurrence} instances.
     */
    private static class FileIterator 
            implements Iterator<WordCoOccurrence> {
        
        /**
         * The reader containing the contents of the reducer output.
         */
        private BufferedReader br;
        
        /**
         * The next line from the reader or {@code null} if there were no
         * further lines to be read.
         */
        private String nextLine;

        /**
         * Creates a {@code FileIterator} over the word co-occurrence
         * information contaned within the reader.  The data is expected to be
         * formatted according to the {@link CooccurrenceReducer} output.
         */
        public FileIterator(BufferedReader br) throws IOException {
            this.br = br;
            nextLine = br.readLine();
        }
        
        /**
         * Returns {@code true} if there are still word co-occurrences left to
         * return
         */ 
        public boolean hasNext() {
            return nextLine != null;
        }

        /**
         * Returns the next word co-occurrence from the file
         */
        public WordCoOccurrence next() {
            if (nextLine == null) {
                throw new NoSuchElementException("No further word occurrences");
            }
            String n = nextLine;
            try {
                nextLine = br.readLine();
            } catch (IOException ioe) {
                throw new IOError(ioe);
            }
            String[] arr = n.split("\t");
            return new SimpleWordCoOccurrence(arr[0], arr[1],
                Integer.parseInt(arr[2]), Integer.parseInt(arr[3]));
        }

        /**
         * Throws an {@link UnsupportedOperatonException} if called.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
        
    /**
     * A straigh-forward implementation of {@link WordCoOccurrence}.
     */
    static class SimpleWordCoOccurrence implements WordCoOccurrence {
        
        private final String focusWord;
        
        private final String relativeWord;

        private final int distance;

        private final int count;

        public SimpleWordCoOccurrence(String focusWord, String relativeWord, 
                                      int distance, int count) {
            this.focusWord = focusWord;
            this.relativeWord = relativeWord;
            this.distance = distance;
            this.count = count;
        }

        /**
         * {@inheritDoc}
         */
        public String focusWord() {
            return focusWord;
        }

        /**
         * {@inheritDoc}
         */
        public String relativeWord() {
            return relativeWord;
        }
        
        /**
         * {@inheritDoc}
         */
        public int getDistance() {
            return distance;
        }

        /**
         * {@inheritDoc}
         */
        public int getCount() {
            return count;
        }
    }
}