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

package edu.ucla.sspace.lsa;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import java.nio.channels.FileChannel;

/**
 * Performs no transform on the input matrix.
 */
public class NoTransform implements MatrixTransformer {

    public File transform(File matrixInput) {
	return matrixInput;
    }

    public void transform(File matrixInput, File matrixOutput) 
	    throws IOException {
        FileChannel original = new FileInputStream(matrixInput).getChannel();
    
        FileChannel copy = new FileOutputStream(matrixOutput).getChannel();
    
	// Duplicate the contents of the input matrix in the provided file
        copy.transferFrom(original, 0, original.size());
    
        original.close();
        copy.close();
    }

    public String toString() {
	return "no";
    }
    
}