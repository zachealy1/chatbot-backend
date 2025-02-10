package uk.gov.hmcts.reform.demo.controllers;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.Message;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.services.ChatService;
import uk.gov.hmcts.reform.demo.utils.ChatGptApi;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private static final Logger logger = LoggerFactory.getLogger(ChatController.class);

    private final ChatGptApi chatGptApi;
    private final ChatService chatService;

    public ChatController(ChatGptApi chatGptApi, ChatService chatService) {
        this.chatGptApi = chatGptApi;
        this.chatService = chatService;
    }

    /**
     * Chat endpoint to handle user queries and return chatbot responses with historical context.
     */
    @PostMapping
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

        // Parse chatId if provided
        String chatIdStr = userInput.get("chatId");
        Long chatId = parseChatId(chatIdStr);

        // 1. Find or create a chat
        Chat chat;
        if (chatId == null) {
            logger.info("No chatId provided; creating a new chat.");
            chat = createNewChat(currentUser, message);
        } else {
            // Retrieve existing chat
            chat = chatService.findChatById(chatId);
            if (chat == null) {
                logger.error("Chat with id {} not found for user {}", chatId, currentUser.getId());
                return badRequest().body(Map.of("error", "Chat not found with the given chatId."));
            }
            if (!chat.getUser().getId().equals(currentUser.getId())) {
                logger.error("User {} is not authorized to post to chat {}", currentUser.getId(), chatId);
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "You are not authorized to continue this chat."));
            }
        }
        Long finalChatId = chat.getId();

        // 2. Save the user's new message
        saveUserMessage(chat, message);

        // 3. Build entire conversation (including this new user message) from DB
        List<Message> allMessages = chatService.getMessagesForChat(chat);

        // 4. Call OpenAI with historical context
        String botReply = getChatGptResponse(allMessages);

        // 5. Save the bot's reply to DB
        saveBotMessage(chat, botReply);

        // 6. Return chatId and the bot's reply
        return ok(Map.of("chatId", finalChatId, "message", botReply));
    }

    /**
     * GET endpoint to retrieve all messages for a given chat id.
     */
    @GetMapping("/messages/{chatId}")
    public ResponseEntity<?> getMessagesForChat(
        @PathVariable Long chatId,
        @AuthenticationPrincipal User currentUser) {

        logger.info("Received request to retrieve messages for chat id: {}", chatId);

        // Retrieve the Chat entity; assume chatService.findChatById exists.
        Chat chat = chatService.findChatById(chatId);
        if (chat == null) {
            return badRequest().body(Map.of("error", "Chat not found."));
        }
        // Verify that the chat belongs to the current user
        if (!chat.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You are not authorized to view these messages."));
        }
        // Get messages
        var messages = chatService.getMessagesForChat(chat);
        return ok(messages);
    }

    /**
     * GET endpoint to retrieve all chats for the currently authenticated user.
     */
    @GetMapping("/chats")
    public ResponseEntity<?> getChatsForUser(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return badRequest().body(Map.of("error", "User not authenticated."));
        }
        try {
            List<Chat> chats = chatService.getChatsForUser(currentUser);
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy, HH:mm");
            List<Map<String, Object>> chatDtos = chats.stream().map(chat -> {
                Map<String, Object> dto = new HashMap<>();
                dto.put("id", chat.getId());
                dto.put("description", chat.getDescription());
                dto.put("createdAt", chat.getCreatedAt() != null ? chat.getCreatedAt().format(formatter) : null);
                return dto;
            }).collect(Collectors.toList());
            return ok(chatDtos);
        } catch (Exception e) {
            logger.error("Error retrieving chats for user id {}: {}", currentUser.getId(), e.getMessage());
            return badRequest().body(Map.of("error", "Unable to retrieve chats for the user."));
        }
    }

    /**
     * DELETE endpoint to delete a chat.
     */
    @DeleteMapping("/chats/{chatId}")
    public ResponseEntity<?> deleteChat(
        @PathVariable Long chatId,
        @AuthenticationPrincipal User currentUser) {
        // Ensure the user is authenticated
        if (currentUser == null) {
            return badRequest().body(Map.of("error", "User not authenticated."));
        }

        // Retrieve the Chat entity
        Chat chat = chatService.findChatById(chatId);
        if (chat == null) {
            return badRequest().body(Map.of("error", "Chat not found."));
        }

        // Verify that the chat belongs to the current user
        if (!chat.getUser().getId().equals(currentUser.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("error", "You are not authorized to delete this chat."));
        }

        try {
            chatService.deleteChat(chat);
            return ok(Map.of("message", "Chat deleted successfully."));
        } catch (Exception e) {
            logger.error("Error deleting chat id {}: {}", chatId, e.getMessage());
            return badRequest().body(Map.of("error", "Unable to delete chat."));
        }
    }

    // ------------------ PRIVATE HELPER METHODS ------------------

    private Long parseChatId(String chatIdStr) {
        if (chatIdStr != null) {
            try {
                return Long.parseLong(chatIdStr);
            } catch (NumberFormatException e) {
                logger.error("Invalid chatId format: {}", chatIdStr, e);
            }
        }
        return null;
    }

    /**
     * Creates a new chat, optionally using the first user message to generate a summary if desired.
     */
    private Chat createNewChat(User user, String initialMessage) {
        // You can summarize the first message or just use "New chat"
        String summary = chatGptApi.summarize(initialMessage);
        return chatService.createChat(user, summary);
    }

    /**
     * Saves the user's message in the database.
     */
    private void saveUserMessage(Chat chat, String message) {
        chatService.saveMessage(chat, "user", message);
        logger.info("Saved user message: {}", message);
    }

    /**
     * Saves the chatbot's response in the database.
     */
    private void saveBotMessage(Chat chat, String response) {
        chatService.saveMessage(chat, "chatbot", response);
        logger.info("Saved chatbot response: {}", response);
    }

    /**
     * Calls OpenAI with all previous messages to maintain conversation context.
     */
    private String getChatGptResponse(List<Message> allMessages) {
        // Convert your DB messages to the format expected by OpenAI
        var openAiMessages = chatService.buildOpenAiConversation(allMessages);

        // Actually call the ChatGPT API
        String reply = chatGptApi.chatGptWithHistory(openAiMessages);
        return reply;
    }
}
