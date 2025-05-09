package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.dto.SessionActivity;
import uk.gov.hmcts.reform.demo.entities.Session;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.ChatRepository;
import uk.gov.hmcts.reform.demo.repositories.SessionRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StatisticsControllerTest {

    @InjectMocks
    private StatisticsController controller;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ChatRepository chatRepository; // not used by this method

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void getUserActivity_noSessions_returnsEmptyList() {
        when(sessionRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<SessionActivity>> resp = controller.getUserActivity();

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
    }

    @Test
    void getUserActivity_orphanedSessions_areSkipped() {
        // one session whose user cannot be found
        User missing = new User();
        missing.setId(99L);
        Session sess = new Session("t", missing, LocalDateTime.now(), LocalDateTime.now());

        when(sessionRepository.findAll()).thenReturn(List.of(sess));
        when(userRepository.findById(99L)).thenReturn(Optional.empty());

        List<SessionActivity> activities = controller.getUserActivity().getBody();

        assertNotNull(activities);
        assertTrue(activities.isEmpty(), "Orphaned sessions should be skipped");
    }

    @Test
    void getUserActivity_variousAges_bucketedCorrectly() {
        LocalDate today = LocalDate.now();

        // User A: age 25 → "20 to 30"
        User u25 = new User();
        u25.setId(1L);
        u25.setDateOfBirth(today.minusYears(25));
        LocalDateTime now = LocalDateTime.now().truncatedTo(java.time.temporal.ChronoUnit.SECONDS);
        Session s25 = new Session("t1", u25, now, now.plusHours(1));

        // User B: age 35 → "31 to 40"
        User u35 = new User();
        u35.setId(2L);
        u35.setDateOfBirth(today.minusYears(35));
        LocalDateTime now2 = now.plusMinutes(5);
        Session s35 = new Session("t2", u35, now2, now2.plusHours(1));

        // User C: age 45 → "41 to 50"
        User u45 = new User();
        u45.setId(3L);
        u45.setDateOfBirth(today.minusYears(45));
        LocalDateTime now3 = now.plusMinutes(10);
        Session s45 = new Session("t3", u45, now3, now3.plusHours(1));

        // User D: age 55 → "51 and over"
        User u55 = new User();
        u55.setId(4L);
        u55.setDateOfBirth(today.minusYears(55));
        LocalDateTime now4 = now.plusMinutes(15);
        Session s55 = new Session("t4", u55, now4, now4.plusHours(1));

        when(sessionRepository.findAll()).thenReturn(List.of(s25, s35, s45, s55));
        when(userRepository.findById(1L)).thenReturn(Optional.of(u25));
        when(userRepository.findById(2L)).thenReturn(Optional.of(u35));
        when(userRepository.findById(3L)).thenReturn(Optional.of(u45));
        when(userRepository.findById(4L)).thenReturn(Optional.of(u55));

        List<SessionActivity> activities = controller.getUserActivity().getBody();

        assertNotNull(activities);
        assertEquals(4, activities.size());

        // Verify each in order
        assertEquals(now,   activities.get(0).getCreatedAt());
        assertEquals("20 to 30",   activities.get(0).getAgeGroup());

        assertEquals(now2,  activities.get(1).getCreatedAt());
        assertEquals("31 to 40",   activities.get(1).getAgeGroup());

        assertEquals(now3,  activities.get(2).getCreatedAt());
        assertEquals("41 to 50",   activities.get(2).getAgeGroup());

        assertEquals(now4,  activities.get(3).getCreatedAt());
        assertEquals("51 and over", activities.get(3).getAgeGroup());
    }
}
