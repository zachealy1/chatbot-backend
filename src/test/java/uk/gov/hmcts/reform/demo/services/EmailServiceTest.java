package uk.gov.hmcts.reform.demo.services;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // Inject the @Value field
        ReflectionTestUtils.setField(emailService, "fromEmail", "no-reply@example.com");
    }

    @Test
    void sendPasswordResetEmail_sendsMessageWithCorrectFields() {
        // Arrange
        String toEmail = "user@domain.com";
        String otp = "654321";

        ArgumentCaptor<SimpleMailMessage> captor =
            ArgumentCaptor.forClass(SimpleMailMessage.class);

        // Act
        emailService.sendPasswordResetEmail(toEmail, otp);

        // Assert
        verify(mailSender).send(captor.capture());
        SimpleMailMessage msg = captor.getValue();

        assertEquals("no-reply@example.com", msg.getFrom(), "From address should come from @Value");
        assertArrayEquals(new String[]{toEmail}, msg.getTo(), "To address should be as passed");
        assertEquals("Password Reset Request", msg.getSubject(), "Subject should be fixed");
        String text = msg.getText();
        assertTrue(text.startsWith("Here is your one time password"), "Body should start correctly");
        assertTrue(text.contains(otp), "Body should contain the OTP");
    }
}
