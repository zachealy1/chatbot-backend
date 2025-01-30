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

    @PostMapping("/")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials) {
        return authenticateUser(credentials, false);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest credentials) {
        return authenticateUser(credentials, true);
    }

    /**
     * Authenticates a user or admin and creates a session if valid.
     *
     * @param credentials The login request containing username and password.
     * @param isAdminLogin Set to true if admin login is required.
     * @return Response entity with login success or error message.
     */
    private ResponseEntity<?> authenticateUser(LoginRequest credentials, boolean isAdminLogin) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (!isValidCredentials(username, password)) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        User user = optionalUser.get();

        if (!isAuthorizedUser(user, isAdminLogin)) {
            return ResponseEntity.status(403).body(isAdminLogin ? "Admin access denied." :
                                                       "Your account is not allowed to "
                                                           + "log in. Please contact support.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        return createSession(user, isAdminLogin ? "Admin logged in successfully." : "User logged in successfully.");
    }

    /**
     * Validates if username and password are non-null and non-empty.
     */
    private boolean isValidCredentials(String username, String password) {
        return username != null && password != null && !username.trim().isEmpty() && !password.trim().isEmpty();
    }

    /**
     * Checks if a user is authorized to log in based on `can_login` and `is_admin` fields.
     */
    private boolean isAuthorizedUser(User user, boolean isAdminLogin) {
        return user.getCanLogin() && (!isAdminLogin || user.getIsAdmin());
    }

    /**
     * Creates a session for the authenticated user.
     */
    private ResponseEntity<?> createSession(User user, String successMessage) {
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusHours(24);

        Session session = new Session(sessionToken, user, createdAt, expiresAt);
        sessionRepository.save(session);

        Map<String, String> response = Map.of(
            "message", successMessage,
            "sessionToken", sessionToken
                                             );

        return ResponseEntity.ok(response);
    }
}
