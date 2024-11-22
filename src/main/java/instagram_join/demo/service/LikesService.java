package instagram_join.demo.service;

import instagram_join.demo.dao.LikesDAO;
import instagram_join.demo.entity.Likes;
import instagram_join.demo.entity.Post;
import instagram_join.demo.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class LikesService {

    @Autowired
    private LikesDAO likesDAO;

    public List<Likes> getLikedPosts(Long userId) {
        return likesDAO.findLikesByUserId(userId);
    }

    public Likes LikePost(User userId, Post postId) {

        Likes newLike = new Likes();
        newLike.setUserId(userId);
        newLike.setPostId(postId);

        return likesDAO.save(newLike);
    }

    @Transactional
    public Likes updateLikedPosts(Long likeId, User userId, Post postId) {
        Likes existingLike = likesDAO.findById(likeId)
                .orElseThrow(() -> new RuntimeException("Like not found"));

        existingLike.setUserId(userId);
        existingLike.setPostId(postId);

        return likesDAO.save(existingLike);
    }

    @Transactional
    public void unlikePost(Long likeId) {
        likesDAO.deleteById(likeId);
    }
}
