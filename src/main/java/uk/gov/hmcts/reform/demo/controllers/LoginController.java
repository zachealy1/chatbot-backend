package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.dto.LoginRequest;
import uk.gov.hmcts.reform.demo.entities.Session;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.SessionRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/login")
public class LoginController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;

    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
    }

    /**
     * Login endpoint to authenticate users and create a new session.
     *
     * @param credentials A LoginRequest object containing username and password.
     * @return A success message with session token upon successful authentication.
     */
    @PostMapping("/")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        User user = optionalUser.get();

        // Verify the password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        // Generate a unique session token
        String sessionToken = UUID.randomUUID().toString();

        // Define session duration (e.g., 24 hours)
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusHours(24);

        // Create and save the session
        Session session = new Session(sessionToken, user, createdAt, expiresAt);
        sessionRepository.save(session);

        // Optionally, you can return additional session details
        Map<String, String> response = Map.of(
            "message", "User logged in successfully.",
            "sessionToken", sessionToken
                                             );

        return ResponseEntity.ok(response);
    }

    /**
     * Login endpoint to authenticate users and create a new session.
     *
     * @param credentials A LoginRequest object containing username and password.
     * @return A success message with session token upon successful authentication.
     */
    @PostMapping("/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest credentials) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (username == null || password == null || username.trim().isEmpty() || password.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        User user = optionalUser.get();

        // Verify the password
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        // Generate a unique session token
        String sessionToken = UUID.randomUUID().toString();

        // Define session duration (e.g., 24 hours)
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusHours(24);

        // Create and save the session
        Session session = new Session(sessionToken, user, createdAt, expiresAt);
        sessionRepository.save(session);

        // Optionally, you can return additional session details
        Map<String, String> response = Map.of(
            "message", "User logged in successfully.",
            "sessionToken", sessionToken
                                             );

        return ResponseEntity.ok(response);
    }
}
