package instagram_join.demo.rest;

import instagram_join.demo.dto.NewsFeedResponse;
import instagram_join.demo.dto.PostDTO;
import instagram_join.demo.service.NewsFeedService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/newsFeed")
public class NewsFeedController {

    // 기본 페이지 크기 설정
    @Value("${newsFeed.page.default-size}")
    private int defaultPageSize;

    @Autowired
    private final NewsFeedService newsFeedService;

    public NewsFeedController(NewsFeedService newsFeedService) {
        this.newsFeedService = newsFeedService;
    }

    // Get newsFeed
    @GetMapping("/{userId}")
    public NewsFeedResponse getNewsFeed(@PathVariable Long userId,
                                        @RequestParam(defaultValue = "0") int page,
                                        @RequestParam(defaultValue = "${newsFeed.page.default-size}") int size) {
        // @PathVariable: URL 경로에서 userId를 변수로 받아옴
        // @RequestParam: 쿼리 매개변수 -> page & size 설정

        // 요청한 페이지 번호와 크기에 따라 페이징 처리를 할 수 있음
        Pageable pageable = PageRequest.of(page, size);

        // 가져온 게시글 Id 리스트를 사용해 PostDTO 를 페이징 처리
        Page<PostDTO> postDTOPage = newsFeedService.getNewsFeed(userId, pageable);

        // NewsFeedResponse 객체를 생성하여 반환
        return new NewsFeedResponse(postDTOPage.getContent(), postDTOPage.getNumber(),
                postDTOPage.getSize(), postDTOPage.getTotalElements());
    }
}
