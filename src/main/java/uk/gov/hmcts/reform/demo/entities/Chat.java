package uk.gov.hmcts.reform.demo.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entity representing a chat session between a user and the chatbot.
 */
@Entity
@Table(name = "chats")
public class Chat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The user who initiated the chat.
     */
    @JsonIgnoreProperties({"hibernateLazyInitializer", "handler"})
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    /**
     * Description of the chat.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * Timestamp when the chat was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Messages associated with this chat.
     */
    @OneToMany(mappedBy = "chat", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @JsonIgnoreProperties({"chat"})
    private Set<Message> messages = new HashSet<>();

    /**
     * Default constructor that initializes the creation timestamp.
     */
    public Chat() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Parameterized constructor for creating a new chat.
     *
     * @param user        The user initiating the chat.
     * @param description Description of the chat.
     */
    public Chat(User user, String description) {
        this.user = user;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    // No setter for 'id' as it's auto-generated.

    public User getUser() {
        return user;
    }

    public void setUser(User user) {
        this.user = user;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // No setter for 'createdAt' as it's set at creation.

    public Set<Message> getMessages() {
        return messages;
    }

    public void setMessages(Set<Message> messages) {
        this.messages = messages;
    }

    /**
     * Helper method to add a message to the chat.
     *
     * @param message The message to add.
     */
    public void addMessage(Message message) {
        messages.add(message);
        message.setChat(this);
    }

    /**
     * Helper method to remove a message from the chat.
     *
     * @param message The message to remove.
     */
    public void removeMessage(Message message) {
        messages.remove(message);
        message.setChat(null);
    }
}
