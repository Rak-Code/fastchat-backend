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
import java.io.InputStream;
import java.util.List;
import java.util.Optional;

/**
 * Orchestrates the file context processing pipeline:
 * <ol>
 *   <li>Validates file size and type</li>
 *   <li>Extracts text using the appropriate {@link FileExtractor}</li>
 *   <li>Chunks and selects relevant portions via {@link DocumentChunker}</li>
 *   <li>Builds enriched prompt via {@link ContextBuilder}</li>
 * </ol>
 * <p>
 * All errors are handled gracefully: on any failure, the original message
 * is returned unchanged with appropriate logging.
 */
@Service
public class FileContextService {

    private static final Logger log = LoggerFactory.getLogger(FileContextService.class);

    /** Maximum allowed file size: 10 MB. */
    public static final long MAX_FILE_SIZE = 10 * 1024 * 1024L;

    private final FileExtractorFactory extractorFactory;
    private final DocumentChunker documentChunker;
    private final ContextBuilder contextBuilder;

    public FileContextService(FileExtractorFactory extractorFactory,
                              DocumentChunker documentChunker,
                              ContextBuilder contextBuilder) {
        this.extractorFactory = extractorFactory;
        this.documentChunker = documentChunker;
        this.contextBuilder = contextBuilder;
    }

    /**
     * Processes an uploaded file and builds an enriched prompt with file context.
     * <p>
     * If the file is null, empty, too large, unsupported, or extraction fails,
     * the original message is returned unchanged.
     *
     * @param file        the uploaded file (may be null)
     * @param userMessage the original user message
     * @return a {@link ContextResult} with the original and (possibly) enriched messages
     */
    public ContextResult processFileContext(MultipartFile file, String userMessage) {
        if (file == null || file.isEmpty()) {
            log.debug("No file provided, returning original message unchanged");
            return new ContextResult(userMessage, userMessage);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File '{}' exceeds maximum size of {} bytes (actual: {}), returning original message",
                    file.getOriginalFilename(), MAX_FILE_SIZE, file.getSize());
            return new ContextResult(userMessage, userMessage);
        }

        String filename = file.getOriginalFilename();
        if (filename == null || filename.isEmpty()) {
            log.warn("File has no name, returning original message unchanged");
            return new ContextResult(userMessage, userMessage);
        }

        Optional<FileExtractor> extractorOpt = extractorFactory.getExtractor(filename);
        if (extractorOpt.isEmpty()) {
            log.warn("Unsupported file type for '{}', returning original message unchanged", filename);
            return new ContextResult(userMessage, userMessage);
        }

        try {
            String extractedText;
            try (InputStream inputStream = file.getInputStream()) {
                extractedText = extractorOpt.get().extractText(inputStream, filename);
            }

            if (extractedText.isEmpty()) {
                log.warn("Extraction returned empty text for '{}', returning original message unchanged", filename);
                return new ContextResult(userMessage, userMessage);
            }

            List<String> chunks = documentChunker.chunkAndSelect(extractedText, userMessage);
            ContextResult result = contextBuilder.buildPromptFromChunks(chunks, userMessage);

            log.info("Successfully processed file '{}' ({} bytes, {} chunks selected)", filename, file.getSize(), chunks.size());
            return result;

        } catch (IOException e) {
            log.error("IO error processing file '{}': {}", filename, e.getMessage(), e);
            return new ContextResult(userMessage, userMessage);
        } catch (Exception e) {
            log.error("Unexpected error processing file '{}': {}", filename, e.getMessage(), e);
            return new ContextResult(userMessage, userMessage);
        }
    }
}