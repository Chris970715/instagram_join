package instagram_join.demo.dao;

import instagram_join.demo.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserDAO extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);
}
