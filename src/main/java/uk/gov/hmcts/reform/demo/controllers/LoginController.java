package uk.gov.hmcts.reform.demo.controllers;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
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

    // Notice that we now add HttpServletRequest as a parameter.
    @PostMapping("/chat")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials,
                                   HttpServletResponse response,
                                   HttpServletRequest request) {
        return authenticateUser(credentials, false, response, request);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest credentials,
                                        HttpServletResponse response,
                                        HttpServletRequest request) {
        return authenticateUser(credentials, true, response, request);
    }

    private ResponseEntity<?> authenticateUser(LoginRequest credentials, boolean isAdminLogin,
                                               HttpServletResponse response,
                                               HttpServletRequest request) {
        String username = credentials.getUsername();
        String password = credentials.getPassword();

        if (!isValidCredentials(username, password)) {
            return ResponseEntity.badRequest().body("Username and password are required.");
        }

        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }
        User user = optionalUser.get();

        if (!isAuthorizedUser(user, isAdminLogin)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(isAdminLogin ? "Admin access denied."
                          : "Your account is not allowed to log in. Please contact support.");
        }

        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid username or password.");
        }

        // --- Set the Authentication in the SecurityContext ---
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (user.getIsAdmin() != null && user.getIsAdmin()) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }
        Authentication authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Save the SecurityContext in the session so that subsequent requests see the authenticated user.
        new HttpSessionSecurityContextRepository().saveContext(SecurityContextHolder.getContext(), request, response);
        logger.info("Authentication set for user: {}", username);
        // -----------------------------------------------------

        // Create a session record (for your own tracking) and let the container manage the JSESSIONID cookie.
        return createSession(
            user,
            isAdminLogin ? "Admin logged in successfully." : "User logged in successfully.",
            response
        );
    }

    private ResponseEntity<?> createSession(User user, String successMessage, HttpServletResponse response) {
        // Generate a custom session token for your own use (this is independent of the container-managed session).
        String sessionToken = UUID.randomUUID().toString();
        LocalDateTime createdAt = LocalDateTime.now();
        LocalDateTime expiresAt = createdAt.plusHours(24);

        Session session = new Session(sessionToken, user, createdAt, expiresAt);
        sessionRepository.save(session);
        logger.debug("Session created for user {} with token {}", user.getUsername(), sessionToken);

        // Do not manually set a JSESSIONID cookie; let the container manage it.
        // If you need to return your custom token, include it in the response body.
        Map<String, String> responseBody = Map.of(
            "message", successMessage,
            "sessionToken", sessionToken
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
