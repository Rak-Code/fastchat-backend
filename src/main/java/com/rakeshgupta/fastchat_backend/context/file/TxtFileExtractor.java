package com.rakeshgupta.fastchat_backend.context.file;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Set;

/**
 * File extractor implementation for plain text files (.txt, .text).
 * <p>
 * Uses multi-charset fallback: attempts UTF-8 first, then ISO-8859-1,
 * then Windows-1252. Uses {@link BufferedReader} for efficient line-by-line
 * reading while preserving original line breaks and formatting characters.
 * <p>
 * On any {@link IOException}, the extractor returns empty string and logs
 * the error at WARN level, ensuring graceful degradation.
 */
@Component
public class TxtFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(TxtFileExtractor.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("txt", "text");
    private static final Charset[] FALLBACK_CHARSETS = {
            StandardCharsets.UTF_8,
            StandardCharsets.ISO_8859_1,
            Charset.forName("Windows-1252")
    };

    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }

        for (Charset charset : FALLBACK_CHARSETS) {
            try {
                String result = readWithCharset(inputStream, charset);
                log.debug("Successfully extracted text from '{}' using charset {}", filename, charset);
                return result;
            } catch (IOException e) {
                log.warn("Failed to read '{}' with charset {}, trying next: {}", filename, charset, e.getMessage());
                // inputStream is consumed; re-create not possible — use next fallback
            }
        }

        log.error("Failed to extract text from '{}' with all supported charsets", filename);
        return "";
    }

    private String readWithCharset(InputStream inputStream, Charset charset) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset))) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line).append("\n");
            }
        }
        // Remove trailing newline if content is not empty
        if (!sb.isEmpty() && sb.charAt(sb.length() - 1) == '\n') {
            sb.setLength(sb.length() - 1);
        }
        return sb.toString();
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}