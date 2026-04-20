package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.activitylog.service.ActivityLogService;
import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.*;
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
import java.util.*;

@Service
@Transactional
public class TicketService {

    private final TicketRepository ticketRepository;
    private final StaffRepository staffRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final S3Client s3Client;
    private final ActivityLogService activityLogService;

    @Value("${app.s3.endpoint}")
    private String endpoint;

    @Value("${app.s3.bucket}")
    private String bucket;

    public TicketService(TicketRepository ticketRepository,
                         StaffRepository staffRepository,
                         TicketAttachmentRepository ticketAttachmentRepository,
                         S3Client s3Client,
                         ActivityLogService activityLogService) {
        this.ticketRepository = ticketRepository;
        this.staffRepository = staffRepository;
        this.ticketAttachmentRepository = ticketAttachmentRepository;
        this.s3Client = s3Client;
        this.activityLogService = activityLogService;
    }

    public TicketResponseDTO createTicket(TicketDTO dto, String creatorEmail) {
        Staff creator = staffRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new StaffNotFoundException("Användare inte funnen i databasen: " + creatorEmail + ". Se till att din epost finns i staff-tabellen."));

        if (dto.getAssignedStaffIds() == null || dto.getAssignedStaffIds().isEmpty()) {
            throw new RuntimeException("Du måste välja minst en person att tilldela ärendet till.");
        }

        Ticket ticket = new Ticket();
        ticket.setTicketCode("TEMP-" + System.currentTimeMillis());
        ticket.setTitle(dto.getTitle());
        ticket.setDescription(dto.getDescription());
        ticket.setPriority(dto.getPriority());
        ticket.setIssueType(dto.getIssueType());
        ticket.setCreatedBy(creator);
        ticket.setStatus(Status.SUBMITTED);

        Set<Long> requestedIds = new HashSet<>(dto.getAssignedStaffIds());
        List<Staff> assignedStaff = staffRepository.findAllById(requestedIds);
        if (assignedStaff.size() != requestedIds.size()) {
            throw new StaffNotFoundException("En eller flera valda personer hittades inte.");
        }
        ticket.setAssignedStaff(assignedStaff);

        Ticket savedTicket = ticketRepository.save(ticket);
        savedTicket.setTicketCode("TICKET-" + (1000 + savedTicket.getId()));
        savedTicket = ticketRepository.save(savedTicket);

        activityLogService.logAssignmentChange(savedTicket, creator, assignedStaff);

        return TicketResponseDTO.from(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return TicketResponseDTO.from(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketCode));

        return TicketResponseDTO.from(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getAllTickets() {
        return ticketRepository.findAll().stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    /**
     * Returnerar tickets filtrerade på valfria parametrar.
     * Alla parametrar är valfria — null-värden ignoreras i queryn.
     * Alltid sorterat nyast först oavsett om filter är satta eller inte.
     */
    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getFilteredTickets(TicketFilterParams filters) {
        if (filters == null) {
            filters = new TicketFilterParams();
        }
        return ticketRepository.findByFilters(
                        filters.getStatus(),
                        filters.getPriority(),
                        filters.getIssueType(),
                        filters.getAssignedStaffId(),
                        filters.getCreatedById(),
                        filters.getSearch()
                ).stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    public TicketResponseDTO advanceTicketStatus(Long id, Long performedById) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById)
                .orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        ticket.advanceStatus();
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, saved.getStatus());
        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO setTicketStatus(Long id, Status newStatus, Long performedById) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById)
                .orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, newStatus);
        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO reopenTicket(Long id, Long performedById) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById)
                .orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        ticket.reopen();
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, Status.REOPENED);
        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO assignTicket(Long ticketId, List<Long> staffIds, Long assignedById) {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));

        if (staffIds == null || staffIds.isEmpty()) {
            throw new RuntimeException("Du måste välja minst en person att tilldela ärendet till.");
        }

        Staff assigner = staffRepository.findById(assignedById)
                .orElseThrow(() -> new StaffNotFoundException(assignedById));

        Set<Long> requestedIds = new HashSet<>(staffIds);
        List<Staff> staffList = staffRepository.findAllById(requestedIds);
        if (staffList.size() != requestedIds.size()) {
            throw new StaffNotFoundException("En eller flera valda personer hittades inte.");
        }

        ticket.setAssignedStaff(staffList);
        Ticket saved = ticketRepository.save(ticket);

        //if SUBMITTED acts as a triage queue, and staff need to manually acknowledge/start the ticket to move it to IN_PROGRESS, regardless of whether it was routed to them at creation or later.
            activityLogService.logAssignmentChange(saved, assigner, staffList);


        return TicketResponseDTO.from(saved);
    }

    public Map<String, Object> uploadFile(Long ticketId, Long uploadedById, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new TicketNotFoundException(ticketId));
        Staff uploader = staffRepository.findById(uploadedById)
                .orElseThrow(() -> new StaffNotFoundException(uploadedById));

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

        activityLogService.logFileUpload(ticket, uploader, originalFileName);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("message", "File uploaded successfully");
        response.put("fileName", attachment.getFileName());
        response.put("fileUrl", attachment.getFileUrl());
        response.put("ticketId", ticket.getId());

        return response;
    }

    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        ticketRepository.delete(ticket);
    }

    private void validateStatusTransition(Status current, Status next) {
        if (current == next) return;

        boolean valid = switch (current) {
            case DRAFT            -> next == Status.SUBMITTED;
            case SUBMITTED        -> next == Status.IN_PROGRESS || next == Status.RESOLVED || next == Status.CLOSED;
            case IN_PROGRESS      -> next == Status.RESOLVED || next == Status.WAITING_FOR_USER || next == Status.CLOSED || next == Status.SUBMITTED;
            case WAITING_FOR_USER -> next == Status.IN_PROGRESS || next == Status.RESOLVED || next == Status.CLOSED;
            case RESOLVED         -> next == Status.CLOSED || next == Status.IN_PROGRESS || next == Status.REOPENED;
            case REOPENED         -> next == Status.IN_PROGRESS || next == Status.RESOLVED || next == Status.CLOSED;
            case CLOSED           -> next == Status.REOPENED || next == Status.IN_PROGRESS || next == Status.SUBMITTED || next == Status.RESOLVED;
        };

        if (!valid) {
            throw new IllegalStateException(
                    "Ogiltig statusövergång: " + current + " → " + next);
        }
    }
}