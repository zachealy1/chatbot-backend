package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class MessageTest {

    @Test
    void defaultConstructor_initializesCreatedAtAndLeavesOtherFieldsNull() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Message msg = new Message();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // createdAt should be set to now
        assertNotNull(msg.getCreatedAt(), "createdAt should be initialized");
        assertFalse(msg.getCreatedAt().isBefore(before),
                    "createdAt should not be before instantiation");
        assertFalse(msg.getCreatedAt().isAfter(after),
                    "createdAt should not be after now plus a small delta");

        // other fields should be null
        assertNull(msg.getId(), "id should be null by default");
        assertNull(msg.getChat(), "chat should be null by default");
        assertNull(msg.getSender(), "sender should be null by default");
        assertNull(msg.getMessage(), "message should be null by default");
    }

    @Test
    void parameterizedConstructor_setsFieldsAndCreatedAt() {
        Chat chat = new Chat(); chat.setId(10L);
        String sender = "user";
        String content = "Hello, world!";

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        Message msg = new Message(chat, sender, content);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // verify fields
        assertSame(chat, msg.getChat(), "Constructor should set chat");
        assertEquals(sender, msg.getSender(), "Constructor should set sender");
        assertEquals(content, msg.getMessage(), "Constructor should set message");

        // createdAt should be set
        assertNotNull(msg.getCreatedAt(), "createdAt should be initialized");
        assertFalse(msg.getCreatedAt().isBefore(before),
                    "createdAt should not be before instantiation");
        assertFalse(msg.getCreatedAt().isAfter(after),
                    "createdAt should not be after now plus a small delta");
    }

    @Test
    void gettersAndSetters_workCorrectly() {
        Message msg = new Message();

        // id
        msg.setId(99L);
        assertEquals(99L, msg.getId());

        // chat
        Chat chat = new Chat(); chat.setId(5L);
        msg.setChat(chat);
        assertSame(chat, msg.getChat());

        // sender
        msg.setSender("chatbot");
        assertEquals("chatbot", msg.getSender());

        // message content
        msg.setMessage("Reply");
        assertEquals("Reply", msg.getMessage());
    }
}
