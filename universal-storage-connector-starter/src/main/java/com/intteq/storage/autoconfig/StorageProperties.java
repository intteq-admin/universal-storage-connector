package com.intteq.storage.autoconfig;

import com.intteq.storage.core.StorageProvider;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "storage")
public class StorageProperties {

    /**
     * Selected storage provider (S3, AZURE, GCS)
     */
    private StorageProvider provider;

    private final S3 s3 = new S3();
    private final Azure azure = new Azure();
    private final Gcs gcs = new Gcs();

    // ---------- getters & setters ----------

    public StorageProvider getProvider() {
        return provider;
    }

    public void setProvider(StorageProvider provider) {
        this.provider = provider;
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

    // ---------- nested config classes ----------

    public static class S3 {
        private String bucket;
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

    public static class Azure {
        private String connectionString;
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

    public static class Gcs {
        private String bucket;
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
