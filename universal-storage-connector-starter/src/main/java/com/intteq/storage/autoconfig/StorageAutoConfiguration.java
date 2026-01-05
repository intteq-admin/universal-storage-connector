package com.intteq.storage.autoconfig;

import com.intteq.storage.azure.AzureStorageService;
import com.intteq.storage.core.NoOpStorageService;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.gcs.GcsStorageService;
import com.intteq.storage.s3.S3StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.AwsCredentialsProvider;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * Auto-configuration for the Universal Storage Connector.
 *
 * <p>This configuration conditionally creates an {@link ObjectStorageService}
 * implementation based on the {@code storage.provider} property.
 *
 * <p>The application is allowed to start even if provider-specific
 * configuration is incomplete. In such cases, the provider bean is
 * skipped and a warning is logged.
 *
 * <p>All provider configurations are guarded with {@link ConditionalOnClass}
 * to prevent {@link NoClassDefFoundError} when optional modules are not present
 * on the classpath.
 *
 * <p>Supported providers:
 * <ul>
 *   <li>S3</li>
 *   <li>AZURE</li>
 *   <li>GCS</li>
 * </ul>
 */
@Slf4j
@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    // =========================================================
    // S3
    // =========================================================

    /**
     * Creates an Amazon S3-backed {@link ObjectStorageService}.
     *
     * <p>This bean is created only if:
     * <ul>
     *   <li>{@code storage.provider=S3}</li>
     *   <li>AWS SDK v2 is present on the classpath</li>
     * </ul>
     *
     * <p>If required properties are missing, the bean is not created and
     * the application continues to start.
     */
    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
    @ConditionalOnClass({S3Client.class, S3Presigner.class, S3StorageService.class})
    ObjectStorageService s3StorageService(
            StorageProperties props,
            Environment env
    ) {

        String bucket = props.getS3().getBucket();
        String region = props.getS3().getRegion();

        if (isBlank(bucket) || isBlank(region)) {
            log.warn(
                    "S3 storage provider selected but configuration is incomplete. " +
                            "Required properties: storage.s3.bucket, storage.s3.region. " +
                            "S3 storage will be DISABLED."
            );
            return null;
        }

        String accessKey = env.getProperty("aws.accessKeyId");
        String secretKey = env.getProperty("aws.secretKey");

        AwsCredentialsProvider credentialsProvider =
                resolveCredentials(accessKey, secretKey);

        Region awsRegion = Region.of(region);

        S3Client s3Client =
                S3Client.builder()
                        .region(awsRegion)
                        .credentialsProvider(credentialsProvider)
                        .build();

        S3Presigner presigner =
                S3Presigner.builder()
                        .region(awsRegion)
                        .credentialsProvider(credentialsProvider)
                        .build();

        log.info("Initialized S3 storage provider for bucket '{}'", bucket);

        return new S3StorageService(
                s3Client,
                presigner,
                bucket,
                props.getReadUrlExpiry()
        );
    }

    private static AwsCredentialsProvider resolveCredentials(
            String accessKey,
            String secretKey
    ) {
        if (!isBlank(accessKey) && !isBlank(secretKey)) {
            return StaticCredentialsProvider.create(
                    AwsBasicCredentials.create(accessKey, secretKey)
            );
        }
        return DefaultCredentialsProvider.create();
    }

    // =========================================================
    // AZURE
    // =========================================================

    /**
     * Creates an Azure Blob Storage-backed {@link ObjectStorageService}.
     *
     * <p>If the Azure connection string or container name is missing,
     * the bean is skipped and the application starts normally.
     */
    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "AZURE")
    @ConditionalOnClass(AzureStorageService.class)
    ObjectStorageService azureStorageService(StorageProperties props) {

        String connectionString = props.getAzure().getConnectionString();
        String container = props.getAzure().getContainer();

        if (isBlank(connectionString) || isBlank(container)) {
            log.warn(
                    "Azure storage provider selected but configuration is incomplete. " +
                            "Required properties: storage.azure.connection-string, storage.azure.container. " +
                            "Azure storage will be DISABLED."
            );
            return null;
        }

        log.info("Initialized Azure Blob Storage provider for container '{}'", container);

        return new AzureStorageService(connectionString, container, props.getReadUrlExpiry());
    }

    // =========================================================
    // GCS
    // =========================================================

    /**
     * Creates a Google Cloud Storage-backed {@link ObjectStorageService}.
     *
     * <p>If required configuration is missing, the bean is skipped
     * and the application continues to start.
     */
    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "GCS")
    @ConditionalOnClass(GcsStorageService.class)
    ObjectStorageService gcsStorageService(StorageProperties props) {

        String bucket = props.getGcs().getBucket();
        String credentialsPath = props.getGcs().getCredentialsPath();

        if (isBlank(bucket) || isBlank(credentialsPath)) {
            log.warn(
                    "GCS storage provider selected but configuration is incomplete. " +
                            "Required properties: storage.gcs.bucket, storage.gcs.credentials-path. " +
                            "GCS storage will be DISABLED."
            );
            return null;
        }

        log.info("Initialized GCS storage provider for bucket '{}'", bucket);

        return new GcsStorageService(bucket, credentialsPath, props.getReadUrlExpiry());
    }

    // =========================================================
    // Utility
    // =========================================================

    private static boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @Bean
    @ConditionalOnMissingBean(ObjectStorageService.class)
    ObjectStorageService noOpStorageService() {
        log.warn(
                "No ObjectStorageService implementation was configured. " +
                        "Using NoOpStorageService. File operations will fail at runtime."
        );
        return new NoOpStorageService();
    }
}
