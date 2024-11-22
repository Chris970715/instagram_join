package instagram_join.demo.rest;

import instagram_join.demo.dao.UserDAO;
import instagram_join.demo.dto.FollowerRequest;
import instagram_join.demo.entity.Follower;
import instagram_join.demo.entity.User;
import instagram_join.demo.service.FollowerService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/followers")
public class FollowerController {

    private final FollowerService followerService;
    private final UserDAO userDAO;

    public FollowerController(FollowerService followerService, UserDAO userDAO) {
        this.followerService = followerService;
        this.userDAO = userDAO;
    }

    // get all followers
    @GetMapping
    public List<Follower> findAll() {
        return followerService.findAll();
    }

    // add mapping for GET /followers/{followerId}
    @GetMapping("/{followerId}")
    public Follower getFollower(@PathVariable Long followerId) {
        return followerService.findById(followerId)
                .orElseThrow(() -> new RuntimeException("Follower id not found: " + followerId));
    }

    // add mapping for POST /followers -> add a new follower
    @PostMapping
    public Follower addFollower(@RequestBody FollowerRequest request) {
        return followerService.followUser(request.getFollowerId(), request.getFollowingId());
    }

    // update existing follower
    @PutMapping("/{followerId}")
    public Follower updateFollower(@PathVariable Long followerId, @RequestBody FollowerRequest request) {

        return followerService.findById(followerId)
                .map(existingFollower -> {

                    User newFollower = userDAO.findById(request.getFollowerId())
                            .orElseThrow(() -> new RuntimeException("Follower not found: " + request.getFollowerId()));
                    User newFollowing = userDAO.findById(request.getFollowingId())
                            .orElseThrow(() -> new RuntimeException("Following not found: " + request.getFollowingId()));

                    existingFollower.setFollower(newFollower);
                    existingFollower.setFollowing(newFollowing);

                    return followerService.save(existingFollower);
                })
                .orElseThrow(() -> new RuntimeException("Follower id not found: " + followerId));
    }

    // Delete a follower
    @DeleteMapping("/{followerId}")
    public void deleteFollower(@PathVariable Long followerId) {
        followerService.deleteById(followerId);
    }

    // Get followers of a specific user
    @GetMapping("/user/{userId}/followers")
    public List<Follower> getFollowers(@PathVariable Long userId) {
        return followerService.findFollowers(userId);
    }

    // Get following users of a specific user
    @GetMapping("/user/{userId}/following")
    public List<Follower> getFollowing(@PathVariable Long userId) {
        return followerService.findFollowingUsers(userId);
    }
}
