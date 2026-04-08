package org.example.cyberwatch.features.form.model;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.IssueType;
import org.example.cyberwatch.shared.model.enums.Priority;
import org.example.cyberwatch.shared.model.enums.Status;

import java.time.LocalDateTime;
import java.util.Set;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ReportFormDTO {

    private Long id;

    @NotBlank(message = "Title cannot be blank")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotBlank(message = "Description cannot be blank")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    private LocalDateTime reportDate;

    @NotNull(message = "Priority cannot be null")
    private Priority priority;

    @NotNull(message = "Status cannot be null")
    private Status status;

    @NotNull(message = "Issue type cannot be null")
    private IssueType issueType;

    @NotNull(message = "Department cannot be null")
    private Department department;

    @NotNull(message = "Staff ID cannot be null")
    private Long staffId;

    @Valid
    private Set<AttachmentDTO> attachments;

}
