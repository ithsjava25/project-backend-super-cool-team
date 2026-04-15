package org.example.cyberwatch.features.ticket.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.util.Arrays;
import java.util.List;

@Service
public class S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3Service.class);
    private final S3Client s3Client;
    private final String bucketName;
    private final String region;
    private final List<String> allowedOrigins;

    public S3Service(
            S3Client s3Client,
            @Value("${app.s3.bucket}") String bucketName,
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.allowed-origins}") String allowedOriginsProperty
    ) {
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.region = region;
        this.allowedOrigins = Arrays.asList(allowedOriginsProperty.split(","));
    }

    @PostConstruct
    public void init() {
        ensureBucketExists();
        configureCors();
    }

    //For uploading a JSON string directly to S3 without needing to create a file on disk
    public String uploadJsonData(String key, String jsonData) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key) // e.g. "archive/forms/employments.json"
                    .contentType("application/json")
                    .build();

            // Create a RequestBody from the JSON string
            s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonData));

            log.info("Document archived in S3: {}", key);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Could not archive to S3", e);
        }
    }

    private void ensureBucketExists() {
        try {
            CreateBucketRequest.Builder request = CreateBucketRequest.builder().bucket(bucketName);
            if (!"us-east-1".equals(region)) {
                request.createBucketConfiguration(cfg ->
                        cfg.locationConstraint(BucketLocationConstraint.fromValue(region)));
            }
            s3Client.createBucket(request.build());
            log.info("Created bucket '{}'", bucketName);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            log.debug("Bucket '{}' already exists", bucketName);
        } catch (Exception e) {
            log.error("Failed to create bucket '{}': {}", bucketName, e.getMessage(), e);
        }
    }

    private void configureCors() {
        try {
            s3Client.putBucketCors(bucketCorsRequest -> bucketCorsRequest
                    .bucket(bucketName)
                    .corsConfiguration(conf -> conf
                            .corsRules(rule -> rule
                                    .allowedOrigins(allowedOrigins)
                                    .allowedMethods("GET", "PUT", "POST", "DELETE")
                                    .allowedHeaders("*")
                                    .maxAgeSeconds(3000)
                            )
                    )
            );
            log.info("Configured CORS for bucket '{}'", bucketName);
        } catch (Exception e) {
            log.error("Failed to configure CORS for bucket '{}': {}", bucketName, e.getMessage(), e);
        }
    }
}