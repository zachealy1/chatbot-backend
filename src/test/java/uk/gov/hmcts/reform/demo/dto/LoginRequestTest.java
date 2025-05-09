package uk.gov.hmcts.reform.demo.dto;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestTest {

    @Test
    void defaultConstructor_initializesWithNullFields() {
        LoginRequest req = new LoginRequest();
        assertNull(req.getUsername(), "Username should be null by default");
        assertNull(req.getPassword(), "Password should be null by default");
    }

    @Test
    void parameterizedConstructor_initializesFieldsCorrectly() {
        LoginRequest req = new LoginRequest("alice", "secret");
        assertEquals("alice", req.getUsername(), "Username should match constructor argument");
        assertEquals("secret", req.getPassword(), "Password should match constructor argument");
    }

    @Test
    void settersAndGetters_workAsExpected() {
        LoginRequest req = new LoginRequest();
        req.setUsername("bob");
        req.setPassword("hunter2");
        assertEquals("bob", req.getUsername(), "Getter should return value set by setter");
        assertEquals("hunter2", req.getPassword(), "Getter should return value set by setter");
    }
}
