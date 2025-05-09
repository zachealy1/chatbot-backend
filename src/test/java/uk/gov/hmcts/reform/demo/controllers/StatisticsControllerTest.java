package uk.gov.hmcts.reform.demo.controllers;

import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.ResponseEntity;
import uk.gov.hmcts.reform.demo.dto.SessionActivity;
import uk.gov.hmcts.reform.demo.entities.Chat;
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

    @Test
    void getChatCategoryBreakdown_calculatesPercentagesPerAgeBucket() {
        LocalDate today = LocalDate.now();

        // Create users in different age groups:
        User u25 = new User(); u25.setId(1L);
        u25.setDateOfBirth(today.minusYears(25));
        User u35 = new User(); u35.setId(2L);
        u35.setDateOfBirth(today.minusYears(35));
        User u45 = new User(); u45.setId(3L);
        u45.setDateOfBirth(today.minusYears(45));

        when(userRepository.findAll()).thenReturn(List.of(u25, u35, u45));

        // Create chats: two in "catA" by u25 & u35, one in "catB" by u45
        Chat c1 = new Chat(); c1.setDescription("catA"); c1.setUser(u25); c1.setCreatedAt(LocalDateTime.now());
        Chat c2 = new Chat(); c2.setDescription("catA"); c2.setUser(u35); c2.setCreatedAt(LocalDateTime.now().plusMinutes(1));
        Chat c3 = new Chat(); c3.setDescription("catB"); c3.setUser(u45); c3.setCreatedAt(LocalDateTime.now().plusMinutes(2));

        when(chatRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        // Act
        ResponseEntity<Map<String, Map<String, Double>>> resp = controller.getChatCategoryBreakdown();
        assertEquals(200, resp.getStatusCodeValue());
        Map<String, Map<String, Double>> breakdown = resp.getBody();
        assertNotNull(breakdown);

        // We expect two categories: "catA" then "catB"
        assertTrue(breakdown.containsKey("catA"));
        assertTrue(breakdown.containsKey("catB"));

        // For catA: 2 users interacted, one in 20-30 and one in 31-40 => each 50%, others 0%
        Map<String, Double> pctA = breakdown.get("catA");
        assertEquals(50.0, pctA.get("20-30"), 0.0001);
        assertEquals(50.0, pctA.get("31-40"), 0.0001);
        assertEquals(0.0, pctA.get("41-50"), 0.0001);
        assertEquals(0.0, pctA.get("51+"),   0.0001);

        // For catB: only u45 interacted => 100% in 41-50, others 0%
        Map<String, Double> pctB = breakdown.get("catB");
        assertEquals(0.0,  pctB.get("20-30"), 0.0001);
        assertEquals(0.0,  pctB.get("31-40"), 0.0001);
        assertEquals(100.0, pctB.get("41-50"), 0.0001);
        assertEquals(0.0,  pctB.get("51+"),   0.0001);

        // Verify repository calls
        verify(userRepository).findAll();
        verify(chatRepository).findAll();
    }

    @Test
    void noChats_returnsEmptyList() {
        when(chatRepository.findAll()).thenReturn(List.of());

        ResponseEntity<List<Map<String, Object>>> resp = controller.getPopularChatCategories();

        assertEquals(200, resp.getStatusCodeValue());
        assertNotNull(resp.getBody());
        assertTrue(resp.getBody().isEmpty());
        verify(chatRepository).findAll();
    }

    @Test
    void singleCategory_returnsOneEntryWithCorrectStats() {
        LocalDateTime t1 = LocalDateTime.of(2022,1,1,10,0);
        LocalDateTime t2 = LocalDateTime.of(2022,1,1,11,0);
        Chat c1 = new Chat(); c1.setDescription("catX"); c1.setCreatedAt(t2);
        Chat c2 = new Chat(); c2.setDescription("catX"); c2.setCreatedAt(t1);
        Chat c3 = new Chat(); c3.setDescription("catX"); c3.setCreatedAt(t2.plusHours(1));

        when(chatRepository.findAll()).thenReturn(List.of(c1, c2, c3));

        var resp = controller.getPopularChatCategories();
        assertEquals(200, resp.getStatusCodeValue());
        var list = resp.getBody();
        assertEquals(1, list.size());

        Map<String,Object> stats = list.get(0);
        assertEquals("catX", stats.get("name"));
        assertEquals(3L, ((Long)stats.get("queries")).longValue());
        assertEquals(t1, stats.get("firstQuery"));
        assertEquals(t2.plusHours(1), stats.get("lastQuery"));
        assertEquals(1, ((Integer)stats.get("order")).intValue());

        verify(chatRepository).findAll();
    }

    @Test
    void multipleCategories_sortedByCountAndOrderAssigned() {
        LocalDateTime now = LocalDateTime.now();
        // catA: 2 chats
        Chat a1 = new Chat(); a1.setDescription("catA"); a1.setCreatedAt(now.minusMinutes(2));
        Chat a2 = new Chat(); a2.setDescription("catA"); a2.setCreatedAt(now.minusMinutes(1));
        // catB: 3 chats
        Chat b1 = new Chat(); b1.setDescription("catB"); b1.setCreatedAt(now.minusMinutes(5));
        Chat b2 = new Chat(); b2.setDescription("catB"); b2.setCreatedAt(now.minusMinutes(4));
        Chat b3 = new Chat(); b3.setDescription("catB"); b3.setCreatedAt(now.minusMinutes(3));
        // catC: 1 chat
        Chat c1 = new Chat(); c1.setDescription("catC"); c1.setCreatedAt(now);

        when(chatRepository.findAll()).thenReturn(List.of(a1,a2,b1,b2,b3,c1));

        var resp = controller.getPopularChatCategories();
        assertEquals(200, resp.getStatusCodeValue());
        var list = resp.getBody();
        assertEquals(3, list.size());

        // Expect order: catB (3), catA (2), catC (1)
        Map<String,Object> top = list.get(0);
        assertEquals("catB", top.get("name"));
        assertEquals(3L, ((Long)top.get("queries")).longValue());
        assertEquals(1, ((Integer)top.get("order")).intValue());

        Map<String,Object> second = list.get(1);
        assertEquals("catA", second.get("name"));
        assertEquals(2L, ((Long)second.get("queries")).longValue());
        assertEquals(2, ((Integer)second.get("order")).intValue());

        Map<String,Object> third = list.get(2);
        assertEquals("catC", third.get("name"));
        assertEquals(1L, ((Long)third.get("queries")).longValue());
        assertEquals(3, ((Integer)third.get("order")).intValue());

        // Verify first/last timestamps are correct
        assertEquals(now.minusMinutes(5), top.get("firstQuery"));
        assertEquals(now.minusMinutes(3), top.get("lastQuery"));

        assertEquals(now.minusMinutes(2), second.get("firstQuery"));
        assertEquals(now.minusMinutes(1), second.get("lastQuery"));

        assertEquals(now, third.get("firstQuery"));
        assertEquals(now, third.get("lastQuery"));

        verify(chatRepository).findAll();
    }
}
