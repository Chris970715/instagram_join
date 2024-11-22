package instagram_join.demo.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

import java.io.Serializable;

@Getter @Setter
@NoArgsConstructor
@ToString
public class UserDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long id;
    private String userName;
    private String email;

    public UserDTO(Long id, String userName, String email) {
        this.id = id;
        this.userName = userName;
        this.email = email;
    }
}
