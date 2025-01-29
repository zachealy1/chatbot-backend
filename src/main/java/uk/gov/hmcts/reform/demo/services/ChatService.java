package uk.gov.hmcts.reform.demo.services;

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
}
