package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class ChatTest {

    @Test
    void defaultConstructor_initializesCreatedAtAndEmptyMessages() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Chat chat = new Chat();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertNotNull(chat.getCreatedAt(), "createdAt should be initialized");
        assertFalse(chat.getCreatedAt().isBefore(before),
                    "createdAt should not be before instantiation");
        assertFalse(chat.getCreatedAt().isAfter(after),
                    "createdAt should not be after now plus small delta");
        assertNotNull(chat.getMessages(), "messages set should be non-null");
        assertTrue(chat.getMessages().isEmpty(), "messages set should start empty");
    }

    @Test
    void parameterizedConstructor_setsUserDescriptionAndCreatedAt() {
        User user = new User();
        user.setId(7L);
        String desc = "Test description";

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Chat chat = new Chat(user, desc);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertSame(user, chat.getUser(), "user should be set by constructor");
        assertEquals(desc, chat.getDescription(), "description should be set by constructor");
        assertNotNull(chat.getCreatedAt());
        assertFalse(chat.getCreatedAt().isBefore(before));
        assertFalse(chat.getCreatedAt().isAfter(after));
        assertTrue(chat.getMessages().isEmpty());
    }

    @Test
    void gettersAndSetters_workProperly() {
        Chat chat = new Chat();
        chat.setId(99L);
        assertEquals(99L, chat.getId());

        User user = new User();
        user.setId(5L);
        chat.setUser(user);
        assertSame(user, chat.getUser());

        chat.setDescription("Hello");
        assertEquals("Hello", chat.getDescription());

        LocalDateTime dt = LocalDateTime.of(2020,1,2,3,4,5);
        chat.setCreatedAt(dt);
        assertEquals(dt, chat.getCreatedAt());

        // setMessages
        Message m = new Message();
        m.setId(11L);
        chat.setMessages(Set.of(m));
        assertEquals(1, chat.getMessages().size());
        assertTrue(chat.getMessages().contains(m));
    }

    @Test
    void addMessage_assignsChatAndAddsToSet() {
        Chat chat = new Chat();
        Message msg = new Message();
        msg.setId(20L);

        chat.addMessage(msg);

        assertTrue(chat.getMessages().contains(msg), "Message should be added to chat.messages");
        assertSame(chat, msg.getChat(), "Message.chat should be set to the chat");
    }

    @Test
    void removeMessage_clearsChatAndRemovesFromSet() {
        Chat chat = new Chat();
        Message msg = new Message();
        msg.setId(30L);

        // first add
        chat.addMessage(msg);
        assertTrue(chat.getMessages().contains(msg));

        // now remove
        chat.removeMessage(msg);

        assertFalse(chat.getMessages().contains(msg), "Message should be removed from chat.messages");
        assertNull(msg.getChat(), "Message.chat should be nulled after removal");
    }
}
