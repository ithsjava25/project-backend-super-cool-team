package org.example.cyberwatch.features.ticket.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CORSRule;
import software.amazon.awssdk.services.s3.model.S3Exception;

import java.util.Arrays;
import java.util.List;

@Service
public class S3Service {

    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final String bucketName;
    private final List<String> allowedOrigins;

    public S3Service(
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucketName,
            @Value("${app.s3.allowed-origins}") String allowedOriginsProperty
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.allowedOrigins = Arrays.stream(allowedOriginsProperty.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
    }

    @PostConstruct
    public void init() {
        ensureBucketExists();
        configureCors();
    }

    private void ensureBucketExists() {
        try {
            s3Client.createBucket(b -> b.bucket(bucketName));
            log.info("Created bucket '{}'", bucketName);
        } catch (BucketAlreadyOwnedByYouException | BucketAlreadyExistsException e) {
            log.info("Bucket '{}' already exists", bucketName);
        } catch (S3Exception e) {
            log.error("Failed to create bucket '{}': {}", bucketName, e.getMessage(), e);
            throw e;
        }
    }

    private void configureCors() {
        try {
            s3Client.putBucketCors(bucketCorsRequest -> bucketCorsRequest
                    .bucket(bucketName)
                    .corsConfiguration(conf -> conf
                            .corsRules(CORSRule.builder()
                                    .allowedOrigins(allowedOrigins)
                                    .allowedMethods("GET", "PUT", "POST", "DELETE")
                                    .allowedHeaders("Content-Type", "Authorization")
                                    .build())
                    )
            );

            log.info("Configured CORS for bucket '{}'", bucketName);
        } catch (S3Exception e) {
            log.error("Failed to configure CORS for bucket '{}': {}", bucketName, e.getMessage(), e);
            throw e;
        }
    }
}