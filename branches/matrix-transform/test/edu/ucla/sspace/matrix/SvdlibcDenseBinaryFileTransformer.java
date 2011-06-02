

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class SvdlibcDenseBinaryFileTransformerTest
        extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.SVDLIBC_DENSE_BINARY;
    }

    FileTransformer fileTransformer() {
        return new SvdlibcDenseBinaryFileTransformer();
    }
}
