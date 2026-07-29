package com.rakeshgupta.fastchat_backend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import java.util.Arrays;

/**
 * CORS configuration for local development and production.
 * Allows the frontend (localhost:5173 for dev, Vercel for production) to communicate with the backend.
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Value("${cors.allowed-origins:http://localhost:5173,http://localhost:3000,https://fastchat-ten.vercel.app,https://fastchat-llm.vercel.app}")
    private String allowedOrigins;

    @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS}")
    private String allowedMethods;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        String[] origins = allowedOrigins.split(",");
        String[] methods = allowedMethods.split(",");
        
        System.out.println("CORS Configuration:");
        System.out.println("Allowed Origins: " + Arrays.toString(origins));
        System.out.println("Allowed Methods: " + Arrays.toString(methods));
        
        registry.addMapping("/api/**")
                .allowedOrigins(origins)
                .allowedMethods(methods)
                .allowedHeaders("*")
                .allowCredentials(true)
                .exposedHeaders("Access-Control-Allow-Origin", "Access-Control-Allow-Credentials")
                .maxAge(3600); // Cache preflight response for 1 hour
    }
}
