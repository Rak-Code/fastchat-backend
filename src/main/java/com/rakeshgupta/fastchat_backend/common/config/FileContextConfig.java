package com.rakeshgupta.fastchat_backend.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for the AI Context Engine file processing features.
 * <p>
 * This configuration class centralizes all file context-related settings and
 * provides a single point of configuration for the AI Context Engine. It loads
 * configuration from application.yml and makes it available to other components.
 * </p>
 * 
 * <h3>Configuration Properties:</h3>
 * <ul>
 *   <li><strong>chunk-size:</strong> Target size for document chunks (default: 500)</li>
 *   <li><strong>top-k-chunks:</strong> Maximum number of relevant chunks to select (default: 10)</li>
 *   <li><strong>max-context-length:</strong> Maximum context length before truncation (default: 5000)</li>
 *   <li><strong>supported-formats:</strong> List of supported file extensions</li>
 * </ul>
 * 
 * <h3>Spring Configuration Location:</h3>
 * <pre>
 * app:
 *   file-context:
 *     chunk-size: 500
 *     top-k-chunks: 10
 *     max-context-length: 5000
 *     supported-formats: txt,pdf,docx,md,markdown
 * </pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Configuration
@ConfigurationProperties(prefix = "app.file-context")
public class FileContextConfig {

    private static final Logger log = LoggerFactory.getLogger(FileContextConfig.class);

    /**
     * Target size for document chunks in characters.
     */
    private int chunkSize = 500;

    /**
     * Maximum number of relevant chunks to select and include in context.
     */
    private int topKChunks = 10;

    /**
     * Maximum context length in characters before truncation.
     */
    private int maxContextLength = 5000;

    /**
     * List of supported file formats (extensions without dots).
     */
    private List<String> supportedFormats = List.of("txt", "pdf", "docx", "md", "markdown");

    /**
     * Logs the configuration after Spring has finished initializing the bean.
     * This provides visibility into the actual configuration values being used.
     */
    @PostConstruct
    public void logConfiguration() {
        log.info("AI Context Engine Configuration:");
        log.info("  Chunk Size: {} characters", chunkSize);
        log.info("  Top-K Chunks: {}", topKChunks);
        log.info("  Max Context Length: {} characters", maxContextLength);
        log.info("  Supported Formats: {}", supportedFormats);
        
        // Validation warnings
        if (chunkSize <= 0) {
            log.warn("Invalid chunk size: {}. Using default: 500", chunkSize);
            chunkSize = 500;
        }
        if (topKChunks <= 0) {
            log.warn("Invalid top-K chunks: {}. Using default: 10", topKChunks);
            topKChunks = 10;
        }
        if (maxContextLength <= 0) {
            log.warn("Invalid max context length: {}. Using default: 5000", maxContextLength);
            maxContextLength = 5000;
        }
        
        log.info("AI Context Engine initialized successfully");
    }

    // Getters and setters for Spring Boot configuration binding

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getTopKChunks() {
        return topKChunks;
    }

    public void setTopKChunks(int topKChunks) {
        this.topKChunks = topKChunks;
    }

    public int getMaxContextLength() {
        return maxContextLength;
    }

    public void setMaxContextLength(int maxContextLength) {
        this.maxContextLength = maxContextLength;
    }

    public List<String> getSupportedFormats() {
        return supportedFormats;
    }

    public void setSupportedFormats(List<String> supportedFormats) {
        this.supportedFormats = supportedFormats;
    }

    /**
     * Checks if a file extension is configured as supported.
     * 
     * @param extension the file extension to check (without dot)
     * @return true if supported, false otherwise
     */
    public boolean isSupportedFormat(String extension) {
        if (extension == null) {
            return false;
        }
        return supportedFormats.contains(extension.toLowerCase());
    }

    /**
     * Gets a formatted summary of the configuration for debugging.
     * 
     * @return formatted configuration summary
     */
    public String getConfigurationSummary() {
        return String.format("FileContextConfig[chunkSize=%d, topK=%d, maxLength=%d, formats=%s]",
                chunkSize, topKChunks, maxContextLength, supportedFormats);
    }
}