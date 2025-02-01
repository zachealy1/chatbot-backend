package uk.gov.hmcts.reform.demo.controllers;

import java.time.LocalDateTime;
import java.util.Comparator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.hmcts.reform.demo.entities.Chat;
import uk.gov.hmcts.reform.demo.entities.User;
import uk.gov.hmcts.reform.demo.repositories.ChatRepository;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import java.time.LocalDate;

import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statistics")
public class StatisticsController {

    private final UserRepository userRepository;
    private final ChatRepository chatRepository;

    public StatisticsController(UserRepository userRepository, ChatRepository chatRepository) {
        this.userRepository = userRepository;
        this.chatRepository = chatRepository;
    }

    /**
     * Retrieves user activity across age groups.
     */
    @GetMapping("/user-activity")
    public ResponseEntity<Map<String, Long>> getUserActivityStats() {
        List<User> users = userRepository.findAll();
        Map<String, Long> ageGroups = calculateUserAgeGroups(users);
        return ResponseEntity.ok(ageGroups);
    }

    /**
     * Retrieves the most popular chat categories and interaction percentages by age group.
     */
    @GetMapping("/chat-category-breakdown")
    public ResponseEntity<Map<String, Map<String, Double>>> getChatCategoryBreakdown() {
        List<User> users = userRepository.findAll();
        List<Chat> chats = chatRepository.findAll();

        // Group users by age range
        Map<String, Long> userAgeGroups = calculateUserAgeGroups(users);

        // Get the most popular chat categories
        Map<String, Long> categoryCounts = getPopularChatCategories(chats);

        // Calculate percentage of users in each age range interacting with each category
        Map<String, Map<String, Double>> breakdown = calculateChatCategoryBreakdown(
            users,
            chats,
            userAgeGroups,
            categoryCounts
        );

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
