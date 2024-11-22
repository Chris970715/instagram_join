package instagram_join.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class LikesRequest {

    private Long userId;
    private Long postId;
}
