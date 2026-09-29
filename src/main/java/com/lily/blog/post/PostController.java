package com.lily.blog.post;

import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/posts")
public class PostController {

    private static final Logger log = LoggerFactory.getLogger(PostController.class);

    private final PostRepository repository;

    public PostController(PostRepository repository) {
        this.repository = repository;
    }

    @GetMapping
    public List<PostDto.Response> list(Pageable pageable) {
        Page<Post> page = repository.findAll(pageable);
        log.info("posts listed: count={}", page.getNumberOfElements());
        return page.map(PostDto.Response::from).getContent();
    }

    @GetMapping("/{id}")
    public PostDto.Response get(@PathVariable Long id) {
        return repository.findById(id)
                .map(PostDto.Response::from)
                .orElseThrow(() -> new PostNotFoundException(id));
    }

    @PostMapping
    public ResponseEntity<PostDto.Response> create(@Valid @RequestBody PostDto.CreateRequest request) {
        Post saved = repository.save(new Post(request.title(), request.content(), request.author()));
        log.info("post created: id={} author={}", saved.getId(), saved.getAuthor());
        return ResponseEntity
                .created(URI.create("/api/posts/" + saved.getId()))
                .body(PostDto.Response.from(saved));
    }

    @PutMapping("/{id}")
    @Transactional
    public PostDto.Response update(@PathVariable Long id, @Valid @RequestBody PostDto.UpdateRequest request) {
        Post post = repository.findById(id).orElseThrow(() -> new PostNotFoundException(id));
        post.update(request.title(), request.content());
        log.info("post updated: id={}", id);
        return PostDto.Response.from(post);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        if (!repository.existsById(id)) {
            throw new PostNotFoundException(id);
        }
        repository.deleteById(id);
        log.info("post deleted: id={}", id);
        return ResponseEntity.noContent().build();
    }
}
