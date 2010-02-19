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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.beagle.Beagle;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.index.DoubleVectorGenerator;
import edu.ucla.sspace.index.DoubleVectorGeneratorMap;
import edu.ucla.sspace.index.GaussianVectorGenerator;

import edu.ucla.sspace.util.SerializableUtil;

import java.io.File;

import java.util.Properties;


/**
 * An executable class for running {@link Beagle} from the
 * command line.  This class takes in several command line arguments.
 *
 * <ul>
 * <li> {@code --dimensions=<int>} how many dimensions to use for the Beagle
 *      vectors.  512 is the default value.
 * </ul>
 *
 * <p>
 *
 * An invocation will produce one file as output {@code
 * beagle-semantic-space.sspace}.  If {@code overwrite} was set to {@code true},
 * this file will be replaced for each new semantic space.  Otherwise, a new
 * output file of the format {@code beagle-semantic-space<number>.sspace} will
 * be created, where {@code <number>} is a unique identifier for that program's
 * invocation.  The output file will be placed in the directory specified on the
 * command line.
 *
 * <p>
 *
 * This main will run with with multiple threads by default.
 *
 * @see Beagle
 *
 * @author Keith Stevens 
 */
public class BeagleMain extends GenericMain {

    /**
     * If no dimension size is given, this will be the size of index vectors and
     * semantic vectors.
     */
    private static final int DEFAULT_DIMENSION = 2048;

    /**
     * The dimensionality of each index and semantic vector.
     */
    private int dimension;

    /**
     * The generator map for automatically creating and retrieving index vectors
     * for beagle.
     */
    private DoubleVectorGeneratorMap generatorMap;

    private BeagleMain() {
    }

    public static void main(String[] args) {
        BeagleMain hMain = new BeagleMain();
        try {
            hMain.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    /**
     * {@inheritDoc}
     */
    public void addExtraOptions(ArgOptions options) {
        options.addOption('n', "dimension",
                          "the length of each beagle vector",
                          true, "INT", "Options");
        options.addOption('S', "saveVectors", 
                          "save word-to-IndexVector mapping after processing",
                          true, "FILE", "Options");
        options.addOption('L', "loadVectors",
                          "load word-to-IndexVector mapping before processing",
                          true, "FILE", "Options");
    }

    /**
     * {@inheritDoc}
     */
    public void handleExtraOptions() {
        dimension = (argOptions.hasOption("dimension"))
          ? argOptions.getIntOption("dimension")
          : DEFAULT_DIMENSION;
        if (argOptions.hasOption("loadVectors")) {
            generatorMap = SerializableUtil.load(
                    new File(argOptions.getStringOption("loadVectors")),
                    DoubleVectorGeneratorMap.class);
        } else {
            double stdev = 1 / Math.sqrt(dimension);
            System.setProperty(
                    GaussianVectorGenerator.STANDARD_DEVIATION_PROPERTY,
                    Double.toString(stdev));
            DoubleVectorGenerator generator = new GaussianVectorGenerator();
            generatorMap = new DoubleVectorGeneratorMap(generator, dimension);
        }
    }

    /**
     * {@inheritDoc}
     */
    protected void postProcessing() {
        if (argOptions.hasOption("saveVectors")) {
            SerializableUtil.save(
                    generatorMap,
                    new File(argOptions.getStringOption("saveVectors")));
        }
    }

    /**
     * {@inheritDoc}
     */
    public SemanticSpace getSpace() {
        return new Beagle(dimension, generatorMap);
    }

    /**
     * {@inheritDoc}
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.BINARY;
    }
}
