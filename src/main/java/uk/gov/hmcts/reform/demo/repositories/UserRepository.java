package uk.gov.hmcts.reform.demo.repositories;

import uk.gov.hmcts.reform.demo.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

/**
 * Repository interface for user-related database operations.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * Find a user by email.
     *
     * @param email the email of the user
     * @return an Optional containing the user if found
     */
    Optional<User> findByEmail(String email);

    /**
     * Find a user by username.
     *
     * @param username the username of the user
     * @return an Optional containing the user if found
     */
    Optional<User> findByUsername(String username);
}
