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
 * FileExtractor implementation for Markdown files (.md, .markdown).
 * <p>
 * This extractor handles Markdown files by treating them as plain text with preserved
 * formatting syntax. It maintains all Markdown formatting elements (headers, lists, 
 * code blocks, links, etc.) without rendering them to HTML, allowing the AI to 
 * understand both content and structure.
 * </p>
 * 
 * <h3>Preserved Markdown Elements:</h3>
 * <ul>
 *   <li>Headers: # ## ### etc.</li>
 *   <li>Lists: - * + and numbered lists</li>
 *   <li>Code blocks: ```code``` and `inline code`</li>
 *   <li>Links: [text](url) and ![alt](image-url)</li>
 *   <li>Emphasis: *italic* **bold** ***bold-italic***</li>
 *   <li>Tables: | column | column |</li>
 *   <li>Blockquotes: > quoted text</li>
 *   <li>Horizontal rules: --- or ***</li>
 * </ul>
 * 
 * <h3>Encoding Strategy:</h3>
 * <p>Uses the same robust encoding fallback as TxtFileExtractor:</p>
 * <ol>
 *   <li>UTF-8 (preferred for international content)</li>
 *   <li>ISO-8859-1 (Latin-1 fallback)</li>
 *   <li>Windows-1252 (Windows compatibility)</li>
 * </ol>
 * 
 * <h3>Use Cases:</h3>
 * <ul>
 *   <li>README files with project documentation</li>
 *   <li>Technical documentation and wikis</li>
 *   <li>Blog posts and articles in Markdown format</li>
 *   <li>API documentation and specifications</li>
 *   <li>Personal notes and structured documents</li>
 * </ul>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class MarkdownFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(MarkdownFileExtractor.class);

    /**
     * List of character encodings to try in order of preference.
     * Matches TxtFileExtractor for consistency.
     */
    private static final List<Charset> FALLBACK_CHARSETS = List.of(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        Charset.forName("Windows-1252")
    );

    /**
     * Supported file extensions for Markdown files.
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("md", "markdown");

    /**
     * Extracts text content from Markdown files preserving all formatting syntax.
     * <p>
     * This method reads Markdown files as plain text, preserving all Markdown
     * syntax elements without rendering. This allows the AI to understand both
     * the content and the structural formatting of the document.
     * </p>
     * 
     * @param inputStream the file content as an InputStream (must not be null)
     * @param filename the original filename for logging purposes (may be null)
     * @return extracted Markdown content with preserved formatting, or empty string if extraction fails
     * @throws IllegalArgumentException if inputStream is null
     * @throws FileProcessingException if a critical error occurs during processing
     */
    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        String displayName = filename != null ? filename : "unknown markdown file";
        log.debug("Starting Markdown extraction for: {}", displayName);

        // Try each charset in order until one succeeds
        for (Charset charset : FALLBACK_CHARSETS) {
            try {
                // Mark the stream to allow reset for charset fallback
                if (inputStream.markSupported()) {
                    inputStream.mark(Integer.MAX_VALUE);
                }

                String content = extractWithCharset(inputStream, charset, displayName);
                if (!content.isEmpty() || isEmptyFileValid(inputStream, charset)) {
                    log.info("Successfully extracted Markdown from {} using {} encoding (length: {} characters)", 
                            displayName, charset.displayName(), content.length());
                    
                    // Log some basic Markdown structure info for debugging
                    logMarkdownStructure(content, displayName);
                    return content;
                }
            } catch (IOException e) {
                log.warn("Failed to extract Markdown from {} using {} encoding: {}", 
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
        log.error("Failed to extract Markdown from {} with all available charsets: {}", 
                 displayName, FALLBACK_CHARSETS.stream().map(Charset::displayName).toList());
        return "";
    }

    /**
     * Attempts to extract Markdown content using a specific character encoding.
     * 
     * @param inputStream the input stream to read from
     * @param charset the character encoding to use
     * @param displayName the filename for logging
     * @return extracted Markdown content with preserved formatting
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
                    content.append('\n'); // Preserve line breaks (critical for Markdown)
                }
                content.append(line);
                firstLine = false;
            }
        }
        
        return content.toString();
    }

    /**
     * Logs basic structural information about the Markdown content for debugging.
     * 
     * @param content the extracted Markdown content
     * @param displayName the filename for logging
     */
    private void logMarkdownStructure(String content, String displayName) {
        if (!log.isDebugEnabled()) {
            return;
        }

        String[] lines = content.split("\n");
        long headerCount = java.util.Arrays.stream(lines)
                .filter(line -> line.trim().startsWith("#"))
                .count();
        
        long listItemCount = java.util.Arrays.stream(lines)
                .filter(line -> line.trim().matches("^[\\-\\*\\+]\\s+.*") || line.trim().matches("^\\d+\\.\\s+.*"))
                .count();
        
        long codeBlockCount = content.split("```").length / 2;
        
        log.debug("Markdown structure for {}: {} headers, {} list items, {} code blocks", 
                 displayName, headerCount, listItemCount, codeBlockCount);
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
            log.debug("Could not determine if Markdown file is empty: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Returns the file extensions supported by this extractor.
     * 
     * @return list of supported extensions: ["md", "markdown"]
     */
    @Override
    public List<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}