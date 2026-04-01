package org.example.cyberwatch.features.form.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class EmploymentFormDTO {

    private Long id;

    @NotNull(message = "Employee ID cannot be null")
    private Long employeeId;

    @NotNull(message = "Status cannot be null")
    private Status status;

    @NotNull(message = "Title cannot be null")
    @Size(min = 5, max = 100, message = "Title must be between 5 and 100 characters")
    private String title;

    @NotNull(message = "Description cannot be null")
    @Size(min = 10, max = 1000, message = "Description must be between 10 and 1000 characters")
    private String description;

    private LocalDateTime createdDate;

    private LocalDateTime approvedDate;

    private Long approverStaffId;

    public EmploymentFormDTO() {
    }
}

