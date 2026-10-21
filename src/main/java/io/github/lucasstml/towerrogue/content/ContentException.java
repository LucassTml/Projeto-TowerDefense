package io.github.lucasstml.towerrogue.content;

/** Invalid or inconsistent content data. Raised at startup, never mid-run. */
public class ContentException extends RuntimeException {
    public ContentException(String message) {
        super(message);
    }

    public ContentException(String message, Throwable cause) {
        super(message, cause);
    }
}
