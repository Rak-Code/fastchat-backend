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
 * File extractor implementation for Markdown files (.md, .markdown).
 * <p>
 * Markdown is plain text with formatting syntax. This extractor preserves
 * the raw Markdown content without rendering, maintaining headers, lists,
 * code blocks, links, and other Markdown syntax elements as-is.
 * <p>
 * Uses the same multi-charset fallback strategy as {@link TxtFileExtractor}.
 */
@Component
public class MarkdownFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(MarkdownFileExtractor.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("md", "markdown");
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
                log.debug("Successfully extracted text from Markdown file '{}' using charset {}", filename, charset);
                return result;
            } catch (IOException e) {
                log.warn("Failed to read Markdown file '{}' with charset {}, trying next: {}",
                        filename, charset, e.getMessage());
            }
        }

        log.error("Failed to extract text from Markdown file '{}' with all supported charsets", filename);
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