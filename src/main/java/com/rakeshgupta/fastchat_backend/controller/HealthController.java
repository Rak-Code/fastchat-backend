package com.rakeshgupta.fastchat_backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class HealthController {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        boolean isHealthy = true;
        
        health.put("timestamp", Instant.now().toString());
        health.put("application", "fastchat-backend");
        
        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "UP");
            log.debug("Database health check: UP");
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            health.put("database", "DOWN");
            health.put("database_error", e.getMessage());
            isHealthy = false;
        }
        
        // Check environment variables
        Map<String, String> env = new HashMap<>();
        String groqApiKey = System.getenv("GROQ_API_KEY");
        String dbUrl = System.getenv("DB_URL");
        
        env.put("groq_api_configured", (groqApiKey != null && !groqApiKey.trim().isEmpty()) ? "YES" : "NO");
        env.put("database_url_configured", (dbUrl != null && !dbUrl.trim().isEmpty()) ? "YES" : "NO");
        
        if (groqApiKey == null || groqApiKey.trim().isEmpty()) {
            isHealthy = false;
            log.warn("GROQ_API_KEY not configured");
        }
        
        health.put("environment", env);
        health.put("status", isHealthy ? "UP" : "DOWN");
        
        if (!isHealthy) {
            return ResponseEntity.status(503).body(health);
        }
        
        return ResponseEntity.ok(health);
    }
}
