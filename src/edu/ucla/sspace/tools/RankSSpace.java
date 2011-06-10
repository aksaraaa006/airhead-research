

package edu.ucla.sspace.tools;

import edu.ucla.sspace.common.*;
import edu.ucla.sspace.common.Similarity.SimType;
import edu.ucla.sspace.matrix.*;
import edu.ucla.sspace.matrix.AffinityMatrixCreator.EdgeType;
import edu.ucla.sspace.matrix.AffinityMatrixCreator.EdgeWeighting;
import edu.ucla.sspace.util.*;
import edu.ucla.sspace.vector.*;

import java.io.*;
import java.util.*;

/**
 * @author Keith Stevens
 */
public class RankSSpace {

    public static void main(String[] args) throws Exception {
        ArgOptions options = new ArgOptions();
        options.addOption('s', "sspace", "The ssapce to rank", 
                          true, "FILE", "Required");
        options.addOption('w', "wordList", "The list of words to rank", 
                          true, "FILE", "Required");
        options.addOption('r', "rankAlg", "The rank algorithm to use", 
                          true, "CLASSNAME", "Required");
        options.addOption('S', "edgeSim", "The min edge similarity to use", 
                          true, "DOUBLE", "Required");
        options.parseOptions(args);

        SemanticSpace sspace = new StaticSemanticSpace(
                options.getStringOption('s'));
        Set<String> words  = new HashSet<String>();
        BufferedReader br = new BufferedReader(new FileReader(
                    options.getStringOption('w')));
        for (String line = null; (line = br.readLine()) != null; )
            words.add(line.trim());

        List<DoubleVector> dataPoints = new ArrayList<DoubleVector>(
                words.size());
        for (String word : words)
            dataPoints.add((DoubleVector) sspace.getVector(word));
        
        MatrixFile affinityMatrixFile = AffinityMatrixCreator.calculate(
                Matrices.asMatrix(dataPoints),
                SimType.COSINE,
                EdgeType.MIN_SIMILARITY,
                options.getDoubleOption('S'),
                EdgeWeighting.COSINE_SIMILARITY,
                0);

        SparseMatrix affinityMatrix = (SparseMatrix) affinityMatrixFile.load();

        MatrixRank rank = ReflectionUtil.getObjectInstance(
                options.getStringOption('r'));

        DoubleVector ranks = rank.rankMatrix(affinityMatrix,
                                             rank.defaultRanks(affinityMatrix));
        int i = 0;
        for (String word : words)
            System.out.printf("%s %f\n", word, ranks.get(i++));
    }
}
