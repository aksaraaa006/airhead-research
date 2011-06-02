

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class SvdlbcSparseTextFileTransformerTest
        extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.SVDLIBC_SPARSE_TEXT;
    }

    FileTransformer fileTransformer() {
        return new SvdlbcSparseTextFileTransformer();
    }
}
