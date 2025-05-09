package uk.gov.hmcts.reform.demo.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.time.Month;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.Message;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.services.ChatService;
import uk.gov.hmcts.reform.demo.utils.ChatGptApi;

class ChatControllerTest {

    @InjectMocks
    private ChatController controller;

    @Mock
    private ChatGptApi chatGptApi;

    @Mock
    private ChatService chatService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void whenUserNotAuthenticated_thenReturnsBadRequest() {
        // Act
        ResponseEntity<Map<String, Object>> response = controller.chat(null, Map.of("message", "hello"));

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertTrue(response.getBody().containsKey("error"));
        assertEquals("User not authenticated.", response.getBody().get("error"));
        // No interactions with services
        verifyNoInteractions(chatService, chatGptApi);
    }

    @Test
    void whenMessageEmpty_thenReturnsBadRequest() {
        User user = new User();
        user.setId(1L);

        // Test null message
        ResponseEntity<Map<String, Object>> resp1 = controller.chat(user, Map.of());
        assertEquals(HttpStatus.BAD_REQUEST, resp1.getStatusCode());
        assertEquals("Message cannot be empty. Please provide a valid input.", resp1.getBody().get("error"));

        // Test blank message
        ResponseEntity<Map<String, Object>> resp2 = controller.chat(user, Map.of("message", "  "));
        assertEquals(HttpStatus.BAD_REQUEST, resp2.getStatusCode());
        assertEquals("Message cannot be empty. Please provide a valid input.", resp2.getBody().get("error"));

        verifyNoInteractions(chatService, chatGptApi);
    }

    @Test
    void whenChatIdProvidedButNotFound_thenReturnsBadRequest() {
        User user = new User();
        user.setId(1L);

        Map<String, String> input = Map.of("message", "hi", "chatId", "5");
        when(chatService.findChatById(5L)).thenReturn(null);

        ResponseEntity<Map<String, Object>> response = controller.chat(user, input);

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals("Chat not found with the given chatId.", response.getBody().get("error"));

        verify(chatService).findChatById(5L);
        verifyNoMoreInteractions(chatService);
        verifyNoInteractions(chatGptApi);
    }

    @Test
    void whenChatBelongsToOtherUser_thenReturnsForbidden() {
        User user = new User();
        user.setId(1L);

        Chat otherChat = new Chat();
        otherChat.setId(5L);
        User owner = new User();
        owner.setId(2L);
        otherChat.setUser(owner);

        Map<String, String> input = Map.of("message", "hi", "chatId", "5");
        when(chatService.findChatById(5L)).thenReturn(otherChat);

        ResponseEntity<Map<String, Object>> response = controller.chat(user, input);

        assertEquals(HttpStatus.FORBIDDEN, response.getStatusCode());
        assertEquals("You are not authorized to continue this chat.", response.getBody().get("error"));

        verify(chatService).findChatById(5L);
        verifyNoMoreInteractions(chatService);
        verifyNoInteractions(chatGptApi);
    }

    @Test
    void whenNoChatId_thenCreatesNewChatAndReturnsReply() {
        User user = new User();
        user.setId(1L);

        String message = "hello";
        String summary = "summary";
        Chat newChat = new Chat();
        newChat.setId(10L);
        newChat.setUser(user);

        when(chatGptApi.categorise(message)).thenReturn(summary);
        when(chatService.createChat(user, summary)).thenReturn(newChat);
        // No existing messages
        when(chatService.getMessagesForChat(newChat)).thenReturn(Collections.emptyList());
        when(chatService.buildOpenAiConversation(Collections.emptyList()))
            .thenReturn(Collections.emptyList());
        when(chatGptApi.chatGptWithAssistant(Collections.emptyList(), anyString()))
            .thenReturn("bot reply");

        ResponseEntity<Map<String, Object>> response = controller.chat(user, Map.of("message", message));

        assertEquals(HttpStatus.OK, response.getStatusCode());
        Map<String, Object> body = response.getBody();
        assertNotNull(body);
        assertEquals(10L, body.get("chatId"));
        assertEquals("bot reply", body.get("message"));

        // Verify flow
        verify(chatGptApi).categorise(message);
        verify(chatService).createChat(user, summary);
        verify(chatService).saveMessage(newChat, "user", message);
        verify(chatService).getMessagesForChat(newChat);
        verify(chatService).buildOpenAiConversation(Collections.emptyList());
        verify(chatGptApi).chatGptWithAssistant(Collections.emptyList(), anyString());
        verify(chatService).saveMessage(newChat, "chatbot", "bot reply");
    }

