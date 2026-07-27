package com.rakeshgupta.fastchat_backend.common.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration marker class for the File Context Engine.
 * <p>
 * Provides initialization logging and serves as a configuration anchor point
 * for {@code app.file-context} properties defined in {@code application.yml}.
 */
@Configuration
public class FileContextConfig {

    private static final Logger log = LoggerFactory.getLogger(FileContextConfig.class);

    @PostConstruct
    public void init() {
        log.info("File Context Engine configuration initialized");
    }
}