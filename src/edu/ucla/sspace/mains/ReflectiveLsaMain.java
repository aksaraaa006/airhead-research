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

package edu.ucla.sspace.mains;

import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.SemanticSpaceIO.SSpaceFormat;

import edu.ucla.sspace.rlsa.ReflectiveLatentSemanticAnalysis;

import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

/**
 * An executable class for running {@link ReflectiveLatentSemanticAnalysis}
 * (LSA) from the command line.  
 *
 * <p>This class is desgined to run multi-threaded and performs well with one
 * thread per core, which is the default setting.
 *
 * @see ReflectiveLatentSemanticAnalysis
 * @see edu.ucla.sspace.matrix.Transform Transform
 *
 * @author David Jurgens
 */
public class ReflectiveLsaMain extends GenericMain {
    private ReflectiveLsaMain() {
    }

    /**
     * Adds all of the options to the {@link ArgOptions}.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('n', "dimensions", 
                          "the number of dimensions in the semantic space",
                          true, "INT", "Algorithm Options"); 
        options.addOption('p', "preprocess", "a MatrixTransform class to "
                          + "use for preprocessing", true, "CLASSNAME",
                          "Algorithm Options");
        options.addOption('S', "svdAlgorithm", "a specific SVD algorithm to use"
                          , true, "SVD.Algorithm", 
                          "Advanced Algorithm Options");
    }

    public static void main(String[] args) {
        ReflectiveLsaMain lsa = new ReflectiveLsaMain();
        try {
            lsa.run(args);
        }
        catch (Throwable t) {
            t.printStackTrace();
        }
    }
    
    protected SemanticSpace getSpace() {
        return new ReflectiveLatentSemanticAnalysis();
    }

    /**
     * Returns the {@likn SSpaceFormat.BINARY binary} format as the default
     * format of a {@code ReflectiveLatentSemanticAnalysis} space.
     */
    protected SSpaceFormat getSpaceFormat() {
        return SSpaceFormat.BINARY;
    }

    protected Properties setupProperties() {
        // use the System properties in case the user specified them as
        // -Dprop=<val> to the JVM directly.
        Properties props = System.getProperties();

        if (argOptions.hasOption("dimensions")) {
            props.setProperty(ReflectiveLatentSemanticAnalysis
                                  .RLSA_DIMENSIONS_PROPERTY,
                              argOptions.getStringOption("dimensions"));
        }

        if (argOptions.hasOption("preprocess")) {
            props.setProperty(ReflectiveLatentSemanticAnalysis
                                  .MATRIX_TRANSFORM_PROPERTY,
                              argOptions.getStringOption("preprocess"));
        }

        if (argOptions.hasOption("svdAlgorithm")) {
            props.setProperty(ReflectiveLatentSemanticAnalysis
                                  .RLSA_SVD_ALGORITHM_PROPERTY,
                              argOptions.getStringOption("svdAlgorithm"));
        }

        return props;
    }

    /**
     * {@inheritDoc}
     */
    protected String getAlgorithmSpecifics() {
        return 
            "The --svdAlgorithm provides a way to manually specify which " + 
            "algorithm should\nbe used internally.  This option should not be" +
            " used normally, as RLSA will\nselect the fastest algorithm " +
            "available.  However, in the event that it\nis needed, valid" +
            " options are: SVDLIBC, MATLAB, OCTAVE, JAMA and COLT";
    }
}
