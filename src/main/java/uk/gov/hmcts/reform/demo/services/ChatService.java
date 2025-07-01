package uk.gov.hmcts.reform.demo.services;

import java.util.ArrayList;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.Message;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.ChatRepository;
import uk.gov.hmcts.reform.demo.repositories.MessageRepository;

import java.util.List;

/**
 * Service class for handling chat-related operations.
 */
@Service
public class ChatService {

    private final ChatRepository chatRepository;
    private final MessageRepository messageRepository;

    /**
     * Constructor for ChatService.
     *
     * @param chatRepository    Repository for Chat entities.
     * @param messageRepository Repository for Message entities.
     */
    public ChatService(ChatRepository chatRepository, MessageRepository messageRepository) {
        this.chatRepository = chatRepository;
        this.messageRepository = messageRepository;
    }

    /**
     * Creates a new chat for a user.
     *
     * @param user        The user initiating the chat.
     * @param description Description of the chat.
     * @return The created Chat entity.
     */
    @Transactional
    public Chat createChat(User user, String description) {
        Chat chat = new Chat(user, description);
        return chatRepository.save(chat);
    }

    /**
     * Saves a message to a chat.
     *
     * @param chat    The chat to which the message belongs.
     * @param sender  The sender of the message ("user" or "chatbot").
     * @param content The content of the message.
     * @return The saved Message entity.
     */
    @Transactional
    public Message saveMessage(Chat chat, String sender, String content) {
        Message message = new Message(chat, sender, content);
        return messageRepository.save(message);
    }

    /**
     * Retrieves all chats for a given user.
     *
     * @param user The user whose chats are to be retrieved.
     * @return A list of Chat entities.
     */
    @Transactional(readOnly = true)
    public List<Chat> getChatsForUser(User user) {
        return chatRepository.findByUser(user);
    }

    /**
     * Retrieves all messages for a given chat.
     *
     * @param chat The chat whose messages are to be retrieved.
     * @return A list of Message entities.
     */
    @Transactional(readOnly = true)
    public List<Message> getMessagesForChat(Chat chat) {
        return messageRepository.findByChat(chat);
    }

    /**
     * Finds and returns the Chat with the specified chatId.
     *
     * @param chatId The ID of the chat to retrieve.
     * @return The Chat object if found; otherwise, null.
     */
    public Chat findChatById(Long chatId) {
        return chatRepository.findById(chatId).orElse(null);
    }

    /**
     * Deletes the provided chat.
     *
     * @param chat The Chat entity to delete.
     */
    @Transactional
    public void deleteChat(Chat chat) {
        List<Message> messages = messageRepository.findByChat(chat);
        messageRepository.deleteAll(messages);
        chatRepository.delete(chat);
    }

    public List<Map<String, String>> buildOpenAiConversation(List<Message> dbMessages) {
        // Optionally add a system role
        List<Map<String, String>> openAiMessages = new ArrayList<>();
        openAiMessages.add(Map.of("role", "system", "content", "You are a helpful assistant."));

        // For each message, map "user" -> "user", "chatbot" -> "assistant"
        for (Message m : dbMessages) {
            String role = m.getSender().equals("user") ? "user" : "assistant";
            openAiMessages.add(Map.of("role", role, "content", m.getMessage()));
        }
        return openAiMessages;
    }
}
