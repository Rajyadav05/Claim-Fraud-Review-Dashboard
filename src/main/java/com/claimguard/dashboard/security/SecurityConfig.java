package com.claimguard.dashboard.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                .ignoringRequestMatchers(
                    new AntPathRequestMatcher("/h2-console/**")
                )
            )
            .headers(headers -> headers
                .frameOptions(frame -> frame.sameOrigin())
            )
            .authorizeHttpRequests(auth -> auth
                // Static resources & Login UI
                .requestMatchers(
                    "/login.html",
                    "/css/**",
                    "/js/**",
                    "/assets/**",
                    "/favicon.ico"
                ).permitAll()
                // Health probes
                .requestMatchers(
                    "/api/health",
                    "/actuator/health",
                    "/actuator/info"
                ).permitAll()
                // Auth management endpoints
                .requestMatchers(
                    "/api/auth/**"
                ).permitAll()
                // Administrative functions
                .requestMatchers(
                    "/api/admin/**"
                ).hasRole("ADMIN")
                // Core claims operations
                .requestMatchers(
                    "/api/claims/**"
                ).hasAnyRole("ADMIN", "REVIEWER")
                // Protected application HTML pages
                .requestMatchers(
                    "/",
                    "/index.html",
                    "/claims.html",
                    "/submit.html",
                    "/review.html"
                ).hasAnyRole("ADMIN", "REVIEWER")
                // All other requests require authentication
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login.html")
                .loginProcessingUrl("/api/auth/login")
                .successHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "authenticated");
                    data.put("username", authentication.getName());
                    data.put("authorities", authentication.getAuthorities().stream().map(Object::toString).toList());
                    data.put("redirectUrl", "/index.html");

                    objectMapper.writeValue(response.getOutputStream(), data);
                })
                .failureHandler((request, response, exception) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", HttpStatus.UNAUTHORIZED.value());
                    data.put("title", "Authentication Failed");
                    data.put("detail", "Invalid username or password.");
                    data.put("timestamp", LocalDateTime.now().toString());

                    objectMapper.writeValue(response.getOutputStream(), data);
                })
                .permitAll()
            )
            .logout(logout -> logout
                .logoutUrl("/api/auth/logout")
                .logoutSuccessHandler((request, response, authentication) -> {
                    response.setStatus(HttpServletResponse.SC_OK);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", "logged_out");
                    data.put("message", "Session successfully invalidated.");

                    objectMapper.writeValue(response.getOutputStream(), data);
                })
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
                .permitAll()
            )
            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    String acceptHeader = request.getHeader("Accept");
                    String requestUri = request.getRequestURI();

                    // If request is for an API endpoint or expects JSON, return 401 JSON
                    if (requestUri.startsWith("/api/") || (acceptHeader != null && acceptHeader.contains(MediaType.APPLICATION_JSON_VALUE))) {
                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                        Map<String, Object> data = new HashMap<>();
                        data.put("status", HttpStatus.UNAUTHORIZED.value());
                        data.put("title", "Unauthorized");
                        data.put("detail", "Full authentication is required to access this resource.");
                        data.put("timestamp", LocalDateTime.now().toString());

                        objectMapper.writeValue(response.getOutputStream(), data);
                    } else {
                        // For browser page navigation, redirect to login page with return target
                        String redirect = requestUri;
                        if (request.getQueryString() != null) {
                            redirect += "?" + request.getQueryString();
                        }
                        response.sendRedirect("/login.html?redirect=" + java.net.URLEncoder.encode(redirect, java.nio.charset.StandardCharsets.UTF_8));
                    }
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);

                    Map<String, Object> data = new HashMap<>();
                    data.put("status", HttpStatus.FORBIDDEN.value());
                    data.put("title", "Forbidden");
                    data.put("detail", "Access denied: insufficient permissions to perform this operation.");
                    data.put("timestamp", LocalDateTime.now().toString());

                    objectMapper.writeValue(response.getOutputStream(), data);
                })
            );

        return http.build();
    }
}
