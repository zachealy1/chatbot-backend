package uk.gov.hmcts.reform.demo.dto;

import java.time.LocalDate;

public class AccountSummary {

    private final Long accountId;
    private final String username;
    private final String email;
    private final String role;
    private final LocalDate createdDate;

    public AccountSummary(Long accountId,
                          String username,
                          String email,
                          String role,
                          LocalDate createdDate) {
        this.accountId = accountId;
        this.username = username;
        this.email = email;
        this.role = role;
        this.createdDate = createdDate;
    }

    public Long getAccountId() {
        return accountId;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getRole() {
        return role;
    }

    public LocalDate getCreatedDate() {
        return createdDate;
    }
}
