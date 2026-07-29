package com.rakeshgupta.fastchat_backend.context.file;

import com.rakeshgupta.fastchat_backend.common.exception.FileProcessingException;

import java.io.InputStream;
import java.util.List;

/**
 * Strategy interface for extracting text content from different file formats.
 * <p>
 * This interface defines the contract for file text extraction implementations.
 * Each implementation handles a specific file format (TXT, PDF, DOCX, etc.) and
 * provides consistent text extraction behavior.
 * </p>
 * 
 * <h3>Implementation Guidelines:</h3>
 * <ul>
 *   <li>All implementations MUST handle encoding gracefully</li>
 *   <li>All implementations MUST NOT throw unchecked exceptions (wrap in FileProcessingException)</li>
 *   <li>Empty string return indicates extraction failure (logged, not thrown)</li>
 *   <li>Preserve line breaks and formatting characters</li>
 *   <li>Close InputStreams properly in finally blocks</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * FileExtractor extractor = new TxtFileExtractor();
 * try (InputStream inputStream = new FileInputStream("document.txt")) {
 *     String content = extractor.extractText(inputStream, "document.txt");
 *     if (!content.isEmpty()) {
 *         // Process extracted text
 *     }
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
public interface FileExtractor {

    /**
     * Extracts text content from the provided input stream.
     * <p>
     * This method reads the file content from the input stream and extracts
     * all readable text content. The extraction process should preserve
     * formatting characters (line breaks, tabs) and handle various encodings
     * gracefully.
     * </p>
     * 
     * @param inputStream the file content as an InputStream (must not be null)
     * @param filename the original filename for context/logging purposes (may be null)
     * @return extracted text content as a String, or empty string if extraction fails
     * @throws FileProcessingException if a critical extraction error occurs that should be reported
     * @throws IllegalArgumentException if inputStream is null
     */
    String extractText(InputStream inputStream, String filename);

    /**
     * Returns the file extensions supported by this extractor.
     * <p>
     * Extensions should be returned in lowercase without the leading dot.
     * For example: ["txt", "text"] for a text file extractor.
     * </p>
     * 
     * @return an immutable list of supported file extensions (never null)
     */
    List<String> getSupportedExtensions();
}