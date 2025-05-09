package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CsrfControllerTest {

    private CsrfController controller;

    @BeforeEach
    void setUp() {
        controller = new CsrfController();
        // Clear any existing Authentication so tests start from a known state
        SecurityContextHolder.clearContext();
    }

    @Test
    void csrfShouldReturnTokenValueInMap() {
        // Arrange
        CsrfToken token = mock(CsrfToken.class);
        when(token.getToken()).thenReturn("abc123");

        // Act
        Map<String, String> result = controller.csrf(token);

        // Assert
        assertNotNull(result, "Resulting map should never be null");
        assertEquals(1, result.size(), "Only one entry expected");
        assertEquals("abc123", result.get("csrfToken"), "Map must contain the exact token string");
    }

    @Test
    void csrfShouldWorkRegardlessOfAuthenticationInSecurityContext() {
        // Arrange
        CsrfToken token = mock(CsrfToken.class);
        when(token.getToken()).thenReturn("xyz789");

        Authentication auth = mock(Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        Map<String, String> result = controller.csrf(token);

        // Assert – method return is unaffected by the presence of an Authentication
        assertEquals("xyz789", result.get("csrfToken"));
        // And the context still holds our mock Authentication
        assertSame(auth, SecurityContextHolder.getContext().getAuthentication());
    }
}
