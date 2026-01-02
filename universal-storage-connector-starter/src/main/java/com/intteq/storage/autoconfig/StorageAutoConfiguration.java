package com.intteq.storage.autoconfig;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.intteq.storage.azure.AzureStorageService;
import com.intteq.storage.core.ObjectStorageService;
import com.intteq.storage.core.StorageProvider;
import com.intteq.storage.gcs.GcsStorageService;
import com.intteq.storage.s3.S3StorageService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

@AutoConfiguration
@EnableConfigurationProperties(StorageProperties.class)
public class StorageAutoConfiguration {

    // ---------------- S3 ----------------

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "S3")
    ObjectStorageService s3StorageService(
            StorageProperties props,
            Environment env
    ) {

        String accessKey = env.getProperty("aws.accessKeyId");
        String secretKey = env.getProperty("aws.secretKey");

        AmazonS3ClientBuilder builder =
                AmazonS3ClientBuilder.standard()
                        .withRegion(props.getS3().getRegion());

        if (accessKey != null && secretKey != null) {
            builder.withCredentials(
                    new AWSStaticCredentialsProvider(
                            new BasicAWSCredentials(accessKey, secretKey)
                    )
            );
        } else {
            builder.withCredentials(
                    DefaultAWSCredentialsProviderChain.getInstance()
            );
        }

        AmazonS3 amazonS3 = builder.build();

        return new S3StorageService(
                amazonS3,
                props.getS3().getBucket()
        );
    }

    // ---------------- AZURE ----------------

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "AZURE")
    ObjectStorageService azureStorageService(StorageProperties props) {
        return new AzureStorageService(
                props.getAzure().getConnectionString(),
                props.getAzure().getContainer()
        );
    }

    // ---------------- GCS ----------------

    @Bean
    @ConditionalOnProperty(name = "storage.provider", havingValue = "GCS")
    ObjectStorageService gcsStorageService(StorageProperties props) {

        return new GcsStorageService(
                props.getGcs().getBucket(),
                props.getGcs().getCredentialsPath()
        );
    }
}