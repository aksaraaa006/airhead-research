package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.Similarity;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO;
import edu.ucla.sspace.common.WordComparator;
import edu.ucla.sspace.vector.Vector;
import edu.ucla.sspace.vector.SparseVector;
import edu.ucla.sspace.vector.Vectors;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.util.SortedMultiMap;
import edu.ucla.sspace.util.TreeMultiMap;

import edu.ucla.sspace.mains.LoggerUtil;

import java.io.BufferedReader;
import java.io.File;
import java.util.Properties;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

import java.util.logging.Level;
import java.util.logging.Logger;

class TruncateSSpace implements SemanticSpace {

    public static void main(String args[]) {
        // load sspace (first arg)
        SemanticSpace sspace = null;
        File file = null;
        try {
            file = new File(args[1]);
            sspace = SemanticSpaceIO.load(args[0]);
        } catch (Throwable t) {
            // Catch Throwable since this method may throw an IOError
            System.out.println( "an error occurred while loading the semantic " +
                                "space from " + args[0] + ":\n" + t);
            return;
        }

        System.out.println("size: "+ sspace.getWords().size() + "\nfeats: " + sspace.getVectorLength());

        LoggerUtil.setLevel(Level.FINE);

        TruncateSSpace tss = new TruncateSSpace(sspace,20000);

        try {
            SemanticSpaceIO.save(tss, file, SemanticSpaceIO.SSpaceFormat.SPARSE_BINARY);
        } catch(Throwable e) {
            System.out.println("bad times: could not save file");
        }
    
        return;
    }

    private Map<String,Vector> word_set;
    int nfeats = 0;
    private static final Logger logger = Logger.getLogger(TruncateSSpace.class.getName());

    // constructor must take semantic space as input
    public TruncateSSpace(SemanticSpace sspace, int new_size) {
        WordComparator wc = new WordComparator();
        word_set = new HashMap<String,Vector>();
        nfeats = sspace.getVectorLength();

        // calculate the magnitude of each vector
        SortedMultiMap<Double,String> magnitude = new TreeMultiMap<Double,String>();
        int k=0;
        for (String word : sspace.getWords()) {
            logger.fine("Calc Magnitude: "+k+"/"+sspace.getWords().size());
            Vector v = sspace.getVector(word);
            double sum = 0.0d;
            for (int j=0; j<v.length(); j++) {
                sum += v.getValue(j).doubleValue();
            }
            magnitude.put(sum,word);
            k++;
        }

        // take the top new_size vectors
        int i=0;
        for (Double d : magnitude.keySet()) {
            for (String w : magnitude.get(d)) {
                logger.fine("Constructing: "+i+"/"+new_size);
                word_set.put(w,sspace.getVector(w));
                i++;
                if(!(i<new_size)) break;
            }
            if(!(i<new_size)) break;
        }

        return;
    }

    //Returns the semantic vector for the provided word.
    public Vector getVector(String word) {
        return word_set.get(word);
    }
    //Returns the length of vectors in this semantic space.
    public int getVectorLength() {
        return nfeats;
    }
    //Returns the set of words that are represented in this semantic space.
    public Set<String> getWords() {
        return word_set.keySet();
    }

    public String getSpaceName() {
        return "TruncateSSpace-semantic-space";
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processDocument(BufferedReader document) {
        throw new UnsupportedOperationException(
            "TruncateSSpace instances cannot be updated");
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processSpace(Properties properties) {
        throw new UnsupportedOperationException(
            "TruncateSSpace instances cannot be updated");
    }

}
