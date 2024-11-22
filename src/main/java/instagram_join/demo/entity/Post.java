package instagram_join.demo.entity;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "Post")
@Getter @Setter

@JsonIgnoreProperties(ignoreUnknown = true)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne // 여러 개의 게시글들이 하나의 유저에 의해 생성될 수 있다
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "caption", nullable = false)
    private String caption;

    @Column(name = "created_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt = LocalDateTime.now();  // 기본값 설정

    @Column(name = "updated_at")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt = LocalDateTime.now();  // 기본값 설정

    @OneToMany(mappedBy = "postId")
    @JsonIgnore
    private List<Likes> likes = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        createdAt = now;
        updatedAt = now;  // 생성 시점에 updatedAt도 설정
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Post() {
    }

    public Post(Long id) {
        this.id = id;
    }

    public Post(User user, String caption, LocalDateTime createdAt, LocalDateTime updatedAt) {
        this.user = user;
        this.caption = caption;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Post{" +
                "id=" + id +
                ", user=" + user +
                ", caption='" + caption + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}
