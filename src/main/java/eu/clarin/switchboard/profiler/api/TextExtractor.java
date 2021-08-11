package eu.clarin.switchboard.profiler.api;

import java.io.File;

public interface TextExtractor {
    String extractText(File file, String mediaType) throws TextExtractionException;
}
