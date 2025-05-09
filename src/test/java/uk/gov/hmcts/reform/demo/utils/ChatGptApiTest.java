package uk.gov.hmcts.reform.demo.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ChatGptApiTest {

    private static final String DUMMY_KEY = "dummy-api-key";
    private static final String ASSISTANT_ID = "asst_F5Q8YV7e2ntIYd2SPjeHyFSP";

    @Spy
    @InjectMocks
    private ChatGptApi api;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // our spy needs the real constructor called
        api = spy(new ChatGptApi(DUMMY_KEY));
    }

    @Test
    void categorise_delegatesToChatGptWithAssistant() {
        // Arrange: spy the real API but stub out chatGptWithAssistant
        ChatGptApi api = spy(new ChatGptApi("dummy-key"));
        doReturn("my‐summary").when(api)
            .chatGptWithAssistant(
                eq(List.of(Map.of("role","user","content","Hello"))),
                eq("asst_F5Q8YV7e2ntIYd2SPjeHyFSP")
            );

        // Act
        String summary = api.categorise("Hello");

        // Assert
        assertEquals("my‐summary", summary);
        verify(api).chatGptWithAssistant(
            eq(List.of(Map.of("role","user","content","Hello"))),
            eq("asst_F5Q8YV7e2ntIYd2SPjeHyFSP")
        );
    }

    @Test
    void categorise_shouldDelegateToChatGptWithAssistant() {
        // arrange: stub chatGptWithAssistant
        doReturn("the‐summary")
            .when(api).chatGptWithAssistant(
                eq(List.of(Map.of("role","user","content","Hello"))),
                eq(ASSISTANT_ID)
            );

        // act
        String result = api.categorise("Hello");

        // assert
        assertEquals("the‐summary", result);
        verify(api).chatGptWithAssistant(
            eq(List.of(Map.of("role","user","content","Hello"))),
            eq(ASSISTANT_ID)
        );
    }

    @Test
    void chatGptWithAssistant_ioError_shouldWrapInRuntimeException() throws Exception {
        // arrange: stub createThread to throw an IOException
        doThrow(new IOException("fail"))
            .when(api)
            .createThread();              // note: using reflection to make private method accessible

        // act & assert
        RuntimeException ex = assertThrows(RuntimeException.class, () ->
            api.chatGptWithAssistant(List.of(), "anyId")
        );
        assertTrue(ex.getMessage().contains("Error in chatGptWithAssistant"));
    }
}
