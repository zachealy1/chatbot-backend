package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AccountRequestTest {

    @Test
    void defaultConstructor_initializesDefaults() {
        AccountRequest req = new AccountRequest();
        assertNull(req.getId(), "ID should be null by default");
        assertNull(req.getUser(), "User should be null by default");
        assertFalse(req.isApproved(), "Approved flag should be false by default");
        assertNotNull(req.getRequestedAt(), "requestedAt should be initialized to now");
        assertNull(req.getApprovedAt(), "approvedAt should be null by default");
        assertEquals(AccountRequest.Status.PENDING, req.getStatus(),
                     "Status should be PENDING by default");
    }

    @Test
    void userConstructor_setsUserAndPendingStatus() {
        User user = new User();
        user.setId(42L);

        AccountRequest req = new AccountRequest(user);
        assertSame(user, req.getUser(), "Constructor should set the user");
        assertFalse(req.isApproved(), "Approved flag should still be false");
        assertNull(req.getApprovedAt(), "approvedAt should be null initially");
        assertEquals(AccountRequest.Status.PENDING, req.getStatus(),
                     "Status should be PENDING after using user constructor");
    }

    @Test
    void approveRequest_setsApprovedAndStatusAndTimestamp() {
        User user = new User();
        user.setId(1L);
        AccountRequest req = new AccountRequest(user);

        LocalDateTime before = LocalDateTime.now();
        req.approveRequest();
        LocalDateTime after = LocalDateTime.now();

        assertTrue(req.isApproved(), "Approved flag should be true after approval");
        assertEquals(AccountRequest.Status.APPROVED, req.getStatus(),
                     "Status should be APPROVED after approval");
        assertNotNull(req.getApprovedAt(), "approvedAt should be set after approval");
        // approvedAt should lie between before and after
        assertFalse(req.getApprovedAt().isBefore(before),
                    "approvedAt should not be before approval call");
        assertFalse(req.getApprovedAt().isAfter(after),
                    "approvedAt should not be after approval call");
    }

    @Test
    void rejectRequest_setsStatusRejectedAndClearsApprovedAt() {
        User user = new User();
        user.setId(2L);
        AccountRequest req = new AccountRequest(user);
        // first approve then reject to test clearing
        req.approveRequest();
        assertTrue(req.isApproved());
        assertNotNull(req.getApprovedAt());

        req.rejectRequest();
        assertFalse(req.isApproved(), "Approved flag should be false after reject");
        assertEquals(AccountRequest.Status.REJECTED, req.getStatus(),
                     "Status should be REJECTED after reject");
        assertNull(req.getApprovedAt(), "approvedAt should be cleared after reject");
    }

    @Test
    void isPending_returnsTrueOnlyWhenStatusPending() {
        AccountRequest req = new AccountRequest();
        req.setStatus(AccountRequest.Status.PENDING);
        assertTrue(req.isPending(), "isPending should be true when status=PENDING");

        req.setStatus(AccountRequest.Status.APPROVED);
        assertFalse(req.isPending(), "isPending should be false when status=APPROVED");

        req.setStatus(AccountRequest.Status.REJECTED);
        assertFalse(req.isPending(), "isPending should be false when status=REJECTED");
    }

    @Test
    void gettersAndSetters_workProperly() {
        AccountRequest req = new AccountRequest();
        // id
        req.setId(99L);
        assertEquals(99L, req.getId());
        // user
        User u = new User(); u.setId(5L);
        req.setUser(u);
        assertSame(u, req.getUser());
        // approved
        req.setApproved(true);
        assertTrue(req.isApproved());
        req.setApproved(false);
        assertFalse(req.isApproved());
        // requestedAt
        LocalDateTime dt1 = LocalDateTime.of(2000,1,1,0,0);
        req.setRequestedAt(dt1);
        assertEquals(dt1, req.getRequestedAt());
        // approvedAt
        LocalDateTime dt2 = LocalDateTime.of(2001,2,3,4,5);
        req.setApprovedAt(dt2);
        assertEquals(dt2, req.getApprovedAt());
        // status
        req.setStatus(AccountRequest.Status.REJECTED);
        assertEquals(AccountRequest.Status.REJECTED, req.getStatus());
    }
}
