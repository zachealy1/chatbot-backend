package uk.gov.hmcts.reform.demo.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;

import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.builders.AuthenticationManagerBuilder;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import uk.gov.hmcts.reform.demo.services.AccountUserDetailsService;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final AccountUserDetailsService userDetailsService;

    @Autowired
    public SecurityConfig(AccountUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            // Ignore CSRF for registration and login endpoints
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/account/register", "/login/chat")
            )
            // Configure authorization rules
            .authorizeHttpRequests(authorize -> authorize
                .requestMatchers(
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/login/chat",
                    "/account/register"
                ).permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(exception -> exception
                .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            )
            // Enable form login with default settings
            .formLogin(Customizer.withDefaults())
            // Enable logout with default settings
            .logout(Customizer.withDefaults());

        return http.build();
    }


    @Bean
    public AuthenticationManager authenticationManager(HttpSecurity http) throws Exception {
        AuthenticationManagerBuilder authManagerBuilder =
            http.getSharedObject(AuthenticationManagerBuilder.class);
        authManagerBuilder
            .userDetailsService(userDetailsService)
            .passwordEncoder(passwordEncoder());
        return authManagerBuilder.build();
    }

    /**
     * Defines the PasswordEncoder bean using BCrypt hashing algorithm.
     *
     * @return An instance of BCryptPasswordEncoder.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
