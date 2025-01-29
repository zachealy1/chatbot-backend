package uk.gov.hmcts.reform.demo.repositories;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import uk.gov.hmcts.reform.demo.entities.Message;
import uk.gov.hmcts.reform.demo.entities.Chat;

import java.util.List;

/**
 * Repository interface for Message entity.
 */
@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * Finds all messages within a specific chat.
     *
     * @param chat The chat whose messages are to be retrieved.
     * @return A list of messages belonging to the chat.
     */
    List<Message> findByChat(Chat chat);
}
