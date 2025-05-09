package uk.gov.hmcts.reform.demo.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.entities.PasswordResetToken;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.PasswordResetTokenRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.services.EmailService;

class ForgotPasswordControllerTest {

    @InjectMocks
    private ForgotPasswordController controller;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;

    @Mock
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenEmailMissingOrInvalid_thenReturnsBadRequest() {
        // missing
        ResponseEntity<String> r1 = controller.enterEmail(Map.of());
        assertEquals(400, r1.getStatusCodeValue());
        assertEquals("Please enter a valid email address.", r1.getBody());

        // invalid
        ResponseEntity<String> r2 = controller.enterEmail(Map.of("email", "not-an-email"));
        assertEquals(400, r2.getStatusCodeValue());
        assertEquals("Please enter a valid email address.", r2.getBody());

        verifyNoInteractions(userRepository, passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void whenEmailNotFound_thenReturnsBadRequest() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.enterEmail(Map.of("email", "unknown@example.com"));
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("No account found with this email address.", resp.getBody());

        verify(userRepository).findByEmail("unknown@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void whenEmailFound_thenSavesTokenAndSendsOtp() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        // stub all OTPs to hash to fixed string
        when(passwordEncoder.encode(anyString())).thenReturn("hashedOtp");

        ResponseEntity<String> resp = controller.enterEmail(Map.of("email", "user@example.com"));
        assertEquals(200, resp.getStatusCodeValue());

        // capture the saved token
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();

        // the saved token must be for our user, and use the hashed value
        assertEquals(user, saved.getUser());
        assertEquals("hashedOtp", saved.getToken());
        assertNotNull(saved.getExpiryDate());

        // capture the raw OTP sent by email
        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetEmail(eq("user@example.com"), otpCaptor.capture());
        String rawOtp = otpCaptor.getValue();

        // rawOtp must be exactly 6 digits
        assertTrue(
            Pattern.matches("\\d{6}", rawOtp),
            "Raw OTP should be a 6-digit number");

        verify(userRepository).findByEmail("user@example.com");
        verify(passwordResetTokenRepository).findByUser(user);
        verify(passwordEncoder).encode(rawOtp);
        verifyNoMoreInteractions(userRepository, passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void invalidOrMissingEmail_returns400() {
        // missing
        ResponseEntity<String> r1 = controller.resendOtp(Map.of());
        assertEquals(400, r1.getStatusCodeValue());
        assertEquals("Please enter a valid email address.", r1.getBody());

        // invalid format
        ResponseEntity<String> r2 = controller.resendOtp(Map.of("email", "no-at-sign"));
        assertEquals(400, r2.getStatusCodeValue());
        assertEquals("Please enter a valid email address.", r2.getBody());

        verifyNoInteractions(userRepository, passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void unknownEmail_returns400() {
        when(userRepository.findByEmail("foo@example.com")).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.resendOtp(Map.of("email", "foo@example.com"));
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("No account found with this email address.", resp.getBody());

        verify(userRepository).findByEmail("foo@example.com");
        verifyNoMoreInteractions(userRepository);
        verifyNoInteractions(passwordResetTokenRepository, passwordEncoder, emailService);
    }

    @Test
    void firstTimeResend_createsNewTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.empty());
        // capture & stub hashing
        ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);
        when(passwordEncoder.encode(rawOtpCaptor.capture())).thenReturn("hashed123");

        ResponseEntity<String> resp = controller.resendOtp(Map.of("email", "user@example.com"));

        assertEquals(200, resp.getStatusCodeValue());

        // verify find and save
        verify(userRepository).findByEmail("user@example.com");
        verify(passwordResetTokenRepository).findByUser(user);

        ArgumentCaptor<PasswordResetToken> tokenCaptor =
            ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken saved = tokenCaptor.getValue();

        // saved token has hashed value and correct user
        assertEquals(user, saved.getUser());
        assertEquals("hashed123", saved.getToken());
        assertNotNull(saved.getExpiryDate());
        // expiry ~10 minutes from now
        long minutesAhead = java.time.Duration.between(LocalDateTime.now(), saved.getExpiryDate())
            .toMinutes();
        assertTrue(minutesAhead >= 9 && minutesAhead <= 10, "Expiry ~10m ahead");

        // verify email was sent with the raw OTP
        String rawOtp = rawOtpCaptor.getValue();
        assertTrue(Pattern.matches("\\d{6}", rawOtp), "Raw OTP must be 6 digits");
        verify(emailService).sendPasswordResetEmail("user@example.com", rawOtp);
    }

    @Test
    void existingTokenResend_updatesTokenAndSendsEmail() {
        User user = new User();
        user.setEmail("again@example.com");
        when(userRepository.findByEmail("again@example.com")).thenReturn(Optional.of(user));

        PasswordResetToken existing = new PasswordResetToken(user, "oldhash");
        existing.setExpiryDate(LocalDateTime.now().minusMinutes(1)); // expired
        when(passwordResetTokenRepository.findByUser(user)).thenReturn(Optional.of(existing));

        ArgumentCaptor<String> rawOtpCaptor = ArgumentCaptor.forClass(String.class);
        when(passwordEncoder.encode(rawOtpCaptor.capture())).thenReturn("newhash");

        ResponseEntity<String> resp = controller.resendOtp(Map.of("email", "again@example.com"));
        assertEquals(200, resp.getStatusCodeValue());

        // verify we fetched existing and then saved it
        verify(passwordResetTokenRepository).findByUser(user);
        ArgumentCaptor<PasswordResetToken> savedCaptor =
            ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(savedCaptor.capture());
        PasswordResetToken updated = savedCaptor.getValue();

        assertSame(existing, updated, "Should update the same token object");
        assertEquals("newhash", updated.getToken());
        assertNotNull(updated.getExpiryDate());
        assertTrue(updated.getExpiryDate().isAfter(LocalDateTime.now()),
                   "Expiry should be in the future");

        // raw OTP is 6 digits
        String rawOtp = rawOtpCaptor.getValue();
        assertTrue(Pattern.matches("\\d{6}", rawOtp));

        verify(emailService).sendPasswordResetEmail("again@example.com", rawOtp);
    }
}