    @Test
    void whenExistingChat_thenUsesThatChatAndReturnsReply() {
        User user = new User();
        user.setId(1L);

        Chat chat = new Chat();
        chat.setId(20L);
        chat.setUser(user);

        Message m1 = new Message();
        m1.setId(100L);
        m1.setMessage("prev");

        List<Message> messages = List.of(m1);
        List<Map<String, String>> openAiMsgs =
            List.of(Map.of("role", "user", "content", "prev"));

        when(chatService.findChatById(20L)).thenReturn(chat);
        when(chatService.getMessagesForChat(chat)).thenReturn(messages);
        when(chatService.buildOpenAiConversation(messages)).thenReturn(openAiMsgs);
        when(chatGptApi.chatGptWithAssistant(openAiMsgs, anyString())).thenReturn("response");

        Map<String, Object> result = controller.chat(user, Map.of("message", "new", "chatId", "20")).getBody();

        assertNotNull(result);
        assertEquals(20L, result.get("chatId"));
        assertEquals("response", result.get("message"));

        verify(chatService).saveMessage(chat, "user", "new");
        verify(chatService).getMessagesForChat(chat);
        verify(chatService).buildOpenAiConversation(messages);
        verify(chatGptApi).chatGptWithAssistant(openAiMsgs, anyString());
        verify(chatService).saveMessage(chat, "chatbot", "response");
    }

