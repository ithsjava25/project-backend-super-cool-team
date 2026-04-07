package org.example.cyberwatch.features.comment.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDTO {

    @NotNull(message = "authorId cannot be null")
    private Long authorId;

    @NotBlank(message = "Comment text cannot be blank")
    private String text;
}