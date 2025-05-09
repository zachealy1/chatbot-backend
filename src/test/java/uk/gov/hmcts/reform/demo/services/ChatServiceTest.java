package uk.gov.hmcts.reform.demo.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.Message;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.ChatRepository;
import uk.gov.hmcts.reform.demo.repositories.MessageRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ChatServiceTest {

    @Mock
    private ChatRepository chatRepository;

    @Mock
    private MessageRepository messageRepository;

    @InjectMocks
    private ChatService chatService;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void createChat_savesAndReturnsChat() {
        User user = new User(); user.setId(1L);
        String description = "Test chat";

        // Capture the Chat passed to save
        ArgumentCaptor<Chat> captor = ArgumentCaptor.forClass(Chat.class);
        Chat saved = new Chat(user, description);
        when(chatRepository.save(any(Chat.class))).thenReturn(saved);

        Chat result = chatService.createChat(user, description);

        assertSame(saved, result);
        verify(chatRepository).save(captor.capture());
        Chat toSave = captor.getValue();
        assertSame(user, toSave.getUser());
        assertEquals(description, toSave.getDescription());
        assertNotNull(toSave.getCreatedAt());
    }

    @Test
    void saveMessage_savesAndReturnsMessage() {
        Chat chat = new Chat();
        chat.setId(2L);
        String sender = "user", content = "Hello";

        ArgumentCaptor<Message> captor = ArgumentCaptor.forClass(Message.class);
        Message saved = new Message(chat, sender, content);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);

        Message result = chatService.saveMessage(chat, sender, content);

        assertSame(saved, result);
        verify(messageRepository).save(captor.capture());
        Message toSave = captor.getValue();
        assertSame(chat, toSave.getChat());
        assertEquals(sender, toSave.getSender());
        assertEquals(content, toSave.getMessage());
        assertNotNull(toSave.getCreatedAt());
    }

    @Test
    void getChatsForUser_returnsRepositoryList() {
        User user = new User(); user.setId(3L);
        List<Chat> list = List.of(new Chat(user, "A"), new Chat(user, "B"));
        when(chatRepository.findByUser(user)).thenReturn(list);

        List<Chat> result = chatService.getChatsForUser(user);
        assertSame(list, result);
        verify(chatRepository).findByUser(user);
    }

    @Test
    void getMessagesForChat_returnsRepositoryList() {
        Chat chat = new Chat();
        chat.setId(4L);
        List<Message> list = List.of(new Message(chat,"u","m1"), new Message(chat,"b","m2"));
        when(messageRepository.findByChat(chat)).thenReturn(list);

        List<Message> result = chatService.getMessagesForChat(chat);
        assertSame(list, result);
        verify(messageRepository).findByChat(chat);
    }

    @Test
    void findChatById_returnsChatOrNull() {
        Chat chat = new Chat();
        chat.setId(5L);
        when(chatRepository.findById(5L)).thenReturn(Optional.of(chat));
        assertSame(chat, chatService.findChatById(5L));

        when(chatRepository.findById(6L)).thenReturn(Optional.empty());
        assertNull(chatService.findChatById(6L));
    }

    @Test
    void deleteChat_deletesMessagesThenChat() {
        Chat chat = new Chat();
        chat.setId(7L);
        List<Message> msgs = List.of(
            new Message(chat,"u","x"),
            new Message(chat,"b","y")
        );
        when(messageRepository.findByChat(chat)).thenReturn(msgs);

        chatService.deleteChat(chat);

        InOrder inOrder = inOrder(messageRepository, chatRepository);
        inOrder.verify(messageRepository).findByChat(chat);
        inOrder.verify(messageRepository).deleteAll(msgs);
        inOrder.verify(chatRepository).delete(chat);
    }

    @Test
    void buildOpenAiConversation_includesSystemAndMapsRoles() {
        Chat chat = new Chat();
        chat.setId(8L);
        Message m1 = new Message(chat, "user", "Hi");
        Message m2 = new Message(chat, "chatbot", "Hello");
        List<Map<String, String>> conv = chatService.buildOpenAiConversation(List.of(m1, m2));

        // First entry is system
        assertEquals("system", conv.get(0).get("role"));
        assertTrue(conv.get(0).get("content").contains("assistant"));

        // Next maps user->user
        assertEquals("user", conv.get(1).get("role"));
        assertEquals("Hi", conv.get(1).get("content"));

        // Next maps chatbot->assistant
        assertEquals("assistant", conv.get(2).get("role"));
        assertEquals("Hello", conv.get(2).get("content"));
    }
}
