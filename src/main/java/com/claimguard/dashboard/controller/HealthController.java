package com.claimguard.dashboard.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/health")
@CrossOrigin(origins = "*")
public class HealthController {

    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);

    private final DataSource dataSource;

    @Autowired(required = false)
    public HealthController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public HealthController() {
        this.dataSource = null;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getHealth() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("application", "ClaimGuard Fraud Review Dashboard");
        response.put("version", "1.0.0-SNAPSHOT");
        response.put("timestamp", LocalDateTime.now());
        response.put("database", determineDatabaseStatus());
        return ResponseEntity.ok(response);
    }

    String determineDatabaseStatus() {
        if (dataSource == null) {
            return "Unknown (No DataSource)";
        }
        try (Connection conn = dataSource.getConnection()) {
            DatabaseMetaData metaData = conn.getMetaData();
            String url = metaData != null ? metaData.getURL() : null;
            if (url != null) {
                String normalizedUrl = url.toLowerCase();
                if (normalizedUrl.contains(":h2:mem:") || normalizedUrl.startsWith("jdbc:h2:mem")) {
                    return "H2 In-Memory (Operational)";
                } else if (normalizedUrl.contains(":h2:file:") || normalizedUrl.startsWith("jdbc:h2:file")) {
                    return "H2 File-Backed (Operational)";
                } else if (normalizedUrl.contains(":h2:tcp:") || normalizedUrl.contains(":h2:ssl:")) {
                    return "H2 Remote Server (Operational)";
                } else {
                    String productName = metaData.getDatabaseProductName();
                    return (productName != null ? productName : "Database") + " (Operational)";
                }
            }
        } catch (Exception e) {
            logger.warn("Could not determine database status from DataSource metadata: {}", e.getMessage());
        }
        return "Database Connection Degraded";
    }
}
