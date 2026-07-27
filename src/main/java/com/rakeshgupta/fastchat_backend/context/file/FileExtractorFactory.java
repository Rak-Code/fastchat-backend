package com.rakeshgupta.fastchat_backend.context.file;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Factory for discovering and selecting {@link FileExtractor} implementations.
 * <p>
 * Uses constructor injection to receive all registered {@link FileExtractor} beans
 * from the Spring context. Builds an immutable map of extension-to-extractor mappings
 * at construction time. Extension matching is case-insensitive.
 * <p>
 * This factory enables easy addition of new file formats: simply create a new
 * {@link FileExtractor} implementation annotated with {@link org.springframework.stereotype.Component}
 * and it will be automatically discovered.
 */
@Component
public class FileExtractorFactory {

    private static final Logger log = LoggerFactory.getLogger(FileExtractorFactory.class);

    private final Map<String, FileExtractor> extractorMap;

    public FileExtractorFactory(List<FileExtractor> extractors) {
        Map<String, FileExtractor> mutableMap = new HashMap<>();
        for (FileExtractor extractor : extractors) {
            for (String ext : extractor.getSupportedExtensions()) {
                mutableMap.put(ext.toLowerCase(), extractor);
            }
        }
        this.extractorMap = Collections.unmodifiableMap(mutableMap);
    }

    @PostConstruct
    public void logRegisteredExtractors() {
        log.info("Registered file extractors ({} supported extensions): {}",
                extractorMap.size(), extractorMap.keySet());
    }

    /**
     * Returns the appropriate {@link FileExtractor} for the given filename,
     * based on its extension.
     *
     * @param filename the original filename (e.g., "document.txt")
     * @return an Optional containing the matching extractor, or empty if unsupported
     */
    public Optional<FileExtractor> getExtractor(String filename) {
        if (filename == null || filename.isEmpty()) {
            return Optional.empty();
        }
        int lastDot = filename.lastIndexOf('.');
        if (lastDot < 0 || lastDot == filename.length() - 1) {
            return Optional.empty();
        }
        String extension = filename.substring(lastDot + 1).toLowerCase();
        return Optional.ofNullable(extractorMap.get(extension));
    }

    /**
     * Returns the set of all supported file extensions.
     *
     * @return an immutable set of lowercase extensions (e.g., "txt", "md", "pdf")
     */
    public Set<String> getSupportedExtensions() {
        return extractorMap.keySet();
    }
}