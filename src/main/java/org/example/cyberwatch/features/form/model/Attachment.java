package org.example.cyberwatch.features.form.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

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
    Long id;

    @Column(name = "file_name")
    String fileName;

    @Column(name = "content_type")
    String contentType;


    @Column(name = "file_size")
    Long fileSize;

    @Column(name = "s3_key")
    String s3Key;

    @Column(name = "upload_date")
    LocalDateTime uploadDate;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "report_form_id", nullable = false)
    ReportForm reportForm;


    public Attachment() {
    }
}
