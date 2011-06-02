

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class SvdlbcSparseBinaryFileTransformerTest
        extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.SVDLIBC_SPARSE_BINARY;
    }

    FileTransformer fileTransformer() {
        return new SvdlbcSparseBinaryFileTransformer();
    }
}
