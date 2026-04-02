package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public class ReportForm {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    String title;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    String description;

    @CreationTimestamp
    LocalDateTime reportDate;

    @NotNull(message = "Priority cannot be null")
    @Enumerated(EnumType.STRING)
    Priority priority;

    @NotNull(message = "Status cannot be null")
    @Enumerated(EnumType.STRING)
    Status status;

    @NotNull(message = "Issue type cannot be null")
    @Enumerated(EnumType.STRING)
    IssueType issueType;

    @NotNull(message = "Department cannot be null")
    @Enumerated(EnumType.STRING)
    Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    @NotNull(message = "Staff cannot be null")
    Staff createdBy;

    @OneToMany(mappedBy = "reportForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "attachment_order")
    Set<Attachment> attachments = new HashSet<>();

    public ReportForm() {

    }

}
