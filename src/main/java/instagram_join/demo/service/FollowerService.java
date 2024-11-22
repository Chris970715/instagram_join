package instagram_join.demo.service;

import instagram_join.demo.entity.Follower;

import java.util.List;
import java.util.Optional;

public interface FollowerService {

    List<Follower> findAll();

    Optional<Follower> findById(Long followerId);

    void deleteById(Long followerId);

    Follower followUser(Long followerId, Long followingId);

    List<Follower> findFollowers(Long userId);

    List<Follower> findFollowingUsers(Long userId);

    Follower save(Follower existingFollower);

}
