package uk.gov.hmcts.reform.demo.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.security.AccountUserDetails;

@Service
public class AccountUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Autowired
    public AccountUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Loads the user from the database using their username.
     * You can also modify this method to load by email if needed.
     */
    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
            .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));
        return new AccountUserDetails(user);
    }
}
