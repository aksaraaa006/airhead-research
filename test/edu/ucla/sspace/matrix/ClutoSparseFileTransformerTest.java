

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class ClutoSparseFileTransformerTest extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.CLUTO_SPARSE;
    }

    FileTransformer fileTransformer() {
        return new ClutoSparseFileTransformer();
    }
}
