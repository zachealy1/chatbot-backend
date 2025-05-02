package uk.gov.hmcts.reform.demo.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.RequestContextUtils;
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
    private final MessageSource messages;

    public LoginController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                           SessionRepository sessionRepository, MessageSource messages) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.sessionRepository = sessionRepository;
        this.messages = messages;
    }

    @PostMapping("/chat")
    public ResponseEntity<?> login(@RequestBody LoginRequest credentials,
                                   HttpServletRequest request,
                                   HttpServletResponse response) {
        return authenticateUser(credentials, false, request, response);
    }

    @PostMapping("/admin")
    public ResponseEntity<?> loginAdmin(@RequestBody LoginRequest credentials,
                                        HttpServletRequest request,
                                        HttpServletResponse response) {
        return authenticateUser(credentials, true, request, response);
    }

    private ResponseEntity<?> authenticateUser(
        LoginRequest credentials,
        boolean isAdminLogin,
        HttpServletRequest request,
        HttpServletResponse response
    ) {
        // Resolve locale from the request
        Locale locale = RequestContextUtils.getLocale(request);

        String username = credentials.getUsername();
        String password = credentials.getPassword();

        // 1) Basic non-empty check
        if (!isValidCredentials(username, password)) {
            String msg = messages.getMessage("login.required", null, locale);
            return ResponseEntity.badRequest().body(msg);
        }

        // 2) Lookup user
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isEmpty()) {
            String msg = messages.getMessage("login.invalid", null, locale);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
        }
        User user = optionalUser.get();

        // 3) Authorization (admin vs normal)
        if (!isAuthorizedUser(user, isAdminLogin)) {
            String key = isAdminLogin ? "login.admin.denied" : "login.user.denied";
            String msg = messages.getMessage(key, null, locale);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(msg);
        }

        // 4) Password check
        if (!passwordEncoder.matches(password, user.getPasswordHash())) {
            String msg = messages.getMessage("login.invalid", null, locale);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(msg);
        }

        // --- exactly as before: set up Spring SecurityContext ---
        List<SimpleGrantedAuthority> authorities = new ArrayList<>();
        if (Boolean.TRUE.equals(user.getIsAdmin())) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        } else {
            authorities.add(new SimpleGrantedAuthority("ROLE_USER"));
        }

        Authentication authentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Persist into the HTTP session so subsequent requests are authenticated
        new HttpSessionSecurityContextRepository()
            .saveContext(SecurityContextHolder.getContext(), request, response);
        logger.info("Authentication set for user: {}", username);
        // -----------------------------------------------------

        // 5) Success path: localized success message
        String successKey = isAdminLogin ? "login.success.admin" : "login.success.user";
        String successMsg = messages.getMessage(successKey, null, locale);

        return createSession(user, successMsg, response);
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
