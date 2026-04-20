package org.example.cyberwatch.features.ticket.service;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;

@Service
public class S3Service {
    private static final Logger log = LoggerFactory.getLogger(S3Service.class);

    private final S3Client s3Client;
    private final S3Presigner s3Presigner;
    private final String bucketName;
    private final String region;
    private final List<String> allowedOrigins;

    public S3Service(
            S3Client s3Client,
            S3Presigner s3Presigner,
            @Value("${app.s3.bucket}") String bucketName,
            @Value("${app.s3.region}") String region,
            @Value("${app.s3.allowed-origins}") String allowedOriginsProperty
    ) {
        this.s3Client = s3Client;
        this.s3Presigner = s3Presigner;
        this.bucketName = bucketName;
        this.region = region;
        this.allowedOrigins = Arrays.asList(allowedOriginsProperty.split(","));
    }

    @PostConstruct
    public void init() {
        ensureBucketExists();
        configureCors();
    }

    /**
     * Genererar en tidsbegränsad, signerad nedladdningslänk för ett objekt i S3.
     *
     * Länken är giltig under angiven tid (t.ex. 5 minuter) och kräver
     * ingen autentisering mot S3 – behörighetskontrollen sker i vår backend
     * innan URL:en genereras. Efter att giltighetstiden löpt ut ger länken 403.
     *
     * @param s3Key  objektnyckeln i S3, t.ex. "tickets/42/uuid-filnamn.pdf"
     * @param expiry hur länge länken ska vara giltig
     * @return en signerad URL som kan användas direkt för nedladdning
     */
    public URL generatePresignedUrl(String s3Key, Duration expiry) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucketName)
                .key(s3Key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(expiry)
                .getObjectRequest(getObjectRequest)
                .build();

        URL url = s3Presigner.presignGetObject(presignRequest).url();
        log.debug("Genererade presigned URL för nyckel '{}', giltig i {}", s3Key, expiry);
        return url;
    }

    // Laddar upp ett JSON-objekt direkt till S3 utan att skriva till disk.
    public String uploadJsonData(String key, String jsonData) {
        try {
            PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                    .bucket(bucketName)
                    .key(key)
                    .contentType("application/json")
                    .build();

            s3Client.putObject(putObjectRequest, RequestBody.fromString(jsonData));
            log.info("Dokument arkiverat i S3: {}", key);
            return key;
        } catch (Exception e) {
            throw new RuntimeException("Kunde inte arkivera till S3", e);
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
            log.info("Skapade bucket '{}'", bucketName);
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            log.debug("Bucket '{}' finns redan", bucketName);
        } catch (Exception e) {
            log.error("Misslyckades skapa bucket '{}': {}", bucketName, e.getMessage(), e);
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
            log.info("Konfigurerade CORS för bucket '{}'", bucketName);
        } catch (Exception e) {
            log.error("Misslyckades konfigurera CORS för bucket '{}': {}", bucketName, e.getMessage(), e);
        }
    }
}