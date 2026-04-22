package org.example.cyberwatch.features.ticket.service;

import org.example.cyberwatch.features.activitylog.service.ActivityLogService;
import org.example.cyberwatch.features.staff.exception.StaffNotFoundException;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.example.cyberwatch.features.ticket.exception.TicketNotFoundException;
import org.example.cyberwatch.features.ticket.model.*;
import org.example.cyberwatch.features.ticket.repository.TicketAttachmentRepository;
import org.example.cyberwatch.features.ticket.repository.TicketRepository;
import org.example.cyberwatch.shared.model.enums.Role;
import org.example.cyberwatch.shared.model.enums.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(TicketService.class);

    private static final Set<String> ALLOWED_MIME_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/gif",
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "text/plain"
    );

    private static final long MAX_FILE_SIZE = 10L * 1024 * 1024;

    private final TicketRepository ticketRepository;
    private final StaffRepository staffRepository;
    private final TicketAttachmentRepository ticketAttachmentRepository;
    private final S3Client s3Client;
    private final ActivityLogService activityLogService;

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
                .orElseThrow(() -> new StaffNotFoundException("Användare inte funnen: " + creatorEmail));

        if (dto.getAssignedStaffIds() == null || dto.getAssignedStaffIds().isEmpty()) {
            throw new IllegalArgumentException("Du måste välja minst en person att tilldela ärendet till.");
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

        log.info("Ticket skapad: {} av staffId={}", savedTicket.getTicketCode(), creator.getId());

        return TicketResponseDTO.from(savedTicket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketById(Long id) {
        Ticket ticket = ticketRepository.findById(id)
                .orElseThrow(() -> new TicketNotFoundException(id));
        return TicketResponseDTO.fromDetail(ticket);
    }

    @Transactional(readOnly = true)
    public TicketResponseDTO getTicketByCode(String ticketCode) {
        Ticket ticket = ticketRepository.findByTicketCode(ticketCode)
                .orElseThrow(() -> new TicketNotFoundException("Ticket not found: " + ticketCode));
        return TicketResponseDTO.fromDetail(ticket);
    }

    @Transactional(readOnly = true)
    public List<TicketResponseDTO> getFilteredTickets(TicketFilterParams filters, Staff viewer) {
        if (filters == null) filters = new TicketFilterParams();
        boolean isElevated = viewer.getRole() == Role.ADMIN || viewer.getRole() == Role.CEO || viewer.getRole() == Role.CTO;
        Long viewerId = isElevated ? null : viewer.getId();

        return ticketRepository.findByFilters(
                        filters.getStatus(), filters.getPriority(), filters.getIssueType(),
                        filters.getAssignedStaffId(), filters.getCreatedById(), filters.getSearch(), viewerId
                ).stream()
                .map(TicketResponseDTO::from)
                .toList();
    }

    public TicketResponseDTO advanceTicketStatus(Long id, Long performedById) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById).orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        ticket.advanceStatus();
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, saved.getStatus());

        log.info("Ticket {} status avancerad: {} → {}", saved.getTicketCode(), oldStatus, saved.getStatus());

        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO setTicketStatus(Long id, Status newStatus, Long performedById) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById).orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        validateStatusTransition(oldStatus, newStatus);
        ticket.setStatus(newStatus);
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, newStatus);

        log.info("Ticket {} status ändrad: {} → {}", saved.getTicketCode(), oldStatus, newStatus);

        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO reopenTicket(Long id, Long performedById) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        Staff performer = staffRepository.findById(performedById).orElseThrow(() -> new StaffNotFoundException(performedById));
        Status oldStatus = ticket.getStatus();
        ticket.reopen();
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logStatusChange(saved, performer, oldStatus, Status.REOPENED);

        log.info("Ticket {} återöppnad", saved.getTicketCode());

        return TicketResponseDTO.from(saved);
    }

    public TicketResponseDTO assignTicket(Long ticketId, List<Long> staffIds, Long assignedById) {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        if (staffIds == null || staffIds.isEmpty()) throw new IllegalArgumentException("Välj minst en person.");
        Staff assigner = staffRepository.findById(assignedById).orElseThrow(() -> new StaffNotFoundException(assignedById));
        List<Staff> staffList = staffRepository.findAllById(staffIds);
        ticket.setAssignedStaff(staffList);
        Ticket saved = ticketRepository.save(ticket);
        activityLogService.logAssignmentChange(saved, assigner, staffList);

        log.info("Ticket {} tilldelad till {} person(er)", saved.getTicketCode(), staffList.size());

        return TicketResponseDTO.from(saved);
    }

    public Map<String, Object> uploadFile(Long ticketId, Long uploadedById, MultipartFile file) throws IOException {
        Ticket ticket = ticketRepository.findById(ticketId).orElseThrow(() -> new TicketNotFoundException(ticketId));
        Staff uploader = staffRepository.findById(uploadedById).orElseThrow(() -> new StaffNotFoundException(uploadedById));

        if (file.isEmpty()) throw new IllegalArgumentException("Filen är tom.");
        if (file.getSize() > MAX_FILE_SIZE) throw new IllegalArgumentException("Filen är för stor (max 10MB).");

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_MIME_TYPES.contains(contentType)) {
            throw new IllegalArgumentException("Otillåten filtyp: " + contentType);
        }

        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException e) {}

        String originalFileName = Optional.ofNullable(file.getOriginalFilename()).orElse("unnamed");
        originalFileName = originalFileName.replaceAll("[^a-zA-Z0-9._-]", "_");
        originalFileName = originalFileName.replaceAll("\\.{2,}", "_");

        String key = "attachments/" + ticketId + "/" + UUID.randomUUID() + "-" + originalFileName;

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucket).key(key).contentType(file.getContentType()).build(),
                RequestBody.fromBytes(file.getBytes()));

        TicketAttachment attachment = new TicketAttachment();
        attachment.setFileName(originalFileName);
        attachment.setS3Key(key);
        attachment.setTicket(ticket);
        TicketAttachment saved = ticketAttachmentRepository.save(attachment);

        activityLogService.logFileUpload(ticket, uploader, originalFileName);

        // Loggar attachmentId istället för filnamnet för att undvika att läcka användardata i loggar
        log.info("Fil uppladdad till ticket {}: attachmentId={}", ticket.getTicketCode(), saved.getId());

        return Map.of(
                "message", "Filen laddades upp.",
                "fileName", saved.getFileName(),
                "downloadUrl", "/api/tickets/" + ticketId + "/attachments/" + saved.getId() + "/download",
                "ticketId", ticket.getId()
        );
    }

    public void deleteTicket(Long id) {
        Ticket ticket = ticketRepository.findById(id).orElseThrow(() -> new TicketNotFoundException(id));
        String ticketCode = ticket.getTicketCode();
        ticketRepository.delete(ticket);
        // Loggar efter delete så att koden bara körs om raderingen lyckades
        log.info("Ticket {} raderad", ticketCode);
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
        if (!valid) throw new IllegalStateException("Ogiltig statusövergång: " + current + " → " + next);
    }
}