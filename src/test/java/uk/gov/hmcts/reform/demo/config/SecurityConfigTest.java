package uk.gov.hmcts.reform.demo.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.authentication.configurers.userdetails.DaoAuthenticationConfigurer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import uk.gov.hmcts.reform.demo.services.AccountUserDetailsService;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SecurityConfigTest {

    @InjectMocks
    private SecurityConfig config;

    @Mock
    private AccountUserDetailsService userDetailsService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void passwordEncoder_returnsBcrypt() {
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder, "PasswordEncoder should not be null");
        assertInstanceOf(BCryptPasswordEncoder.class, encoder, "PasswordEncoder should be a BCryptPasswordEncoder");
    }

    @SuppressWarnings("unchecked")
    @Test
    void securityFilterChain_appliesAllConfigurations() throws Exception {
        // Arrange
        HttpSecurity http = mock(HttpSecurity.class);
        SecurityFilterChain chainMock = mock(SecurityFilterChain.class);

        // Stub each step of the fluent API to return the same HttpSecurity
        when(http.csrf(any(Customizer.class))).thenReturn(http);
        when(http.authorizeHttpRequests(any(Customizer.class))).thenReturn(http);
        when(http.exceptionHandling(any(Customizer.class))).thenReturn(http);
        when(http.formLogin(any(Customizer.class))).thenReturn(http);
        when(http.logout(any(Customizer.class))).thenReturn(http);
        // Stub build() to return our mock chain
        doReturn(chainMock).when(http).build();

        // Act
        SecurityFilterChain result = config.securityFilterChain(http);

        // Assert
        assertSame(chainMock, result, "securityFilterChain should return the built chain");

        // Verify that csrf(...) was called with a Customizer<CsrfConfigurer>
        verify(http).csrf(any(Customizer.class));
        // Verify that authorizeHttpRequests(...) was called
        verify(http).authorizeHttpRequests(any(Customizer.class));
        // Verify exceptionHandling(...) was configured with UNAUTHORIZED entry point
        verify(http).exceptionHandling(any(Customizer.class));
        // Verify formLogin and logout used default customizer
        verify(http).formLogin(any(Customizer.class));
        verify(http).logout(any(Customizer.class));
        // And finally build() was invoked
        verify(http).build();
    }

    @Test
    void authenticationManager_registersUserDetailsServiceAndPasswordEncoder() throws Exception {
        // Arrange
        HttpSecurity http = mock(HttpSecurity.class);
        AuthenticationManagerBuilder amb = mock(AuthenticationManagerBuilder.class);
        @SuppressWarnings("unchecked")
        DaoAuthenticationConfigurer<AuthenticationManagerBuilder, AccountUserDetailsService> daoConfigurer =
            mock(DaoAuthenticationConfigurer.class);
        AuthenticationManager expectedManager = mock(AuthenticationManager.class);

        when(http.getSharedObject(AuthenticationManagerBuilder.class)).thenReturn(amb);
        when(amb.userDetailsService(userDetailsService)).thenReturn(daoConfigurer);
        when(daoConfigurer.passwordEncoder(any(PasswordEncoder.class))).thenReturn(daoConfigurer);
        when(amb.build()).thenReturn(expectedManager);

        // Act
        AuthenticationManager actual = config.authenticationManager(http);

        // Assert
        assertSame(expectedManager, actual);

        InOrder inOrder = inOrder(amb, daoConfigurer);
        inOrder.verify(amb).userDetailsService(userDetailsService);
        // loosened to any(PasswordEncoder.class)
        inOrder.verify(daoConfigurer).passwordEncoder(any(PasswordEncoder.class));
        inOrder.verify(amb).build();
    }

    @Test
    void passwordEncoder_returnsBCryptPasswordEncoderInstance() {
        PasswordEncoder encoder = config.passwordEncoder();
        assertNotNull(encoder, "PasswordEncoder should not be null");
        assertTrue(encoder instanceof BCryptPasswordEncoder,
                   "PasswordEncoder should be an instance of BCryptPasswordEncoder");
    }

    @Test
    void passwordEncoder_encodesAndMatchesPasswordsCorrectly() {
        PasswordEncoder encoder = config.passwordEncoder();
        String raw = "My$ecretP@ss";
        String hashed = encoder.encode(raw);

        assertNotNull(hashed, "Encoded password should not be null");
        assertNotEquals(raw, hashed, "Encoded password should differ from raw password");
        assertTrue(encoder.matches(raw, hashed),
                   "PasswordEncoder should match the raw password against its hash");
        assertFalse(encoder.matches("wrongPassword", hashed),
                    "PasswordEncoder should not match an incorrect raw password");
    }

    @Test
    void passwordEncoder_generatesDifferentHashesForSameInput() {
        PasswordEncoder encoder = config.passwordEncoder();
        String raw = "repeatMe";
        String hash1 = encoder.encode(raw);
        String hash2 = encoder.encode(raw);

        assertNotEquals(hash1, hash2,
                        "BCrypt should generate a different salt each time, so hashes should differ");
        assertTrue(encoder.matches(raw, hash1));
        assertTrue(encoder.matches(raw, hash2));
    }
}
