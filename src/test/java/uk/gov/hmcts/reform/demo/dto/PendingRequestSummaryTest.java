package uk.gov.hmcts.reform.demo.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class PendingRequestSummaryTest {

    @Test
    void constructorAndGettersInitializeFields() {
        LocalDateTime now = LocalDateTime.of(2025, 5, 9, 12, 34, 56);
        PendingRequestSummary dto = new PendingRequestSummary(
            99L, "alice", "alice@example.com", "PENDING", now
        );

        assertEquals(99L, dto.getRequestId());
        assertEquals("alice", dto.getUserName());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("PENDING", dto.getStatus());
        assertEquals(now, dto.getSubmittedDate());
    }

    @Test
    void settersUpdateFields() {
        PendingRequestSummary dto = new PendingRequestSummary(
            1L, "bob", "bob@example.com", "NEW", LocalDateTime.now()
        );

        dto.setRequestId(123L);
        dto.setUserName("charlie");
        dto.setEmail("charlie@example.com");
        dto.setStatus("APPROVED");
        LocalDateTime later = LocalDateTime.of(2030, 1, 1, 0, 0);
        dto.setSubmittedDate(later);

        assertEquals(123L, dto.getRequestId());
        assertEquals("charlie", dto.getUserName());
        assertEquals("charlie@example.com", dto.getEmail());
        assertEquals("APPROVED", dto.getStatus());
        assertEquals(later, dto.getSubmittedDate());
    }

    @Test
    void canHandleNullValuesInSetters() {
        PendingRequestSummary dto = new PendingRequestSummary(
            5L, "dave", "dave@example.com", "REJECTED", LocalDateTime.now()
        );

        dto.setRequestId(null);
        dto.setUserName(null);
        dto.setEmail(null);
        dto.setStatus(null);
        dto.setSubmittedDate(null);

        assertNull(dto.getRequestId());
        assertNull(dto.getUserName());
        assertNull(dto.getEmail());
        assertNull(dto.getStatus());
        assertNull(dto.getSubmittedDate());
    }
}
