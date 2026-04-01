package org.example.cyberwatch.features.ticket.service;

import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CORSRule;

@Service
public class S3Service {

    private final S3Client s3Client;
    private static final String BUCKET_NAME = "ticket-files";

    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    @PostConstruct
    public void init() {
        try {
            s3Client.putBucketCors(bucketCorsRequest -> bucketCorsRequest
                    .bucket(BUCKET_NAME)
                    .corsConfiguration(conf -> conf
                            .corsRules(CORSRule.builder()
                                    .allowedOrigins("*")
                                    .allowedMethods("GET", "PUT", "POST", "DELETE")
                                    .allowedHeaders("*")
                                    .build())
                    )
            );
        } catch (Exception e) {
            System.err.println("Could not set CORS: " + e.getMessage());
        }
    }
}