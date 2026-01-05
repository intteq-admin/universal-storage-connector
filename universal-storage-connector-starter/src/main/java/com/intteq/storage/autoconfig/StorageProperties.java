package com.intteq.storage.autoconfig;

import com.intteq.storage.core.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

/**
 * Configuration properties for the Universal Storage Connector.
 *
 * <p>All properties are prefixed with {@code storage}.
 *
 * <p>Example configuration:
 * <pre>
 * storage.provider=S3
 * storage.read-url-expiry=1h
 *
 * storage.s3.bucket=my-bucket
 * storage.s3.region=eu-west-2
 * </pre>
 *
 * <p>The {@code read-url-expiry} property controls the default expiry
 * for signed read URLs across all storage providers.
 */
@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Selected storage provider.
     * Supported values: S3, AZURE, GCS
     */
    private StorageProvider provider;

    /**
     * Default expiry for signed read URLs.
     *
     * <p>If not configured, defaults to 1 hour.
     *
     * <p>Examples:
     * <ul>
     *   <li>{@code 1h}</li>
     *   <li>{@code 8h}</li>
     *   <li>{@code 14d}</li>
     *   <li>{@code PT336H} (ISO-8601)</li>
     * </ul>
     */
    private Duration readUrlExpiry = Duration.ofHours(1);

    private final S3 s3 = new S3();
    private final Azure azure = new Azure();
    private final Gcs gcs = new Gcs();

    // -------------------------------------------------
    // Getters & setters
    // -------------------------------------------------

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
    }

    public Duration getReadUrlExpiry() {
        return readUrlExpiry;
    }

    public void setReadUrlExpiry(Duration readUrlExpiry) {
        this.readUrlExpiry = readUrlExpiry;
    }

    public S3 getS3() {
        return s3;
    }

    public Azure getAzure() {
        return azure;
    }

    public Gcs getGcs() {
        return gcs;
    }

    // -------------------------------------------------
    // Nested provider configuration classes
    // -------------------------------------------------

    /**
     * Amazon S3 / compatible (e.g. DigitalOcean Spaces) configuration.
     */
    public static class S3 {

        /**
         * S3 bucket name.
         */
        private String bucket;

        /**
         * AWS region.
         */
        private String region;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getRegion() {
            return region;
        }

        public void setRegion(String region) {
            this.region = region;
        }
    }

    /**
     * Azure Blob Storage configuration.
     */
    public static class Azure {

        /**
         * Azure Blob Storage connection string.
         */
        private String connectionString;

        /**
         * Azure Blob container name.
         */
        private String container;

        public String getConnectionString() {
            return connectionString;
        }

        public void setConnectionString(String connectionString) {
            this.connectionString = connectionString;
        }

        public String getContainer() {
            return container;
        }

        public void setContainer(String container) {
            this.container = container;
        }
    }

    /**
     * Google Cloud Storage configuration.
     */
    public static class Gcs {

        /**
         * GCS bucket name.
         */
        private String bucket;

        /**
         * Path to GCP service account credentials JSON file.
         */
        private String credentialsPath;

        public String getBucket() {
            return bucket;
        }

        public void setBucket(String bucket) {
            this.bucket = bucket;
        }

        public String getCredentialsPath() {
            return credentialsPath;
        }

        public void setCredentialsPath(String credentialsPath) {
            this.credentialsPath = credentialsPath;
        }
    }
}
