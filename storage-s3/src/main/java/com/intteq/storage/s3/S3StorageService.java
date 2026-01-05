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
import java.util.Objects;
import java.util.UUID;
/**
 * Amazon S3 implementation of {@link ObjectStorageService}.
 *
 * <p>This implementation uses AWS SDK v2 and generates
 * <strong>pre-signed URLs</strong> for both upload and read operations.
 * Buckets are assumed to be private.
 *
 * <h3>Expiry handling</h3>
 * <ul>
 *   <li>If an explicit expiry is provided, it is used</li>
 *   <li>If {@code null}, zero, or negative, a safe default is applied</li>
 * </ul>
 */
public class S3StorageService implements ObjectStorageService {

    private static final Duration FALLBACK_EXPIRY = Duration.ofHours(1);

    private final S3Client s3Client;
    private final S3Presigner presigner;
    private final String bucket;
    private final Duration defaultReadExpiry;

    /**
     * Creates a new {@code S3StorageService}.
     *
     * @param s3Client          S3 client
     * @param presigner        S3 presigner
     * @param bucket           S3 bucket name
     * @param defaultReadExpiry default expiry for signed read URLs
     */
    public S3StorageService(
            S3Client s3Client,
            S3Presigner presigner,
            String bucket,
            Duration defaultReadExpiry
    ) {

        Objects.requireNonNull(s3Client, "s3Client must not be null");
        Objects.requireNonNull(presigner, "presigner must not be null");
        if (bucket == null || bucket.isBlank()) {
            throw new IllegalArgumentException("S3 bucket must not be null or empty");
        }
        this.s3Client = s3Client;
        this.presigner = presigner;
        this.bucket = bucket;
        this.defaultReadExpiry = normalizeExpiry(defaultReadExpiry);
    }

    // =========================================================
    // Upload (PUT pre-signed URL)
    // =========================================================

    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        Duration effectiveExpiry = normalizeExpiry(expiry);
        String dir = normalizeDirectory(directory);

        String fileName =
                UUID.randomUUID() + MimeTypeUtil.toExtension(contentType);

        String objectKey = dir.isEmpty()
                ? fileName
                : dir + "/" + fileName;

        try {
            PutObjectRequest putRequest =
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .contentType(contentType)
                            .build();

            PresignedPutObjectRequest presignedPut =
                    presigner.presignPutObject(p -> p
                            .putObjectRequest(putRequest)
                            .signatureDuration(effectiveExpiry)
                    );

            String readUrl;
            try {
                readUrl = generateReadUrl(dir, fileName);
            } catch (Exception ex) {
                throw new PreSignedUrlGenerationException(
                        "Upload URL generated successfully, but failed to generate read URL for S3 object: " + objectKey,
                        ex
                );
            }

            return PreSignedUpload.builder()
                    .uploadUrl(presignedPut.url().toString())
                    .fileName(fileName)
                    .fileUrl(readUrl)
                    .headers(Map.of(
                            "Content-Type", contentType
                    ))
                    .build();

        } catch (PreSignedUrlGenerationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate S3 upload URL for object: " + objectKey,
                    ex
            );
        }
    }

    // =========================================================
    // Read (GET pre-signed URL)
    // =========================================================

    /**
     * Generates a signed read-only URL using the default expiry.
     */
    public String generateReadUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName, defaultReadExpiry);
    }

    /**
     * Generates a signed read-only URL with a custom expiry.
     *
     * @param expiry expiry duration; if {@code null}, default expiry is applied
     */
    public String generateReadUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        Duration effectiveExpiry = normalizeExpiry(expiry);

        String dir = normalizeDirectory(directory);
        String name = requireFileName(fileName);

        String objectKey = dir.isEmpty()
                ? name
                : dir + "/" + name;

        try {
            GetObjectRequest getRequest =
                    GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build();

            PresignedGetObjectRequest presignedGet =
                    presigner.presignGetObject(p -> p
                            .getObjectRequest(getRequest)
                            .signatureDuration(effectiveExpiry)
                    );

            return presignedGet.url().toString();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate S3 read URL for object: " + objectKey,
                    ex
            );
        }
    }

    // =========================================================
    // Public API
    // =========================================================

    @Override
    public String getFileUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        return generateReadUrl(directory, fileName, expiry);
    }

    // =========================================================
    // Delete
    // =========================================================

    @Override
    public void delete(String directory, String fileName) {
        String dir = normalizeDirectory(directory);
        String name = requireFileName(fileName);

        String objectKey = dir.isEmpty()
                ? name
                : dir + "/" + name;

        try {
            s3Client.deleteObject(
                    DeleteObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build()
            );
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete S3 object: " + objectKey,
                    ex
            );
        }
    }

    // =========================================================
    // Utility
    // =========================================================

    /**
     * Normalizes an expiry duration.
     *
     * <p>If the provided value is {@code null}, zero, or negative,
     * a safe fallback expiry is returned.
     *
     * @param expiry candidate expiry
     * @return non-null, positive expiry
     */
    private static Duration normalizeExpiry(Duration expiry) {
        if (expiry == null || expiry.isZero() || expiry.isNegative()) {
            return FALLBACK_EXPIRY;
        }
        return expiry;
    }

    private static String normalizeDirectory(String directory) {
        if (directory == null || directory.trim().isEmpty()) {
            return "";
        }
        return directory.trim();
    }

    private static String requireFileName(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "fileName must not be null or empty"
            );
        }
        return fileName.trim();
    }
}

