package uk.gov.hmcts.reform.demo.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class AccountSummaryTest {

    @Test
    void constructorAndGettersShouldInitializeFields() {
        LocalDate date = LocalDate.of(2025, 5, 9);
        AccountSummary dto = new AccountSummary(
            123L, "alice", "alice@example.com", "Admin", date
        );

        assertEquals(123L, dto.getAccountId());
        assertEquals("alice", dto.getUsername());
        assertEquals("alice@example.com", dto.getEmail());
        assertEquals("Admin", dto.getRole());
        assertEquals(date, dto.getCreatedDate());
    }

    @Test
    void settersShouldUpdateFields() {
        AccountSummary dto = new AccountSummary(
            1L, "u", "e@x.com", "User", LocalDate.of(2020, 1, 1)
        );

        dto.setAccountId(42L);
        dto.setUsername("bob");
        dto.setEmail("bob@example.com");
        dto.setRole("User");
        LocalDate newDate = LocalDate.of(2030, 12, 31);
        dto.setCreatedDate(newDate);

        assertEquals(42L, dto.getAccountId());
        assertEquals("bob", dto.getUsername());
        assertEquals("bob@example.com", dto.getEmail());
        assertEquals("User", dto.getRole());
        assertEquals(newDate, dto.getCreatedDate());
    }
}
