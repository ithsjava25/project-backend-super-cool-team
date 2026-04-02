package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.Ticket;
import org.example.cyberwatch.features.ticket.model.TicketAttachment;
import org.example.cyberwatch.features.ticket.model.TicketDTO;
import org.example.cyberwatch.features.ticket.model.TicketResponseDTO;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Status;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final StaffRepository staffRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final S3Client s3Client;

    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.bucket}")
    private String bucket;

    public TicketService(TicketRepository ticketRepository,
                         StaffRepository staffRepository,
                         TicketAttachmentRepository ticketAttachmentRepository,
                         S3Client s3Client) {
        this.ticketRepository = ticketRepository;
        this.staffRepository = staffRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.s3Client = s3Client;
    }

    public TicketResponseDTO createTicket(TicketDTO dto) {
        Staff creator = staffRepository.findById(dto.getCreatedById())
                .orElseThrow(() -> new StaffNotFoundException(dto.getCreatedById()));

        Ticket ticket = new Ticket();
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(dto.getPriority());
        ticket.setIssueType(dto.getIssueType());
        ticket.setCreatedBy(creator);
        ticket.setStatus(Status.SUBMITTED);

        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return TicketResponseDTO.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    public TicketResponseDTO advanceTicketStatus(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.advanceStatus();
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO setTicketStatus(Long id, Status newStatus) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        validateStatusTransition(ticket.getStatus(), newStatus);
        ticket.setStatus(newStatus);
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO reopenTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticket.reopen();
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public TicketResponseDTO assignTicket(Long ticketId, Long staffId) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Staff staff = staffRepository.findById(staffId)
                .orElseThrow(() -> new StaffNotFoundException(staffId));
        ticket.setAssignedTo(staff);
        return TicketResponseDTO.from(ticketRepository.save(ticket));
    }

    public Map<String, Object> uploadFile(Long ticketId, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (file.isEmpty()) {
            throw new RuntimeException("File is empty");
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {
            // Bucket finns redan, fortsätt
        }

        String originalFileName = file.getOriginalFilename();
        if (originalFileName == null || originalFileName.isBlank()) {
            originalFileName = "unnamed";
        }

        originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        originalFileName = originalFileName.substring(Math.max(0, originalFileName.lastIndexOf('/') + 1));
        originalFileName = originalFileName.substring(Math.max(0, originalFileName.lastIndexOf('\\') + 1));

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

    private void validateStatusTransition(Status current, Status next) {
        boolean valid = switch (current) {
            case DRAFT            -> next == Status.SUBMITTED;
            case SUBMITTED        -> next == Status.IN_PROGRESS;
            case IN_PROGRESS      -> next == Status.RESOLVED || next == Status.WAITING_FOR_USER;
            case WAITING_FOR_USER -> next == Status.IN_PROGRESS;
            case RESOLVED         -> next == Status.CLOSED;
            case REOPENED         -> next == Status.IN_PROGRESS;
            case CLOSED           -> false;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Ogiltig statusövergång: " + current + " → " + next);
        }
    }
}