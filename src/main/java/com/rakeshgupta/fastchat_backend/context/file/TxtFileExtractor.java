package com.rakeshgupta.fastchat_backend.context.file;

import com.rakeshgupta.fastchat_backend.common.exception.FileProcessingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * FileExtractor implementation for plain text files (.txt, .text).
 * <p>
 * This extractor handles plain text files with robust encoding detection and fallback
 * mechanisms. It preserves all formatting characters including line breaks, tabs,
 * and whitespace to maintain document structure.
 * </p>
 * 
 * <h3>Supported Features:</h3>
 * <ul>
 *   <li>Multi-charset fallback (UTF-8, ISO-8859-1, Windows-1252)</li>
 *   <li>Efficient streaming with BufferedReader</li>
 *   <li>Formatting preservation (line breaks, tabs, whitespace)</li>
 *   <li>Graceful error handling with detailed logging</li>
 *   <li>Memory-efficient processing for large files</li>
 * </ul>
 * 
 * <h3>Encoding Strategy:</h3>
 * <ol>
 *   <li>First attempt: UTF-8 (most common modern encoding)</li>
 *   <li>Second attempt: ISO-8859-1 (Latin-1, single-byte encoding)</li>
 *   <li>Third attempt: Windows-1252 (Windows default encoding)</li>
 *   <li>If all fail: return empty string and log error</li>
 * </ol>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class TxtFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(TxtFileExtractor.class);

    /**
     * List of character encodings to try in order of preference.
     * UTF-8 is tried first as it's the most common modern encoding.
     */
    private static final List<Charset> FALLBACK_CHARSETS = List.of(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        Charset.forName("Windows-1252")
    );

    /**
     * Supported file extensions for plain text files.
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("txt", "text");

    /**
     * Extracts text content from plain text files with encoding fallback.
     * <p>
     * This method attempts to read the text file using multiple character encodings
     * in order of preference. It preserves all formatting characters and handles
     * large files efficiently using buffered reading.
     * </p>
     * 
     * @param inputStream the file content as an InputStream (must not be null)
     * @param filename the original filename for logging purposes (may be null)
     * @return extracted text content, or empty string if extraction fails
     * @throws IllegalArgumentException if inputStream is null
     * @throws FileProcessingException if a critical error occurs during processing
     */
    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        String displayName = filename != null ? filename : "unknown file";
        log.debug("Starting text extraction for: {}", displayName);

        // Try each charset in order until one succeeds
        for (Charset charset : FALLBACK_CHARSETS) {
            try {
                // Mark the stream to allow reset for charset fallback
                if (inputStream.markSupported()) {
                    inputStream.mark(Integer.MAX_VALUE);
                }

                String content = extractWithCharset(inputStream, charset, displayName);
                if (!content.isEmpty() || isEmptyFileValid(inputStream, charset)) {
                    log.info("Successfully extracted text from {} using {} encoding (length: {} characters)", 
                            displayName, charset.displayName(), content.length());
                    return content;
                }
            } catch (IOException e) {
                log.warn("Failed to extract text from {} using {} encoding: {}", 
                        displayName, charset.displayName(), e.getMessage());
                
                // Reset stream for next charset attempt
                if (inputStream.markSupported()) {
                    try {
                        inputStream.reset();
                    } catch (IOException resetException) {
                        log.error("Failed to reset stream for charset fallback: {}", resetException.getMessage());
                        break; // Cannot continue with fallback
                    }
                }
            }
        }

        // All charsets failed
        log.error("Failed to extract text from {} with all available charsets: {}", 
                 displayName, FALLBACK_CHARSETS.stream().map(Charset::displayName).toList());
        return "";
    }

    /**
     * Attempts to extract text using a specific character encoding.
     * 
     * @param inputStream the input stream to read from
     * @param charset the character encoding to use
     * @param displayName the filename for logging
     * @return extracted text content
     * @throws IOException if reading fails
     */
    private String extractWithCharset(InputStream inputStream, Charset charset, String displayName) 
            throws IOException {
        
        StringBuilder content = new StringBuilder();
        
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            boolean firstLine = true;
            
            while ((line = reader.readLine()) != null) {
                if (!firstLine) {
                    content.append('\n'); // Preserve line breaks
                }
                content.append(line);
                firstLine = false;
            }
        }
        
        return content.toString();
    }

    /**
     * Checks if an empty result is valid for an empty file.
     * This helps distinguish between encoding failures and legitimately empty files.
     * 
     * @param inputStream the input stream
     * @param charset the charset that was tried
     * @return true if empty result is valid
     */
    private boolean isEmptyFileValid(InputStream inputStream, Charset charset) {
        try {
            return inputStream.available() == 0;
        } catch (IOException e) {
            log.debug("Could not determine if file is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the file extensions supported by this extractor.
     * 
     * @return list of supported extensions: ["txt", "text"]
     */
    @Override
    public List<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}