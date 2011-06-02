

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class MatlabSparseFileTransformerTest extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.MATLAB_SPARSE;
    }

    FileTransformer fileTransformer() {
        return new MatlabSparseFileTransformer();
    }
}
