package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

import java.time.LocalDate;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void defaultCreatedAtIsToday() {
        User user = new User();
        LocalDate today = LocalDate.now();
        // createdAt should be initialized to today by default
        assertNotNull(user.getCreatedAt());
        assertEquals(today, user.getCreatedAt(), "createdAt should default to LocalDate.now()");
    }

    @Test
    void settersAndGettersWork() {
        User user = new User();

        user.setId(123L);
        assertEquals(123L, user.getId());

        user.setUsername("alice");
        assertEquals("alice", user.getUsername());

        user.setEmail("alice@example.com");
        assertEquals("alice@example.com", user.getEmail());

        user.setPasswordHash("secretHash");
        assertEquals("secretHash", user.getPassword());

        LocalDate dob = LocalDate.of(1990, 1, 1);
        user.setDateOfBirth(dob);
        assertEquals(dob, user.getDateOfBirth());

        user.setCanLogin(true);
        assertTrue(user.isEnabled());

        user.setIsAdmin(true);
        assertTrue(user.getIsAdmin());
    }

    @Test
    void getAuthoritiesReturnsRoleUserWhenNotAdmin() {
        User user = new User();
        user.setIsAdmin(false);
        Collection<? extends GrantedAuthority> auths = user.getAuthorities();
        assertEquals(1, auths.size());
        GrantedAuthority ga = auths.iterator().next();
        assertEquals("ROLE_USER", ga.getAuthority());
    }

    @Test
    void getAuthoritiesReturnsRoleAdminWhenAdmin() {
        User user = new User();
        user.setIsAdmin(true);
        Collection<? extends GrantedAuthority> auths = user.getAuthorities();
        assertEquals(1, auths.size());
        GrantedAuthority ga = auths.iterator().next();
        assertEquals("ROLE_ADMIN", ga.getAuthority());
    }

    @Test
    void setAdminAliasAlsoAffectsAuthorities() {
        User user = new User();
        user.setAdmin(true);
        assertTrue(user.getAdmin());
        assertEquals("ROLE_ADMIN", user.getAuthorities().iterator().next().getAuthority());

        user.setAdmin(false);
        assertFalse(user.getAdmin());
        assertEquals("ROLE_USER", user.getAuthorities().iterator().next().getAuthority());
    }

    @Test
    void createdAtSetterOverridesDefault() {
        User user = new User();
        LocalDate customDate = LocalDate.of(2000, 2, 29);
        user.setCreatedAt(customDate);
        assertEquals(customDate, user.getCreatedAt());
    }

    @Test
    void isEnabledReflectsCanLoginFlag() {
        User user = new User();
        user.setCanLogin(false);
        assertFalse(user.isEnabled());
        user.setCanLogin(true);
        assertTrue(user.isEnabled());
    }
}
