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

package edu.ucla.sspace.matrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOError;
import java.io.IOException;
import java.io.PrintWriter;


/**
 * A {@code FileTransformer} for matrix files in the {@link
 * Format#DENSE_TEXT} format.
 *
 * @author Keith Stevens
 */
class DenseTextFileTransformer implements FileTransformer {

    /**
     * {@inheritDoc}
     */
    public File transform(File inputFile,
                          File outFile,
                          GlobalTransform transform) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(inputFile));
            PrintWriter writer = new PrintWriter(new BufferedWriter(
                        new FileWriter(outFile)));

            String line = null;
            // Traverse each row.
            for (int row = 0; (line = br.readLine()) != null; ++row) {
                // Traverse each entry in the matrix and transform the value for
                // the new matrix.
                String[] values = line.split("\\s+");
                StringBuilder sb = new StringBuilder(values.length * 4);
                for (int col = 0; col < values.length; ++col) {
                    double value = Double.parseDouble(values[col]);
                    if (value != 0d)
                        sb.append(transform.transform(row, col, value));
                    else
                        sb.append(value);
                    sb.append(" ");
                }
                writer.println(sb.toString());
            }

            writer.close();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }

        return outFile;
    }
}
