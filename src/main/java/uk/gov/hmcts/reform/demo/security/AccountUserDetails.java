package uk.gov.hmcts.reform.demo.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import uk.gov.hmcts.reform.demo.entities.User;

import java.util.Collection;
import java.util.Collections;

public class AccountUserDetails implements UserDetails {

    private final User user;

    // Constructor that accepts your User entity
    public AccountUserDetails(User user) {
        this.user = user;
    }

    /**
     * Expose the user's ID for later use (for example, when updating the account).
     */
    public Long getId() {
        return user.getId();
    }

    /**
     * Return the authorities granted to the user. For simplicity, we're granting a single role based on whether the
     * user is an admin.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String role = user.getIsAdmin() ? "ROLE_ADMIN" : "ROLE_USER";
        return Collections.singleton(new SimpleGrantedAuthority(role));
    }

    /**
     * Returns the password used to authenticate the user.
     */
    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    /**
     * Returns the username used to authenticate the user. In this example, we use the user's username, but you might
     * choose to use the email.
     */
    @Override
    public String getUsername() {
        return user.getUsername();
    }

    /**
     * Indicates whether the user's account has expired. Here we return true to indicate the account is non-expired.
     */
    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is locked or unlocked. Here we return true to indicate the account is non-locked.
     */
    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    /**
     * Indicates whether the user's credentials (password) have expired. Here we return true to indicate the credentials
     * are non-expired.
     */
    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    /**
     * Indicates whether the user is enabled or disabled. This example uses the "canLogin" flag from the User entity.
     */
    @Override
    public boolean isEnabled() {
        return user.getCanLogin();
    }
}
