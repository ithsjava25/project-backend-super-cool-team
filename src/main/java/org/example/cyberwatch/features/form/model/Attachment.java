package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

// Represents a file attachment linked to a case.
// Stores only metadata and S3-key (actual file in cloud storage).
// Access controlled per case authorization.
@Getter
@Setter
@Entity
public class Attachment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false, length = 255)
    @NotBlank(message = "File name cannot be blank")
    @Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    @NotBlank(message = "Content type cannot be blank")
    @Size(max = 100, message = "Content type must be max 100 characters")
    private String contentType;

    @Column(name = "file_size", nullable = false)
    @NotNull(message = "File size cannot be null")
    @Positive(message = "File size must be positive")
    private Long fileSize;

    @Column(name = "s3_key", nullable = false, length = 500)
    @NotBlank(message = "S3 key cannot be blank")
    @Size(min = 1, max = 500, message = "S3 key must be between 1 and 500 characters")
    private String s3Key;

    @Column(name = "upload_date", nullable = false, updatable = false)
    @CreationTimestamp
    private LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_form_id", nullable = false)
    private ReportForm reportForm;


    public Attachment() {
    }
}
