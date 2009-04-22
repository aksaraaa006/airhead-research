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
    
}