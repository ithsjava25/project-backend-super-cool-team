package org.example.cyberwatch.features.form.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@AllArgsConstructor
public class AttachmentDTO {

    private Long id;

    @NotNull(message = "File name cannot be null")
    @Size(min = 1, max = 255, message = "File name must be between 1 and 255 characters")
    private String fileName;

    @NotNull(message = "Content type cannot be null")
    private String contentType;

    @NotNull(message = "File size cannot be null")
    private Long fileSize;

    @NotNull(message = "S3 key cannot be null")
    private String s3Key;

    private LocalDateTime uploadDate;

    private Long reportFormId;

    public AttachmentDTO() {
    }
}

