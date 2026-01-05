package com.intteq.storage.core.exception;

/**
 * Thrown when deleting an object from storage fails.
 */
public class StorageDeleteException extends StorageException {

    public StorageDeleteException(String message, Throwable cause) {
        super(message, cause);
    }
    public StorageDeleteException(String message) {
        super(message);
    }
}
