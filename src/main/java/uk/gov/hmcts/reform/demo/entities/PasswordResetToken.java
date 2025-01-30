package uk.gov.hmcts.reform.demo.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;

import java.time.LocalDateTime;

@Entity
public class PasswordResetToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true)
    private String token;

    @Column(nullable = false)
    private LocalDateTime expiryDate;

    public PasswordResetToken() {

    }

    public PasswordResetToken(User user, String token) {
        this.user = user;
        this.token = token;
        this.expiryDate = LocalDateTime.now().plusHours(1); // Token expires in 1 hour
    }

    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiryDate);
    }

    // Getters and setters...
}

