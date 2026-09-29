package com.lily.blog.post;

public class PostNotFoundException extends RuntimeException {
    public PostNotFoundException(Long id) {
        super("post not found: id=" + id);
    }
}
