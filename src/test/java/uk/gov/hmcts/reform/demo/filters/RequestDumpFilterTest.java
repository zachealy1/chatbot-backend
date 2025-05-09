package uk.gov.hmcts.reform.demo.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.web.util.ContentCachingRequestWrapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class RequestDumpFilterTest {

    private RequestDumpFilter filter;
    private MockHttpServletRequest originalRequest;
    private MockHttpServletResponse response;

    @BeforeEach
    void setUp() {
        filter = new RequestDumpFilter();
        originalRequest = new MockHttpServletRequest();
        response = new MockHttpServletResponse();
    }

    @Test
    void doFilterInternal_wrapsRequestAndInvokesChain() throws ServletException, IOException {
        // Arrange
        originalRequest.setMethod("POST");
        originalRequest.setRequestURI("/test/path");
        originalRequest.setQueryString("foo=bar");
        originalRequest.addHeader("X-Test", "value");
        byte[] payload = "hello".getBytes(StandardCharsets.UTF_8);
        originalRequest.setContent(payload);

        AtomicReference<ContentCachingRequestWrapper> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            // Simulate downstream reading so ContentCachingRequestWrapper caches the body
            ContentCachingRequestWrapper wrapper = (ContentCachingRequestWrapper) req;
            wrapper.getInputStream().readAllBytes();
            captured.set(wrapper);
        };

        // Act
        filter.doFilterInternal(originalRequest, response, chain);

        // Assert
        ContentCachingRequestWrapper wrapper = captured.get();
        assertNotNull(wrapper, "FilterChain should have been invoked with a wrapped request");
        assertTrue(wrapper instanceof ContentCachingRequestWrapper,
                   "Request should be wrapped in a ContentCachingRequestWrapper");

        // Method, URI, query string and header preserved
        assertEquals("POST", wrapper.getMethod());
        assertEquals("/test/path", wrapper.getRequestURI());
        assertEquals("foo=bar", wrapper.getQueryString());
        assertEquals("value", wrapper.getHeader("X-Test"));

        // Payload should now be cached after reading
        byte[] cached = wrapper.getContentAsByteArray();
        assertArrayEquals(payload, cached, "Payload should be cached in the wrapper");
    }

    @Test
    void doFilterInternal_includesCsrfTokenAttribute() throws ServletException, IOException {
        // Arrange: store a CSRF token attribute on the original request
        DefaultCsrfToken csrf = new DefaultCsrfToken("headerName", "paramName", "the-token");
        originalRequest.setAttribute(DefaultCsrfToken.class.getName(), csrf);

        AtomicReference<ContentCachingRequestWrapper> captured = new AtomicReference<>();
        FilterChain chain = (req, res) -> {
            // capture the wrapper
            captured.set((ContentCachingRequestWrapper) req);
            // inside the chain, attribute should be accessible
            Object attr = req.getAttribute(DefaultCsrfToken.class.getName());
            assertSame(csrf, attr, "CSRF token attribute should be propagated to the wrapper");
        };

        // Act
        filter.doFilterInternal(originalRequest, response, chain);

        // Assert: after chain completes, the wrapper still holds the attribute
        ContentCachingRequestWrapper wrapper = captured.get();
        assertNotNull(wrapper, "FilterChain should have been invoked");
        Object afterAttr = wrapper.getAttribute(DefaultCsrfToken.class.getName());
        assertSame(csrf, afterAttr, "CSRF token attribute should still be present after filter");
    }
}
