package instagram_join.demo.dao;

import instagram_join.demo.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostDAO extends JpaRepository<Post, Long> {

    @Query("SELECT p FROM Post p WHERE p.user.id IN :userIds ORDER BY p.updatedAt DESC")
    Page<Post> findByUserIdInOrderByUpdatedAtDesc(List<Long> userIds, Pageable pageable);

    @Query("SELECT p FROM Post p JOIN FETCH p.user u WHERE p.id IN :postIds ORDER BY p.updatedAt DESC")
    List<Post> findByIdIn(@Param("postIds") List<Long> postIds);

    // 특정 유저가 작성한 모든 게시글을 가져온다 -> 특정 유저를 삭제하면 그 유저가 작성한
    // 게시글 전부 삭제
    @Query("SELECT p FROM Post p WHERE p.user.id = :userId")
    List<Post> findAllPostsByUserId(@Param("userId") Long userId);
}
