package uk.gov.hmcts.reform.demo.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.utils.ChatGptApi;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
public class RootController {

    private static final Logger logger = LoggerFactory.getLogger(RootController.class);

    private final UserRepository userRepository;

    public RootController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/")
    public ResponseEntity<String> welcome() {
        return ok("Welcome to the chatbot backend service");
    }

    /**
     * Chat endpoint to handle user queries and return chatbot responses.
     *
     * @param userInput A map containing the user's input message.
     * @return Simulated chatbot response.
     */
    @PostMapping("/chat")
    public ResponseEntity<String> chat(@RequestBody Map<String, String> userInput) {
        String message = userInput.get("message");

        if (message == null || message.trim().isEmpty()) {
            return badRequest().body("Message cannot be empty. Please provide a valid input.");
        }

        logger.info("User sent message: {}", message);

        try {
            String response = ChatGptApi.chatGpt(message);

            logger.info("ChatGPT response: {}", response);

            return ok(response);
        } catch (RuntimeException e) {
            logger.error("Error while communicating with ChatGPT API: {}", e.getMessage());
            return badRequest().body("An error occurred while processing your request. Please try again later.");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> userDetails) {
        // Extract user details
        String username = userDetails.get("username");
        String email = userDetails.get("email");
        String password = userDetails.get("password");
        String confirmPassword = userDetails.get("confirmPassword");
        String dateOfBirthStr = userDetails.get("dateOfBirth");

        // Validate user input
        String validationError = validateUserInput(username, email, password, confirmPassword, dateOfBirthStr);
        if (validationError != null) {
            return badRequest().body(validationError);
        }

        // Parse date of birth
        LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthStr);
        if (dateOfBirth == null) {
            return badRequest().body("Invalid date format. Use YYYY-MM-DD.");
        }

        // Check if user already exists
        String existenceError = checkUserExistence(username, email);
        if (existenceError != null) {
            return badRequest().body(existenceError);
        }

        // Create and save the new user
        User newUser = createNewUser(username, email, password, dateOfBirth);
        userRepository.save(newUser);

        return ok("User registered successfully.");
    }


    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        if (username == null || password == null) {
            return badRequest().body("Username and password are required.");
        }

        return ok("User logged in successfully.");
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout() {
        return ok("User logged out successfully.");
    }

    @PostMapping("/forgot-password/enter-email")
    public ResponseEntity<String> enterEmail(@RequestBody Map<String, String> body) {
        String email = body.get("email");

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Please enter a valid email address.");
        }

        return ok("Password reset email sent successfully.");
    }

    @PostMapping("/forgot-password/reset-password")
    public ResponseEntity<String> resetPassword(@RequestBody Map<String, String> passwords) {
        String password = passwords.get("password");
        String confirmPassword = passwords.get("confirmPassword");

        if (!password.equals(confirmPassword)) {
            return badRequest().body("Passwords do not match.");
        }

        if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
            return badRequest().body(
                "Password must be at least 8 characters long and include an uppercase letter, "
                    + "a lowercase letter, a number, and a special character."
                                    );
        }

        return ok("Password reset successfully.");
    }

    @PostMapping("/forgot-password/resend-otp")
    public ResponseEntity<String> resendOtp() {
        return ok("OTP has been resent successfully.");
    }

    @PostMapping("/forgot-password/verify-otp")
    public ResponseEntity<String> verifyOtp(@RequestBody Map<String, String> body) {
        String otp = body.get("otp");
        String expectedOtp = "123456"; // Replace with real logic

        if (otp == null || !otp.equals(expectedOtp)) {
            return badRequest().body("The one-time password is incorrect. Please try again.");
        }

        return ok("OTP verified successfully.");
    }

    @PostMapping("/account/update")
    public ResponseEntity<String> updateAccount(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String email = userDetails.get("email");

        if (username == null || username.trim().isEmpty()) {
            return badRequest().body("Username is required.");
        }

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Enter a valid email address.");
        }

        return ok("Account updated successfully.");
    }

    /**
     * Validates the user input for registration.
     *
     * @param username        The desired username.
     * @param email           The user's email address.
     * @param password        The user's password.
     * @param confirmPassword The password confirmation.
     * @param dateOfBirthStr  The user's date of birth as a string.
     * @return An error message if validation fails; otherwise, null.
     */
    private String validateUserInput(String username, String email, String password,
                                     String confirmPassword, String dateOfBirthStr) {
        if (username == null || username.trim().isEmpty()) {
            return "Username is required.";
        }

        if (email == null || !isValidEmail(email)) {
            return "Enter a valid email address.";
        }

        if (password == null || password.trim().isEmpty()) {
            return "Password is required.";
        }

        if (!password.equals(confirmPassword)) {
            return "Passwords do not match.";
        }

        if (dateOfBirthStr == null || dateOfBirthStr.trim().isEmpty()) {
            return "Date of birth is required.";
        }

        return null;
    }

    /**
     * Validates the email format.
     *
     * @param email The email address to validate.
     * @return True if the email is valid; otherwise, false.
     */
    private boolean isValidEmail(String email) {
        String emailRegex = "^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$";
        return email.matches(emailRegex);
    }

    /**
     * Parses the date of birth string into a LocalDate.
     *
     * @param dateOfBirthStr The date of birth as a string.
     * @return The parsed LocalDate, or null if parsing fails.
     */
    private LocalDate parseDateOfBirth(String dateOfBirthStr) {
        try {
            return LocalDate.parse(dateOfBirthStr);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    /**
     * Checks if a user with the given username or email already exists.
     *
     * @param username The desired username.
     * @param email    The user's email address.
     * @return An error message if the user exists; otherwise, null.
     */
    private String checkUserExistence(String username, String email) {
        Optional<User> existingUserByEmail = userRepository.findByEmail(email);
        if (existingUserByEmail.isPresent()) {
            return "Email is already registered.";
        }

        Optional<User> existingUserByUsername = userRepository.findByUsername(username);
        if (existingUserByUsername.isPresent()) {
            return "Username is already taken.";
        }

        return null;
    }

    /**
     * Creates a new User entity.
     *
     * @param username       The desired username.
     * @param email          The user's email address.
     * @param hashedPassword The hashed password.
     * @param dateOfBirth    The user's date of birth.
     * @return A new User instance.
     */
    private User createNewUser(String username, String email, String hashedPassword, LocalDate dateOfBirth) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(hashedPassword);
        newUser.setDateOfBirth(dateOfBirth);
        return newUser;
    }
}
