package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/account")
public class AccountController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
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

        // Hash the password
        String hashedPassword = hashPassword(password);

        // Create and save the new user
        User newUser = createNewUser(username, email, hashedPassword, dateOfBirth);
        userRepository.save(newUser);

        return ok("User registered successfully.");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateAccount(@RequestBody Map<String, String> userDetails) {
        String email = userDetails.get("email");
        String username = userDetails.get("username");
        String dateOfBirthStr = userDetails.get("dateOfBirth");

        // Validate user input
        ResponseEntity<String> validationResponse = validateUserDetails(email, username, dateOfBirthStr);
        if (validationResponse != null) {
            return validationResponse;
        }

        // Find user by email
        Optional<User> optionalUser = userRepository.findByEmail(email);
        if (!optionalUser.isPresent()) {
            return badRequest().body("No account found with this email address.");
        }

        User user = optionalUser.get();

        // Ensure username is unique
        ResponseEntity<String> usernameValidationResponse = validateUniqueUsername(username, user);
        if (usernameValidationResponse != null) {
            return usernameValidationResponse;
        }

        // Update user details
        user.setUsername(username);
        user.setEmail(email);

        // Update date of birth if provided
        if (dateOfBirthStr != null && !dateOfBirthStr.trim().isEmpty()) {
            ResponseEntity<String> dobValidationResponse = validateAndUpdateDateOfBirth(dateOfBirthStr, user);
            if (dobValidationResponse != null) {
                return dobValidationResponse;
            }
        }

        // Update password if provided
        if (userDetails.containsKey("password") && userDetails.containsKey("confirmPassword")) {
            final String password = userDetails.get("password");
            final String confirmPassword = userDetails.get("confirmPassword");

            ResponseEntity<String> passwordValidationResponse = validateAndUpdatePassword(password,
                                                                                          confirmPassword,
                                                                                          user);
            if (passwordValidationResponse != null) {
                return passwordValidationResponse;
            }
        }

        // Save updated user details
        userRepository.save(user);

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

    /**
     * Hashes the plain-text password using BCrypt.
     *
     * @param password The plain-text password.
     * @return The hashed password.
     */
    private String hashPassword(String password) {
        return passwordEncoder.encode(password);
    }

    /**
     * Validates the email and username fields.
     */
    private ResponseEntity<String> validateUserDetails(String email, String username, String dateOfBirthStr) {
        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Enter a valid email address.");
        }

        if (username == null || username.trim().isEmpty()) {
            return badRequest().body("Username is required.");
        }

        if (dateOfBirthStr != null && !dateOfBirthStr.trim().isEmpty()) {
            if (parseDateOfBirth(dateOfBirthStr) == null) {
                return badRequest().body("Invalid date format. Use YYYY-MM-DD.");
            }
        }

        return null;
    }

    /**
     * Ensures that the new username is unique.
     */
    private ResponseEntity<String> validateUniqueUsername(String username, User currentUser) {
        Optional<User> existingUser = userRepository.findByUsername(username);
        if (existingUser.isPresent() && !existingUser.get().getId().equals(currentUser.getId())) {
            return badRequest().body("Username is already taken. Please choose another one.");
        }

        return null;
    }

    /**
     * Validates and updates the password if provided.
     */
    private ResponseEntity<String> validateAndUpdatePassword(String password, String confirmPassword, User user) {
        if (password != null && confirmPassword != null) {
            if (!password.equals(confirmPassword)) {
                return badRequest().body("Passwords do not match.");
            }

            if (!password.matches("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@$!%*?&])[A-Za-z\\d@$!%*?&]{8,}$")) {
                return badRequest().body(
                    "Password must be at least 8 characters long and include an uppercase letter, "
                        + "a lowercase letter, a number, and a special character."
                                        );
            }

            // Hash and update the password
            user.setPasswordHash(passwordEncoder.encode(password));
        }

        return null;
    }

    private ResponseEntity<String> validateAndUpdateDateOfBirth(String dateOfBirthStr, User user) {
        LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthStr);
        if (dateOfBirth == null) {
            return badRequest().body("Invalid date format. Use YYYY-MM-DD.");
        }

        user.setDateOfBirth(dateOfBirth);
        return null;
    }
}
