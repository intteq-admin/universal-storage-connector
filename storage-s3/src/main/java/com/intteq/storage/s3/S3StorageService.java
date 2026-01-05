package com.intteq.storage.s3;

import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.PreSignedUpload;
import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;
import com.intteq.storage.core.util.MimeTypeUtil;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;


/**
 * Amazon S3 implementation of {@link ObjectStorageService}.
 *
 * <p>This implementation uses AWS SDK v2 and supports:
 * <ul>
 *   <li>V4 pre-signed PUT URLs for uploads</li>
 *   <li>V4 pre-signed GET URLs for reads</li>
 *   <li>Private bucket access (default)</li>
 * </ul>
 *
 * <p>All file URLs returned by this service are signed and time-limited.
 * Public bucket access is not assumed.
 */
public class S3StorageService implements ObjectStorageService {

    /**
     * Default expiry for signed read URLs.
     */
    private static final Duration DEFAULT_READ_EXPIRY = Duration.ofDays(1);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;

    public S3StorageService(
            S3Client s3Client,
            S3Presigner presigner,
            String bucket
    ) {
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
    }

    /**
     * Generates a pre-signed upload URL (PUT) for Amazon S3.
     *
     * <p>The Content-Type is included in the signature and must be used
     * exactly as provided when uploading the object.
     *
     * @param directory   logical directory/prefix
     * @param contentType MIME content type of the object
     * @param expiry      expiry duration for the upload URL
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

            String objectKey = directory + "/" + fileName;

            PutObjectRequest putObjectRequest =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(contentType)
                            .build();

            PresignedPutObjectRequest presignedRequest =
                    presigner.presignPutObject(p -> p
                            .signatureDuration(expiry)
                            .putObjectRequest(putObjectRequest)
                    );

            return PreSignedUpload.builder()
                    .uploadUrl(presignedRequest.url().toString())
                    .fileName(fileName)
                    .fileUrl(getFileUrl(directory, fileName))
                    .headers(Map.of(
                            "Content-Type", contentType
                    ))
                    .build();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate S3 pre-signed upload URL",
                    ex
            );
        }
    }

    /**
     * Returns a signed, read-only URL for the object using a default expiry.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     * @return signed read URL
     */
    @Override
    public String getFileUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName, DEFAULT_READ_EXPIRY);
    }

    /**
     * Generates a signed, read-only URL with a custom expiry.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     * @param expiry    expiry duration
     * @return signed read URL
     */
    public String generateReadUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        try {
            GetObjectRequest getObjectRequest =
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(directory + "/" + fileName)
                            .build();

            PresignedGetObjectRequest presignedRequest =
                    presigner.presignGetObject(p -> p
                            .signatureDuration(expiry)
                            .getObjectRequest(getObjectRequest)
                    );

            return presignedRequest.url().toString();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate S3 pre-signed read URL",
                    ex
            );
        }
    }

    /**
     * Deletes an object from the S3 bucket.
     *
     * @param directory logical directory/prefix
     * @param fileName  object name
     */
    @Override
    public void delete(String directory, String fileName) {
        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(directory + "/" + fileName)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete object from S3: " + fileName,
                    ex
            );
        }
    }
}

