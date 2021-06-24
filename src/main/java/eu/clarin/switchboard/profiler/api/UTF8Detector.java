package eu.clarin.switchboard.profiler.api;

import java.io.File;
import java.io.IOException;

public interface UTF8Detector {
    /**
     * Checks if the text encoding is UTF8
     *
     * @param file the input data container
     * @return true if the file is UTF8 (or ASCII which is a subset of UTF8)
     * @throws IOException if the file IO fails
     */
    boolean isUTF8(File file) throws IOException;
}
