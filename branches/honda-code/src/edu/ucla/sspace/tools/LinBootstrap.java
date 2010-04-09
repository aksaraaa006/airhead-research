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

class LinBootstrap implements SemanticSpace {

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

        LinBootstrap lbs = new LinBootstrap(sspace,Similarity.SimType.COSINE,
                                            0.2,0.5);
                                            //0,0);

        try {
            SemanticSpaceIO.save(lbs, file, SemanticSpaceIO.SSpaceFormat.SPARSE_BINARY);
        } catch(Throwable e) {
            System.out.println("bad times: could not save file");
        }
    
        return;
    }

    private Map<String,SparseVector> word_set;
    private int nfeats;
    private static final Logger logger = Logger.getLogger(LinBootstrap.class.getName());

    // constructor must take semantic space as input
    public LinBootstrap(SemanticSpace sspace, Similarity.SimType simType,
                        double theta_weight, double theta_sim) {
        WordComparator wc = new WordComparator();
        word_set = new HashMap<String,SparseVector>();
        // find number of features
        nfeats = sspace.getVectorLength();

        // calculate the similarity for each word
        // order: n*n*O(sim)
        int count_word=1;
        for (String word : sspace.getWords()) {

            // calculate similarity between each neighbor
            SortedMultiMap<Double,String> sim =
                wc.getMostSimilar(word, sspace, sspace.getWords().size(), simType);

            // create a set of neighbors with their respective similarities
            Map<String,Double> neighbors = new HashMap<String,Double>();
            for (Map.Entry<Double,String> e : sim.entrySet()) {
                if(e.getKey() > theta_sim) {
                    neighbors.put(e.getValue(), e.getKey());
                }
                else {
                    // we can do this because we know the keyset is sorted
                    //break;
                }
            }

            logger.fine(count_word + "/" + sspace.getWords().size() + "\t" + word + ":" + neighbors.size());

//DEBUG
double dbg_sum=0.0d;
//logger.fine("nNeighbors : " + neighbors.size());
            // sum the similarity over all words with active features that
            //  are also neighbors
            double[] new_weights = new double[nfeats];
            Arrays.fill(new_weights,0.0d);
            for (String neigh : neighbors.keySet()) {

                for (int i=0; i<nfeats; i++) {
                    // test for active feature set
                    double neigh_feat = sspace.getVector(neigh).getValue(i).doubleValue();
                    double word_feat = sspace.getVector(word).getValue(i).doubleValue();
                    if((word_feat > theta_weight) && (neigh_feat > theta_weight)) {
dbg_sum+=1.0d;
                        //new_weights[i] += feat;
                        new_weights[i] += neighbors.get(neigh);
//logger.fine("features: " + new_weights[i]);
                    }
                }
            }
logger.fine("active feature percentage: " + dbg_sum/(nfeats*neighbors.size())*100);
            word_set.put(word,new CompactSparseVector(new_weights));
            count_word++;
// logging stuff
//if (neighbors.size()>0) {
//    logger.fine("vector: " + Arrays.toString(word_set.get(word).getNonZeroIndices()));
//}

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
        return "LinBootstrap-semantic-space";
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processDocument(BufferedReader document) {
        throw new UnsupportedOperationException(
            "LinBootstrap instances cannot be updated");
    }

    /**
     * Not supported; throws an {@link UnsupportedOperationException} if called.
     *
     * @throws an {@link UnsupportedOperationException} if called
     */
    public void processSpace(Properties properties) {
        throw new UnsupportedOperationException(
            "LinBootstrap instances cannot be updated");
    }

}
