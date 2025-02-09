package uk.gov.hmcts.reform.demo.controllers;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

@RestController
@RequestMapping("/login")
public class LoginController {

    private static final Logger logger = LoggerFactory.getLogger(LoginController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SessionRepository sessionRepository;

    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           SessionRepository sessionRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials, HttpServletResponse response) {
        logger.info("Attempting login for username: {}", credentials.getUsername());
        return authenticateUser(credentials, false, response);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest credentials, HttpServletResponse response) {
        return authenticateUser(credentials, true, response);
    }

    private ResponseEntity<?> authenticateUser(LoginRequest credentials,
                                               boolean isAdminLogin, HttpServletResponse response) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();
        logger.debug("Attempting login for username: {}", username);

        if (!isValidCredentials(username, password)) {
            logger.debug("Credentials are invalid: username or password is empty");
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            logger.debug("User not found for username: {}", username);
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        User user = optionalUser.get();
        logger.debug("User found: {} (canLogin: {}, isAdmin: {})",
                     user.getUsername(), user.getCanLogin(), user.getIsAdmin());

        if (!isAuthorizedUser(user, isAdminLogin)) {
            logger.debug("User not authorized: canLogin={}, isAdmin={}, requestedAdmin={}",
                         user.getCanLogin(), user.getIsAdmin(), isAdminLogin);
            return ResponseEntity.status(403).body(isAdminLogin ? "Admin access denied." :
                "Your account is not allowed to log in. Please contact support.");
        }

        logger.debug("User is authorized for login.");

        // Log a masked version of the password (never log the raw password in production)
        logger.debug("Comparing provided password with stored hash for user: {}", username);
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            logger.debug("Password mismatch for username: {}. Provided password: [{}] does not match stored hash.",
                         username, maskPassword(password));
            return ResponseEntity.status(401).body("Invalid username or password.");
        }

        logger.debug("Password matched for username: {}", username);
        return createSession(user, isAdminLogin ? "Admin logged in successfully."
            : "User logged in successfully.", response);
    }

    private String maskPassword(String password) {
        if (password.length() < 4) {
            return "****";
        }
        return password.substring(0, 2) + "****" + password.substring(password.length() - 2);
    }


    private ResponseEntity<?> createSession(User user, String successMessage, HttpServletResponse response) {
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusHours(24);

        Session session = new Session(sessionToken, user, createdAt, expiresAt);
        sessionRepository.save(session);
        logger.debug("Session created for user {} with token {}", user.getUsername(), sessionToken);

        // Set the session token as a cookie in the response.
        Cookie cookie = new Cookie("JSESSIONID", sessionToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        // Optionally, set the max age to 24 hours (in seconds)
        cookie.setMaxAge(24 * 60 * 60);
        response.addCookie(cookie);
        logger.debug("Cookie set with name JSESSIONID and token {}", sessionToken);

        Map<String, String> responseBody = Map.of(
            "message", successMessage,
            "sessionToken", sessionToken  // Optionally include it in the response body as well.
        );

        return ResponseEntity.ok(responseBody);
    }

    /**
     * Validates if username and password are non-null and non-empty.
     */
    private boolean isValidCredentials(String username, String password) {
        return username != null && password != null && !username.trim().isEmpty() && !password.trim().isEmpty();
    }

    /**
     * Checks if a user is authorized to log in based on can_login and is_admin fields.
     */
    private boolean isAuthorizedUser(User user, boolean isAdminLogin) {
        return user.getCanLogin() && (!isAdminLogin || user.getIsAdmin());
    }
}
