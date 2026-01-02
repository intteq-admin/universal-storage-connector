package com.intteq.storage.gcs;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.HttpMethod;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.PreSignedUpload;
import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;

import java.io.FileInputStream;
import java.net.URL;
import java.time.Duration;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class GcsStorageService implements ObjectStorageService {

    private static final Duration DEFAULT_READ_EXPIRY = Duration.ofDays(1);

    private final Storage storage;
    private final String bucket;

    public GcsStorageService(String bucket, String credentialsPath) {
        try {
            GoogleCredentials credentials =
                    GoogleCredentials.fromStream(
                            new FileInputStream(credentialsPath)
                    );

            this.storage =
                    StorageOptions.newBuilder()
                            .setCredentials(credentials)
                            .build()
                            .getService();

            this.bucket = bucket;

        } catch (Exception ex) {
            throw new RuntimeException(
                    "Failed to load GCS credentials from: " + credentialsPath,
                    ex
            );
        }
    }

    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        try {
            String fileName = UUID.randomUUID() + getExtension(contentType);
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

    public String generateReadUrl(String directory, String fileName) {
        return generateReadUrl(directory, fileName, DEFAULT_READ_EXPIRY);
    }

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

    @Override
    public String getFileUrl(String directory, String fileName) {
        return String.format(
                "https://storage.googleapis.com/%s/%s/%s",
                bucket, directory, fileName
        );
    }

    private String getExtension(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return ".bin";
        }
        return "." + contentType.substring(contentType.indexOf('/') + 1);
    }
}
