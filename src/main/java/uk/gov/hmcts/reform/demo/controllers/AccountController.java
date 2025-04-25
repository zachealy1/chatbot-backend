package uk.gov.hmcts.reform.demo.controllers;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.GetMapping;
import uk.gov.hmcts.reform.demo.dto.AccountSummary;
import uk.gov.hmcts.reform.demo.dto.PendingRequestSummary;
import uk.gov.hmcts.reform.demo.entities.AccountRequest;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.AccountRequestRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/account")
public class AccountController {

    private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AccountRequestRepository accountRequestRepository;

    public AccountController(UserRepository userRepository, PasswordEncoder passwordEncoder,
                             AccountRequestRepository accountRequestRepository) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.accountRequestRepository = accountRequestRepository;
    }

    @PostMapping("/register")
    public ResponseEntity<String> registerUser(@RequestBody Map<String, String> userDetails) {
        return register(userDetails, false);
    }

    @PostMapping("/register/admin")
    public ResponseEntity<String> registerAdmin(@RequestBody Map<String, String> userDetails) {
        return register(userDetails, true);
    }

    @PostMapping("/approve/{requestId}")
    public ResponseEntity<String> approveAccountRequest(@PathVariable Long requestId) {
        Optional<AccountRequest> optionalRequest = accountRequestRepository.findById(requestId);
        if (!optionalRequest.isPresent()) {
            return badRequest().body("Account request not found.");
        }

        AccountRequest request = optionalRequest.get();
        if (request.isApproved()) {
            return badRequest().body("Account request is already approved.");
        }

        request.setApprovedAt(LocalDateTime.now());
        request.setStatus(AccountRequest.Status.APPROVED);

        User user = request.getUser();
        user.setCanLogin(true); // Allow the user to log in
        userRepository.save(user);

        request.setApproved(true);
        accountRequestRepository.save(request);

        return ok("Account request approved successfully.");
    }

    @Transactional
    @PostMapping("/reject/{requestId}")
    public ResponseEntity<String> rejectAccountRequest(@PathVariable Long requestId) {
        AccountRequest request = accountRequestRepository.findById(requestId)
            .orElseThrow(() -> new RuntimeException("Account request not found."));

        User user = request.getUser();

        // Ensure both deletions happen in the same transaction
        accountRequestRepository.delete(request);
        userRepository.delete(user);

        return ResponseEntity.ok("Account request rejected, and user has been deleted.");
    }

    @GetMapping("/all")
    public ResponseEntity<List<AccountSummary>> listAllAccounts(
        @AuthenticationPrincipal User currentUser
    ) {
        if (currentUser == null || !currentUser.getIsAdmin()) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        var summaries = userRepository.findAll().stream()
            .map(u -> new AccountSummary(
                u.getId(),
                u.getUsername(),
                u.getEmail(),
                u.getIsAdmin() ? "Admin" : "User",
                u.getCreatedAt()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    /**
     * Retrieves all pending account requests.
     */
    @GetMapping("/pending")
    public ResponseEntity<List<PendingRequestSummary>> getPendingAccountRequests() {
        List<PendingRequestSummary> summaries = accountRequestRepository
            .findByStatus(AccountRequest.Status.PENDING)
            .stream()
            .map(r -> new PendingRequestSummary(
                r.getId(),
                r.getUser().getUsername(),
                r.getUser().getEmail(),
                r.getStatus().name(),
                r.getRequestedAt()
            ))
            .collect(Collectors.toList());

        return ResponseEntity.ok(summaries);
    }

    /**
     * Deletes a user account by ID.
     */
    @DeleteMapping("/{userId}")
    public ResponseEntity<String> deleteAccount(@PathVariable Long userId) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (!optionalUser.isPresent()) {
            return ResponseEntity.badRequest().body("User not found.");
        }

        User user = optionalUser.get();

        // Remove any associated account requests first
        Optional<AccountRequest> optionalRequest = accountRequestRepository.findByUser(user);
        optionalRequest.ifPresent(accountRequestRepository::delete);

        // Delete the user account
        userRepository.delete(user);

        return ResponseEntity.ok("User account deleted successfully.");
    }

    @PostMapping("/update")
    public ResponseEntity<String> updateUser(
        @AuthenticationPrincipal User currentUser,
        @RequestBody Map<String, String> userDetailsMap,
        HttpServletRequest request,
        HttpServletResponse response) {

        // Log whether an authenticated user was resolved
        if (currentUser == null) {
            logger.error("No authenticated user found. Authentication principal is null. "
                             + "The request may be missing a valid session cookie or CSRF token.");
            return ResponseEntity.status(401).body("User not authenticated.");
        }
        logger.info("UpdateUser called for user with id: {}", currentUser.getId());

        // Look up the user by the authenticated user's ID.
        Optional<User> optionalUser = userRepository.findById(currentUser.getId());
        if (!optionalUser.isPresent()) {
            logger.error("User with id {} not found in database", currentUser.getId());
            return ResponseEntity.badRequest().body("User not found.");
        }
        User user = optionalUser.get();
        logger.info("User found: {}", user);

        // Extract new values from the payload
        String newUsername = userDetailsMap.get("username");
        String newEmail = userDetailsMap.get("email");
        String dateOfBirthStr = userDetailsMap.get("dateOfBirth");
        logger.info("Received update request with newUsername: '{}', newEmail: '{}', dateOfBirth: '{}'",
                    newUsername, newEmail, dateOfBirthStr);

        // Validate basic details
        ResponseEntity<String> detailsValidation = validateUserDetails(newEmail, newUsername, dateOfBirthStr);
        if (detailsValidation != null) {
            logger.error("Validation error for user details: {}", detailsValidation.getBody());
            return detailsValidation;
        }
        logger.info("Basic details validated successfully.");

        // Validate uniqueness of the new username if it has changed
        ResponseEntity<String> uniqueValidation = validateUniqueUsername(newUsername, user);
        if (uniqueValidation != null) {
            logger.error("Username uniqueness validation failed: {}", uniqueValidation.getBody());
            return uniqueValidation;
        }
        logger.info("Username uniqueness validated successfully.");

        // Update username and email
        user.setUsername(newUsername);
        user.setEmail(newEmail);
        logger.info("Updated user's username and email to: '{}' and '{}'", newUsername, newEmail);

        // Validate and update the date of birth
        ResponseEntity<String> dobValidation = validateAndUpdateDateOfBirth(dateOfBirthStr, user);
        if (dobValidation != null) {
            logger.error("Date of birth validation/update failed: {}", dobValidation.getBody());
            return dobValidation;
        }
        logger.info("Date of birth validated and updated successfully.");

        // Check and update password if provided
        String password = userDetailsMap.get("password");
        String confirmPassword = userDetailsMap.get("confirmPassword");
        if (password != null && !password.trim().isEmpty()) {
            logger.info("New password provided, validating password update.");
            ResponseEntity<String> passwordValidation = validateAndUpdatePassword(password, confirmPassword, user);
            if (passwordValidation != null) {
                logger.error("Password validation/update failed: {}", passwordValidation.getBody());
                return passwordValidation;
            }
            logger.info("Password validated and updated successfully.");
        } else {
            logger.info("No new password provided; skipping password update.");
        }

        // Save updated user information
        userRepository.save(user);
        logger.info("User information saved successfully for user id: {}", user.getId());

        // Refresh the authentication principal with the updated user data.
        Authentication updatedAuthentication =
            new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(updatedAuthentication);
        new HttpSessionSecurityContextRepository()
            .saveContext(SecurityContextHolder.getContext(), request, response);

        return ResponseEntity.ok("Account updated successfully.");
    }


    /**
     * Returns the authenticated user's username.
     */
    @GetMapping("/username")
    public ResponseEntity<String> getUsername(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        return ResponseEntity.ok(currentUser.getUsername());
    }

    /**
     * Returns the authenticated user's email.
     */
    @GetMapping("/email")
    public ResponseEntity<String> getEmail(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("User not authenticated");
        }
        return ResponseEntity.ok(currentUser.getEmail());
    }

    /**
     * Returns the day (1-31) of the authenticated user's date of birth.
     */
    @GetMapping("/date-of-birth/day")
    public ResponseEntity<Integer> getDateOfBirthDay(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getDateOfBirth() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int day = currentUser.getDateOfBirth().getDayOfMonth();
        return ResponseEntity.ok(day);
    }

    /**
     * Returns the month (1-12) of the authenticated user's date of birth.
     */
    @GetMapping("/date-of-birth/month")
    public ResponseEntity<Integer> getDateOfBirthMonth(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getDateOfBirth() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int month = currentUser.getDateOfBirth().getMonthValue();
        return ResponseEntity.ok(month);
    }

    /**
     * Returns the year of the authenticated user's date of birth.
     */
    @GetMapping("/date-of-birth/year")
    public ResponseEntity<Integer> getDateOfBirthYear(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null || currentUser.getDateOfBirth() == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        int year = currentUser.getDateOfBirth().getYear();
        return ResponseEntity.ok(year);
    }

    private ResponseEntity<String> register(Map<String, String> userDetails, boolean isAdmin) {
        String username = userDetails.get("username");
        String email = userDetails.get("email");
        String password = userDetails.get("password");
        String confirmPassword = userDetails.get("confirmPassword");
        String dateOfBirthStr = userDetails.get("dateOfBirth");

        String validationError = validateUserInput(username, email, password, confirmPassword, dateOfBirthStr);
        if (validationError != null) {
            return badRequest().body(validationError);
        }

        LocalDate dateOfBirth = parseDateOfBirth(dateOfBirthStr);
        if (dateOfBirth == null) {
            return badRequest().body("Invalid date format. Use YYYY-MM-DD.");
        }

        String existenceError = checkUserExistence(username, email);
        if (existenceError != null) {
            return badRequest().body(existenceError);
        }

        User newUser = createNewUser(username, email, passwordEncoder.encode(password), dateOfBirth, isAdmin);
        newUser = userRepository.save(newUser); // 🔹 Save User to Database First

        AccountRequest accountRequest = new AccountRequest();
        accountRequest.setUser(newUser);
        accountRequest.setStatus(AccountRequest.Status.PENDING);
        accountRequestRepository.save(accountRequest);

        return ok(isAdmin ? "Admin registered successfully." : "User registered successfully.");
    }

    /**
     * Logs an account request for the newly registered user.
     *
     * @param user The registered user.
     */
    private void logAccountRequest(User user) {
        AccountRequest accountRequest = new AccountRequest(user);
        accountRequestRepository.save(accountRequest);
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
     * Creates a new user entity.
     */
    private User createNewUser(String username, String email, String hashedPassword,
                               LocalDate dateOfBirth, boolean isAdmin) {
        User newUser = new User();
        newUser.setUsername(username);
        newUser.setEmail(email);
        newUser.setPasswordHash(hashedPassword);
        newUser.setDateOfBirth(dateOfBirth);
        newUser.setIsAdmin(isAdmin);
        newUser.setCanLogin(false);
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
