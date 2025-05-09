package uk.gov.hmcts.reform.demo.config;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.web.servlet.config.annotation.CorsRegistration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

class CorsConfigTest {

    @Test
    void corsConfigurerBeanShouldNotBeNull() {
        CorsConfig config = new CorsConfig();
        WebMvcConfigurer corsConfigurer = config.corsConfigurer();
        assertNotNull(corsConfigurer, "corsConfigurer bean should not be null");
    }

    @Test
    void corsMappingsShouldBeConfiguredCorrectly() {
        CorsConfig config = new CorsConfig();
        WebMvcConfigurer corsConfigurer = config.corsConfigurer();

        // Create a mock CorsRegistry and spy CorsRegistration
        CorsRegistry registry = mock(CorsRegistry.class);
        CorsRegistration registration = spy(new CorsRegistration("/**"));
        when(registry.addMapping("/**")).thenReturn(registration);

        // Apply CORS configuration
        corsConfigurer.addCorsMappings(registry);

        // Verify mapping and settings
        verify(registry).addMapping("/**");
        verify(registration).allowedOrigins("http://localhost:3100");
        verify(registration).allowedMethods("GET", "POST", "PUT", "DELETE");
        verify(registration).allowCredentials(true);
    }

}
