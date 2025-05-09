package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.entities.SupportBanner;
import uk.gov.hmcts.reform.demo.repositories.SupportBannerRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SupportBannerControllerTest {

    @InjectMocks
    private SupportBannerController controller;

    @Mock
    private SupportBannerRepository repository;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getAllBanners_returnsListFromRepository() {
        SupportBanner b1 = new SupportBanner();
        b1.setId(1L);
        b1.setTitle("T1");
        b1.setContent("C1");
        SupportBanner b2 = new SupportBanner();
        b2.setId(2L);
        b2.setTitle("T2");
        b2.setContent("C2");

        when(repository.findAll()).thenReturn(List.of(b1, b2));

        ResponseEntity<List<SupportBanner>> resp = controller.getAllBanners();
        assertEquals(200, resp.getStatusCodeValue());
        List<SupportBanner> body = resp.getBody();
        assertNotNull(body);
        assertEquals(2, body.size());
        assertSame(b1, body.get(0));
        assertSame(b2, body.get(1));

        verify(repository).findAll();
    }

    @Test
    void getBannerById_whenFound_returnsOkWithBanner() {
        SupportBanner b = new SupportBanner();
        b.setId(5L);
        b.setTitle("Hello");
        b.setContent("World");

        when(repository.findById(5L)).thenReturn(Optional.of(b));

        ResponseEntity<?> resp = controller.getBannerById(5L);
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(b, resp.getBody());

        verify(repository).findById(5L);
    }

    @Test
    void getBannerById_whenNotFound_returns404() {
        when(repository.findById(7L)).thenReturn(Optional.empty());

        ResponseEntity<?> resp = controller.getBannerById(7L);
        assertEquals(404, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(repository).findById(7L);
    }

    @Test
    void createBanner_savesAndReturnsSavedEntity() {
        SupportBanner toCreate = new SupportBanner();
        toCreate.setTitle("New");
        toCreate.setContent("Banner");

        SupportBanner saved = new SupportBanner();
        saved.setId(10L);
        saved.setTitle("New");
        saved.setContent("Banner");

        when(repository.save(toCreate)).thenReturn(saved);

        ResponseEntity<SupportBanner> resp = controller.createBanner(toCreate);
        assertEquals(200, resp.getStatusCodeValue());
        assertSame(saved, resp.getBody());

        verify(repository).save(toCreate);
    }

    @Test
    void updateBanner_whenExists_updatesFieldsAndReturnsOk() {
        SupportBanner existing = new SupportBanner();
        existing.setId(3L);
        existing.setTitle("Old");
        existing.setContent("OldC");

        SupportBanner updates = new SupportBanner();
        updates.setTitle("NewT");
        updates.setContent("NewC");

        when(repository.findById(3L)).thenReturn(Optional.of(existing));
        when(repository.save(existing)).thenReturn(existing);

        ResponseEntity<SupportBanner> resp = controller.updateBanner(3L, updates);
        assertEquals(200, resp.getStatusCodeValue());
        SupportBanner body = resp.getBody();
        assertNotNull(body);
        assertEquals(3L, body.getId());
        assertEquals("NewT", body.getTitle());
        assertEquals("NewC", body.getContent());

        verify(repository).findById(3L);
        verify(repository).save(existing);
    }

    @Test
    void updateBanner_whenNotFound_returns404() {
        SupportBanner updates = new SupportBanner();
        updates.setTitle("Whatever");
        updates.setContent("Whatever");

        when(repository.findById(4L)).thenReturn(Optional.empty());

        ResponseEntity<SupportBanner> resp = controller.updateBanner(4L, updates);
        assertEquals(404, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(repository).findById(4L);
        verify(repository, never()).save(any());
    }

    @Test
    void deleteBanner_whenExists_deletesAndReturnsNoContent() {
        SupportBanner b = new SupportBanner();
        b.setId(8L);

        when(repository.findById(8L)).thenReturn(Optional.of(b));
        // no need to stub deleteById

        ResponseEntity<Void> resp = controller.deleteBanner(8L);
        assertEquals(204, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(repository).findById(8L);
        verify(repository).deleteById(8L);
    }

    @Test
    void deleteBanner_whenNotFound_returns404() {
        when(repository.findById(9L)).thenReturn(Optional.empty());

        ResponseEntity<Void> resp = controller.deleteBanner(9L);
        assertEquals(404, resp.getStatusCodeValue());
        assertNull(resp.getBody());

        verify(repository).findById(9L);
        verify(repository, never()).deleteById(anyLong());
    }
}
