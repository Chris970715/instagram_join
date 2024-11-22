package instagram_join.demo.dao;

import instagram_join.demo.entity.Follower;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FollowerDAO extends JpaRepository<Follower, Long> {

    @Query("SELECT f FROM Follower f JOIN FETCH f.follower WHERE f.following.id = :userId")
    List<Follower> findFollowers(@Param("userId") Long userId);

    @Query("SELECT f FROM Follower f JOIN FETCH f.following WHERE f.follower.id = :userId")
    List<Follower> findFollowing(@Param("userId") Long userId);
}
