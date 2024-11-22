package instagram_join.demo.service;

import instagram_join.demo.dao.UserDAO;
import instagram_join.demo.entity.User;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserServiceImpl implements UserService {

    private final UserDAO userDAO;
    private final PostService postService;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserDAO userDAO, PostService postService, PasswordEncoder passwordEncoder) {
        this.userDAO = userDAO;
        this.postService = postService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public List<User> findAll() {
        return userDAO.findAll();
    }

    @Override
    public Optional<User> findById(Long id) {
        return userDAO.findById(id);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userDAO.findByEmail(email);
    }

    @Override
    @Transactional
    public User save(User theUser) {
        theUser.setPassword(passwordEncoder.encode(theUser.getPassword()));
        return userDAO.save(theUser);
    }

    @Override
    @Transactional
    public void deleteById(Long userId) {

        // 해당 사용자의 게시글을 삭제
        postService.deleteByUserId(userId);
        // 사용자 삭제
        userDAO.deleteById(userId);
    }
}
