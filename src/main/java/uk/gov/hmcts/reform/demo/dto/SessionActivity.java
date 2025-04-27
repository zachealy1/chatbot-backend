package uk.gov.hmcts.reform.demo.dto;

import java.time.LocalDateTime;

public class SessionActivity {

    LocalDateTime createdAt;
    String ageGroup;

    public SessionActivity(LocalDateTime createdAt, String ageGroup) {
        this.createdAt = createdAt;
        this.ageGroup = ageGroup;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getAgeGroup() {
        return ageGroup;
    }

    public void setAgeGroup(String ageGroup) {
        this.ageGroup = ageGroup;
    }
}
