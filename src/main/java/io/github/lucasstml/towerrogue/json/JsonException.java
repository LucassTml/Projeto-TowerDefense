package io.github.lucasstml.towerrogue.json;

/** Thrown for malformed JSON text or when a typed accessor finds a missing/mistyped value. */
public class JsonException extends RuntimeException {
    public JsonException(String message) {
        super(message);
    }
}
