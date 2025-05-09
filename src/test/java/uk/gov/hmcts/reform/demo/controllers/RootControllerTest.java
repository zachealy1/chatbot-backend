package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.*;

class RootControllerTest {

    private final RootController controller = new RootController();

    @Test
    void welcomeShouldReturn200AndExpectedBody() {
        // Act
        ResponseEntity<String> response = controller.welcome();

        // Assert status code
        assertEquals(200, response.getStatusCodeValue(),
                     "HTTP status should be 200 OK");

        // Assert body
        assertEquals("Welcome to the chatbot backend service",
                     response.getBody(),
                     "Response body should be the welcome message");
    }
}
