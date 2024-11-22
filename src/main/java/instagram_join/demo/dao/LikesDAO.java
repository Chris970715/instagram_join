package instagram_join.demo.dao;

import instagram_join.demo.entity.Likes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikesDAO extends JpaRepository<Likes, Long> {

    @Query("SELECT l FROM Likes l JOIN FETCH l.postId p JOIN FETCH l.userId u WHERE l.userId.id = :userId")
    List<Likes> findLikesByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM Likes l WHERE l.postId.id = :postId")
    void deleteLikesByPostId(@Param("postId") Long postId);
}
