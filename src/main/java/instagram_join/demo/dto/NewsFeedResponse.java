package instagram_join.demo.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class NewsFeedResponse {

    private List<PostDTO> posts;
    private int pageNumber;
    private int pageSize;
    private long totalElements;

    public NewsFeedResponse(List<PostDTO> posts, int pageNumber, int pageSize, long totalElements) {
        this.posts = posts;
        this.pageNumber = pageNumber;
        this.pageSize = pageSize;
        this.totalElements = totalElements;
    }
}
