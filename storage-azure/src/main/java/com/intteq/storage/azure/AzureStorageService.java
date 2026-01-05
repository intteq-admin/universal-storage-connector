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

public class AzureStorageService implements ObjectStorageService {

    /**
     * Default read URL expiry.
     * Azure Blob Storage requires an explicit expiry for SAS tokens.
     * This default provides short-lived access and is NOT permanent.
     */
    private static final Duration DEFAULT_READ_EXPIRY = Duration.ofDays(1); // 1 day

    private final BlobContainerClient containerClient;

    public AzureStorageService(String connectionString, String container) {
        this.containerClient =
                new BlobContainerClientBuilder()
                        .connectionString(connectionString)
                        .containerName(container)
                        .buildClient();
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
            String fileName = UUID.randomUUID() + MimeTypeUtil.toExtension(contentType);
            String blobName = directory + "/" + fileName;

            BlobClient blobClient = containerClient.getBlobClient(blobName);

            BlobSasPermission uploadPermission = new BlobSasPermission()
                    .setCreatePermission(true)
                    .setWritePermission(true);

            BlobServiceSasSignatureValues uploadSas =
                    new BlobServiceSasSignatureValues(
                            OffsetDateTime.now().plus(expiry),
                            uploadPermission
                    );

            String uploadUrl =
                    blobClient.getBlobUrl() + "?" + blobClient.generateSas(uploadSas);

            return PreSignedUpload.builder()
                    .uploadUrl(uploadUrl)
                    .fileName(fileName)
                    // IMPORTANT: return a VIEWABLE link, not raw blob URL
                    .fileUrl(generateReadUrl(directory, fileName))
                    .headers(Map.of(
                            "x-ms-blob-type", "BlockBlob"
                    ))
                    .build();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate Azure Blob upload URL",
                    ex
            );
        }
    }

    // =========================================================
    // View (GET SAS) — REQUIRED for private container
    // =========================================================

    /**
     * Generates a long-lived, read-only, viewable URL.
     * Container remains PRIVATE.
     */
    public String generateReadUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName, DEFAULT_READ_EXPIRY);
    }

    /**
     * Generates a time-limited, read-only SAS URL.
     */
    public String generateReadUrl(
            String directory,
            String fileName,
            Duration expiry
    ) {
        try {
            String blobName = directory + "/" + fileName;
            BlobClient blobClient = containerClient.getBlobClient(blobName);

            BlobSasPermission readPermission = new BlobSasPermission()
                    .setReadPermission(true);

            BlobServiceSasSignatureValues readSas =
                    new BlobServiceSasSignatureValues(
                            OffsetDateTime.now().plus(expiry),
                            readPermission
                    );

            return blobClient.getBlobUrl() + "?" + blobClient.generateSas(readSas);

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate Azure Blob read URL",
                    ex
            );
        }
    }

    // =========================================================
    // Raw URL (NOT viewable when container is private)
    // =========================================================

    @Override
    public String getFileUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName);
    }

    // =========================================================
    // Delete
    // =========================================================

    @Override
    public void delete(String directory, String fileName) {
        try {
            containerClient
                    .getBlobClient(directory + "/" + fileName)
                    .delete();
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete Azure Blob: " + fileName,
                    ex
            );
        }
    }

}
