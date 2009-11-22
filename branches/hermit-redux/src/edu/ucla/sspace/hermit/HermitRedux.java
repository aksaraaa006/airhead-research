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

package edu.ucla.sspace.hermit;

import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering;
import edu.ucla.sspace.clustering.HierarchicalAgglomerativeClustering.ClusterLinkage;

import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.matrix.LogEntropyTransform;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.MatrixBuilder;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.SVD;
import edu.ucla.sspace.matrix.Transform;

import edu.ucla.sspace.ri.RandomIndexing;

import edu.ucla.sspace.text.IteratorFactory;

import edu.ucla.sspace.util.SparseArray;
import edu.ucla.sspace.util.SparseIntHashArray;

import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vector;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.File;
import java.io.FileReader;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;

import java.nio.IntBuffer;

import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Queue;
import java.util.Set;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentNavigableMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import java.util.concurrent.atomic.AtomicInteger;

import java.util.logging.Logger;

/**
 * TODO
 */
public class HermitRedux implements SemanticSpace {

    /** 
     * The prefix for naming publically accessible properties
     */
    private static final String PROPERTY_PREFIX = 
        "edu.ucla.sspace.hermit.HermitRedux";

    /**
     * The property to define the {@link Transform} class to be used
     * when processing the space after all the documents have been seen.
     */
    public static final String MATRIX_TRANSFORM_PROPERTY =
        PROPERTY_PREFIX + ".transform";

    /**
     * The property to set the number of dimension to which the space should be
     * reduced using the SVD
     */
    public static final String DIMENSIONS_PROPERTY =
        PROPERTY_PREFIX + ".dimensions";

    /**
     * The property to set the minimum similarity between context distributions.
     * Similarity values are drawn from a range of [-1, 1] where 1 indicates the
     * distributions are identical.
     */
    public static final String MIN_CONTEXT_SIMILARITY_PROPERTY =
        PROPERTY_PREFIX + ".minContextSimilarity";

    /**
     * The property to set the size of the context for determining first-order
     * co-occurrence statistics.  The size specifies the number of terms to
     * include in each direction as co-occurring.
     */
    public static final String CONTEXT_SIZE_PROPERTY =
        PROPERTY_PREFIX + ".contextSize";

    /**
     * The directory in which to place Hermit context files prior to clustering.
     * For systems with multiple I/O devices, using a separate device that is
     * different than the {@code java.io.tmpdir} directory may result in
     * improved performance.
     */
    public static final String CONTEXT_FILE_DIR =
        PROPERTY_PREFIX + ".contextFileDir";

    /**
     * The name prefix used with {@link #getName()}
     */
    public static final String HERMIT_SSPACE_NAME =
        "hermit-redux-s-space";

    /**
     * The logger used to record all output
     */
    private static final Logger LOGGER = 
        Logger.getLogger(HermitRedux.class.getName());

    private final File contextFileDir;

    /**
     * The output stream for writing to the {@link documentContexts} file.  This
     * output strem is no longer open once {@link #processSpace(Properties)
     * processSpace} has been called.
     */
    private final DataOutputStream contextWriter;

    /**
     * The file that records the words that comprise a document context.  Each
     * document is represented as the indices for each occurring word and the
     * number of times that word occurs.  This file will only be fully built
     * after all the document have been processed.
     */
    private final File documentContexts;

    /**
     * A semantic space responsible for building all the first-order
     * co-occurrence statistics.  These statistics will be combined when this
     * semantic space is being processed to form the second-order co-occurrence
     * statistics.
     *
     * @see #processSpace(Properties)
     */
    private final RandomIndexing firstOrderCoOccurrences;

    /**
     * A mapping from a word to the row index in the that word-document matrix
     * that contains occurrence counts for that word.
     */
    private final ConcurrentMap<String,Integer> termToIndex;

    private final Map<String,Integer> termToSenseIndex;

    private final List<AtomicInteger> termFrequency;

