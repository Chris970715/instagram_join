package instagram_join.demo.service;

import instagram_join.demo.dao.LikesDAO;
import instagram_join.demo.dao.PostDAO;
import instagram_join.demo.entity.Post;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PostServiceImpl implements PostService {

    private final PostDAO postDAO;
    private final LikesDAO likesDAO;
    private final NewsFeedService newsFeedService;

    public PostServiceImpl(PostDAO postDAO, LikesDAO likesDAO, NewsFeedService newsFeedService) {
        this.postDAO = postDAO;
        this.likesDAO = likesDAO;
        this.newsFeedService = newsFeedService;
    }

    @Override
    public List<Post> findAll() {
        return postDAO.findAll();
    }

    @Override
    public Optional<Post> findById(Long id) {
        return postDAO.findById(id);
    }

    @Override
    @Transactional
    public Post save(Post thePost) {

        // 게시글 DB 저장
        Post savedPost = postDAO.save(thePost);

        // Fan-out 작업은 메시지 큐에 등록만 함 (비동기 처리)
        newsFeedService.enqueueFanOutPost(savedPost);
        return savedPost;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        // 뉴스피드에서 게시글 제거
        newsFeedService.removePostFromNewsFeeds(id);
        // 종아요 삭제
        likesDAO.deleteLikesByPostId(id);
        // 게시글 삭제
        postDAO.deleteById(id);
    }

    @Override
    @Transactional
    public void deleteByUserId(Long userId) {
        // 유저 ID로 해당 유저의 모든 게시글을 조회
        List<Post> posts = postDAO.findAllPostsByUserId(userId);

        // 각 게시글에 대한 좋아요 삭제 후 게시글 삭제
        for (Post post : posts) {
            // 뉴스피드에서 게시글 제거
            newsFeedService.removePostFromNewsFeeds(post.getId());
            // 좋아요 삭제
            likesDAO.deleteLikesByPostId(post.getId());
            // 게시글 삭제
            postDAO.delete(post);
        }
    }
}
