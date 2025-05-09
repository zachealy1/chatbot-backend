package uk.gov.hmcts.reform.demo.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import uk.gov.hmcts.reform.demo.dto.AccountSummary;
import uk.gov.hmcts.reform.demo.dto.PendingRequestSummary;
import uk.gov.hmcts.reform.demo.entities.AccountRequest;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;
import uk.gov.hmcts.reform.demo.repositories.AccountRequestRepository;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class AccountControllerTest {

    @InjectMocks
    @Spy
    private AccountController controller;

    @Mock
    private MessageSource messages;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AccountRequestRepository accountRequestRepository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // ensure LocaleContextHolder.getLocale() == English
        LocaleContextHolder.setLocale(Locale.ENGLISH);
    }

    @Test
    void whenInvalidDate_thenReturnsBadRequest() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("username", "user1");
        userDetails.put("email", "user1@example.com");
        userDetails.put("password", "pass");
        userDetails.put("confirmPassword", "pass");
        userDetails.put("dateOfBirth", "not-a-date");

        when(messages.getMessage(eq("error.invalid.date"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Invalid date");

        ResponseEntity<String> resp = controller.registerUser(userDetails);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid date", resp.getBody());
        verify(messages).getMessage("error.invalid.date", null, Locale.ENGLISH);
    }

    @Test
    void whenAllValid_thenPersistsAndReturnsOk() {
        Map<String, String> userDetails = new HashMap<>();
        userDetails.put("username", "user1");
        userDetails.put("email", "user1@example.com");
        userDetails.put("password", "pass");
        userDetails.put("confirmPassword", "pass");
        // ISO format so your parseDateOfBirth(...) should succeed
        userDetails.put("dateOfBirth", "2000-01-01");

        // stub encoding & success message
        when(passwordEncoder.encode("pass")).thenReturn("encodedPass");
        when(messages.getMessage(eq("success.user.registered"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Registration successful");

        ResponseEntity<String> resp = controller.registerUser(userDetails);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Registration successful", resp.getBody());

        // verify that both save() calls happened
        verify(userRepository).save(any());
        verify(accountRequestRepository).save(any());
    }

    @Test
    void whenInvalidDateAdmin_thenReturnsBadRequest() {
        Map<String, String> adminDetails = new HashMap<>();
        adminDetails.put("username", "admin");
        adminDetails.put("email", "admin@example.com");
        adminDetails.put("password", "pw");
        adminDetails.put("confirmPassword", "pw");
        adminDetails.put("dateOfBirth", "bad-date");

        when(messages.getMessage(eq("error.invalid.date"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Invalid admin date");

        ResponseEntity<String> resp = controller.registerAdmin(adminDetails);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid admin date", resp.getBody());
        verify(messages).getMessage("error.invalid.date", null, Locale.ENGLISH);
    }

    @Test
    void whenAllValidAdmin_thenPersistsAndReturnsOkAdmin() {
        Map<String, String> adminDetails = new HashMap<>();
        adminDetails.put("username", "adminUser");
        adminDetails.put("email", "admin@example.com");
        adminDetails.put("password", "secret");
        adminDetails.put("confirmPassword", "secret");
        adminDetails.put("dateOfBirth", "1980-12-31");

        when(passwordEncoder.encode("secret")).thenReturn("encodedSecret");
        when(messages.getMessage(eq("success.admin.registered"), isNull(), eq(Locale.ENGLISH)))
            .thenReturn("Admin registration successful");

        ResponseEntity<String> resp = controller.registerAdmin(adminDetails);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Admin registration successful", resp.getBody());

        // Should have created & saved both User and AccountRequest
        verify(userRepository).save(any());
        verify(accountRequestRepository).save(any());
    }

    @Test
    void whenRequestNotFound_thenReturnsBadRequest() {
        // Arrange
        Long requestId = 42L;
        when(accountRequestRepository.findById(requestId))
            .thenReturn(Optional.empty());

        // Act
        ResponseEntity<String> resp = controller.approveAccountRequest(requestId);

        // Assert
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Account request not found.", resp.getBody());

        // No side-effects
        verify(accountRequestRepository).findById(requestId);
        verifyNoMoreInteractions(accountRequestRepository, userRepository);
    }

    @Test
    void whenAlreadyApproved_thenReturnsBadRequest() {
        // Arrange
        Long requestId = 100L;
        AccountRequest already = new AccountRequest();
        already.setApproved(true);
        when(accountRequestRepository.findById(requestId))
            .thenReturn(Optional.of(already));

        // Act
        ResponseEntity<String> resp = controller.approveAccountRequest(requestId);

        // Assert
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Account request is already approved.", resp.getBody());

        verify(accountRequestRepository).findById(requestId);
        verifyNoMoreInteractions(accountRequestRepository, userRepository);
    }

    @Test
    void whenValidRequest_thenApproveAndPersistAndReturnOk() {
        // Arrange
        Long requestId = 7L;

        User user = new User();
        user.setCanLogin(false);

        AccountRequest req = new AccountRequest();
        req.setId(requestId);
        req.setUser(user);
        req.setApproved(false);
        req.setStatus(AccountRequest.Status.PENDING);

        when(accountRequestRepository.findById(requestId))
            .thenReturn(Optional.of(req));

        // Act
        ResponseEntity<String> resp = controller.approveAccountRequest(requestId);

        // Assert response
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Account request approved successfully.", resp.getBody());

        // Assert that user was enabled
        assertTrue(user.getCanLogin());

        // Assert that request was marked approved
        assertTrue(req.isApproved());
        assertEquals(AccountRequest.Status.APPROVED, req.getStatus());

        // approvedAt should be set to "now" (within a small window)
        LocalDateTime then = req.getApprovedAt();
        assertNotNull(then);
        assertTrue(then.isBefore(LocalDateTime.now().plusSeconds(1)));

        // Verify repository interactions
        InOrder inOrder = inOrder(accountRequestRepository, userRepository);
        // find first
        inOrder.verify(accountRequestRepository).findById(requestId);
        // then save user
        inOrder.verify(userRepository).save(user);
        // then save request
        inOrder.verify(accountRequestRepository).save(req);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenRequestNotFound_thenThrowsRuntimeException() {
        Long reqId = 123L;
        when(accountRequestRepository.findById(reqId)).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> {
            controller.rejectAccountRequest(reqId);
        });

        assertEquals("Account request not found.", ex.getMessage());
        verify(accountRequestRepository).findById(reqId);
        verifyNoMoreInteractions(accountRequestRepository, userRepository);
    }

    @Test
    void whenRequestFound_thenDeletesAndReturnsOk() {
        // Arrange: build a request with a linked user
        Long reqId = 456L;
        User user = new User();
        user.setId(99L);
        AccountRequest req = new AccountRequest();
        req.setId(reqId);
        req.setUser(user);

        when(accountRequestRepository.findById(reqId)).thenReturn(Optional.of(req));

        // Act
        ResponseEntity<String> resp = controller.rejectAccountRequest(reqId);

        // Assert response
        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Account request rejected, and user has been deleted.", resp.getBody());

        // Verify deletion order: first request, then user
        InOrder inOrder = inOrder(accountRequestRepository, userRepository);
        inOrder.verify(accountRequestRepository).delete(req);
        inOrder.verify(userRepository).delete(user);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenCurrentUserIsNull_thenReturnsForbidden() {
        ResponseEntity<List<AccountSummary>> resp = controller.listAllAccounts(null);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    void whenCurrentUserIsNotAdmin_thenReturnsForbidden() {
        User nonAdmin = new User();
        nonAdmin.setIsAdmin(false);
        ResponseEntity<List<AccountSummary>> resp = controller.listAllAccounts(nonAdmin);
        assertEquals(HttpStatus.FORBIDDEN, resp.getStatusCode());
        assertNull(resp.getBody());
    }

    @Test
    void whenCurrentUserIsAdmin_thenReturnsSummaries() {
        // Arrange
        User adminUser = new User();
        adminUser.setIsAdmin(true);

        // Create two test users
        User u1 = new User();
        u1.setId(1L);
        u1.setUsername("alice");
        u1.setEmail("alice@example.com");
        u1.setIsAdmin(false);
        LocalDate t1 = LocalDate.of(2020, 1, 1);
        u1.setCreatedAt(t1);

        User u2 = new User();
        u2.setId(2L);
        u2.setUsername("bob");
        u2.setEmail("bob@example.com");
        u2.setIsAdmin(true);
        LocalDate t2 = LocalDate.of(2021, 2, 2);
        u2.setCreatedAt(t2);

        when(userRepository.findAll()).thenReturn(Arrays.asList(u1, u2));

        // Act
        ResponseEntity<List<AccountSummary>> resp = controller.listAllAccounts(adminUser);

        // Assert status
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<AccountSummary> summaries = resp.getBody();
        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        // Verify first summary (regular user)
        AccountSummary s1 = summaries.get(0);
        assertEquals(1L, s1.getAccountId());
        assertEquals("alice", s1.getUsername());
        assertEquals("alice@example.com", s1.getEmail());
        assertEquals("User", s1.getRole());
        assertEquals(t1, s1.getCreatedDate());

        // Verify second summary (admin)
        AccountSummary s2 = summaries.get(1);
        assertEquals(2L, s2.getAccountId());
        assertEquals("bob", s2.getUsername());
        assertEquals("bob@example.com", s2.getEmail());
        assertEquals("Admin", s2.getRole());
        assertEquals(t2, s2.getCreatedDate());

        // Verify repository interaction
        verify(userRepository).findAll();
        verifyNoMoreInteractions(userRepository);
    }

    @Test
    void whenThereArePendingRequests_thenReturnSummaries() {
        // Arrange
        User user1 = new User();
        user1.setUsername("alice");
        user1.setEmail("alice@example.com");
        AccountRequest r1 = new AccountRequest();
        r1.setId(1L);
        r1.setUser(user1);
        r1.setStatus(AccountRequest.Status.PENDING);
        LocalDateTime t1 = LocalDateTime.of(2023,5,1,14,0);
        r1.setRequestedAt(t1);

        User user2 = new User();
        user2.setUsername("bob");
        user2.setEmail("bob@example.com");
        AccountRequest r2 = new AccountRequest();
        r2.setId(2L);
        r2.setUser(user2);
        r2.setStatus(AccountRequest.Status.PENDING);
        LocalDateTime t2 = LocalDateTime.of(2023,5,2,15,30);
        r2.setRequestedAt(t2);

        when(accountRequestRepository.findByStatus(AccountRequest.Status.PENDING))
            .thenReturn(Arrays.asList(r1, r2));

        // Act
        ResponseEntity<List<PendingRequestSummary>> resp = controller.getPendingAccountRequests();

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        List<PendingRequestSummary> summaries = resp.getBody();
        assertNotNull(summaries);
        assertEquals(2, summaries.size());

        PendingRequestSummary s1 = summaries.get(0);
        assertEquals(1L, s1.getRequestId());
        assertEquals("alice", s1.getUserName());
        assertEquals("alice@example.com", s1.getEmail());
        assertEquals("PENDING", s1.getStatus());
        assertEquals(t1, s1.getSubmittedDate());

        PendingRequestSummary s2 = summaries.get(1);
        assertEquals(2L, s2.getRequestId());
        assertEquals("bob", s2.getUserName());
        assertEquals("bob@example.com", s2.getEmail());
        assertEquals("PENDING", s2.getStatus());
        assertEquals(t2, s2.getSubmittedDate());

        verify(accountRequestRepository).findByStatus(AccountRequest.Status.PENDING);
        verifyNoMoreInteractions(accountRequestRepository);
    }

    @Test
    void whenNoPendingRequests_thenReturnEmptyList() {
        // Arrange
        when(accountRequestRepository.findByStatus(AccountRequest.Status.PENDING))
            .thenReturn(Collections.emptyList());

        // Act
        ResponseEntity<List<PendingRequestSummary>> resp = controller.getPendingAccountRequests();

        // Assert
        assertEquals(HttpStatus.OK, resp.getStatusCode());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());

        verify(accountRequestRepository).findByStatus(AccountRequest.Status.PENDING);
        verifyNoMoreInteractions(accountRequestRepository);
    }

    @Test
    void whenUserNotFound_thenReturnsBadRequest() {
        Long userId = 1L;
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.deleteAccount(userId);

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("User not found.", resp.getBody());

        verify(userRepository).findById(userId);
        verifyNoMoreInteractions(userRepository, accountRequestRepository);
    }

    @Test
    void whenNoAssociatedRequest_thenDeletesUserOnly() {
        Long userId = 2L;
        User user = new User();
        user.setId(userId);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRequestRepository.findByUser(user)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.deleteAccount(userId);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("User account deleted successfully.", resp.getBody());

        InOrder inOrder = inOrder(accountRequestRepository, userRepository);
        // should check for associated request first
        inOrder.verify(accountRequestRepository).findByUser(user);
        // then delete only the user
        inOrder.verify(userRepository).delete(user);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenAssociatedRequestExists_thenDeletesRequestThenUser() {
        Long userId = 3L;
        User user = new User();
        user.setId(userId);
        AccountRequest req = new AccountRequest();
        req.setId(10L);
        req.setUser(user);

        when(userRepository.findById(userId)).thenReturn(Optional.of(user));
        when(accountRequestRepository.findByUser(user)).thenReturn(Optional.of(req));

        ResponseEntity<String> resp = controller.deleteAccount(userId);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("User account deleted successfully.", resp.getBody());

        InOrder inOrder = inOrder(accountRequestRepository, userRepository);
        // first find associated request
        inOrder.verify(accountRequestRepository).findByUser(user);
        // then delete the request
        inOrder.verify(accountRequestRepository).delete(req);
        // then delete the user
        inOrder.verify(userRepository).delete(user);
        inOrder.verifyNoMoreInteractions();
    }

    @Test
    void whenNoPrincipal_then401() {
        ResponseEntity<String> resp = controller.updateUser(
            null,
            Map.of(),
            new MockHttpServletRequest(),
            new MockHttpServletResponse()
        );
        assertEquals(401, resp.getStatusCodeValue());
        assertEquals("User not authenticated.", resp.getBody());
    }

    @Test
    void whenUserNotInDatabase_then400() {
        User principal = new User();
        principal.setId(99L);
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseEntity<String> resp = controller.updateUser(
            principal,
            Map.of("username", "x", "email", "y", "dateOfBirth", "2000-01-01"),
            new MockHttpServletRequest(),
            new MockHttpServletResponse()
        );

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("User not found.", resp.getBody());
        verify(userRepository).findById(99L);
    }

    @Test
    void whenDetailsValidationFails_thenReturnsThatResponse() throws Exception {
        User principal = new User();
        principal.setId(1L);
        User stored = new User();
        stored.setId(1L);

        when(userRepository.findById(1L)).thenReturn(Optional.of(stored));
        // stub out the private validator to return a custom bad-request
        doReturn(ResponseEntity.badRequest().body("bad details"))
            .when(controller)
            .validateUserDetails(anyString(), anyString(), anyString());

        Map<String,String> body = Map.of(
            "username", "foo",
            "email", "bar",
            "dateOfBirth", "2000-01-01"
        );

        ResponseEntity<String> resp = controller.updateUser(
            principal,
            body,
            new MockHttpServletRequest(),
            new MockHttpServletResponse()
        );

        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("bad details", resp.getBody());
        // ensure we never get as far as saving
        verify(userRepository, never()).save(any());
    }

    @Test
    void whenValidWithoutPassword_thenUpdatesAndRefreshesAuth() throws Exception {
        User principal = new User();
        principal.setId(2L);
        User stored = new User();
        stored.setId(2L);

        when(userRepository.findById(2L)).thenReturn(Optional.of(stored));
        doReturn(null).when(controller).validateUserDetails(anyString(), anyString(), anyString());
        doReturn(null).when(controller).validateUniqueUsername(anyString(), eq(stored));
        doReturn(null).when(controller).validateAndUpdateDateOfBirth(anyString(), eq(stored));
        // note: no password in map → skip password branch

        Map<String,String> body = new HashMap<>();
        body.put("username", "newuser");
        body.put("email", "new@example.com");
        body.put("dateOfBirth", "2000-01-01");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseEntity<String> resp = controller.updateUser(principal, body, req, res);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Account updated successfully.", resp.getBody());

        // Verify the user was saved with updated fields
        assertEquals("newuser", stored.getUsername());
        assertEquals("new@example.com", stored.getEmail());
        verify(userRepository).save(stored);

        // Check that SecurityContext was refreshed to use the updated user
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertTrue(auth instanceof UsernamePasswordAuthenticationToken);
        assertSame(stored, auth.getPrincipal());
    }

    @Test
    void whenValidWithPassword_thenInvokesPasswordUpdate() throws Exception {
        User principal = new User();
        principal.setId(3L);
        User stored = new User();
        stored.setId(3L);

        when(userRepository.findById(3L)).thenReturn(Optional.of(stored));
        doReturn(null).when(controller).validateUserDetails(anyString(), anyString(), anyString());
        doReturn(null).when(controller).validateUniqueUsername(anyString(), eq(stored));
        doReturn(null).when(controller).validateAndUpdateDateOfBirth(anyString(), eq(stored));
        // stub password-update to succeed
        doReturn(null).when(controller).validateAndUpdatePassword(eq("pw1"), eq("pw1"), eq(stored));

        Map<String,String> body = new HashMap<>();
        body.put("username", "u3");
        body.put("email", "e3@example.com");
        body.put("dateOfBirth", "1995-05-05");
        body.put("password", "pw1");
        body.put("confirmPassword", "pw1");

        MockHttpServletRequest req = new MockHttpServletRequest();
        MockHttpServletResponse res = new MockHttpServletResponse();

        ResponseEntity<String> resp = controller.updateUser(principal, body, req, res);

        assertEquals(200, resp.getStatusCodeValue());
        assertEquals("Account updated successfully.", resp.getBody());

        // Verify the private validateAndUpdatePassword was invoked
        verify(controller).validateAndUpdatePassword("pw1","pw1",stored);
        verify(userRepository).save(stored);
    }

    @Test
    void whenCurrentUserIsNull_thenReturnsUnauthorized() {
        // Act
        ResponseEntity<String> response = controller.getUsername(null);

        // Assert
        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatusCode());
        assertEquals("User not authenticated", response.getBody());

        // No repository interactions should occur
        verifyNoInteractions(userRepository, accountRequestRepository);
    }

    @Test
    void whenCurrentUserIsPresent_thenReturnsUsername() {
        // Arrange
        User currentUser = new User();
        currentUser.setUsername("alice");

        // Act
        ResponseEntity<String> response = controller.getUsername(currentUser);

        // Assert
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals("alice", response.getBody());

        // No repository interactions should occur
        verifyNoInteractions(userRepository, accountRequestRepository);
    }
}
