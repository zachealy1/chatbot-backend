package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.MessageSource;
import org.springframework.http.ResponseEntity;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.repositories.AccountRequestRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @InjectMocks
    private AccountController controller;

    @Mock
    private MessageSource messages;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRequestRepository accountRequestRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ensure LocaleContextHolder.getLocale() == English
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void whenInvalidDate_thenReturnsBadRequest() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("username", "user1");
        userDetails.put("email", "user1@example.com");
        userDetails.put("password", "pass");
        userDetails.put("confirmPassword", "pass");
        userDetails.put("dateOfBirth", "not-a-date");

        when(messages.getMessage(eq("error.invalid.date"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Invalid date");

        ResponseEntity<String> resp = controller.registerUser(userDetails);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid date", resp.getBody());
        verify(messages).getMessage("error.invalid.date", null, Locale.ENGLISH);
    }

    @Test
    void whenAllValid_thenPersistsAndReturnsOk() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("username", "user1");
        userDetails.put("email", "user1@example.com");
        userDetails.put("password", "pass");
        userDetails.put("confirmPassword", "pass");
        // ISO format so your parseDateOfBirth(...) should succeed
        userDetails.put("dateOfBirth", "2000-01-01");

        // stub encoding & success message
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(messages.getMessage(eq("success.user.registered"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Registration successful");

        ResponseEntity<String> resp = controller.registerUser(userDetails);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Registration successful", resp.getBody());

        // verify that both save() calls happened
        verify(userRepository).save(any());
        verify(accountRequestRepository).save(any());
    }
}
