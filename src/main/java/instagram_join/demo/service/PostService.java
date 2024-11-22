package instagram_join.demo.service;

import instagram_join.demo.entity.Post;

import java.util.List;
import java.util.Optional;

public interface PostService {

    List<Post> findAll();

    Optional<Post> findById(Long id);

    Post save(Post thePost);

    void deleteById(Long id);

    void deleteByUserId(Long userId);
}
