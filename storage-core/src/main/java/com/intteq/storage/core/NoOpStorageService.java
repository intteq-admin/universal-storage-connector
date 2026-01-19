package com.intteq.storage.core;

import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;
import com.intteq.storage.core.exception.StorageException;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.util.Map;

/**
 * Fallback {@link ObjectStorageService} used when no real
 * storage provider is configured.
 *
 * <p>This implementation allows the application to start,
 * but fails fast when storage operations are attempted.
 */
@Slf4j
public class NoOpStorageService implements ObjectStorageService {

    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        log.error("Storage operation attempted but no storage provider is configured");
        throw new PreSignedUrlGenerationException(
                "Storage is not configured. Please configure storage.provider and provider-specific properties."
        );
    }

    @Override
    public String getFileUrl(String directory, String fileName, Duration expiry) {
        log.error("File URL requested but no storage provider is configured");
        throw new IllegalStateException(
                "Storage is not configured. File access is unavailable."
        );
    }

    @Override
    public void delete(String directory, String fileName) {
        log.error("Delete operation attempted but no storage provider is configured");
        throw new StorageDeleteException(
                "Storage is not configured. Delete operation is unavailable."
        );
    }

    @Override
    public String upload(
            String directory,
            String fileName,
            byte[] content,
            String contentType,
            Map<String, String> metadata
    ) {
        log.error("Upload attempted but no storage provider is configured");
        throw new StorageException(
                "Storage is not configured. Upload operation is unavailable."
        );
    }

    @Override
    public boolean exists(String directory, String fileName) {
        log.error("Exists check attempted but no storage provider is configured");
        throw new StorageException(
                "Storage is not configured. Exists check is unavailable."
        );
    }


}
