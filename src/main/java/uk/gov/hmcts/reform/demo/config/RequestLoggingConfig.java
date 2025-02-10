package uk.gov.hmcts.reform.demo.config;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.CommonsRequestLoggingFilter;
import uk.gov.hmcts.reform.demo.filters.RequestDumpFilter;

@Configuration
public class RequestLoggingConfig {

    @Bean
    public CommonsRequestLoggingFilter requestLoggingFilter() {
        CommonsRequestLoggingFilter loggingFilter = new CommonsRequestLoggingFilter();
        // Include client information such as remote address.
        loggingFilter.setIncludeClientInfo(true);
        // Include the query string in the logs.
        loggingFilter.setIncludeQueryString(true);
        // Include request headers.
        loggingFilter.setIncludeHeaders(true);
        // Include the request payload (body) for POST/PUT requests.
        loggingFilter.setIncludePayload(true);
        // Set a maximum length for the payload (adjust as needed).
        loggingFilter.setMaxPayloadLength(10000);
        loggingFilter.setAfterMessagePrefix("REQUEST DATA: ");
        return loggingFilter;
    }

    @Bean
    public FilterRegistrationBean<RequestDumpFilter> loggingFilter() {
        FilterRegistrationBean<RequestDumpFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new RequestDumpFilter());
        registrationBean.addUrlPatterns("/*");  // Log all requests.
        registrationBean.setOrder(1); // Order must be before the Spring Security filter chain.
        return registrationBean;
    }
}
