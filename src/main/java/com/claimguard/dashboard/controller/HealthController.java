package com.claimguard.dashboard.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "ClaimGuard Fraud Review Dashboard");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("timestamp", LocalDateTime.now());
        response.put("database", "H2 In-Memory (Operational)");
        return ResponseEntity.ok(response);
    }
}
