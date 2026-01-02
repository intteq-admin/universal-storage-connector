package com.intteq.storage.s3;

import com.amazonaws.HttpMethod;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.PreSignedUpload;
import com.intteq.storage.core.exception.PreSignedUrlGenerationException;
import com.intteq.storage.core.exception.StorageDeleteException;

import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

public class S3StorageService implements ObjectStorageService {

    private final AmazonS3 amazonS3;
    private final String bucket;

    public S3StorageService(AmazonS3 amazonS3, String bucket) {
        this.amazonS3 = amazonS3;
        this.bucket = bucket;
    }

    @Override
    public PreSignedUpload generateUploadUrl(
            String directory,
            String contentType,
            Duration expiry
    ) {
        try {
            String fileName = generateFileName(contentType);
            String objectKey = directory + "/" + fileName;

            Date expiration = Date.from(Instant.now().plus(expiry));

            GeneratePresignedUrlRequest request =
                    new GeneratePresignedUrlRequest(bucket, objectKey)
                            .withMethod(HttpMethod.PUT)
                            .withExpiration(expiration);

            URL uploadUrl = amazonS3.generatePresignedUrl(request);

            return PreSignedUpload.builder()
                    .uploadUrl(uploadUrl.toString())
                    .fileName(fileName)
                    .fileUrl(getFileUrl(directory, fileName))
                    .headers(Map.of("Content-Type", contentType))
                    .build();

        } catch (Exception ex) {
            throw new PreSignedUrlGenerationException(
                    "Failed to generate S3 pre-signed upload URL",
                    ex
            );
        }
    }

    @Override
    public String getFileUrl(String directory, String fileName) {
        return amazonS3
                .getUrl(bucket, directory + "/" + fileName)
                .toString();
    }

    @Override
    public void delete(String directory, String fileName) {
        try {
            amazonS3.deleteObject(bucket, directory + "/" + fileName);
        } catch (Exception ex) {
            throw new StorageDeleteException(
                    "Failed to delete object from S3: " + fileName,
                    ex
            );
        }
    }

    private String generateFileName(String contentType) {
        String extension = extractExtension(contentType);
        return UUID.randomUUID() + "." + extension;
    }

    private String extractExtension(String contentType) {
        if (contentType == null || !contentType.contains("/")) {
            return "bin";
        }
        return contentType.substring(contentType.indexOf("/") + 1);
    }
}

