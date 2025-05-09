package uk.gov.hmcts.reform.demo;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.boot.SpringApplication;

class ApplicationTest {

    @Test
    void main_invokesSpringApplicationRun() {
        try (MockedStatic<SpringApplication> springApp = Mockito.mockStatic(SpringApplication.class)) {
            // Act
            Application.main(new String[] { "arg1", "arg2" });

            // Assert: SpringApplication.run(Application.class, args) was called
            springApp.verify(() ->
                                 SpringApplication.run(Application.class, new String[] { "arg1", "arg2" }),
                             Mockito.times(1)
            );
        }
    }

    @Test
    void main_withNoArgs_invokesSpringApplicationRunWithEmptyArray() {
        try (MockedStatic<SpringApplication> springApp = Mockito.mockStatic(SpringApplication.class)) {
            // Act
            Application.main(new String[0]);

            // Assert: SpringApplication.run(Application.class, new String[0]) was called
            springApp.verify(() ->
                                 SpringApplication.run(Application.class, new String[0]),
                             Mockito.times(1)
            );
        }
    }
}
