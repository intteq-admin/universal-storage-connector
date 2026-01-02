package com.intteq.storage.core.exception;

/**
 * Thrown when a pre-signed upload URL cannot be generated.
 */
public class PreSignedUrlGenerationException extends StorageException {

    public PreSignedUrlGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}