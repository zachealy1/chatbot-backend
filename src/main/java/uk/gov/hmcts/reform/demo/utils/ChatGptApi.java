package uk.gov.hmcts.reform.demo.utils;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Utility class for interacting with the OpenAI ChatGPT API and custom Assistants via Threads API.
 */
@Component
public class ChatGptApi {

    private static final Logger logger = LoggerFactory.getLogger(ChatGptApi.class);

    private final String apiKey;
    private final ObjectMapper objectMapper;

    public ChatGptApi(@Value("${openai.api.key}") String apiKey) {
        this.apiKey = apiKey;
        this.objectMapper = new ObjectMapper();
    }

    /**
     * Original summarize method (left untouched).
     */
    public String categorise(String message) {
        logger.info("Summarize called for message: {}", message);

        // Build a single‐message conversation for the Assistant
        List<Map<String, String>> conversation = List.of(
            Map.of("role", "user", "content", message)
        );

        // Your custom summarization assistant ID
        String assistantId = "asst_F5Q8YV7e2ntIYd2SPjeHyFSP";
        logger.debug("Using assistant {} to summarize", assistantId);

        // Delegate to the Threads-based method
        String summary = chatGptWithAssistant(conversation, assistantId);

        logger.info("Summary result: {}", summary);
        return summary;
    }

    public String chatGptWithAssistant(List<Map<String, String>> conversation, String assistantId) {
        logger.info(
            "chatGptWithAssistant called: conversation size = {}, assistantId = {}",
            conversation.size(),
            assistantId
        );
        try {
            logger.debug("Creating new thread for assistant {}", assistantId);
            String threadId = createThread();
            logger.debug("Thread created with id {}", threadId);

            logger.debug("Replaying {} messages into thread {}", conversation.size(), threadId);
            for (Map<String, String> msg : conversation) {
                String role = msg.get("role");
                String content = msg.get("content");
                logger.debug("Adding message to thread {}: role='{}', content='{}'", threadId, role, content);
                addMessage(threadId, role, content);
            }

            logger.debug("Starting run for thread {} with assistant {}", threadId, assistantId);
            String runId = runThread(threadId, assistantId);
            logger.debug("Run started with id {}", runId);

            logger.info("Waiting up to 60s for run {} to complete", runId);
            waitForRunCompletion(threadId, runId, Duration.ofSeconds(60));

            logger.info("Fetching assistant response for thread {}", threadId);
            String reply = fetchLastAssistantMessage(threadId);
            logger.debug("Received reply: {}", reply);
            return reply;
        } catch (IOException e) {
            logger.error("IOException in chatGptWithAssistant", e);
            throw new RuntimeException("Error in chatGptWithAssistant: " + e.getMessage(), e);
        }
    }

    // ------------------ Private Helper Methods ------------------

    private String createThread() throws IOException {
        HttpURLConnection conn = openApiConnection("https://api.openai.com/v1/threads");
        conn.setRequestProperty("OpenAI-Beta", "assistants=v2");
        sendJsonRequest(conn, "{}");
        String response = readResponse(conn);
        JsonNode root = objectMapper.readTree(response);
        return root.get("id").asText();
    }

    private void addMessage(String threadId, String role, String content) throws IOException {
        HttpURLConnection conn = openApiConnection(
            "https://api.openai.com/v1/threads/" + threadId + "/messages"
        );
        conn.setRequestProperty("OpenAI-Beta", "assistants=v2");
        String payload = objectMapper.writeValueAsString(Map.of("role", role, "content", content));
        sendJsonRequest(conn, payload);
        readResponse(conn);
    }

    private String runThread(String threadId, String assistantId) throws IOException {
        HttpURLConnection conn = openApiConnection(
            "https://api.openai.com/v1/threads/" + threadId + "/runs"
        );
        conn.setRequestProperty("OpenAI-Beta", "assistants=v2");
        String payload = objectMapper.writeValueAsString(Map.of("assistant_id", assistantId));
        sendJsonRequest(conn, payload);
        String response = readResponse(conn);
        JsonNode root = objectMapper.readTree(response);
        return root.get("id").asText();
    }

