package com.rakeshgupta.fastchat_backend.context.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Factory for selecting the appropriate FileExtractor based on file extension.
 * <p>
 * This factory uses Spring's dependency injection to automatically discover all
 * FileExtractor implementations in the application context and builds a lookup
 * map for efficient O(1) extractor selection by file extension.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li>Automatic discovery of all FileExtractor implementations via Spring DI</li>
 *   <li>Case-insensitive extension matching</li>
 *   <li>Thread-safe concurrent access</li>
 *   <li>O(1) lookup performance</li>
 *   <li>Comprehensive logging for debugging and monitoring</li>
 * </ul>
 * 
 * <h3>Supported Extensions (auto-discovered):</h3>
 * <ul>
 *   <li>txt, text - Plain text files</li>
 *   <li>md, markdown - Markdown files</li>
 *   <li>pdf - PDF documents (when PdfFileExtractor is added)</li>
 *   <li>docx - Microsoft Word documents (when DocxFileExtractor is added)</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Autowired
 * private FileExtractorFactory factory;
 * 
 * public String extractText(MultipartFile file) {
 *     Optional<FileExtractor> extractor = factory.getExtractor(file.getOriginalFilename());
 *     if (extractor.isPresent()) {
 *         return extractor.get().extractText(file.getInputStream(), file.getOriginalFilename());
 *     }
 *     return "";
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class FileExtractorFactory {

    private static final Logger log = LoggerFactory.getLogger(FileExtractorFactory.class);

    /**
     * Thread-safe map of file extensions to their corresponding extractors.
     * Key: file extension (lowercase, without dot)
     * Value: FileExtractor implementation
     */
    private final Map<String, FileExtractor> extractorMap = new ConcurrentHashMap<>();

    /**
     * All FileExtractor implementations discovered by Spring.
     * Injected automatically via constructor injection.
     */
    private final List<FileExtractor> extractors;

    /**
     * Constructor that receives all FileExtractor implementations via Spring DI.
     * 
     * @param extractors list of all FileExtractor beans in the application context
     */
    public FileExtractorFactory(List<FileExtractor> extractors) {
        this.extractors = extractors != null ? extractors : Collections.emptyList();
        log.info("FileExtractorFactory initialized with {} extractor implementations", this.extractors.size());
    }

    /**
     * Initializes the extractor map after Spring dependency injection is complete.
     * This method is called automatically by Spring after the bean is constructed.
     */
    @PostConstruct
    public void initializeExtractorMap() {
        log.info("Building file extractor lookup map...");
        
        int totalExtensions = 0;
        for (FileExtractor extractor : extractors) {
            List<String> extensions = extractor.getSupportedExtensions();
            if (extensions == null || extensions.isEmpty()) {
                log.warn("FileExtractor {} supports no extensions - skipping", 
                        extractor.getClass().getSimpleName());
                continue;
            }

            for (String extension : extensions) {
                String normalizedExtension = normalizeExtension(extension);
                if (normalizedExtension.isEmpty()) {
                    log.warn("Invalid extension '{}' from {} - skipping", 
                            extension, extractor.getClass().getSimpleName());
                    continue;
                }

                // Check for conflicts
                FileExtractor existing = extractorMap.get(normalizedExtension);
                if (existing != null) {
                    log.warn("Extension '{}' is supported by both {} and {} - {} will be used", 
                            normalizedExtension, 
                            existing.getClass().getSimpleName(),
                            extractor.getClass().getSimpleName(),
                            existing.getClass().getSimpleName());
                } else {
                    extractorMap.put(normalizedExtension, extractor);
                    totalExtensions++;
                    log.debug("Registered '{}' extension with {}", 
                            normalizedExtension, extractor.getClass().getSimpleName());
                }
            }
        }

        log.info("FileExtractorFactory registration complete: {} extractors supporting {} extensions", 
                extractors.size(), totalExtensions);
        
        if (log.isInfoEnabled()) {
            logRegisteredExtractors();
        }
    }

    /**
     * Gets the appropriate FileExtractor for the given filename.
     * 
     * @param filename the filename to extract extension from (may be null or empty)
     * @return Optional containing the FileExtractor if supported, empty otherwise
     */
    public Optional<FileExtractor> getExtractor(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            log.debug("Cannot determine extractor for null or empty filename");
            return Optional.empty();
        }

        String extension = extractFileExtension(filename);
        if (extension.isEmpty()) {
            log.debug("No file extension found in filename: {}", filename);
            return Optional.empty();
        }

        String normalizedExtension = normalizeExtension(extension);
        FileExtractor extractor = extractorMap.get(normalizedExtension);
        
        if (extractor != null) {
            log.debug("Found {} for extension '{}' (filename: {})", 
                    extractor.getClass().getSimpleName(), normalizedExtension, filename);
        } else {
            log.debug("No extractor found for extension '{}' (filename: {})", 
                    normalizedExtension, filename);
        }

        return Optional.ofNullable(extractor);
    }

    /**
     * Returns all supported file extensions across all registered extractors.
     * 
     * @return unmodifiable set of supported extensions (lowercase, without dots)
     */
    public Set<String> getSupportedExtensions() {
        return Collections.unmodifiableSet(extractorMap.keySet());
    }

    /**
     * Returns the total number of registered extractors.
     * 
     * @return number of FileExtractor implementations
     */
    public int getExtractorCount() {
        return extractors.size();
    }

    /**
     * Checks if a specific file extension is supported.
     * 
     * @param extension the file extension to check (with or without dot)
     * @return true if the extension is supported, false otherwise
     */
    public boolean isExtensionSupported(String extension) {
        if (extension == null || extension.trim().isEmpty()) {
            return false;
        }
        String normalizedExtension = normalizeExtension(extension);
        return extractorMap.containsKey(normalizedExtension);
    }

    /**
     * Extracts the file extension from a filename.
     * 
     * @param filename the filename to process
     * @return the file extension (without dot), or empty string if no extension
     */
    private String extractFileExtension(String filename) {
        if (filename == null || filename.trim().isEmpty()) {
            return "";
        }

        // Handle filenames that might have path separators
        String nameOnly = filename.contains("/") ? 
            filename.substring(filename.lastIndexOf('/') + 1) : filename;
        nameOnly = nameOnly.contains("\\") ? 
            nameOnly.substring(nameOnly.lastIndexOf('\\') + 1) : nameOnly;

        int lastDotIndex = nameOnly.lastIndexOf('.');
        if (lastDotIndex == -1 || lastDotIndex == nameOnly.length() - 1) {
            return ""; // No extension or dot at end
        }

        return nameOnly.substring(lastDotIndex + 1);
    }

    /**
     * Normalizes a file extension to lowercase without leading dot.
     * 
     * @param extension the extension to normalize
     * @return normalized extension (lowercase, no dot)
     */
    private String normalizeExtension(String extension) {
        if (extension == null) {
            return "";
        }
        
        String trimmed = extension.trim();
        if (trimmed.isEmpty()) {
            return "";
        }

        // Remove leading dot if present
        String normalized = trimmed.startsWith(".") ? trimmed.substring(1) : trimmed;
        return normalized.toLowerCase();
    }

    /**
     * Logs all registered extractors and their supported extensions for debugging.
     */
    private void logRegisteredExtractors() {
        StringBuilder summary = new StringBuilder("Registered FileExtractors:\n");
        
        Map<String, List<String>> extractorToExtensions = new HashMap<>();
        for (Map.Entry<String, FileExtractor> entry : extractorMap.entrySet()) {
            String extractorName = entry.getValue().getClass().getSimpleName();
            extractorToExtensions.computeIfAbsent(extractorName, k -> new ArrayList<>())
                               .add(entry.getKey());
        }

        for (Map.Entry<String, List<String>> entry : extractorToExtensions.entrySet()) {
            List<String> extensions = entry.getValue();
            Collections.sort(extensions);
            summary.append(String.format("  - %s: [%s]\n", 
                          entry.getKey(), String.join(", ", extensions)));
        }

        log.info(summary.toString().trim());
    }
}