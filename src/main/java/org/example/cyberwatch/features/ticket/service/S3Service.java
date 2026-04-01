package org.example.cyberwatch.features.ticket.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.BucketLocationConstraint;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import jakarta.annotation.PostConstruct;
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