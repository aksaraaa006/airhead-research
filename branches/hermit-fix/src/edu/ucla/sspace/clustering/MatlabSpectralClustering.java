/*
 * Copyright 2009 Keith Stevens 
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

package edu.ucla.sspace.clustering;

import edu.ucla.sspace.matrix.Matrix;
import edu.ucla.sspace.matrix.SparseMatrix;
import edu.ucla.sspace.matrix.Matrix.Type;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.IOError;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;

import java.util.Properties;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.StaticSemanticSpace;
import edu.ucla.sspace.matrix.Matrices;
import edu.ucla.sspace.vector.CompactSparseVector;
import edu.ucla.sspace.vector.DoubleVector;
import edu.ucla.sspace.vector.Vectors;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Set;


/**
 * A wrapper for a matlab based implementation of Spectral Clustering
 *
 * @author Keith Stevens
 */
public class MatlabSpectralClustering implements OfflineClustering {

    public static final String PROPERTY_PREFIX =
        "edu.ucla.sspace.clustering.MatlabSpectralClustering";

    public static final String THRESHOLD_PROPERTY = 
        PROPERTY_PREFIX + ".threshold";

    public static final String DEFAULT_THRESHOLD = ".50";

    public static final String OCTAVE_CODE = 
        "T = cluster_spectral_ckvw_make_tree(A);\n" +
        "z = cluster_spectral_ckvw_merge_on_correlation(A, T, %f)';\n" +
        "save('-ascii', '%s', 'z');\n";

    private final double similarityThreshold;

    public MatlabSpectralClustering() {
        this(System.getProperties());
    }

    public MatlabSpectralClustering(Properties props) {
        similarityThreshold = Double.parseDouble(props.getProperty(
                    THRESHOLD_PROPERTY, DEFAULT_THRESHOLD));
    }

    public int[] cluster(Matrix m) {
        boolean isSparse = (m instanceof SparseMatrix);
        try {
            File dataMatrix = File.createTempFile("ZMAT", ".dat");
            if (isSparse)
                MatrixIO.writeMatrix(m, dataMatrix, Format.MATLAB_SPARSE);
            else
                MatrixIO.writeMatrix(m, dataMatrix, Format.DENSE_TEXT);
            //dataMatrix.deleteOnExit();

            File clusterOut =
                File.createTempFile("octave-cluster-assignment",".dat");
            //clusterOut.deleteOnExit();

            File octaveFile =
                File.createTempFile("octave-spectral-clustering",".m");
           // octaveFile.deleteOnExit();

            PrintWriter pw = new PrintWriter(octaveFile);
            pw.println("addpath('lib/matlab/')");
            pw.printf("load '%s'\n", dataMatrix.getAbsolutePath());
            String matrixName = dataMatrix.getName().split("\\.")[0];

            if (isSparse)
                pw.printf("A = spconvert(%s);\n", matrixName);
            else 
                pw.printf("A = %s;\n", matrixName);

            pw.printf(OCTAVE_CODE,
                      similarityThreshold,
                      clusterOut.getAbsolutePath());
            pw.close();

            String commandLine = "octave " + octaveFile.getAbsolutePath();
            Process octave = Runtime.getRuntime().exec(commandLine);

            BufferedReader stdout = new BufferedReader(
                    new InputStreamReader(octave.getInputStream()));
            BufferedReader stderr = new BufferedReader(
                    new InputStreamReader(octave.getErrorStream()));

            StringBuilder output = new StringBuilder("Octave output:\n");
            for (String line = null; (line = stdout.readLine()) != null; ) 
                output.append(line).append("\n");

            int exitStatus = octave.waitFor();

            if (exitStatus == 0) {
                Matrix clusterAssignment= MatrixIO.readMatrix(
                        clusterOut, Format.DENSE_TEXT, Type.DENSE_IN_MEMORY);
                int[] assignments = new int[m.rows()];
                for (int i = 0; i < clusterAssignment.columns(); ++i)
                    assignments[i] = (int) clusterAssignment.get(0, i);
                return assignments;
            } else {
                System.out.println("wtf");
            }
        } catch (IOException ioe) {
            throw new IOError(ioe);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) throws Exception {
        SemanticSpace sspace = new StaticSemanticSpace(args[0]);
        List<DoubleVector> vectors = new ArrayList<DoubleVector>();

        Set<String> words = sspace.getWords();
        int i = 0;
        for (String word : words) {
            if (i == 100)
                break;
            i++;
            vectors.add(Vectors.asDouble(sspace.getVector(word)));
        }

        OfflineClustering clustering = null;
        clustering = new MatlabSpectralClustering();
        int[] assignments =
            clustering.cluster(Matrices.asMatrix(vectors));

        i = 0;
        for (String word : words) {
            if (i == 100)
                break;
            i++;
            System.out.printf("%s %d\n", word, assignments[i++]);
        }
    }
}
