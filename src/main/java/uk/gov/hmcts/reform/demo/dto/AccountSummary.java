package uk.gov.hmcts.reform.demo.dto;

import java.time.LocalDate;

public class AccountSummary {

    private Long accountId;
    private String username;
    private String email;
    private String role;
    private LocalDate createdDate;

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

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setCreatedDate(LocalDate createdDate) {
        this.createdDate = createdDate;
    }
}
