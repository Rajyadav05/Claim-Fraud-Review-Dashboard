package com.claimguard.dashboard.controller;

import com.claimguard.dashboard.entity.User;
import com.claimguard.dashboard.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserRepository userRepository;

    public AuthController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> getCurrentUser(Authentication authentication) {
        Map<String, Object> response = new HashMap<>();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            response.put("authenticated", false);
            return ResponseEntity.ok(response);
        }

        String username = authentication.getName();
        Optional<User> userOpt = userRepository.findByUsername(username);

        response.put("authenticated", true);
        response.put("username", username);
        response.put("fullName", userOpt.map(User::getFullName).orElse(username));
        response.put("role", userOpt.map(u -> u.getRole().name()).orElse("REVIEWER"));
        response.put("authorities", authentication.getAuthorities().stream().map(GrantedAuthority::getAuthority).toList());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/csrf")
    public ResponseEntity<Map<String, String>> getCsrfToken(CsrfToken csrfToken) {
        Map<String, String> response = new HashMap<>();
        if (csrfToken != null) {
            response.put("token", csrfToken.getToken());
            response.put("headerName", csrfToken.getHeaderName());
            response.put("parameterName", csrfToken.getParameterName());
        }
        return ResponseEntity.ok(response);
    }
}
