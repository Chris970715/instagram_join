package instagram_join.demo.rest;

import instagram_join.demo.dto.PostRequest;
import instagram_join.demo.entity.Post;
import instagram_join.demo.service.PostService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/posts")
public class PostController {

    private final PostService postService;

    public PostController(PostService postService) {
        this.postService = postService;
    }

    // expose "/posts" and return a list of getPost
    @GetMapping
    public List<Post> findAll() {
        return postService.findAll();
    }

    // add mapping for GET /posts/{postId}
    @GetMapping("/{postId}")
    public Post getPost(@PathVariable Long postId) {
        return postService.findById(postId)
                .orElseThrow(() -> new RuntimeException("Post id not found: " + postId));
    }

    // add mapping for POST /posts -> add a new post
    @PostMapping
    public Post addPost(@RequestBody PostRequest postRequest) {

        Post post = new Post();
        post.setCaption(postRequest.getCaption());
        post.setUser(postRequest.getUser());
        post.setCreatedAt(LocalDateTime.now());

        return postService.save(post);
    }

    // Update existing post
    @PutMapping("/{postId}")
    public Post updatePost(@PathVariable Long postId, @RequestBody PostRequest postRequest) {
        return postService.findById(postId)
                .map(existingPost -> {
                    existingPost.setCaption(postRequest.getCaption());
                    existingPost.setUser(postRequest.getUser());
                    return postService.save(existingPost);
                })
                .orElseThrow(() -> new RuntimeException("Post id not found: " + postId));
    }

    // Delete existing post
    @DeleteMapping("/{postId}")
    public void deletePost(@PathVariable Long postId) {
        postService.findById(postId)
                .ifPresentOrElse(
                        existingPost -> postService.deleteById(postId),
                        () -> {
                            throw new RuntimeException("Post id not found: " + postId);
                        }
                );
    }
}