    private final List<AtomicInteger> termDocFrequency;

    private final double minContextSimilarity;

    /**
     * The counter for recording the current, largest word index in the
     * word-document matrix.
     */
    private int termIndexCounter;

    private final AtomicInteger documentCounter;

    private final Set<String> excludeFromAnalysis;

    /**
     * The word space of the Hermit model.  This matrix is only available after
     * the {@link #processSpace(Properties) processSpace} method has been
     * called.
     */
    private Matrix wordSpace;

    /**
     * Constructs the {@code HermitRedux}.
     *
     * @throws IOException if this instance encounters any errors when creatng
     *         the backing array files required for processing
     */
    public HermitRedux() throws IOException {
        this(System.getProperties());
    }
    
    /**
     *
     */
    public HermitRedux(Properties properties) 
        throws IOException {

	termToIndex = new ConcurrentHashMap<String,Integer>();
        termToSenseIndex = new HashMap<String,Integer>();
	termIndexCounter = 0;
        documentCounter = new AtomicInteger(0);
        excludeFromAnalysis = new HashSet<String>();
        termFrequency = new ArrayList<AtomicInteger>();
        termDocFrequency = new ArrayList<AtomicInteger>();
        documentContexts = File.createTempFile("hermit-redux-contexts", ".dat");
        contextWriter = new DataOutputStream(
            new BufferedOutputStream(new FileOutputStream(documentContexts)));

        String contextSizeProp = properties.getProperty(CONTEXT_SIZE_PROPERTY);
        if (contextSizeProp != null) {
            properties.setProperty(RandomIndexing.WINDOW_SIZE_PROPERTY,
                                   contextSizeProp);
        }
        properties.setProperty(RandomIndexing.USE_SPARSE_SEMANTICS_PROPERTY,
                               "false");

        String contextFileDirProp = properties.getProperty(CONTEXT_FILE_DIR);
        contextFileDir = (contextFileDirProp == null)
            ? new File(System.getProperty("java.io.tmpdir"))
            : new File(contextFileDirProp);
        if (!contextFileDir.exists() || !contextFileDir.isDirectory())
            throw new IllegalStateException("cannot write context files "
                + "to non-existent or non-directory file");

        String minContextSimProp = 
            properties.getProperty(MIN_CONTEXT_SIMILARITY_PROPERTY);
        minContextSimilarity = (minContextSimProp != null)
            ? Double.parseDouble(minContextSimProp)
            : .9;
        if (minContextSimilarity > 1 || minContextSimilarity < -1)
            throw new IllegalArgumentException(
                MIN_CONTEXT_SIMILARITY_PROPERTY + " property must be within " +
                "the range [-1, 1]");

        firstOrderCoOccurrences = new RandomIndexing(properties);
        
	wordSpace = null;        
    }    

