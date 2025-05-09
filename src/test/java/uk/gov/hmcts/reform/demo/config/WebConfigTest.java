package uk.gov.hmcts.reform.demo.config;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class WebConfigTest {

    @Test
    void localeResolverBeanShouldBeCookieLocaleResolver() {
        WebConfig config = new WebConfig();
        LocaleResolver resolver = config.localeResolver();

        assertNotNull(resolver, "localeResolver() should not be null");
        assertTrue(resolver instanceof CookieLocaleResolver,
                   "Should return a CookieLocaleResolver");
    }

    @Test
    void localeChangeInterceptorBeanShouldUseLangParam() {
        WebConfig config = new WebConfig();
        LocaleChangeInterceptor interceptor = config.localeChangeInterceptor();

        assertNotNull(interceptor, "localeChangeInterceptor() should not be null");
        assertEquals("lang", interceptor.getParamName(),
                     "Param name must be 'lang'");
    }

    @Test
    void addInterceptorsShouldRegisterLocaleChangeInterceptor() {
        WebConfig config = new WebConfig();
        InterceptorRegistry registry = mock(InterceptorRegistry.class);

        // stub to avoid NPE; return value not used by our method
        when(registry.addInterceptor(any(HandlerInterceptor.class))).thenReturn(null);

        // call the override
        config.addInterceptors(registry);

        // capture the interceptor that was registered
        @SuppressWarnings("unchecked")
        ArgumentCaptor<LocaleChangeInterceptor> captor =
            ArgumentCaptor.forClass((Class) LocaleChangeInterceptor.class);

        verify(registry).addInterceptor(captor.capture());
        LocaleChangeInterceptor registered = captor.getValue();

        // ensure it's our bean and properly configured
        assertNotNull(registered, "Should have registered a LocaleChangeInterceptor");
        assertEquals("lang", registered.getParamName(),
                     "Registered interceptor must use 'lang' param");
    }
}
