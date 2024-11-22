package instagram_join.demo.service;

import instagram_join.demo.entity.User;

import java.util.List;
import java.util.Optional;

public interface UserService {

    List<User> findAll();

    Optional<User> findById(Long id);

    Optional<User> findByEmail(String email);

    User save(User theUser);

    void deleteById(Long id);
}
