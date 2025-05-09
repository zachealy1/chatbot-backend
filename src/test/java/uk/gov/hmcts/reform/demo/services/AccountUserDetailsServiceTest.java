package uk.gov.hmcts.reform.demo.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.security.AccountUserDetails;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AccountUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AccountUserDetailsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void loadUserByUsername_userExists_returnsAccountUserDetails() {
        // Arrange
        User user = new User();
        user.setId(123L);
        user.setUsername("alice");
        user.setPasswordHash("hash");
        user.setCanLogin(true);
        user.setIsAdmin(false);
        user.setDateOfBirth(LocalDate.of(1990, 1, 1));

        when(userRepository.findByUsername("alice"))
            .thenReturn(java.util.Optional.of(user));

        // Act
        UserDetails details = service.loadUserByUsername("alice");

        // Assert
        assertNotNull(details);
        assertTrue(details instanceof AccountUserDetails);
        AccountUserDetails aud = (AccountUserDetails) details;
        assertEquals(123L, aud.getId());
        assertEquals("alice", aud.getUsername());
        assertEquals("hash", aud.getPassword());
        assertTrue(aud.isEnabled());
        // ROLE_USER since isAdmin=false
        assertEquals("ROLE_USER", aud.getAuthorities().iterator().next().getAuthority());

        verify(userRepository).findByUsername("alice");
    }

    @Test
    void loadUserByUsername_userNotFound_throwsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByUsername("bob"))
            .thenReturn(java.util.Optional.empty());

        // Act & Assert
        UsernameNotFoundException ex = assertThrows(
            UsernameNotFoundException.class,
            () -> service.loadUserByUsername("bob")
        );
        assertTrue(ex.getMessage().contains("User not found with username: bob"));

        verify(userRepository).findByUsername("bob");
    }
}
