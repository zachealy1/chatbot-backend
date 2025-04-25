package uk.gov.hmcts.reform.demo.dto;

import java.time.LocalDate;

public class PendingRequestSummary {

    Long requestId;
    String userName;
    String email;
    String status;
    LocalDate submittedDate;

    public PendingRequestSummary(Long requestId, String userName, String email,
                                 String status, LocalDate submittedDate) {
        this.requestId = requestId;
        this.userName = userName;
        this.email = email;
        this.status = status;
        this.submittedDate = submittedDate;
    }

    public Long getRequestId() {
        return requestId;
    }

    public void setRequestId(Long requestId) {
        this.requestId = requestId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getSubmittedDate() {
        return submittedDate;
    }

    public void setSubmittedDate(LocalDate submittedDate) {
        this.submittedDate = submittedDate;
    }
}
