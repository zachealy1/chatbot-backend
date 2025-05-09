package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PasswordResetTokenTest {

    @Test
    void defaultConstructor_initializesFieldsToNullOrDefault() {
        PasswordResetToken token = new PasswordResetToken();

        assertNull(token.getId(), "ID should be null by default");
        assertNull(token.getUser(), "User should be null by default");
        assertNull(token.getToken(), "Token string should be null by default");
        assertFalse(token.isUsed(), "Used flag should be false by default");
        assertNull(token.getExpiryDate(), "Expiry date should be null by default");
    }

    @Test
    void parameterizedConstructor_setsUserTokenUsedAndExpiryDate() {
        User user = new User();
        user.setId(123L);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        PasswordResetToken prt = new PasswordResetToken(user, "otp123");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // Fields set correctly
        assertSame(user, prt.getUser(), "Constructor should set the user");
        assertEquals("otp123", prt.getToken(), "Constructor should set the token string");
        assertFalse(prt.isUsed(), "Used flag should be false after construction");

        // expiryDate ~10 minutes after construction
        assertNotNull(prt.getExpiryDate(), "Expiry date should be initialized");
        Duration diff = Duration.between(before.plusMinutes(10), prt.getExpiryDate());
        assertTrue(Math.abs(diff.toSeconds()) < 5,
                   "Expiry date should be approximately 10 minutes after now");

        // Immediately after construction, token should not be expired
        assertFalse(prt.isExpired(), "New token should not be expired");
    }

    @Test
    void isExpired_returnsTrueWhenExpiryDateInPast() {
        PasswordResetToken prt = new PasswordResetToken();
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        prt.setExpiryDate(tenMinutesAgo);

        assertTrue(prt.isExpired(), "Token with expiryDate in the past should be expired");
    }

    @Test
    void isExpired_returnsFalseWhenExpiryDateInFuture() {
        PasswordResetToken prt = new PasswordResetToken();
        LocalDateTime tenMinutesFromNow = LocalDateTime.now().plusMinutes(10);
        prt.setExpiryDate(tenMinutesFromNow);

        assertFalse(prt.isExpired(), "Token with expiryDate in the future should not be expired");
    }

    @Test
    void settersAndGetters_workAsExpected() {
        PasswordResetToken prt = new PasswordResetToken();

        prt.setId(999L);
        assertEquals(999L, prt.getId(), "ID setter/getter should work");

        User user = new User();
        user.setId(5L);
        prt.setUser(user);
        assertSame(user, prt.getUser(), "User setter/getter should work");

        prt.setToken("newToken");
        assertEquals("newToken", prt.getToken(), "Token setter/getter should work");

        prt.setUsed(true);
        assertTrue(prt.isUsed(), "Used flag setter/getter should work");

        LocalDateTime dt = LocalDateTime.of(2030, 1, 1, 0, 0);
        prt.setExpiryDate(dt);
        assertEquals(dt, prt.getExpiryDate(), "Expiry date setter/getter should work");
    }
}
