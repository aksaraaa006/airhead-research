

package edu.ucla.sspace.matrix;

import java.io.File;
import java.io.IOException;


/**
 * @author Keith Stevens
 */
public class DenseTextFileTransformerTest extends FileTransformerTestBase {

    MatrixIO.Format format() {
        return MatrixIO.Format.DENSE_TEXT;
    }

    FileTransformer fileTransformer() {
        return new DenseTextFileTransformer();
    }
}
