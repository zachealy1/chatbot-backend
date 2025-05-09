package uk.gov.hmcts.reform.demo.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import uk.gov.hmcts.reform.demo.filters.RequestDumpFilter;

import static org.junit.jupiter.api.Assertions.*;

class RequestLoggingConfigTest {

    @Test
    void requestLoggingFilterBeanShouldBeConfiguredCorrectly() {
        RequestLoggingConfig config = new RequestLoggingConfig();
        CommonsRequestLoggingFilter filter = config.requestLoggingFilter();
        assertNotNull(filter, "requestLoggingFilter bean should not be null");
    }

    @Test
    void loggingFilterRegistrationBeanShouldBeConfiguredCorrectly() {
        RequestLoggingConfig config = new RequestLoggingConfig();
        FilterRegistrationBean<RequestDumpFilter> registrationBean = config.loggingFilter();
        assertNotNull(registrationBean, "FilterRegistrationBean should not be null");
        RequestDumpFilter filter = registrationBean.getFilter();
        assertNotNull(filter, "Registered filter should not be null");
        assertTrue(filter instanceof RequestDumpFilter, "Filter should be instance of RequestDumpFilter");

        assertNotNull(registrationBean.getUrlPatterns(), "URL patterns should not be null");
        assertEquals(1, registrationBean.getUrlPatterns().size(), "There should be one URL pattern");
        assertTrue(registrationBean.getUrlPatterns().contains("/*"), "URL patterns should contain '/*'");

        assertEquals(1, registrationBean.getOrder(), "Filter order should be 1");
    }
}
