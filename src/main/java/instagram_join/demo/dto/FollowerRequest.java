package instagram_join.demo.dto;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class FollowerRequest {

    private Long followerId;
    private Long followingId;

}
