package uk.gov.hmcts.reform.demo.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.entities.PasswordResetToken;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.PasswordResetTokenRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.services.EmailService;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/forgot-password")
public class ForgotPasswordController {

    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    public ForgotPasswordController(UserRepository userRepository,
                                    PasswordResetTokenRepository passwordResetTokenRepository,
                                    PasswordEncoder passwordEncoder,
                                    EmailService emailService) {
        this.userRepository = userRepository;
        this.passwordResetTokenRepository = passwordResetTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.emailService = emailService;
    }


    /**
     * Request OTP for password reset.
     */
    @PostMapping("/enter-email")
    public ResponseEntity<String> enterEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Please enter a valid email address.");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return badRequest().body("No account found with this email address.");
        }

        User user = optionalUser.get();
        sendOtpToUser(user);

        return ok("A one-time password (OTP) has been sent to your email. The OTP is valid for 10 minutes.");
    }

    /**
     * Resend OTP for password reset.
     */
    @PostMapping("/resend-otp")
    public ResponseEntity<String> resendOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Please enter a valid email address.");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return badRequest().body("No account found with this email address.");
        }

        User user = optionalUser.get();
        sendOtpToUser(user);

        return ok("A one-time password (OTP) has been sent to your email. The OTP is valid for 10 minutes.");
    }


    @PostMapping("/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> body) {
        String email = body.get("email");
        String otp = body.get("otp");

        if (email == null || otp == null) {
            return badRequest().body("Email and OTP are required.");
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return badRequest().body("No account found with this email address.");
        }

        User user = optionalUser.get();

        // Retrieve the OTP from the database
        Optional<PasswordResetToken> optionalResetToken = passwordResetTokenRepository.findByUser(user);
        if (!optionalResetToken.isPresent()) {
            return badRequest().body("No OTP request found. Please request a password reset first.");
        }

        PasswordResetToken resetToken = optionalResetToken.get();

        // Check if the OTP is expired
        if (resetToken.isExpired()) {
            return badRequest().body("The OTP has expired. Please request a new one.");
        }

        // Verify the OTP using password hashing
        if (!passwordEncoder.matches(otp, resetToken.getToken())) {
            return badRequest().body("The one-time password is incorrect. Please try again.");
        }

        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        return ok("OTP verified successfully. Proceed to reset your password.");
    }


    @PostMapping("/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> requestBody) {
        String email = requestBody.get("email");
        String otp = requestBody.get("otp");
        String password = requestBody.get("password");
        String confirmPassword = requestBody.get("confirmPassword");

        if (email == null || otp == null || password == null || confirmPassword == null) {
            return badRequest().body("Email, OTP, and passwords are required.");
        }

        // Check if passwords match
        if (!password.equals(confirmPassword)) {
            return badRequest().body("Passwords do not match.");
        }

        // Validate password strength
        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            return badRequest().body(
                "Password must be at least 8 characters long and include an uppercase letter, "
                    + "a lowercase letter, a number, and a special character."
                                    );
        }

        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return badRequest().body("No account found with this email address.");
        }

        User user = optionalUser.get();

        // Retrieve the OTP from the database
        Optional<PasswordResetToken> optionalResetToken = passwordResetTokenRepository.findByUser(user);
        if (!optionalResetToken.isPresent()) {
            return badRequest().body("No OTP request found. Please request a password reset first.");
        }

        PasswordResetToken resetToken = optionalResetToken.get();

        // Check if the OTP is expired
        if (resetToken.isExpired()) {
            return badRequest().body("The OTP has expired. Please request a new one.");
        }

        // ✅ Ensure the OTP was verified before resetting the password
        if (!resetToken.isUsed()) {
            return badRequest().body("OTP verification required before resetting the password.");
        }

        // Hash and update the new password
        String hashedPassword = passwordEncoder.encode(password);
        user.setPasswordHash(hashedPassword);
        userRepository.save(user);

        passwordResetTokenRepository.delete(resetToken);

        return ok("Password reset successfully.");
    }

    /**
     * Helper method to generate an OTP, store it in the database, and send an email.
     */
    private void sendOtpToUser(User user) {
        String rawOtp = generateOtp();
        String hashedOtp = passwordEncoder.encode(rawOtp);

        Optional<PasswordResetToken> existingToken = passwordResetTokenRepository.findByUser(user);
        PasswordResetToken resetToken;

        if (existingToken.isPresent()) {
            resetToken = existingToken.get();
            resetToken.setToken(hashedOtp);
            resetToken.setExpiryDate(LocalDateTime.now().plusMinutes(10));
            logger.info("Updating existing OTP for user: {}", user.getEmail());
        } else {
            resetToken = new PasswordResetToken(user, hashedOtp);
            logger.info("Creating new OTP for user: {}", user.getEmail());
        }

        passwordResetTokenRepository.save(resetToken);
        logger.info("OTP saved successfully in the database for user: {}", user.getEmail());

        emailService.sendPasswordResetEmail(user.getEmail(), rawOtp);
    }

    /**
     * Generates a 6-digit OTP.
     */
    private String generateOtp() {
        SecureRandom random = new SecureRandom();
        int otp = 100000 + random.nextInt(900000); // Generates a 6-digit OTP
        return String.valueOf(otp);
    }
}
