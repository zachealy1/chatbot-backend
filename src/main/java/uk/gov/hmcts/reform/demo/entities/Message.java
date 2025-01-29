package uk.gov.hmcts.reform.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

/**
 * Entity representing a message within a chat.
 */
@Entity
@Table(name = "messages")
public class Message {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The chat to which this message belongs.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "chat_id", nullable = false)
    private Chat chat;

    /**
     * The sender of the message (e.g., "user" or "chatbot").
     */
    @Column(nullable = false, length = 50)
    private String sender;

    /**
     * The content of the message.
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    /**
     * Timestamp when the message was created.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Default constructor that initializes the creation timestamp.
     */
    public Message() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Parameterized constructor for creating a new message.
     *
     * @param chat    The chat to which the message belongs.
     * @param sender  The sender of the message.
     * @param message The content of the message.
     */
    public Message(Chat chat, String sender, String message) {
        this.chat = chat;
        this.sender = sender;
        this.message = message;
        this.createdAt = LocalDateTime.now();
    }

    // Getters and Setters

    public Long getId() {
        return id;
    }

    // No setter for 'id' as it's auto-generated.

    public Chat getChat() {
        return chat;
    }

    public void setChat(Chat chat) {
        this.chat = chat;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // No setter for 'createdAt' as it's set at creation.
}
