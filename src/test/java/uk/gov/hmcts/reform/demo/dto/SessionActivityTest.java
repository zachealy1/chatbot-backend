package uk.gov.hmcts.reform.demo.dto;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SessionActivityTest {

    @Test
    void constructorAndGettersInitializeFields() {
        LocalDateTime timestamp = LocalDateTime.of(2025, 5, 9, 14, 30);
        String group = "20 to 30";

        SessionActivity activity = new SessionActivity(timestamp, group);

        assertEquals(timestamp, activity.getCreatedAt(), "Constructor should set createdAt");
        assertEquals(group, activity.getAgeGroup(),     "Constructor should set ageGroup");
    }

    @Test
    void settersUpdateFields() {
        SessionActivity activity = new SessionActivity(
            LocalDateTime.of(2025, 1, 1, 0, 0),
            "31 to 40"
        );

        LocalDateTime newTimestamp = LocalDateTime.of(2030, 12, 31, 23, 59);
        String newGroup = "41 to 50";

        activity.setCreatedAt(newTimestamp);
        activity.setAgeGroup(newGroup);

        assertEquals(newTimestamp, activity.getCreatedAt(), "Setter should update createdAt");
        assertEquals(newGroup,      activity.getAgeGroup(),    "Setter should update ageGroup");
    }

    @Test
    void canHandleNullValues() {
        SessionActivity activity = new SessionActivity(
            LocalDateTime.now(),
            "51 and over"
        );

        activity.setCreatedAt(null);
        activity.setAgeGroup(null);

        assertNull(activity.getCreatedAt(), "createdAt should be null after setting null");
        assertNull(activity.getAgeGroup(),   "ageGroup should be null after setting null");
    }
}
