package instagram_join.demo.dto;


import instagram_join.demo.entity.User;
import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class PostRequest {

    private String caption;
    private User user;
}
