package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import uk.gov.hmcts.reform.demo.dto.LoginRequest;
import uk.gov.hmcts.reform.demo.entities.Session;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.SessionRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class LoginControllerTest {

    @InjectMocks
    private LoginController controller;

    @Mock
    private UserRepository userRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private org.springframework.context.MessageSource messages;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
        SecurityContextHolder.clearContext();
    }

    @Test
    void whenCredentialsMissing_thenReturnsBadRequest() {
        LoginRequest creds = new LoginRequest(null, null);
        when(messages.getMessage(eq("login.required"), isNull(), any(Locale.class)))
            .thenReturn("Credentials required");

        ResponseEntity<?> resp = controller.login(creds, request, response);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Credentials required", resp.getBody());
        verify(messages).getMessage("login.required", null, request.getLocale());
        verifyNoInteractions(userRepository, passwordEncoder, sessionRepository);
    }

    @Test
    void whenUserNotFound_thenReturnsUnauthorized() {
        LoginRequest creds = new LoginRequest("alice", "pass");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.empty());
        when(messages.getMessage(eq("login.invalid"), isNull(), any(Locale.class)))
            .thenReturn("Invalid credentials");

        ResponseEntity<?> resp = controller.login(creds, request, response);

        assertEquals(401, resp.getStatusCodeValue());
        assertEquals("Invalid credentials", resp.getBody());
        verify(userRepository).findByUsername("alice");
        verify(messages).getMessage("login.invalid", null, request.getLocale());
        verifyNoMoreInteractions(passwordEncoder, sessionRepository);
    }

    @Test
    void whenUserCannotLogin_thenReturnsForbidden() {
        LoginRequest creds = new LoginRequest("bob", "pw");
        User user = new User();
        user.setUsername("bob");
        user.setCanLogin(false);
        when(userRepository.findByUsername("bob")).thenReturn(Optional.of(user));
        when(messages.getMessage(eq("login.user.denied"), isNull(), any(Locale.class)))
            .thenReturn("User denied");

        ResponseEntity<?> resp = controller.login(creds, request, response);

        assertEquals(403, resp.getStatusCodeValue());
        assertEquals("User denied", resp.getBody());
        verify(userRepository).findByUsername("bob");
        verify(messages).getMessage("login.user.denied", null, request.getLocale());
        verifyNoInteractions(passwordEncoder, sessionRepository);
    }

    @Test
    void whenPasswordMismatch_thenReturnsUnauthorized() {
        LoginRequest creds = new LoginRequest("carol", "wrong");
        User user = new User();
        user.setUsername("carol");
        user.setCanLogin(true);
        user.setPasswordHash("hashed");
        when(userRepository.findByUsername("carol")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "hashed")).thenReturn(false);
        when(messages.getMessage(eq("login.invalid"), isNull(), any(Locale.class)))
            .thenReturn("Invalid credentials");

        ResponseEntity<?> resp = controller.login(creds, request, response);

        assertEquals(401, resp.getStatusCodeValue());
        assertEquals("Invalid credentials", resp.getBody());
        verify(passwordEncoder).matches("wrong", "hashed");
        verify(messages).getMessage("login.invalid", null, request.getLocale());
        verifyNoMoreInteractions(sessionRepository);
    }

    @Test
    void whenValidCredentials_thenCreatesSessionAndReturnsOk() {
        LoginRequest creds = new LoginRequest("dave", "secret");
        User user = new User();
        user.setUsername("dave");
        user.setCanLogin(true);
        user.setPasswordHash("hash");
        user.setIsAdmin(false);
        when(userRepository.findByUsername("dave")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);
        when(messages.getMessage(eq("login.success.user"), isNull(), any(Locale.class)))
            .thenReturn("Welcome user");

        // capture saved session
        ArgumentCaptor<Session> sessionCaptor = ArgumentCaptor.forClass(Session.class);
        doAnswer(invocation -> {
            Session saved = invocation.getArgument(0);
            // simulate JPA setting token
            assertNotNull(saved.getSessionToken());
            return null;
        }).when(sessionRepository).save(sessionCaptor.capture());

        ResponseEntity<?> resp = controller.login(creds, request, response);

        assertEquals(200, resp.getStatusCodeValue());
        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) resp.getBody();
        assertEquals("Welcome user", body.get("message"));
        String token = body.get("sessionToken");
        assertNotNull(token);
        assertEquals(token, sessionCaptor.getValue().getSessionToken());

        // SecurityContext should hold authentication
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(user, auth.getPrincipal());
    }
}
