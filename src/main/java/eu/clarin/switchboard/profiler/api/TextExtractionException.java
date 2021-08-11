package eu.clarin.switchboard.profiler.api;

public class TextExtractionException extends Exception {
    public TextExtractionException(Throwable cause) {
        super(cause);
    }

    public TextExtractionException(String message) {
        super(message);
    }

    public TextExtractionException(String message, Throwable cause) {
        super(message, cause);
    }
}
