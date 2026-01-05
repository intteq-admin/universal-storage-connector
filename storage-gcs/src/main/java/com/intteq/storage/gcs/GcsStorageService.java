package com.intteq.storage.gcs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.PreSignedUpload;
import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;
import com.intteq.storage.core.util.MimeTypeUtil;

import java.io.FileInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
/**
 * Google Cloud Storage implementation of {@link ObjectStorageService}.
 *
 * <p>This service supports:
 * <ul>
 *   <li>V4 signed upload URLs (PUT)</li>
 *   <li>V4 signed read URLs (GET)</li>
 *   <li>Private bucket access (default)</li>
 * </ul>
 *
 * <p><strong>Important:</strong> This implementation does NOT assume public
 * bucket access. All file URLs returned by this service are signed and
 * time-limited.
 */
public class GcsStorageService implements ObjectStorageService {

    /**
     * Default expiry for signed read URLs.
     *
     * <p>Google Cloud Storage requires an explicit expiry for signed URLs.
     * This value provides short-lived access and is NOT permanent.
     */
    private final Duration defaultReadExpiry;

    private final Storage storage;
    private final String bucket;

    /**
     * Creates a new {@code GcsStorageService} using an explicit service account
     * credentials file.
     *
     * @param bucket           the GCS bucket name
     * @param credentialsPath  path to the service account JSON credentials file
     *
     * @throws RuntimeException if credentials cannot be loaded or the client
     *                          cannot be initialized
     */
    public GcsStorageService(String bucket, String credentialsPath, Duration defaultReadExpiry) {
        try (FileInputStream inputStream =
                     new FileInputStream(credentialsPath)) {

            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(inputStream);

            this.storage =
                    StorageOptions.newBuilder()
                            .setCredentials(credentials)
                            .build()
                            .getService();

            this.bucket = bucket;

            this.defaultReadExpiry = defaultReadExpiry;

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to load GCS credentials from: " + credentialsPath,
                    ex
            );
        }
    }

    /**
     * Generates a signed upload URL (PUT) for a new object.
     *
     * <p>The returned {@link PreSignedUpload} includes:
     * <ul>
     *   <li>A signed upload URL</li>
     *   <li>A generated filename</li>
     *   <li>A signed, viewable read URL</li>
     *   <li>Required HTTP headers</li>
     * </ul>
     *
     * @param directory   logical directory/prefix in the bucket
     * @param contentType MIME content type of the object
     * @param expiry      expiry duration for the upload URL
     *
     * @return a {@link PreSignedUpload} descriptor
     */
    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        try {
            String fileName =
                    UUID.randomUUID() + MimeTypeUtil.toExtension(contentType);

            String objectName = directory + "/" + fileName;

            BlobInfo blobInfo =
                    BlobInfo.newBuilder(bucket, objectName)
                            .setContentType(contentType)
                            .build();

            URL uploadUrl =
                    storage.signUrl(
                            blobInfo,
                            expiry.toSeconds(),
                            TimeUnit.SECONDS,
                            Storage.SignUrlOption.httpMethod(HttpMethod.PUT),
                            Storage.SignUrlOption.withV4Signature()
                    );

            return PreSignedUpload.builder()
                    .uploadUrl(uploadUrl.toString())
                    .fileName(fileName)
                    .fileUrl(generateReadUrl(directory, fileName))
                    .headers(Map.of(
                            "Content-Type", contentType
                    ))
                    .build();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate GCS upload URL",
                    ex
            );
        }
    }

    /**
     * Generates a signed, read-only URL using the default expiry.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     * @return signed read URL
     */
    public String generateReadUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName, defaultReadExpiry);
    }

    /**
     * Generates a signed, read-only URL with a custom expiry.
     *
     * <p>The returned URL works for private buckets and expires automatically.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     * @param expiry    expiry duration
     *
     * @return signed read URL
     */
    public String generateReadUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        try {
            BlobInfo blobInfo =
                    BlobInfo.newBuilder(bucket, directory + "/" + fileName)
                            .build();

            return storage.signUrl(
                    blobInfo,
                    expiry.toSeconds(),
                    TimeUnit.SECONDS,
                    Storage.SignUrlOption.httpMethod(HttpMethod.GET),
                    Storage.SignUrlOption.withV4Signature()
            ).toString();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate GCS read URL",
                    ex
            );
        }
    }

    /**
     * Deletes an object from the bucket.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     *
     * @throws StorageDeleteException if deletion fails
     */
    @Override
    public void delete(String directory, String fileName) {
        try {
            storage.delete(bucket, directory + "/" + fileName);
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete GCS object: " + fileName,
                    ex
            );
        }
    }

    /**
     * Returns a signed, viewable URL for the object.
     *
     * <p>This method always returns a signed URL and does NOT assume
     * public bucket access.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     * @return signed read URL
     */
    @Override
    public String getFileUrl(String directory, String fileName, Duration expiry) {
        Duration effectiveExpiry =
                (expiry != null ? expiry : defaultReadExpiry);
        return generateReadUrl(directory, fileName, effectiveExpiry);
    }
}
