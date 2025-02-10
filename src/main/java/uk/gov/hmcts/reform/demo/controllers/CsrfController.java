package uk.gov.hmcts.reform.demo.controllers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class CsrfController {

    private static final Logger logger = LoggerFactory.getLogger(CsrfController.class);

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        logger.info("CSRF token from /csrf endpoint: {}", token.getToken());
        logger.info("Current authentication: {}", auth);
        return Map.of("csrfToken", token.getToken());
    }
}

