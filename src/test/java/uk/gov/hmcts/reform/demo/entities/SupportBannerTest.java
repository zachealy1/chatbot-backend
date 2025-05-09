package uk.gov.hmcts.reform.demo.entities;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class SupportBannerTest {

    @Test
    void defaultConstructor_initializesTimestampsAndLeavesFieldsNull() {
        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SupportBanner banner = new SupportBanner();
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        // id/title/content should be null
        assertNull(banner.getId(), "ID should be null by default");
        assertNull(banner.getTitle(), "Title should be null by default");
        assertNull(banner.getContent(), "Content should be null by default");

        // createdAt and updatedAt should be initialized to now
        assertNotNull(banner.getCreatedAt(), "createdAt should be initialized");
        assertFalse(banner.getCreatedAt().isBefore(before),
                    "createdAt should not be before instantiation");
        assertFalse(banner.getCreatedAt().isAfter(after),
                    "createdAt should not be after now plus small delta");

        assertNotNull(banner.getUpdatedAt(), "updatedAt should be initialized");
        assertFalse(banner.getUpdatedAt().isBefore(before),
                    "updatedAt should not be before instantiation");
        assertFalse(banner.getUpdatedAt().isAfter(after),
                    "updatedAt should not be after now plus small delta");
    }

    @Test
    void parameterizedConstructor_setsTitleContentAndUpdatesUpdatedAt() {
        String title = "Support";
        String content = "We are here to help";

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        SupportBanner banner = new SupportBanner(title, content);
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals(title, banner.getTitle(), "Constructor should set title");
        assertEquals(content, banner.getContent(), "Constructor should set content");

        // createdAt remains default from field initializer
        assertNotNull(banner.getCreatedAt(), "createdAt should be non-null");
        // updatedAt should be reset in setter logic or constructor
        assertNotNull(banner.getUpdatedAt(), "updatedAt should be non-null");
        // updatedAt should be roughly now
        assertFalse(banner.getUpdatedAt().isBefore(before),
                    "updatedAt should not be before instantiation");
        assertFalse(banner.getUpdatedAt().isAfter(after),
                    "updatedAt should not be after now plus small delta");
    }

    @Test
    void setTitle_updatesTitleAndUpdatedAt() throws InterruptedException {
        SupportBanner banner = new SupportBanner("Old", "Content");
        LocalDateTime oldUpdatedAt = banner.getUpdatedAt();

        // Sleep briefly to ensure timestamp difference
        Thread.sleep(10);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        banner.setTitle("New Title");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals("New Title", banner.getTitle(), "Title should be updated");
        assertNotEquals(oldUpdatedAt, banner.getUpdatedAt(), "updatedAt should change on title set");
        assertFalse(banner.getUpdatedAt().isBefore(before),
                    "updatedAt should not be before setter call");
        assertFalse(banner.getUpdatedAt().isAfter(after),
                    "updatedAt should not be after now plus small delta");
    }

    @Test
    void setContent_updatesContentAndUpdatedAt() throws InterruptedException {
        SupportBanner banner = new SupportBanner("Title", "Old Content");
        LocalDateTime oldUpdatedAt = banner.getUpdatedAt();

        Thread.sleep(10);

        LocalDateTime before = LocalDateTime.now().minusSeconds(1);
        banner.setContent("New Content");
        LocalDateTime after = LocalDateTime.now().plusSeconds(1);

        assertEquals("New Content", banner.getContent(), "Content should be updated");
        assertNotEquals(oldUpdatedAt, banner.getUpdatedAt(), "updatedAt should change on content set");
        assertFalse(banner.getUpdatedAt().isBefore(before),
                    "updatedAt should not be before setter call");
        assertFalse(banner.getUpdatedAt().isAfter(after),
                    "updatedAt should not be after now plus small delta");
    }

    @Test
    void settersAndGetters_workForId() {
        SupportBanner banner = new SupportBanner();
        banner.setId(123L);
        assertEquals(123L, banner.getId(), "ID setter/getter should work");
    }
}
