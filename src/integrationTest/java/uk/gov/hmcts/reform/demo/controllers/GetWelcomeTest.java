package uk.gov.hmcts.reform.demo.controllers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import uk.gov.hmcts.reform.demo.repositories.UserRepository;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class GetWelcomeTest {

    private MockMvc mockMvc;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private RootController rootController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockMvc = MockMvcBuilders.standaloneSetup(rootController).build();
    }

    @DisplayName("Should welcome upon root request with 200 response code")
    @Test
    void welcomeRootEndpoint() throws Exception {
        // Since the GET "/" endpoint doesn't interact with UserRepository,
        // there's no need to define specific behavior for the mock.
        // However, if your controller logic changes in the future to use the repository,
        // you can configure the mock accordingly.

        String expectedResponse = "Welcome to the chatbot backend service";

        var response = mockMvc.perform(get("/")
                                           .accept(MediaType.TEXT_PLAIN))
            .andExpect(status().isOk())
            .andReturn();

        assertThat(response.getResponse().getContentAsString())
            .isEqualTo(expectedResponse);
    }
}
