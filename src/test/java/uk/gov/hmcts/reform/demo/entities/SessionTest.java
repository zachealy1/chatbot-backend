package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionTest {

    @Test
    void defaultConstructor_leavesFieldsNull() {
        Session session = new Session();
        assertNull(session.getId(), "ID should be null by default");
        assertNull(session.getSessionToken(), "sessionToken should be null by default");
        assertNull(session.getUser(), "user should be null by default");
        assertNull(session.getCreatedAt(), "createdAt should be null by default");
        assertNull(session.getExpiresAt(), "expiresAt should be null by default");
    }

    @Test
    void parameterizedConstructor_initializesFields() {
        User user = new User();
        user.setId(7L);
        String token = "token123";
        LocalDateTime created = LocalDateTime.of(2025, 5, 9, 12, 0);
        LocalDateTime expires = created.plusHours(24);

        Session session = new Session(token, user, created, expires);

        assertEquals(token, session.getSessionToken(), "Constructor should set sessionToken");
        assertSame(user, session.getUser(), "Constructor should set user");
        assertEquals(created, session.getCreatedAt(), "Constructor should set createdAt");
        assertEquals(expires, session.getExpiresAt(), "Constructor should set expiresAt");
    }

    @Test
    void settersAndGetters_workProperly() {
        Session session = new Session();

        session.setSessionToken("abc");
        assertEquals("abc", session.getSessionToken(), "Setter/getter for sessionToken");

        User user = new User();
        user.setId(5L);
        session.setUser(user);
        assertSame(user, session.getUser(), "Setter/getter for user");

        LocalDateTime now = LocalDateTime.now();
        session.setCreatedAt(now);
        assertEquals(now, session.getCreatedAt(), "Setter/getter for createdAt");

        LocalDateTime later = now.plusDays(1);
        session.setExpiresAt(later);
        assertEquals(later, session.getExpiresAt(), "Setter/getter for expiresAt");
    }
}
