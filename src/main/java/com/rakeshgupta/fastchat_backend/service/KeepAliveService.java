package com.rakeshgupta.fastchat_backend.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

@Service
@Slf4j
@RequiredArgsConstructor
public class KeepAliveService {

    private final RestClient restClient;

    @Value("${app.url:https://fastchat-backend-xujp.onrender.com}")
    private String appUrl;

    @Value("${app.health-check.endpoint:/api/health}")
    private String healthCheckEndpoint;

    /**
     * Scheduled task that runs every 5 minutes to keep the service active
     * This prevents the Render free plan from shutting down due to inactivity
     */
    @Scheduled(fixedDelay = 100000) // 300000 milliseconds = 5 minutes
    public void keepAliveHealthCheck() {
        try {
            String url = appUrl + healthCheckEndpoint;
            String response = restClient.get()
                    .uri(url)
                    .retrieve()
                    .body(String.class);
            log.info("Keep-alive health check executed successfully: {}", response);
        } catch (Exception e) {
            log.warn("Keep-alive health check failed: {}", e.getMessage());
        }
    }
}
