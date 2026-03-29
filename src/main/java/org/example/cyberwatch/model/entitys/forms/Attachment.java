package org.example.cyberwatch.model.entitys.forms;

import jakarta.persistence.*;

import java.time.LocalDateTime;

// Represents a file attachment linked to a case.
// Stores only metadata and S3-key (actual file in cloud storage).
// Access controlled per case authorization.
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

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getFileSize() {
        return fileSize;
    }

    public void setFileSize(Long fileSize) {
        this.fileSize = fileSize;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public ReportForm getReportForm() {
        return reportForm;
    }

    public void setReportForm(ReportForm reportForm) {
        this.reportForm = reportForm;
    }

    public String getS3Key() {
        return s3Key;
    }

    public void setS3Key(String s3Key) {
        this.s3Key = s3Key;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}
