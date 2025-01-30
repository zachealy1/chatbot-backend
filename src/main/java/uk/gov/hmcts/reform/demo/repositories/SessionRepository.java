package uk.gov.hmcts.reform.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.demo.entities.Session;

import java.util.Optional;

/**
 * Repository interface for Session entities.
 */
@Repository
public interface SessionRepository extends JpaRepository<Session, Long> {

    /**
     * Finds a session by its session token.
     *
     * @param sessionToken The session token.
     * @return An Optional containing the Session if found.
     */
    Optional<Session> findBySessionToken(String sessionToken);
}
