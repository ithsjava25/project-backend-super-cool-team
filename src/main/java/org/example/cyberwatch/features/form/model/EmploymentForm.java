package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.shared.model.enums.Status;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

//Could it be an idea that the EmployeeForm needs to be approved by an Manager, like a signature on a paper form?
// Then we could have a status field in the EmployeeForm with the following states:
//DRAFT -> SUBMITTED -> APPROVED -> COMPLETED -> REJECTED
@Getter
@Setter
@Entity
public class EmploymentForm {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Long id;

    @Column(name = "first_name")
    @NotBlank(message = "First name cannot be blank")
    @Size(min = 2, max = 50, message = "First name must be between 2 and 50 characters")
    String firstName;

    @Column(name = "last_name")
    @NotBlank(message = "Last name cannot be blank")
    @Size(min = 2, max = 50, message = "Last name must be between 2 and 50 characters")
    String lastName;

    @Column(name = "email")
    @Email(message = "Email should be valid")
    @NotBlank(message = "Email cannot be blank")
    String email;

    @Enumerated(EnumType.STRING)
    @NotNull(message = "Status cannot be null")
    Status status;

    @Column(name = "created_date")
    @NotNull(message = "Created date cannot be null")
    @CreationTimestamp
    LocalDateTime createdDate;

    // Relation - Ett formulär kan tillhöra en anställd (vid ändringar/uppdateringar)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "staff_id", nullable = true)
    private Staff staff;

    public EmploymentForm() {
    }

}
