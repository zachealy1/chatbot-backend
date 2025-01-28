package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

import static org.springframework.http.ResponseEntity.badRequest;
import static org.springframework.http.ResponseEntity.ok;

@RestController
@RequestMapping("/")
public class RootController {

    @GetMapping
    public ResponseEntity<String> welcome() {
        return ok("Welcome to the chatbot backend service");
    }

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody Map<String, String> userDetails) {
        String username = userDetails.get("username");
        String email = userDetails.get("email");
        String password = userDetails.get("password");
        String confirmPassword = userDetails.get("confirmPassword");

        if (username == null || username.trim().isEmpty()) {
            return badRequest().body("Username is required.");
        }

        if (email == null || !email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            return badRequest().body("Enter a valid email address.");
        }

        if (!password.equals(confirmPassword)) {
            return badRequest().body("Passwords do not match.");
        }

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
}
