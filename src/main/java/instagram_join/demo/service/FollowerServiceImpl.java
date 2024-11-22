package instagram_join.demo.service;

import instagram_join.demo.dao.FollowerDAO;
import instagram_join.demo.entity.Follower;
import instagram_join.demo.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class FollowerServiceImpl implements FollowerService {

    private final FollowerDAO followerDAO;

    public FollowerServiceImpl(FollowerDAO followerDAO) {
        this.followerDAO = followerDAO;
    }

    @Override
    public List<Follower> findAll() {
        return followerDAO.findAll();
    }

    @Override
    public Optional<Follower> findById(Long followerId) {
        return followerDAO.findById(followerId);
    }

    @Override
    @Transactional
    public void deleteById(Long followerId) {
        followerDAO.deleteById(followerId);
    }

    @Override
    @Transactional
    public Follower followUser(Long followerId, Long followingId) {

        // User 객체를 ID만 설정해서 생성
        User follower = new User();
        follower.setId(followerId);

        User following = new User();
        following.setId(followingId);

        Follower newFollower = new Follower(follower, following);
        return followerDAO.save(newFollower);
    }

    @Override
    public List<Follower> findFollowers(Long userId) {
        return followerDAO.findFollowers(userId);
    }

    @Override
    public List<Follower> findFollowingUsers(Long userId) {
        return followerDAO.findFollowing(userId);
    }

    @Override
    public Follower save(Follower theFollower) {
        return followerDAO.save(theFollower);
    }

}
