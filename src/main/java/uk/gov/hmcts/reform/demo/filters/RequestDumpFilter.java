package uk.gov.hmcts.reform.demo.filters;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.util.ContentCachingRequestWrapper;

public class RequestDumpFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestDumpFilter.class);

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain)
        throws ServletException, IOException {

        // Wrap the request to cache its content
        ContentCachingRequestWrapper wrappedRequest = new ContentCachingRequestWrapper(request);

        // Proceed with the filter chain first.
        filterChain.doFilter(wrappedRequest, response);

        // Now log request details.
        StringBuilder sb = new StringBuilder();
        sb.append("REQUEST DATA: ").append(wrappedRequest.getMethod())
            .append(" ").append(wrappedRequest.getRequestURI());

        if (wrappedRequest.getQueryString() != null) {
            sb.append("?").append(wrappedRequest.getQueryString());
        }
        sb.append("\nHeaders:\n");
        wrappedRequest.getHeaderNames().asIterator().forEachRemaining(headerName -> {
            sb.append(headerName).append(": ").append(wrappedRequest.getHeader(headerName)).append("\n");
        });

        // Attempt to retrieve the CSRF token from request attributes
        CsrfToken csrfToken = (CsrfToken) wrappedRequest.getAttribute(CsrfToken.class.getName());
        if (csrfToken != null) {
            sb.append("CSRF Token: ").append(csrfToken.getToken()).append("\n");
        } else {
            sb.append("CSRF Token: not found in request attributes\n");
        }

        // Get cached payload
        byte[] buf = wrappedRequest.getContentAsByteArray();
        if (buf.length > 0) {
            String payload = new String(buf, wrappedRequest.getCharacterEncoding());
            sb.append("Payload: ").append(payload);
        }

        logger.info(sb.toString());
    }
}
