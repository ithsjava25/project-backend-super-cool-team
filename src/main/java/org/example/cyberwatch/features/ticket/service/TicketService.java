package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketAttachment;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class TicketService {

    private final TicketRepository ticketRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;

    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.region}")
    private String region;

    @Value("${app.s3.accessKey}")
    private String accessKey;

    @Value("${app.s3.secretKey}")
    private String secretKey;

    @Value("${app.s3.bucket}")
    private String bucket;

    public TicketService(TicketRepository ticketRepository, TicketAttachmentRepository ticketAttachmentRepository) {
        this.ticketRepository = ticketRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
    }

    public Ticket createTicket(TicketDTO dto) {
        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(dto.getPriority());
        ticket.setStatus(Status.SUBMITTED);

        return ticketRepository.save(ticket);
    }

    public Map<String, Object> uploadFile(Long ticketId, MultipartFile file) throws Exception {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found with id " + ticketId));

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        S3Client s3Client = S3Client.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.of(region))
                .credentialsProvider(
                        StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)
                        )
                )
                .forcePathStyle(true)
                .build();

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (Exception e) {
            // bucket finns redan
        }

        String originalFileName = file.getOriginalFilename();
        String key = "tickets/" + ticketId + "/" + UUID.randomUUID() + "-" + originalFileName;

        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(file.getContentType())
                .build();

        s3Client.putObject(putObjectRequest, RequestBody.fromBytes(file.getBytes()));

        String fileUrl = endpoint + "/" + bucket + "/" + key;

        TicketAttachment attachment = new TicketAttachment();
        attachment.setFileName(originalFileName);
        attachment.setFileUrl(fileUrl);
        attachment.setS3Key(key);
        attachment.setTicket(ticket);

        ticketAttachmentRepository.save(attachment);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "File uploaded successfully");
        response.put("fileName", attachment.getFileName());
        response.put("fileUrl", attachment.getFileUrl());
        response.put("ticketId", ticket.getId());

        return response;
    }
}