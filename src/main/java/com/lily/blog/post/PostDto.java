package com.lily.blog.post;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.Instant;

public class PostDto {

    public record CreateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content,
            @NotBlank @Size(max = 100) String author) {}

    public record UpdateRequest(
            @NotBlank @Size(max = 200) String title,
            @NotBlank String content) {}

    public record Response(
            Long id,
            String title,
            String content,
            String author,
            Instant createdAt,
            Instant updatedAt) {

        public static Response from(Post post) {
            return new Response(
                    post.getId(), post.getTitle(), post.getContent(),
                    post.getAuthor(), post.getCreatedAt(), post.getUpdatedAt());
        }
    }
}
