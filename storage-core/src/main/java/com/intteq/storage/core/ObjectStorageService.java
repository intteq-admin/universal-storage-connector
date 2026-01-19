package com.intteq.storage.core;

import java.time.Duration;
import java.util.Map;

/**
 * Abstraction for object storage providers such as
 * Amazon S3, Azure Blob Storage, and Google Cloud Storage.
 */
public interface ObjectStorageService {

    /**
     * Generates a pre-signed URL that allows a client
     * to upload a file directly to object storage.
     *
     * @param directory   logical directory (prefix) inside the bucket/container
     * @param contentType MIME type of the file to be uploaded
     * @param expiry      duration for which the URL remains valid
     * @return pre-signed upload information
     * @throws com.intteq.storage.core.exception.PreSignedUrlGenerationException
     *         if the URL cannot be generated
     */
    PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    );

    /**
     * Returns a file URL using the default configured expiry.
     */
    default String getFileUrl(String directory, String fileName) {
        return getFileUrl(directory, fileName, null);
    }

    /**
     * Returns the publicly accessible URL of a stored file.
     *
     * @param directory logical directory (prefix)
     * @param fileName  stored file name
     * @param expiry    duration for which the URL remains valid (null for default)
     * @return` signed read URL of the uploaded file (time-limited; uses provider default expiry)
     */
    String getFileUrl(String directory, String fileName, Duration expiry);

    /**
     * Deletes a file from object storage.
     *
     * @param directory logical directory (prefix)
     * @param fileName  stored file name
     * @throws com.intteq.storage.core.exception.StorageDeleteException
     *         if deletion fails
     */
    void delete(String directory, String fileName);

    /**
     * Uploads file directly to object storage (server-side upload).
     * Supports all file types including PDFs, images, videos, etc.
     *
     * @param directory   logical directory (prefix) inside the bucket/container
     * @param fileName    name of the file to store
     * @param content     file content as byte array
     * @param contentType MIME type of the file (e.g., "application/pdf", "image/jpeg")
     * @param metadata    optional metadata key-value pairs
     * @return` signed read URL of the uploaded file (time-limited; uses provider default expiry)
     * @throws com.intteq.storage.core.exception.StorageException
     *         if upload fails
     */
    String upload(
            String directory,
            String fileName,
            byte[] content,
            String contentType,
            Map<String, String> metadata
    );

    /**
     * Checks if a file exists in object storage.
     *
     * @param directory logical directory (prefix)
     * @param fileName  stored file name
     * @return true if file exists, false otherwise
     * @throws com.intteq.storage.core.exception.StorageException
     *         if check operation fails
     */
    boolean exists(String directory, String fileName);
}