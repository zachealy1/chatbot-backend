package uk.gov.hmcts.reform.demo.config;

import io.swagger.v3.oas.models.ExternalDocumentation;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OpenAPIConfigurationTest {

    @Test
    void openAPIBeanShouldNotBeNull() {
        OpenAPIConfiguration config = new OpenAPIConfiguration();
        OpenAPI openAPI = config.openAPI();
        assertNotNull(openAPI, "openAPI bean should not be null");
    }

    @Test
    void infoShouldBeConfiguredCorrectly() {
        OpenAPIConfiguration config = new OpenAPIConfiguration();
        OpenAPI openAPI = config.openAPI();

        Info info = openAPI.getInfo();
        assertNotNull(info, "Info should not be null");
        assertEquals("rpe demo", info.getTitle());
        assertEquals("rpe demo", info.getDescription());
        assertEquals("v0.0.1", info.getVersion());

        License license = info.getLicense();
        assertNotNull(license, "License should not be null");
        assertEquals("MIT", license.getName());
        assertEquals("https://opensource.org/licenses/MIT", license.getUrl());
    }

    @Test
    void externalDocsShouldBeConfiguredCorrectly() {
        OpenAPIConfiguration config = new OpenAPIConfiguration();
        OpenAPI openAPI = config.openAPI();

        ExternalDocumentation externalDocs = openAPI.getExternalDocs();
        assertNotNull(externalDocs, "ExternalDocumentation should not be null");
        assertEquals("README", externalDocs.getDescription());
        assertEquals("https://github.com/hmcts/spring-boot-template", externalDocs.getUrl());
    }
}
