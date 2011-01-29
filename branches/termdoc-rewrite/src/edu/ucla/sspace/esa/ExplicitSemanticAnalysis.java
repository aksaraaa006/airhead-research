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

package edu.ucla.sspace.esa;

import edu.ucla.sspace.common.SemanticSpace;
import edu.ucla.sspace.common.GenericTermDocumentVectorSpace;

import edu.ucla.sspace.matrix.MatrixFile;
import edu.ucla.sspace.matrix.MatrixIO;
import edu.ucla.sspace.matrix.MatrixIO.Format;
import edu.ucla.sspace.matrix.TfIdfTransform;

import java.io.File;
import java.io.IOError;
import java.io.IOException;

import java.util.Properties;

import java.util.concurrent.ConcurrentHashMap;


/**
 * An implementation of Explicit Semanic Analysis proposed by Evgeniy
 * Gabrilovich and Shaul Markovitch.    For full details see:
 *
 * <ul>
 *
 *     <li style="font-family:Garamond, Georgia, serif"> Evgeniy Gabrilovich and
 *         Shaul Markovitch. (2007). "Computing Semantic Relatedness using
 *         Wikipedia-based Explicit Semantic Analysis," Proceedings of The 20th
 *         International Joint Conference on Artificial Intelligence (IJCAI),
 *         Hyderabad, India, January 2007. </li>
 *
 * </ul>
 *
 * @author Keith Stevens 
 */
public class ExplicitSemanticAnalysis extends GenericTermDocumentVectorSpace {

    public static final String ESA_SSPACE_NAME =
        "esa-semantic-space";

    /**
     * Constructs a new {@link ExplicitSemanticAnalysis} instance.
     */
    public ExplicitSemanticAnalysis() throws IOException {
        super(true, new ConcurrentHashMap<String, Integer>());
    }

    /**
     * {@inheritDoc}
     */
    public void processSpace(Properties properties) {
        try {
            MatrixFile processedSpace = processSpace(
                    new TfIdfTransform());
            setWordSpace(MatrixIO.readMatrix(
                        processedSpace.getFile(), processedSpace.getFormat()));
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }

    /**
     * {@inheritDoc}
     */
    public String getSpaceName() {
        return ESA_SSPACE_NAME;
    }
}
