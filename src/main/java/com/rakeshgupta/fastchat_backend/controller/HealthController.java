package com.rakeshgupta.fastchat_backend.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.actuator.health.Health;
import org.springframework.boot.actuator.health.HealthIndicator;
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
public class HealthController implements HealthIndicator {

    private static final Logger log = LoggerFactory.getLogger(HealthController.class);

    @Autowired
    private DataSource dataSource;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        health.put("status", "UP");
        health.put("timestamp", Instant.now().toString());
        
        // Check database connectivity
        try (Connection connection = dataSource.getConnection()) {
            health.put("database", "UP");
        } catch (Exception e) {
            log.error("Database health check failed: {}", e.getMessage());
            health.put("database", "DOWN");
            health.put("database_error", e.getMessage());
        }
        
        // Check environment variables
        Map<String, String> env = new HashMap<>();
        env.put("groq_api_configured", System.getenv("GROQ_API_KEY") != null ? "YES" : "NO");
        env.put("database_url_configured", System.getenv("DB_URL") != null ? "YES" : "NO");
        health.put("environment", env);
        
        return ResponseEntity.ok(health);
    }

    @Override
    public Health health() {
        try (Connection connection = dataSource.getConnection()) {
            return Health.up()
                    .withDetail("database", "UP")
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        } catch (Exception e) {
            return Health.down()
                    .withDetail("database", "DOWN")
                    .withDetail("error", e.getMessage())
                    .withDetail("timestamp", Instant.now().toString())
                    .build();
        }
    }
}
