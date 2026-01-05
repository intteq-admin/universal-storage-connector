package com.intteq.storage.core;

import java.time.Duration;
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
     * @return public file URL
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
}
