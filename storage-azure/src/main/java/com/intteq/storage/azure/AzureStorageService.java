package com.intteq.storage.azure;

import com.azure.storage.blob.*;
import com.azure.storage.blob.sas.*;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.PreSignedUpload;
import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;
import com.intteq.storage.core.util.MimeTypeUtil;

import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

/**
 * Azure Blob Storage implementation of {@link ObjectStorageService}.
 *
 * <p>This implementation supports private containers and always returns
 * signed, time-limited SAS URLs for read and upload operations.
 *
 * <h3>Expiry handling</h3>
 * <ul>
 *   <li>If an explicit expiry is provided, it is used</li>
 *   <li>If {@code null}, zero, or negative, a safe default is applied</li>
 * </ul>
 */
public class AzureStorageService implements ObjectStorageService {

    private static final Duration FALLBACK_EXPIRY = Duration.ofHours(1);

    private final BlobContainerClient containerClient;
    private final Duration defaultReadExpiry;

    /**
     * Creates a new {@code AzureStorageService}.
     *
     * @param connectionString Azure storage connection string
     * @param container        container name
     * @param defaultReadExpiry default expiry for signed read URLs
     */
    public AzureStorageService(
            String connectionString,
            String container,
            Duration defaultReadExpiry
    ) {

        if (connectionString == null || connectionString.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Azure Blob Storage connectionString must not be null or empty"
            );
        }

        if (container == null || container.trim().isEmpty()) {
            throw new IllegalArgumentException(
                    "Azure Blob Storage container name must not be null or empty"
            );
        }
        this.containerClient =
                new BlobContainerClientBuilder()
                        .connectionString(connectionString)
                        .containerName(container)
                        .buildClient();

        this.defaultReadExpiry = normalizeExpiry(defaultReadExpiry);
    }

    // =========================================================
    // Upload (PUT SAS)
    // =========================================================

    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        try {
            Duration effectiveExpiry = normalizeExpiry(expiry);
            String dir = normalizeDirectory(directory);

            String fileName =
                    UUID.randomUUID() + MimeTypeUtil.toExtension(contentType);

            String blobName = dir.isEmpty()
                    ? fileName
                    : dir + "/" + fileName;

            BlobClient blobClient = containerClient.getBlobClient(blobName);

            BlobSasPermission uploadPermission = new BlobSasPermission()
                    .setCreatePermission(true)
                    .setWritePermission(true);

            BlobServiceSasSignatureValues uploadSas =
                    new BlobServiceSasSignatureValues(
                            OffsetDateTime.now().plus(effectiveExpiry),
                            uploadPermission
                    );

            String uploadUrl =
                    blobClient.getBlobUrl() + "?" +
                            blobClient.generateSas(uploadSas);

            String readUrl;
            try {
                readUrl = generateReadUrl(dir, fileName);
            } catch (Exception ex) {
                throw new PreSignedUrlGenerationException(
                        "Upload URL generated successfully, but failed to generate read URL",
                        ex
                );
            }

            return PreSignedUpload.builder()
                    .uploadUrl(uploadUrl)
                    .fileName(fileName)
                    .fileUrl(readUrl)
                    .headers(Map.of(
                            "x-ms-blob-type", "BlockBlob"
                    ))
                    .build();

        } catch (PreSignedUrlGenerationException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate Azure Blob upload URL",
                    ex
            );
        }
    }

    // =========================================================
    // Read (GET SAS)
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
     * @param expiry expiry duration; if {@code null}, default expiry is used
     */
    public String generateReadUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        try {
            Duration effectiveExpiry = normalizeExpiry(expiry);

            String dir = normalizeDirectory(directory);
            String name = requireFileName(fileName);

            String blobName = dir.isEmpty()
                    ? name
                    : dir + "/" + name;

            BlobClient blobClient =
                    containerClient.getBlobClient(blobName);

            BlobSasPermission readPermission =
                    new BlobSasPermission()
                            .setReadPermission(true);

            BlobServiceSasSignatureValues readSas =
                    new BlobServiceSasSignatureValues(
                            OffsetDateTime.now().plus(effectiveExpiry),
                            readPermission
                    );

            return blobClient.getBlobUrl() + "?" +
                    blobClient.generateSas(readSas);

        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate Azure Blob read URL",
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

        String blobPath = dir.isEmpty()
                ? name
                : dir + "/" + name;

        try {
            containerClient
                    .getBlobClient(blobPath)
                    .delete();
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete Azure Blob: " + blobPath,
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
