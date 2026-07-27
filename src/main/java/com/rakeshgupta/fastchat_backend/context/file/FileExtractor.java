package com.rakeshgupta.fastchat_backend.context.file;

import java.io.InputStream;
import java.util.Set;

/**
 * Strategy interface for extracting text content from uploaded files.
 * <p>
 * Implementations handle specific file formats and are automatically discovered
 * by {@link FileExtractorFactory} via Spring dependency injection.
 * All implementations must:
 * <ul>
 *   <li>Handle {@link java.io.IOException} gracefully by returning empty string</li>
 *   <li>Be thread-safe (stateless or use synchronization)</li>
 *   <li>Be annotated with {@link org.springframework.stereotype.Component}</li>
 * </ul>
 */
public interface FileExtractor {

    /**
     * Extracts plain text from the given input stream.
     *
     * @param inputStream the file content as an input stream (never null)
     * @param filename    the original filename for encoding detection (never null)
     * @return the extracted text, or empty string if extraction fails
     * @throws IllegalArgumentException if inputStream or filename is null
     */
    String extractText(InputStream inputStream, String filename);

    /**
     * Returns the set of file extensions supported by this extractor.
     * Extensions are lowercase and do not include the dot prefix.
     *
     * @return an immutable set of supported extensions (e.g., {"txt", "text"})
     */
    Set<String> getSupportedExtensions();
}