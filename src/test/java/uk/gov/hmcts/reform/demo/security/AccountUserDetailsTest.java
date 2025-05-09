package uk.gov.hmcts.reform.demo.security;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;
import uk.gov.hmcts.reform.demo.entities.User;

import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class AccountUserDetailsTest {

    @Test
    void getId_returnsUserId() {
        User user = new User();
        user.setId(42L);

        AccountUserDetails details = new AccountUserDetails(user);
        assertEquals(42L, details.getId());
    }

    @Test
    void getUsernameAndPassword_delegateToUser() {
        User user = new User();
        user.setUsername("alice");
        user.setPasswordHash("hashedPw");

        AccountUserDetails details = new AccountUserDetails(user);
        assertEquals("alice", details.getUsername());
        assertEquals("hashedPw", details.getPassword());
    }

    @Test
    void getAuthorities_returnsRoleAdmin_whenUserIsAdmin() {
        User user = new User();
        user.setIsAdmin(true);
        // canLogin irrelevant for authorities
        user.setCanLogin(false);

        AccountUserDetails details = new AccountUserDetails(user);
        Collection<? extends GrantedAuthority> auths = details.getAuthorities();

        assertEquals(1, auths.size());
        GrantedAuthority ga = auths.iterator().next();
        assertEquals("ROLE_ADMIN", ga.getAuthority());
    }

    @Test
    void getAuthorities_returnsRoleUser_whenUserIsNotAdmin() {
        User user = new User();
        user.setIsAdmin(false);

        AccountUserDetails details = new AccountUserDetails(user);
        Collection<? extends GrantedAuthority> auths = details.getAuthorities();

        assertEquals(1, auths.size());
        GrantedAuthority ga = auths.iterator().next();
        assertEquals("ROLE_USER", ga.getAuthority());
    }

    @Test
    void accountStatusFlags_alwaysTrue() {
        User user = new User();
        AccountUserDetails details = new AccountUserDetails(user);

        assertTrue(details.isAccountNonExpired(), "Account should be non-expired");
        assertTrue(details.isAccountNonLocked(),  "Account should be non-locked");
        assertTrue(details.isCredentialsNonExpired(), "Credentials should be non-expired");
    }

    @Test
    void isEnabled_reflectsCanLoginFlag() {
        User user = new User();

        user.setCanLogin(false);
        AccountUserDetails details1 = new AccountUserDetails(user);
        assertFalse(details1.isEnabled(), "Disabled when canLogin=false");

        user.setCanLogin(true);
        AccountUserDetails details2 = new AccountUserDetails(user);
        assertTrue(details2.isEnabled(), "Enabled when canLogin=true");
    }
}
