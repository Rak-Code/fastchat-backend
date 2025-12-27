package com.rakeshgupta.fastchat_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * CORS configuration for local development and production.
 * Allows the frontend (localhost:5173 for dev, Vercel for production) to communicate with the backend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173,https://fastchat-ten.vercel.app}")
    private String allowedOrigins;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOrigins(allowedOrigins.split(","))
                .allowedMethods("GET", "POST", "DELETE", "OPTIONS")
                .allowedHeaders("Content-Type")
                .allowCredentials(true);
    }
}
