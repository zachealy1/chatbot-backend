package uk.gov.hmcts.reform.demo.controllers;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.dto.SessionActivity;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.ChatRepository;
import uk.gov.hmcts.reform.demo.repositories.SessionRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final SessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public StatisticsController(SessionRepository sessionRepository,
                                UserRepository userRepository, ChatRepository chatRepository) {
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    /**
     * Retrieves user activity across age groups.
     */
    @GetMapping("/user-activity")
    public ResponseEntity<List<SessionActivity>> getUserActivity() {
        List<SessionActivity> activities = sessionRepository.findAll().stream()
            .map(session -> {
                // find the user linked to this session
                //    (assumes Session has a getUserId() or similar)
                var userOpt = userRepository.findById(session.getUser().getId());
                if (userOpt.isEmpty()) {
                    // skip orphaned sessions
                    return null;
                }
                User user = userOpt.get();

                // compute their age
                int age = Period.between(user.getDateOfBirth(), LocalDate.now()).getYears();

                // bucket into a group
                String bucket;
                if (age <= 30) {
                    bucket = "20 to 30";
                } else if (age <= 40) {
                    bucket = "31 to 40";
                } else if (age <= 50) {
                    bucket = "41 to 50";
                } else {
                    bucket = "51 and over";
                }

                // 4) build the DTO
                return new SessionActivity(session.getCreatedAt(), bucket);
            })
            .filter(Objects::nonNull)
            .collect(Collectors.toList());

        return ResponseEntity.ok(activities);
    }

    /**
     * Retrieves the most popular chat categories and interaction percentages by age group.
     */
    @GetMapping("/chat-category-breakdown")
    public ResponseEntity<Map<String, Map<String, Double>>> getChatCategoryBreakdown() {
        List<User> users = userRepository.findAll();
        List<Chat> chats = chatRepository.findAll();
        LocalDate today = LocalDate.now();

        // build overall user‐buckets
        Map<String, Long> userAgeGroups = calculateUserAgeGroups(users);

        // count chats per category
        Map<String, Long> categoryCounts = getPopularChatCategories(chats);

        // take top 6
        List<String> topCats = categoryCounts.entrySet().stream()
            .sorted(Map.Entry.<String, Long>comparingByValue(Comparator.reverseOrder()))
            .limit(6)
            .map(Map.Entry::getKey)
            .toList();

        Map<String, Map<String, Double>> breakdown = new LinkedHashMap<>();

        for (String cat : topCats) {
            // first find the set of distinct user-IDs who chatted in this category
            Set<Long> interactedUserIds = chats.stream()
                .filter(c -> c.getDescription().equals(cat))
                .map(c -> c.getUser().getId())
                .collect(Collectors.toSet());

            double totalInteracted = interactedUserIds.size();
            Map<String, Double> pctByBucket = new LinkedHashMap<>();

            for (String bucket : userAgeGroups.keySet()) {
                // count how many of those interacted users fall in this bucket
                long inBucket = users.stream()
                    .filter(u -> interactedUserIds.contains(u.getId()))
                    .filter(u -> isUserInAgeGroup(u.getDateOfBirth(), today, bucket))
                    .count();

                double pct = totalInteracted > 0
                    ? (inBucket / totalInteracted) * 100.0
                    : 0.0;

                pctByBucket.put(bucket, pct);
            }

            breakdown.put(cat, pctByBucket);
        }

        return ResponseEntity.ok(breakdown);
    }

    /**
     * Retrieves a list of the most popular chat categories.
     */
    @GetMapping("/popular-chat-categories")
    public ResponseEntity<List<Map<String, Object>>> getPopularChatCategories() {
        List<Chat> chats = chatRepository.findAll();

        // Group chats by description (category) and collect additional stats
        Map<String, List<Chat>> groupedChats = chats.stream()
            .collect(Collectors.groupingBy(Chat::getDescription));

        // Process categories and sort them by popularity
        List<Map<String, Object>> sortedCategories = groupedChats.entrySet().stream()
            .map(entry -> {
                String category = entry.getKey();
                List<Chat> categoryChats = entry.getValue();

                // Extract statistics
                long queryCount = categoryChats.size();
                LocalDateTime firstQuery = categoryChats.stream()
                    .min(Comparator.comparing(Chat::getCreatedAt))
                    .map(Chat::getCreatedAt)
                    .orElse(null);
                LocalDateTime lastQuery = categoryChats.stream()
                    .max(Comparator.comparing(Chat::getCreatedAt))
                    .map(Chat::getCreatedAt)
                    .orElse(null);

                // Create category statistics
                Map<String, Object> categoryStats = new HashMap<>();
                categoryStats.put("name", category);
                categoryStats.put("queries", queryCount);
                categoryStats.put("firstQuery", firstQuery);
                categoryStats.put("lastQuery", lastQuery);
                return categoryStats;
            })
            .sorted((a, b) -> Long.compare((long) b.get("queries"), (long) a.get("queries")))
            .collect(Collectors.toList());

        // Assign order ranking
        for (int i = 0; i < sortedCategories.size(); i++) {
            sortedCategories.get(i).put("order", i + 1);
        }

        return ResponseEntity.ok(sortedCategories);
    }


    /**
     * Helper method to retrieve popular chat categories from the database.
     */
    private Map<String, Long> getPopularChatCategories(List<Chat> chats) {
        return chats.stream().collect(Collectors.groupingBy(Chat::getDescription, Collectors.counting()));
    }

    /**
     * Helper method to calculate user activity across age groups.
     */
    private Map<String, Long> calculateUserAgeGroups(List<User> users) {
        LocalDate today = LocalDate.now();
        Map<String, Long> ageGroups = new HashMap<>();

        ageGroups.put(
            "20-30",
            users.stream().filter(user -> isAgeBetween(user.getDateOfBirth(), today, 20, 30)).count()
        );
        ageGroups.put(
            "31-40",
            users.stream().filter(user -> isAgeBetween(user.getDateOfBirth(), today, 31, 40)).count()
        );
        ageGroups.put(
            "41-50",
            users.stream().filter(user -> isAgeBetween(user.getDateOfBirth(), today, 41, 50)).count()
        );
        ageGroups.put(
            "51+",
            users.stream().filter(user -> isAgeBetween(user.getDateOfBirth(), today, 51, 200)).count()
        );

        return ageGroups;
    }

    /**
     * Helper method to check if a user's age falls within a given range.
     */
    private boolean isAgeBetween(LocalDate birthDate, LocalDate today, int minAge, int maxAge) {
        int age = today.getYear() - birthDate.getYear();
        return age >= minAge && age <= maxAge;
    }

    /**
     * Helper method to calculate chat category breakdown by age group.
     */
    private Map<String, Map<String, Double>> calculateChatCategoryBreakdown(List<User> users, List<Chat> chats,
                                                                            Map<String, Long> userAgeGroups,
                                                                            Map<String, Long> categoryCounts) {
        Map<String, Map<String, Double>> breakdown = new HashMap<>();
        LocalDate today = LocalDate.now();

        for (String category : categoryCounts.keySet()) {
            Map<String, Double> ageGroupPercentages = new HashMap<>();

            for (String ageGroup : userAgeGroups.keySet()) {
                long totalUsersInAgeGroup = userAgeGroups.get(ageGroup);
                long usersInAgeGroupInteractedWithCategory = users.stream()
                    .filter(user -> userHasInteractedWithCategory(
                        user,
                        chats,
                        category
                    ) && isUserInAgeGroup(
                        user.getDateOfBirth(),
                        today,
                        ageGroup
                    )).count();

                double percentage = totalUsersInAgeGroup > 0
                    ? (double) usersInAgeGroupInteractedWithCategory / totalUsersInAgeGroup * 100 : 0;
                ageGroupPercentages.put(ageGroup, percentage);
            }

            breakdown.put(category, ageGroupPercentages);
        }

        return breakdown;
    }

    /**
     * Helper method to check if a user has interacted with a chat category.
     */
    private boolean userHasInteractedWithCategory(User user, List<Chat> chats, String category) {
        return chats.stream()
            .anyMatch(chat -> chat.getUser().getId().equals(user.getId()) && chat.getDescription().equals(category));
    }

    /**
     * Helper method to check if a user belongs to a specific age group.
     */
    private boolean isUserInAgeGroup(LocalDate birthDate, LocalDate today, String ageGroup) {
        int age = today.getYear() - birthDate.getYear();

        switch (ageGroup) {
            case "20-30":
                return age >= 20 && age <= 30;
            case "31-40":
                return age >= 31 && age <= 40;
            case "41-50":
                return age >= 41 && age <= 50;
            case "51+":
                return age >= 51;
            default:
                return false;
        }
    }
}
