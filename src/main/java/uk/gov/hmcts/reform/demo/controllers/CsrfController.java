package uk.gov.hmcts.reform.demo.controllers;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import java.util.Map;

@RestController
public class CsrfController {

    @GetMapping("/csrf")
    public Map<String, String> csrf(CsrfToken token) {
        // The CsrfToken object is automatically populated by Spring Security.
        return Map.of("csrfToken", token.getToken());
    }
}

