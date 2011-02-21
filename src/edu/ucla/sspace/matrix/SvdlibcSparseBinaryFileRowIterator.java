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

package edu.ucla.sspace.matrix;

import edu.ucla.sspace.vector.SparseDoubleVector;
import edu.ucla.sspace.vector.SparseHashDoubleVector;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOError;
import java.io.IOException;
import java.io.RandomAccessFile;

import java.nio.ByteBuffer;

import java.nio.channels.FileChannel;
import java.nio.channels.FileChannel.MapMode;

import java.util.Iterator;
import java.util.NoSuchElementException;


/**
 * An iterator for sequentially accessing the columns of a {@link
 * MatrixIO.Format.SVDLIBC_SPARSE_BINARY} formatted file.
 */
class SvdlibcSparseBinaryFileRowIterator
    implements Iterator<SparseDoubleVector> {

    //private final DataInputStream dis;
    private final ByteBuffer data;

    private SparseDoubleVector next;
    
    /**
     * The entry number that will next be returned from the matrix
     */
    private int entry;

    /**
     * The total number of non-zero entries in the matrix
     */
    private int nzEntriesInMatrix;

    /**
     * The index of the current column
     */
    private int curCol;

    private final int rows;

    private final int cols;

    public SvdlibcSparseBinaryFileRowIterator(File matrixFile) 
            throws IOException {
//         dis = new DataInputStream(
//             new BufferedInputStream(new FileInputStream(matrixFile), 
//                                     1024 * 4));
        RandomAccessFile raf = new RandomAccessFile(matrixFile, "r");
        FileChannel fc = raf.getChannel();
        data = fc.map(MapMode.READ_ONLY, 0, fc.size());
        fc.close();
        this.rows = data.getInt();
        this.cols = data.getInt();
        nzEntriesInMatrix = data.getInt();
        curCol = 0;
        advance();
    }

    private void advance() throws IOException {        
        if (entry >= nzEntriesInMatrix) {
            next = null;
        }
        else {
            next = new SparseHashDoubleVector(rows);
            int nzInCol = data.getInt();
            for (int i = 0; i < nzInCol; ++i, ++entry) {
                int row = data.getInt();
                double value = data.getFloat();
                next.set(row, value);          
            }
            curCol++;
        }
    }

    public boolean hasNext() {
        return next != null;
    }

    public SparseDoubleVector next() {
        if (next == null) 
            throw new NoSuchElementException("No futher entries");
        SparseDoubleVector curCol = next;
        try {
            advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
        return curCol;
    }

    /**
     * Throws an {@link UnsupportedOperationException} if called.
     */
    public void remove() {
        throw new UnsupportedOperationException("Cannot remove from file");
    }

    /**
     * Resets the iterator to the start of the file's data.
     */
    public void reset() {
        data.rewind();
        // read off the rows, columns, and non-zero elements
        data.getInt();
        data.getInt();
        data.getInt();
        curCol = 0;
        entry = 0;
        try {
            advance();
        } catch (IOException ioe) {
            throw new IOError(ioe);
        }
    }
}