    @Test
    void whenChatNotFound_thenReturnsBadRequest() {
        // Arrange
        Long chatId = 42L;
        when(chatService.findChatById(chatId)).thenReturn(null);

        // Act
        ResponseEntity<?> resp = controller.getMessagesForChat(chatId, new User());

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(((Map<?,?>)resp.getBody()).containsKey("error"));
        assertEquals("Chat not found.", ((Map<?,?>)resp.getBody()).get("error"));

        verify(chatService).findChatById(chatId);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void getMessagesForChat_whenChatBelongsToOtherUser_thenReturnsForbidden() {
        // Arrange
        Long chatId = 100L;
        User currentUser = new User();
        currentUser.setId(1L);

        User other = new User();
        other.setId(2L);
        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUser(other);

        when(chatService.findChatById(chatId)).thenReturn(chat);

        // Act
        ResponseEntity<?> resp = controller.getMessagesForChat(chatId, currentUser);

        // Assert
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("You are not authorized to view these messages.",
                     ((Map<?,?>)resp.getBody()).get("error"));

        verify(chatService).findChatById(chatId);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void whenChatBelongsToUser_thenReturnsMessages() {
        // Arrange
        Long chatId = 7L;
        User currentUser = new User();
        currentUser.setId(5L);

        Chat chat = new Chat();
        chat.setId(chatId);
        chat.setUser(currentUser);

        Message m1 = new Message();
        m1.setId(1L);
        m1.setMessage("hello");
        Message m2 = new Message();
        m2.setId(2L);
        m2.setMessage("world");
        List<Message> messages = Arrays.asList(m1, m2);

        when(chatService.findChatById(chatId)).thenReturn(chat);
        when(chatService.getMessagesForChat(chat)).thenReturn(messages);

        // Act
        ResponseEntity<?> resp = controller.getMessagesForChat(chatId, currentUser);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Message> body = (List<Message>) resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertSame(m1, body.get(0));
        assertSame(m2, body.get(1));

        InOrder inOrder = inOrder(chatService);
        inOrder.verify(chatService).findChatById(chatId);
        inOrder.verify(chatService).getMessagesForChat(chat);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenCurrentUserIsNull_thenReturnsBadRequest() {
        // Act
        ResponseEntity<?> resp = controller.getChatsForUser(null);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertTrue(((Map<?,?>)resp.getBody()).containsKey("error"));
        assertEquals("User not authenticated.", ((Map<?,?>)resp.getBody()).get("error"));

        verifyNoInteractions(chatService);
    }

    @Test
    void whenServiceThrowsException_thenReturnsBadRequest() {
        // Arrange
        User user = new User();
        user.setId(7L);
        when(chatService.getChatsForUser(user)).thenThrow(new RuntimeException("db down"));

        // Act
        ResponseEntity<?> resp = controller.getChatsForUser(user);

        // Assert
        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Unable to retrieve chats for the user.",
                     ((Map<?,?>)resp.getBody()).get("error"));

        verify(chatService).getChatsForUser(user);
    }

    @Test
    void whenChatsExist_thenReturnsFormattedDtos() {
        // Arrange
        User user = new User();
        user.setId(5L);

        Chat c1 = new Chat();
        c1.setId(101L);
        c1.setDescription("First chat");
        c1.setCreatedAt(LocalDateTime.of(2021, Month.MARCH, 15, 9, 30));

        Chat c2 = new Chat();
        c2.setId(202L);
        c2.setDescription("Second chat");
        c2.setCreatedAt(LocalDateTime.of(2022, Month.DECEMBER, 1, 18, 5));

        when(chatService.getChatsForUser(user)).thenReturn(List.of(c1, c2));

        // Act
        ResponseEntity<?> resp = controller.getChatsForUser(user);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> body = (List<Map<String, Object>>) resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());

        Map<String, Object> dto1 = body.get(0);
        assertEquals(101L, dto1.get("id"));
        assertEquals("First chat", dto1.get("description"));
        assertEquals("15 Mar 2021, 09:30", dto1.get("createdAt"));

        Map<String, Object> dto2 = body.get(1);
        assertEquals(202L, dto2.get("id"));
        assertEquals("Second chat", dto2.get("description"));
        assertEquals("01 Dec 2022, 18:05", dto2.get("createdAt"));

        verify(chatService).getChatsForUser(user);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void whenChatCreatedAtIsNull_thenDtoHasNullCreatedAt() {
        // Arrange
        User user = new User();
        user.setId(3L);

        Chat c = new Chat();
        c.setId(303L);
        c.setDescription("No date chat");
        c.setCreatedAt(null);

        when(chatService.getChatsForUser(user)).thenReturn(List.of(c));

        // Act
        ResponseEntity<?> resp = controller.getChatsForUser(user);

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> body = (List<Map<String, Object>>) resp.getBody();
        assertNotNull(body);
        assertEquals(1, body.size());
        assertNull(body.get(0).get("createdAt"));

        verify(chatService).getChatsForUser(user);
    }

    @Test
    void deleteChat_whenCurrentUserIsNull_thenReturnsBadRequest() {
        ResponseEntity<?> resp = controller.deleteChat(1L, null);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("User not authenticated.", ((Map<?,?>)resp.getBody()).get("error"));
        verifyNoInteractions(chatService);
    }

    @Test
    void deleteChat_whenChatNotFound_thenReturnsBadRequest() {
        User user = new User();
        user.setId(10L);
        when(chatService.findChatById(5L)).thenReturn(null);

        ResponseEntity<?> resp = controller.deleteChat(5L, user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Chat not found.", ((Map<?,?>)resp.getBody()).get("error"));
        verify(chatService).findChatById(5L);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void deleteChat_whenChatBelongsToOtherUser_thenReturnsForbidden() {
        User user = new User();
        user.setId(1L);

        User owner = new User();
        owner.setId(2L);
        Chat chat = new Chat();
        chat.setId(7L);
        chat.setUser(owner);

        when(chatService.findChatById(7L)).thenReturn(chat);

        ResponseEntity<?> resp = controller.deleteChat(7L, user);

        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertEquals("You are not authorized to delete this chat.",
                     ((Map<?,?>)resp.getBody()).get("error"));

        verify(chatService).findChatById(7L);
        verifyNoMoreInteractions(chatService);
    }

    @Test
    void whenDeleteSucceeds_thenReturnsOk() {
        User user = new User();
        user.setId(3L);

        Chat chat = new Chat();
        chat.setId(8L);
        chat.setUser(user);

        when(chatService.findChatById(8L)).thenReturn(chat);
        // deleteChat does not throw

        ResponseEntity<?> resp = controller.deleteChat(8L, user);

        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertEquals("Chat deleted successfully.",
                     ((Map<?,?>)resp.getBody()).get("message"));

        InOrder inOrder = inOrder(chatService);
        inOrder.verify(chatService).findChatById(8L);
        inOrder.verify(chatService).deleteChat(chat);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenDeleteThrows_thenReturnsBadRequest() {
        User user = new User();
        user.setId(4L);

        Chat chat = new Chat();
        chat.setId(9L);
        chat.setUser(user);

        when(chatService.findChatById(9L)).thenReturn(chat);
        doThrow(new RuntimeException("db error")).when(chatService).deleteChat(chat);

        ResponseEntity<?> resp = controller.deleteChat(9L, user);

        assertEquals(HttpStatus.BAD_REQUEST, resp.getStatusCode());
        assertEquals("Unable to delete chat.",
                     ((Map<?,?>)resp.getBody()).get("error"));

        InOrder inOrder = inOrder(chatService);
        inOrder.verify(chatService).findChatById(9L);
        inOrder.verify(chatService).deleteChat(chat);
        inOrder.verifyNoMoreInteractions();
    }

}