    /**
     * {@inheritDoc}
     *
     * <p>
     *
     * This method is thread-safe and may be called in parallel with separate
     * documents to speed up overall processing time.
     *
     * @param document {@inheritDoc}
     */
    public void processDocument(BufferedReader document) throws IOException {
        documentCounter.getAndIncrement();

        // Copy the input into a separate buffered so we can process it twice,
        // once for RandomIndexing and once for this instance.
        ByteArrayOutputStream baos = new ByteArrayOutputStream(10240);
        PrintWriter pw = new PrintWriter(baos);
        for (String line = null; (line = document.readLine()) != null; ) {
            pw.println(line);
        }
        pw.close();
	document.close();

        byte[] documentAsBytes = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(documentAsBytes);
        BufferedReader documentCopy = new BufferedReader(
            new InputStreamReader(bais));

        // Use RandomIndexing to generate the first-order co-occurrence
        // statistics
        firstOrderCoOccurrences.processDocument(documentCopy);

        // Rewind the copy back to the start
        bais = new ByteArrayInputStream(documentAsBytes);
        documentCopy = new BufferedReader(new InputStreamReader(bais));

        // Create a mapping for each term that is seen in the document to the
        // number of times it has been seen. 
        SparseArray<Integer> termCounts = new SparseIntHashArray();
        Iterator<String> documentTokens = 
            IteratorFactory.tokenize(documentCopy);

        // For each word in the text document, keep a count of how many times it
        // has occurred
        while (documentTokens.hasNext()) {
            String word = documentTokens.next();

            // Skip added empty tokens for words that have been filtered out
            if (word.equals(IteratorFactory.EMPTY_TOKEN))
                continue;
            
            // Increment the number of times the word appeared
            Integer termIndex = getTermIndex(word);
            termCounts.set(termIndex, termCounts.get(termIndex) + 1);            
        }

        // Update each term's frequency and document frequency
        for (int termIndex : termCounts.getElementIndices()) {
            termFrequency.get(termIndex).addAndGet(termCounts.get(termIndex));
            termDocFrequency.get(termIndex).incrementAndGet();
        }

        // Write the document context out to file.  Synchronize to ensure that
        // documents are not interleaved.
        synchronized(contextWriter) {
            contextWriter.writeInt(termCounts.cardinality());
            for (int nonZeroIndex : termCounts.getElementIndices()) {
                contextWriter.writeInt(nonZeroIndex);
                contextWriter.writeInt(termCounts.get(nonZeroIndex));
            }
        }
    }
    
    /**
     * Returns the index for the specified term, creating an index for it if the
     * term had not already been added.
     *
     * @param term a word in the semantic space
     *
     * @return the index for the word
     */
    private Integer getTermIndex(String term) {
	Integer index = termToIndex.get(term);
	if (index == null) {
	    synchronized(this) {
		// Recheck to see if the term was added while blocking
		index = termToIndex.get(term);
		// If some other thread has not already added this term while
		// the current thread was blocking waiting on the lock, then add
		// it.
		if (index == null) {
		    index = termIndexCounter++;
		    termToIndex.put(term, index);
                    termFrequency.add(new AtomicInteger(0));
                    termDocFrequency.add(new AtomicInteger(0));
		}
	    }
	}
        return index;
    }

    /**
     * {@inheritDoc}
     */
    public Set<String> getWords() {
        return Collections.unmodifiableSet(termToIndex.keySet());
    }

    /**
     * {@inheritDoc}
     */
    public Vector getVector(String word) {
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return "hermit-redux";
    }

    /**
     * {@inheritDoc}
     */
    public int getVectorLength() {
        return 0; 
    }

    public void setExcludedWords(Set<String> wordsToExclude) {
        excludeFromAnalysis.clear();
        excludeFromAnalysis.addAll(wordsToExclude);
    }

