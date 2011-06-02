

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class SvdlibcDenseTextFileTransformerTest
        extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.SVDLIBC_DENSE_TEXT;
    }

    FileTransformer fileTransformer() {
        return new SvdlibcDenseTextFileTransformer();
    }
}
