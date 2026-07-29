package com.rakeshgupta.fastchat_backend.service;

import com.rakeshgupta.fastchat_backend.context.builder.ContextBuilder;
import com.rakeshgupta.fastchat_backend.context.builder.ContextResult;
import com.rakeshgupta.fastchat_backend.context.file.FileExtractor;
import com.rakeshgupta.fastchat_backend.context.file.FileExtractorFactory;
import com.rakeshgupta.fastchat_backend.document.parser.DocumentChunker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Main orchestrator service for file context processing in the AI Context Engine.
 * <p>
 * This service coordinates the entire file-to-context pipeline, integrating all
 * components of the AI Context Engine to provide a simple, high-level API for
 * converting uploaded files into context-enriched prompts.
 * </p>
 * 
 * <h3>Processing Pipeline:</h3>
 * <ol>
 *   <li><strong>Validation:</strong> File size, type, and input validation</li>
 *   <li><strong>Extraction:</strong> Text extraction using appropriate FileExtractor</li>
 *   <li><strong>Chunking:</strong> Split large text into manageable segments</li>
 *   <li><strong>Selection:</strong> Find most relevant chunks based on user query</li>
 *   <li><strong>Context Building:</strong> Create enriched prompt with context</li>
 * </ol>
 * 
 * <h3>Graceful Degradation Strategy:</h3>
 * <ul>
 *   <li>File processing errors never break chat requests</li>
 *   <li>Unsupported file types log warning and return unchanged message</li>
 *   <li>File size limits enforced with appropriate logging</li>
 *   <li>All exceptions caught and logged, returning fallback results</li>
 * </ul>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li>Target: < 2 seconds for files up to 5MB</li>
 *   <li>Memory efficient: files processed in streaming fashion</li>
 *   <li>Automatic cleanup: resources released after processing</li>
 *   <li>Timeout logging for performance monitoring</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Autowired
 * private FileContextService fileContextService;
 * 
 * public String handleChatWithFile(MultipartFile file, String userMessage) {
 *     ContextResult result = fileContextService.processFileContext(file, userMessage);
 *     
 *     // Use enriched prompt for AI request
 *     return chatClient.prompt()
 *         .user(result.enrichedPrompt())
 *         .call()
 *         .content();
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Service
public class FileContextService {

    private static final Logger log = LoggerFactory.getLogger(FileContextService.class);

    /**
     * Maximum allowed file size in bytes (10MB).
     * Chosen to balance functionality with memory usage and processing time.
     */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB

    /**
     * Processing timeout threshold in milliseconds (2 seconds).
     * Used for performance monitoring and warning logs.
     */
    private static final long PROCESSING_TIMEOUT_MS = 2000;

    private final FileExtractorFactory extractorFactory;
    private final DocumentChunker documentChunker;
    private final ContextBuilder contextBuilder;

    /**
     * Constructor with dependency injection.
     * All dependencies are required and validated at startup.
     * 
     * @param extractorFactory factory for selecting appropriate file extractors
     * @param documentChunker service for chunking and selecting relevant content
     * @param contextBuilder service for building context-enriched prompts
     */
    public FileContextService(
            FileExtractorFactory extractorFactory,
            DocumentChunker documentChunker,
            ContextBuilder contextBuilder) {
        this.extractorFactory = extractorFactory;
        this.documentChunker = documentChunker;
        this.contextBuilder = contextBuilder;
        
        log.info("FileContextService initialized with max file size: {} MB", MAX_FILE_SIZE / (1024 * 1024));
    }

    /**
     * Processes uploaded file and builds context-enriched prompt.
     * <p>
     * This is the main entry point for file context processing. It handles the
     * complete pipeline from file upload to context-enriched prompt, with
     * comprehensive error handling and graceful degradation.
     * </p>
     * 
     * <h4>Error Handling Strategy:</h4>
     * <ul>
     *   <li>No file provided → return unchanged message</li>
     *   <li>File too large → log warning, return unchanged message</li>
     *   <li>Unsupported format → log warning, return unchanged message</li>
     *   <li>Extraction failure → log error, return unchanged message</li>
     *   <li>Processing timeout → log warning, continue with result</li>
     *   <li>Unexpected exception → log error with stack trace, return unchanged message</li>
     * </ul>
     * 
     * @param file the uploaded file (may be null)
     * @param userMessage the user's original message (must not be null or empty)
     * @return ContextResult with enriched prompt, or unchanged message if processing fails
     * @throws IllegalArgumentException if userMessage is null or empty
     */
    public ContextResult processFileContext(MultipartFile file, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }

        String normalizedMessage = userMessage.trim();
        long startTime = System.currentTimeMillis();

        try {
            // Quick exit if no file provided
            if (file == null || file.isEmpty()) {
                log.debug("No file provided - returning unchanged message");
                return ContextResult.withoutContext(normalizedMessage);
            }

            // Validate file size
            if (file.getSize() > MAX_FILE_SIZE) {
                log.warn("File size {} exceeds maximum limit {} - skipping context processing",
                        formatFileSize(file.getSize()), formatFileSize(MAX_FILE_SIZE));
                return ContextResult.withoutContext(normalizedMessage);
            }

            String filename = file.getOriginalFilename();
            log.info("Processing file context: filename={}, size={}, contentType={}",
                    filename, formatFileSize(file.getSize()), file.getContentType());

            // Extract text from file
            String extractedText = extractTextFromFile(file, filename);
            if (extractedText.isEmpty()) {
                log.info("No text extracted from file {} - returning unchanged message", filename);
                return ContextResult.withoutContext(normalizedMessage);
            }

            // Process extracted text into context
            ContextResult result = buildContextFromText(extractedText, normalizedMessage);

            // Performance monitoring
            long processingTime = System.currentTimeMillis() - startTime;
            if (processingTime > PROCESSING_TIMEOUT_MS) {
                log.warn("File context processing took {}ms (> {} ms threshold) for file: {}",
                        processingTime, PROCESSING_TIMEOUT_MS, filename);
            } else {
                log.debug("File context processing completed in {}ms for file: {}", processingTime, filename);
            }

            log.info("File context processing successful: {} → {} ({})",
                    filename, result.getSummary(), formatDuration(processingTime));

            return result;

        } catch (Exception e) {
            long processingTime = System.currentTimeMillis() - startTime;
            log.error("File context processing failed after {}ms: {}", processingTime, e.getMessage(), e);
            
            // Graceful degradation: return unchanged message
            return ContextResult.withoutContext(normalizedMessage);
        }
    }

    /**
     * Extracts text content from the uploaded file using the appropriate extractor.
     * 
     * @param file the uploaded file
     * @param filename the original filename for logging
     * @return extracted text content, or empty string if extraction fails
     * @throws IOException if file reading fails
     */
    private String extractTextFromFile(MultipartFile file, String filename) throws IOException {
        // Find appropriate extractor
        Optional<FileExtractor> extractorOpt = extractorFactory.getExtractor(filename);
        if (extractorOpt.isEmpty()) {
            String extension = getFileExtension(filename);
            log.warn("No extractor found for file type: {} (extension: {}). Supported types: {}",
                    filename, extension, extractorFactory.getSupportedExtensions());
            return "";
        }

        FileExtractor extractor = extractorOpt.get();
        log.debug("Using {} for text extraction from: {}", extractor.getClass().getSimpleName(), filename);

        // Extract text
        try {
            String extractedText = extractor.extractText(file.getInputStream(), filename);
            log.info("Text extraction successful: {} characters extracted from {}",
                    extractedText.length(), filename);
            return extractedText;
        } catch (Exception e) {
            log.error("Text extraction failed for {}: {}", filename, e.getMessage(), e);
            return "";
        }
    }

    /**
     * Builds context-enriched prompt from extracted text and user message.
     * 
     * @param extractedText the text content extracted from the file
     * @param userMessage the user's original message
     * @return ContextResult with context-enriched prompt
     */
    private ContextResult buildContextFromText(String extractedText, String userMessage) {
        // Chunk and select relevant content
        List<String> relevantChunks = documentChunker.chunkAndSelect(extractedText, userMessage);
        
        // Build context-enriched prompt
        return contextBuilder.buildPromptFromChunks(relevantChunks, userMessage);
    }

    /**
     * Gets file extension from filename.
     * 
     * @param filename the filename (may be null)
     * @return file extension (lowercase, without dot), or "unknown" if not determinable
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "unknown";
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }

    /**
     * Formats file size in human-readable format.
     * 
     * @param bytes the file size in bytes
     * @return formatted string (e.g., "1.5 MB", "256 KB")
     */
    private String formatFileSize(long bytes) {
        if (bytes < 1024) {
            return bytes + " B";
        } else if (bytes < 1024 * 1024) {
            return String.format("%.1f KB", bytes / 1024.0);
        } else {
            return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
        }
    }

    /**
     * Formats duration in human-readable format.
     * 
     * @param milliseconds the duration in milliseconds
     * @return formatted string (e.g., "1.5s", "250ms")
     */
    private String formatDuration(long milliseconds) {
        if (milliseconds < 1000) {
            return milliseconds + "ms";
        } else {
            return String.format("%.1fs", milliseconds / 1000.0);
        }
    }

    /**
     * Gets the maximum file size limit.
     * 
     * @return maximum file size in bytes
     */
    public long getMaxFileSize() {
        return MAX_FILE_SIZE;
    }

    /**
     * Gets the processing timeout threshold.
     * 
     * @return timeout threshold in milliseconds
     */
    public long getProcessingTimeoutMs() {
        return PROCESSING_TIMEOUT_MS;
    }

    /**
     * Checks if a file size is within the allowed limit.
     * 
     * @param fileSize the file size in bytes
     * @return true if within limit, false otherwise
     */
    public boolean isFileSizeValid(long fileSize) {
        return fileSize > 0 && fileSize <= MAX_FILE_SIZE;
    }

    /**
     * Gets processing statistics for monitoring and debugging.
     * 
     * @return formatted string with service statistics
     */
    public String getServiceStats() {
        return String.format("FileContextService[maxFileSize=%s, timeout=%dms, supportedTypes=%s]",
                formatFileSize(MAX_FILE_SIZE),
                PROCESSING_TIMEOUT_MS,
                extractorFactory.getSupportedExtensions());
    }
}