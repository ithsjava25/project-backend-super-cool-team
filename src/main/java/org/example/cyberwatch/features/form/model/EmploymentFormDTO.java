package org.example.cyberwatch.features.form.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.example.cyberwatch.shared.model.enums.ApprovalStatus;
import org.example.cyberwatch.shared.model.enums.Department;
import org.example.cyberwatch.shared.model.enums.Role;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EmploymentFormDTO {

    private Long id;

    private String socialSecurityNumber;

    private String firstName;

    private String lastName;

    private String email;

    private String phoneNumber;

    private Role role;

    private Department department;

    private ApprovalStatus status;

    private LocalDateTime createdDate;

    //Long when fetching the Id?
    private Long hrId;

    private Long approverManagementId;

}

