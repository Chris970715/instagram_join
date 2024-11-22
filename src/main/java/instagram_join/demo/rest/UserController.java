package instagram_join.demo.rest;

import instagram_join.demo.entity.User;
import instagram_join.demo.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;

    public UserController(UserService userService, PasswordEncoder passwordEncoder) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
    }

    // expose "/users" and return a list of users
    @GetMapping
    public List<User> findAll() {
        return userService.findAll();
    }

    // add mapping for GET /users/{userId}
    @GetMapping("/{userId}")
    public ResponseEntity<User> getUser(@PathVariable Long userId, HttpServletRequest request) {
        // 세션에서 userId 가져오기
        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute("userId") == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        // 세션에 저장된 userId와 요청된 userId 비교
        Long sessionUserId = Long.valueOf(session.getAttribute("userId").toString());
        if (!sessionUserId.equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(null);
        }

        // 세션이 유효하면 userId를 사용하여 사용자 정보 반환
        return userService.findById(userId)
                .map(ResponseEntity::ok)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "User id not found"));
    }

    // add mapping for POST /users - signup
    @PostMapping("/signup")
    public User addUser(@RequestBody User theUser) {
        try {
            return userService.save(theUser);
        } catch (Exception e) {
            throw new RuntimeException("Failed to signup");
        }
    }

    // add mapping for POST /users - login
    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody User loginUser, HttpServletRequest request) {
        User user = userService.findByEmail(loginUser.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid Email or password"));

        if (!passwordEncoder.matches(loginUser.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid password");
        }

        HttpSession session = request.getSession();
        // 세션에 사용자 정보 저장
        session.setAttribute("userId", user.getId());
        session.setAttribute("userName", user.getUserName());

        return ResponseEntity.ok("Logged in successfully");
    }

    // add mapping for POST /users - logout
    @PostMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate(); // 세션 무효화
        }
        return ResponseEntity.ok("Logged out successfully");
    }

    // Update existing user
    @PutMapping("/{userId}")
    public User updateUser(@PathVariable Long userId, @RequestBody User theUser) {
        return userService.findById(userId)
                .map(existingUser -> {
                    existingUser.setUserName(theUser.getUserName());
                    existingUser.setEmail(theUser.getEmail());
                    existingUser.setPassword(theUser.getPassword());
                    return userService.save(existingUser);
                })
                .orElseThrow(() -> new RuntimeException("User id not found: " + userId));
    }

    // Delete a user
    @DeleteMapping("/{userId}")
    public void deleteUser(@PathVariable Long userId) {
        userService.deleteById(userId);
    }
}
