package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

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

    String title;

    String description;

    LocalDateTime reportDate;

    LocalDateTime issueFound;

    @Enumerated(EnumType.STRING)
    Priority priority;

    @Enumerated(EnumType.STRING)
    Status status;

    @Enumerated(EnumType.STRING)
    IssueType issueType;

    @Enumerated(EnumType.STRING)
    Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = false)
    @NotNull(message = "Staff cannot be null")
    private Staff staff;

    @OneToMany(mappedBy = "reportForm", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderColumn(name = "attachment_order")
    private Set<Attachment> attachments = new HashSet<>();

    public ReportForm() {

    }

}
