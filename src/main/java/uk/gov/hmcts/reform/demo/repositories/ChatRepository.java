package uk.gov.hmcts.reform.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.User;

import java.util.List;

/**
 * Repository interface for Chat entity.
 */
@Repository
public interface ChatRepository extends JpaRepository<Chat, Long> {

    /**
     * Finds all chats associated with a specific user.
     *
     * @param user The user whose chats are to be retrieved.
     * @return A list of chats belonging to the user.
     */
    List<Chat> findByUser(User user);
}
