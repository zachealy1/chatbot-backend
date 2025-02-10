package uk.gov.hmcts.reform.demo.controllers;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.services.ChatService;
import uk.gov.hmcts.reform.demo.utils.ChatGptApi;

@RestController
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatGptApi chatGptApi;
    private final ChatService chatService;

    public ChatController(ChatGptApi chatGptApi, ChatService chatService) {
        this.chatGptApi = chatGptApi;
        this.chatService = chatService;
    }

    /**
     * Chat endpoint to handle user queries and return chatbot responses.
     *
     * @param currentUser The currently authenticated user injected by Spring Security.
     * @param userInput   A map containing the user's input message and optional chatId.
     * @return Chatbot response along with chatId.
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(
        @AuthenticationPrincipal User currentUser,
        @RequestBody Map<String, String> userInput) {
        logger.info("Received chat request: {}", userInput);

        // Ensure the user is authenticated
        if (currentUser == null) {
            return badRequest().body(Map.of("error", "User not authenticated."));
        }

        String message = userInput.get("message");
        if (message == null || message.trim().isEmpty()) {
            return badRequest().body(Map.of("error", "Message cannot be empty. Please provide a valid input."));
        }

        logger.info("User sent message: {}", message);

        User user = currentUser;

        String chatIdStr = userInput.get("chatId");
        Long chatId = parseChatId(chatIdStr);
        Chat chat = getOrCreateChat(user, chatId, message);
        chatId = chat.getId();

        saveUserMessage(chat, message);
        String response = getChatGptResponse(message);
        saveBotMessage(chat, response);

        return ok(Map.of("chatId", chatId, "message", response));
    }

    /**
     * Parses the chatId from the input string.
     *
     * @param chatIdStr The chatId string from the request.
     * @return The parsed chatId or null if invalid.
     */
    private Long parseChatId(String chatIdStr) {
        if (chatIdStr != null) {
            try {
                return Long.parseLong(chatIdStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid chatId format.");
            }
        }
        return null;
    }

    /**
     * Retrieves an existing chat or creates a new one.
     *
     * @param user    The user requesting the chat.
     * @param chatId  The optional chat ID.
     * @param message The user's message.
     * @return The Chat object.
     */
    private Chat getOrCreateChat(User user, Long chatId, String message) {
        if (chatId != null) {
            return chatService.getChatsForUser(user).stream()
                .filter(c -> c.getId().equals(chatId))
                .findFirst()
                .orElseGet(() -> createNewChat(user, message));
        }
        return createNewChat(user, message);
    }

    /**
     * Creates a new chat with a summarized description.
     *
     * @param user    The user starting the chat.
     * @param message The initial message.
     * @return The newly created chat.
     */
    private Chat createNewChat(User user, String message) {
        String summary = chatGptApi.summarize(message);
        return chatService.createChat(user, summary);
    }

    /**
     * Saves the user's message in the database.
     *
     * @param chat    The chat to save the message in.
     * @param message The user's message.
     */
    private void saveUserMessage(Chat chat, String message) {
        chatService.saveMessage(chat, "user", message);
        logger.info("Saved user message: {}", message);
    }

    /**
     * Sends a message to ChatGPT and retrieves the response.
     *
     * @param message The user's message.
     * @return The ChatGPT response.
     */
    private String getChatGptResponse(String message) {
        return chatGptApi.chatGpt(message);
    }

    /**
     * Saves the chatbot's response in the database.
     *
     * @param chat     The chat to save the response in.
     * @param response The chatbot's response.
     */
    private void saveBotMessage(Chat chat, String response) {
        chatService.saveMessage(chat, "chatbot", response);
        logger.info("Saved chatbot response: {}", response);
    }
}
