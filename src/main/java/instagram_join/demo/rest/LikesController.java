package instagram_join.demo.rest;

import instagram_join.demo.dto.LikesRequest;
import instagram_join.demo.entity.Likes;
import instagram_join.demo.entity.Post;
import instagram_join.demo.entity.User;
import instagram_join.demo.service.LikesService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/likes")
public class LikesController {

    private final LikesService likesService;

    public LikesController(LikesService likesService) {
        this.likesService = likesService;
    }

    @GetMapping("/{userId}")
    public List<Likes> getLike(@PathVariable Long userId) {
        return likesService.getLikedPosts(userId);
    }

    @PostMapping
    public Likes addLike(@RequestBody LikesRequest request) {

        User user = new User(request.getUserId());
        Post post = new Post(request.getPostId());

        return likesService.LikePost(user, post);
    }

    @PutMapping("/{likeId}")
    public Likes updateLike(@PathVariable Long likeId, @RequestBody LikesRequest request) {

        User user = new User(request.getUserId());
        Post post = new Post(request.getPostId());

        return likesService.updateLikedPosts(likeId, user, post);
    }

    @DeleteMapping("/{likeId}")
    public void unlike(@PathVariable Long likeId) {
        likesService.unlikePost(likeId);
    }
}