    /**
     * {@inheritDoc}
     *
     * @param properties {@inheritDoc} See this class's {@link HermitRedux
     *        javadoc} for the full list of supported properties.
     */
    public void processSpace(final Properties properties) {
        try {
            // Flush any buffered context data out 
            contextWriter.close();
            int senseMatrixRow = 0;

            // Rewrite the context maxtrix to a row-aligned format.  As we
            // cluster the contexts, this will enable us to rewrite the final
            // term-document matrix with the original occurrence counts
            LOGGER.fine("finalizing the term document matrix");
            Matrix termDocMatrix =  
                //Matrices.create(termToIndex.size(), documentCounter.get(), 
                //                Matrix.Type.SPARSE_IN_MEMORY); 
            getTermDocMatrix();
            // Get a builder for streaming the newly resized matrix to file.  We
            // will have to transpose the matrix since we will be processing the
            // data one row at a time, whereas the MatrixBuilder operates using
            // columns
            MatrixBuilder termSenseDocMatrix = 
                Matrices.getMatrixBuilderForSVD(true);
            
            LOGGER.fine("Analyzing term contexts");
            Map<String,File> termToContexts = generateWordContexts4(properties);
            for (Map.Entry<String,File> e : termToContexts.entrySet()) {
                String term = e.getKey();
                int termIndex = termToIndex.get(term);
                File contexts = e.getValue();
                int[] clusterAssignment = clusterContexts(contexts);
                BitSet bs = new BitSet();
                // Count how many different clusters are present
                for (int i : clusterAssignment)
                    bs.set(i);
                int numSenses = bs.cardinality();
                LOGGER.fine("Rewriting matrix based on the " + numSenses +
                            " discovered senses of " + term);

                // Get the original vector for the term, which will be divided
                // up into several rows based on the senses
                double[] originalRow = termDocMatrix.getRow(termIndex);

                // Special case if the word only has one sense to avoid
                // traversing the row for the index list unnecessarily
                if (numSenses == 1) {
                    // Write the row to file
                    termSenseDocMatrix.addColumn(originalRow);
                    continue;
                }

                // Since we will be making multiple passes, traverse once to
                // find only those indices that will need to be separated into
                // different rows (i.e. those that are non-zero)
                BitSet nonZeroIndices = new BitSet(originalRow.length);
                for (int i = 0; i < originalRow.length; ++i) {
                    if (originalRow[i] != 0)
                        nonZeroIndices.set(i);
                }

                // For each of the senses, output a row with the occurrences for
                // that sense
                for (int sense = 0; sense < numSenses; ++sense) {
                    SparseArray<Integer> termSenseVector = 
                        new SparseIntHashArray();
                    for (int context = 0, occurrenceIndex = -1; 
                             context < clusterAssignment.length; ++context) {
                        // Identify which index in the row vector to which this
                        // context refers
                        occurrenceIndex = 
                            nonZeroIndices.nextSetBit(occurrenceIndex);
                        // If the assignment for this document occurrence
                        // matches with the sense of the row that is currently
                        // being written, then mark the value
                        if (clusterAssignment[context] == sense) {
                            termSenseVector.set(occurrenceIndex,
                                (int)originalRow[occurrenceIndex]);
                        }
                    }
                    // Write the sense-specific vector to the matrix
                    termSenseDocMatrix.addColumn(termSenseVector);
                    // Add the row's index as a sense vector for the term
                    termToSenseIndex.put(term + "-" + sense, senseMatrixRow++);
                }
                LOGGER.fine("finished writing all " + numSenses + " senses " +
                            "for " + term);
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * Reads in the finalized term document matrix as a memory mapped file and
     * converts it to a {@code Matrix} instance.
     *
     * @return the term-document matrix
     */
    private Matrix getTermDocMatrix() throws IOException {
        FileInputStream fis = new FileInputStream(documentContexts);
        FileChannel fc = fis.getChannel();
        IntBuffer docContextsBuf = 
            fc.map(MapMode.READ_ONLY, 0, fc.size()).asIntBuffer();
        fc.close();

        int numDocs = documentCounter.get();
        Matrix termDocMatrix = Matrices.create(termIndexCounter, numDocs, 
                                               Matrix.Type.SPARSE_ON_DISK);
        System.out.printf("size: %d x %d%n", termIndexCounter, numDocs);
        for (int doc = 0; doc < numDocs; ++doc) {
            // Find out how many different words appeared in the document
            int documentContextSize = docContextsBuf.get();
            for (int i = 0; i < documentContextSize; ++i) {
                int termIndex = docContextsBuf.get();
                int occurrences = docContextsBuf.get();
                //System.out.println(termIndex + " " + doc);
                termDocMatrix.set(termIndex, doc, occurrences);
            }
        }
        fis.close();
        fc.close();
        return termDocMatrix;
    }

    /**
     * multiple open files; buffering
     */
    private Map<String,File> generateWordContexts4(Properties properties) 
        throws IOException {

        // For the terms that were not to be analyzed, remove them from the
        // excluded set prior to the full analysis.  This ensure that any calls
        // to getIndexFor() will not return null.
        final Set<String> notPresentInCorpus = new HashSet<String>();
        for (String notAnalyzed : excludeFromAnalysis)
            if (!termToIndex.containsKey(notAnalyzed))
                notPresentInCorpus.add(notAnalyzed);
        excludeFromAnalysis.removeAll(notPresentInCorpus);


        final ConcurrentMap<String,File> termToContextMatrix = 
            new ConcurrentHashMap<String,File>();
        
        FileChannel fc = new FileInputStream(documentContexts).getChannel();
        final IntBuffer contextBuffer = 
            fc.map(FileChannel.MapMode.READ_ONLY, 0, fc.size()).asIntBuffer();
        fc.close();
        
        double corpusWordCount = 0;
        for (AtomicInteger i : termFrequency)
            corpusWordCount += i.get();
        
        final String[] indexToTerm = new String[termToIndex.size()];
        final double[] indexToGfIdf = new double[termToIndex.size()];
        for (Map.Entry<String,Integer> e : termToIndex.entrySet()) {
            String term = e.getKey();
            int index  = e.getValue();
            indexToTerm[index] = term;
            double gf = termFrequency.get(index).get() / corpusWordCount;
            double idf = documentCounter.get()
                / termDocFrequency.get(index).get();
            indexToGfIdf[index] = gf * idf;
        }

        final BitSet indicesToDiscount = new BitSet(excludeFromAnalysis.size());
        for (String excluded : excludeFromAnalysis)
            indicesToDiscount.set(termToIndex.get(excluded));

        // Calculate the size of each distribution vector 
        final int distributionSize = firstOrderCoOccurrences.getVectorLength();
        // Generate a work queue and threads for processing all of the
        // clustering tasks.  These daemon threads dequeue tasks that each build
        // the second-order context vectors for a single term.
        BlockingQueue<Runnable> workQueue = new LinkedBlockingQueue<Runnable>();
        for (int i = 0; i < Runtime.getRuntime().availableProcessors(); ++i)
            new WorkerThread(workQueue).start();
        // Create a semaphore with no available permits.  Permits will stand for
        // word clustering tasks that have been issued.  Once all the valid
        // tasks have been issued in the following for-loop, try to acquire the
        // permits for each task.
        final Semaphore termWithCompeteContexts = new Semaphore(0);

        int termsToProcess = 0;
        for (int termIndex = 0; termIndex < termToIndex.size(); ++termIndex) {

            final int curTermIndex = termIndex;            
            final String curTerm = indexToTerm[curTermIndex];

            // Skip terms that are not words
            if (!curTerm.matches("[a-zA-Z]+") || 
                excludeFromAnalysis.contains(curTerm))
                continue;            
            
            // If the term was a word, mark that this will be one more term
            // cluster task on whom the main thread must wait
            termsToProcess++;

            Runnable termClusteringTask = new Runnable() {
                public void run() {
                    try {
                        process();
                    } catch (IOException ioe) {
                        throw new Error(ioe);
                    }
                }

                public void process() throws IOException {
                    // create a local copy of the context buffer for this
                    // thread, which allows us to iterate independently
                    IntBuffer contexts = contextBuffer.slice();
                                        
                    LOGGER.fine(String.format(
                                "generating second-order context file " 
                                + "for %s (%d/%d)",
                                curTerm, curTermIndex, termToIndex.size()));
                    
                    ContextWriter cw = new ContextWriter(curTerm);
                    
                    while (contexts.remaining() > 0) {
                        
                        // Find out how many different words appeared in the
                        // document
                        int documentContextSize = contexts.get();
                        boolean curTermSeen = false;
                        SparseArray<Integer> context = new SparseIntHashArray();
                        for (int i = 0; i < documentContextSize; ++i) {
                            int tIndex = contexts.get();
                            int occurrences = contexts.get();
                            // Skip counting indices that are excluded from the
                            // analysis
                            if (indicesToDiscount.get(tIndex))
                                continue;
                            curTermSeen = 
                                curTermSeen || tIndex == curTermIndex;
                            context.set(tIndex, occurrences);
                        }
                        
                        // Don't bother building the context vector if the
                        // current term was not in the document
                        if (!curTermSeen)
                            continue;
                        
                        // Create a vector for the second order co-occurrences
                        int[] documentContext = new int[distributionSize];
                        
                        for (int tIndex : context.getElementIndices()) {
                            String term = indexToTerm[tIndex];
                            int occurrences = context.get(tIndex);
                            
                            Vector firstOrder = 
                                firstOrderCoOccurrences.getVector(term);
                            // Sum the first-order co-occurrences, weighting
                            // each by the number of times that word occurred
                            // in the document
                            if (firstOrder instanceof SparseVector) {
                                SparseVector sv = (SparseVector)firstOrder;
                                for (int j : sv.getNonZeroIndices()) {
                                    documentContext[j] += 
                                        (int)(firstOrder.get(j)) 
                                        * occurrences
                                        * indexToGfIdf[j];
                                }
                            }
                            else {
                                for (int j = 0; j < firstOrder.length(); ++j) {
                                    documentContext[j] += 
                                        (int)(firstOrder.get(j)) 
                                        * occurrences
                                        * indexToGfIdf[j];
                                }
                            }
                        }
                        cw.addContext(documentContext);
                    }
                    termToContextMatrix.put(curTerm, cw.finish());
                    termWithCompeteContexts.release();
                }
                };
            
            workQueue.offer(termClusteringTask);            
        }
        
        while (true) {
            try {
                System.out.println("waiting to process " + termsToProcess 
                                   + " terms");
                termWithCompeteContexts.acquire(termsToProcess);
                break;
            } catch (InterruptedException ie) { }
        }
        
        return termToContextMatrix;
    }



    private int[] clusterContexts(File termContexts) throws IOException {
        return HierarchicalAgglomerativeClustering.
            clusterRows(termContexts, MatrixIO.Format.DENSE_TEXT,
                        minContextSimilarity, 
                        ClusterLinkage.MEAN_LINKAGE);
    }

    private class ContextWriter {

        private static final int MAX_BUFFER_LENGTH = 1024 * 1024;

        private final File backingFile;

        public StringBuilder textBuffer;

        public ContextWriter(String word) throws IOException {
            backingFile = File.createTempFile("ht_" + word, ".contexts",
                                              contextFileDir);
            textBuffer = new StringBuilder();
        }

        public void addContext(int[] documentContext) throws IOException {
            for (int col = 0; col < documentContext.length; ++col) {
                int val = documentContext[col];
                textBuffer.append(val);
                if (col + 1 < documentContext.length)
                    textBuffer.append(" ");
                else
                    textBuffer.append("\n");
            }
            if (textBuffer.length() > MAX_BUFFER_LENGTH)
                flush();
        }

        public File finish() throws IOException {
            flush();
            return backingFile;
        }

        private void flush() throws IOException {
            PrintWriter pw = new PrintWriter(
                new BufferedOutputStream(
                    new FileOutputStream(backingFile, true)));
            pw.print(textBuffer.toString());
            pw.close();
            textBuffer = new StringBuilder();
        }
    }

    /**
     * A daemon thread that continuously dequeues {@code Runnable} instances
     * from a queue and executes them.
     */
    protected static final class WorkerThread extends Thread {

        static int threadInstanceCount;

        private final BlockingQueue<Runnable> workQueue;

        public WorkerThread(BlockingQueue<Runnable> workQueue) {
            this.workQueue = workQueue;
            setDaemon(true);
            synchronized(WorkerThread.class) {
                setName("WordComparator-WorkerThread-"
                        + (threadInstanceCount++));
            }
        }

        public void run() {
            while (true) {
                try {
                    Runnable r = workQueue.take();
                    r.run();
                } catch (InterruptedException ie) {
                    throw new Error(ie);
                }
            }
        }
    }

}
