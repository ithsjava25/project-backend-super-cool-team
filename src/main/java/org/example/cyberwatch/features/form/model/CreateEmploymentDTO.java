package org.example.cyberwatch.features.form.model;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;
import org.example.cyberwatch.shared.model.enums.Status;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CreateEmploymentDTO {

    @NotBlank(message = "Social security number cannot be blank")
    @Pattern(regexp = "\\d{6}-\\d{4}|\\d{8}-\\d{4}", message = "Social security number must be in format YYMMDD-NNNN or YYYYMMDD-NNNN")
    private String socialSecurityNumber;

    @NotBlank(message = "First name cannot be blank")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    private String firstName;

    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    private String lastName;

    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    private String email;

    @NotBlank(message = "Phone number cannot be blank")
    @Pattern(regexp = "^\\d{7,15}$", message = "Phone number must be between 7 and 15 digits")
    private String phoneNumber;

    @NotNull(message = "Role cannot be null")
    private Role role;

    @NotNull(message = "Department cannot be null")
    private Department department;

    @NotNull(message = "Status cannot be null")
    private Status status;

    private LocalDateTime createdDate;

    //Long when fetching the Id?
    private Long hrId;

}
