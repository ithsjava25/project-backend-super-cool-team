package org.example.cyberwatch.features.comment.model;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CommentDTO {

    // authorId är borttaget — författaren hämtas från den inloggade användaren via SecurityContext i CommentService för att förhindra imitation.
    @NotBlank(message = "Comment text cannot be blank")
    private String text;
}