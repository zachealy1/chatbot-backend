package uk.gov.hmcts.reform.demo.controllers;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

}