    private void waitForRunCompletion(String threadId, String runId, Duration timeout) throws IOException {
        logger.info(
            "Polling run completion: threadId='{}', runId='{}', timeout={}s",
            threadId,
            runId,
            timeout.getSeconds()
        );
        Instant start = Instant.now();

        while (true) {
            Duration elapsed = Duration.between(start, Instant.now());
            if (elapsed.compareTo(timeout) > 0) {
                String msg = String.format(
                    "Timeout after %d seconds waiting for run %s in thread %s",
                    elapsed.getSeconds(),
                    runId,
                    threadId
                );
                logger.error(msg);
                throw new RuntimeException(msg);
            }

            URL statusUrl = new URL("https://api.openai.com/v1/threads/" + threadId + "/runs/" + runId);
            logger.debug("Checking status at URL: {}", statusUrl);
            HttpURLConnection conn = (HttpURLConnection) statusUrl.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + apiKey);
            conn.setRequestProperty("OpenAI-Beta", "assistants=v2");

            String resp = readResponse(conn);
            JsonNode root = objectMapper.readTree(resp);
            String status = root.get("status").asText();
            logger.debug("Run {} status: {}", runId, status);

            if ("completed".equals(status)) {
                logger.info("Run {} succeeded after {}ms", runId, elapsed.toMillis());
                return;
            }

            if ("failed".equals(status)) {
                logger.error("Assistant run {} failed: {}", runId, resp);
                throw new RuntimeException("Assistant run failed: " + resp);
            }

            try {
                Thread.sleep(500);
            } catch (InterruptedException ie) {
                logger.warn("Polling interrupted for run {}", runId, ie);
                Thread.currentThread().interrupt();
                throw new RuntimeException("Interrupted while waiting for assistant run", ie);
            }
        }
    }

    private String fetchLastAssistantMessage(String threadId) throws IOException {
        String url = "https://api.openai.com/v1/threads/" + threadId + "/messages";
        HttpURLConnection conn = openApiConnection(url);
        conn.setRequestMethod("GET");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("OpenAI-Beta", "assistants=v2");

        int status = conn.getResponseCode();
        String response = readResponse(conn);
        logger.debug("Messages response for thread {}: {}", threadId, response);

        JsonNode data = objectMapper.readTree(response).path("data");
        JsonNode latestAssistantMsg = null;
        long latestTimestamp = Long.MIN_VALUE;

        // Find the assistant message with the highest `created_at`
        for (JsonNode msg : data) {
            if ("assistant".equals(msg.path("role").asText())) {
                long ts = msg.path("created_at").asLong(0L);
                if (ts > latestTimestamp) {
                    latestTimestamp = ts;
                    latestAssistantMsg = msg;
                }
            }
        }

        if (latestAssistantMsg == null) {
            throw new RuntimeException("No assistant response found in thread " + threadId);
        }

        // Extract and concatenate the `text.value` fields
        StringBuilder sb = new StringBuilder();
        for (JsonNode block : latestAssistantMsg.path("content")) {
            JsonNode valueNode = block.path("text").path("value");
            if (!valueNode.isMissingNode()) {
                sb.append(valueNode.asText());
            }
        }

        String result = sb.toString().trim();
        logger.debug("Extracted latest assistant content (ts={}): {}", latestTimestamp, result);
        return result;
    }


    private HttpURLConnection openApiConnection(String urlStr) throws IOException {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + apiKey);
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        return conn;
    }

    private void sendJsonRequest(HttpURLConnection connection, String json) throws IOException {
        try (OutputStream os = connection.getOutputStream()) {
            os.write(json.getBytes("utf-8"));
        }
    }

    private String readResponse(HttpURLConnection connection) throws IOException {
        int status = connection.getResponseCode();
        InputStream is = status >= 200 && status < 300
            ? connection.getInputStream()
            : connection.getErrorStream();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is, "utf-8"))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private String extractMessageFromJsonResponse(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode choices = root.path("choices");
        if (choices.isArray() && choices.size() > 0) {
            return choices.get(0).path("message").path("content").asText().trim();
        }
        throw new RuntimeException("Invalid response structure from ChatGPT API.");
    }

    private String extractErrorMessage(String response) throws IOException {
        JsonNode root = objectMapper.readTree(response);
        JsonNode err = root.path("error").path("message");
        return err.isMissingNode() ? "Unknown error" : err.asText().trim();
    }

    private String escapeJson(String text) {
        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\n", "\\n")
            .replace("\r", "\\r")
            .replace("\t", "\\t");
    }
}
