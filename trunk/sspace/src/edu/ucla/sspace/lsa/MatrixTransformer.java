package edu.ucla.sspace.lsa;

import java.io.File;
import java.io.IOException;

/**
 * A class that transforms one matrix into another using properites of the
 * matrix itself.  Instances of this interface is most commonly used with
 * post-processing a term-document matrix.
 */
public interface MatrixTransformer {

    /**
     * Transforms the provided input and returns the {@code File} containing the
     * output.  Note that the output maybe the same file as the input.
     */
    File transform(File matrixInput) throws IOException;

    void transform(File matrixInput, File matrixOutput) throws IOException;
    
}