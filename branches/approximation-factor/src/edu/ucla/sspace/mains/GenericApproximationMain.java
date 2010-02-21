/*
 * Copyright 2010 Keith Stevens 
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

import edu.ucla.sspace.common.ApproximationSpace;
import edu.ucla.sspace.common.ArgOptions;
import edu.ucla.sspace.common.SemanticSpace;

import edu.ucla.sspace.util.SerializableUtil;

import edu.ucla.sspace.vector.Vector;

import java.io.File;

import java.util.Map;

import java.util.logging.Logger;


public abstract class GenericApproximationMain<T extends Vector>
        extends GenericMain {

    private static final Logger LOGGER =
        Logger.getLogger(GenericApproximationMain.class.getName());

    /**
     * The {@link ApproximationSpace} instance used by this runnable.  This
     * variable is assigned after {@link #getSpace()} is called.
     */
    private ApproximationSpace<T> space;

    public GenericApproximationMain() {
        super();
    }

    public GenericApproximationMain(boolean isMultiThreaded) {
        super(isMultiThreaded);
    }

    /**
     * {@inheritDoc}
     *
     * </p>
     *
     * Adds options for serializing and deserializing index vectors.
     */
    protected void addExtraOptions(ArgOptions options) {
        options.addOption('S', "saveVectors",
                          "save word-to-IndexVector mapping after processing",
                          true, "FILE", "Algorithm Options");
        options.addOption('L', "loadVectors",
                          "load word-to-IndexVector mapping before processing",
                          true, "FILE", "Algorithm Options");
    }

    /**
     * Returns an instance of an {@link ApproximationSpace}.  This method is
     * called after {@link #setupProperties} and after the argument options have
     * been processed, so implementations may assume values set by those methods
     * are avaiable.
     */
    abstract protected ApproximationSpace<T> getApproximationSpace();

    /**
     * Returns the token to index vector map to use when the {@code
     * --loadVectors} option is not provided.  This method is called after
     *  {@link #setupProperties} and after the argument options have been
     *  processed, so implementations may assume values set by those methods are
     *  avaiable.
     */
    abstract protected Map<String, T> getDefaultMap();

    /**
     * Returns an instance of a {@link SemanticSpace}.  If {@code
     * --loadVectors} is specified in the command line options, this method will
     *  also initialize the word-to-{@link Vector} mapping.  This method is
     *  called after {@link #setupProperties} and after the argument options
     *  have been processed so values set by those steps are safe to use in this
     *  method.
     */
    protected SemanticSpace getSpace() {
        space = getApproximationSpace();

        if (argOptions.hasOption("loadVectors")) {
            String fileName = argOptions.getStringOption("loadVectors");
            LOGGER.info("loading index vectors from " + fileName);
            Map<String,T> wordToIndexVector =
                SerializableUtil.load(new File(fileName));
            space.setWordToIndexVector(wordToIndexVector);
        } else
            space.setWordToIndexVector(getDefaultMap());

        return space;
    }

    /**
     * If {@code --saveVectors} was specified, write the accumulated
     * word-to-{@link Vector} vector mapping to file.
     */
    @Override protected void postProcessing() {
        if (argOptions.hasOption("saveVectors")) {
            String fileName = argOptions.getStringOption("saveVectors");
            LOGGER.info("saving index vectors to " + fileName);
            SerializableUtil.save(space.getWordToIndexVector(),
                                  new File(fileName));
        }
    }
}
