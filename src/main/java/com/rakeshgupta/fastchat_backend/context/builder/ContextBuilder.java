package com.rakeshgupta.fastchat_backend.context.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds enriched prompts by prepending file context to the user's message.
 * <p>
 * Uses a Builder pattern to construct prompts with the following structure:
 * <pre>
 * File Context:
 * [truncated file context]
 *
 * [original user message]
 * </pre>
 * <p>
 * If the file context is null, blank, or empty, the original message is returned
 * unchanged (passthrough). Context longer than {@value #MAX_CONTEXT_LENGTH}
 * characters is truncated.
 */
@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    /** Maximum length of file context that will be prepended (in characters). */
    public static final int MAX_CONTEXT_LENGTH = 5000;

    /** Prefix added before file context in the enriched prompt. */
    public static final String CONTEXT_PREFIX = "File Context:\n";

    /**
     * Builds an enriched prompt by prepending file context to the user's message.
     *
     * @param fileContext  the extracted file text (may be null or blank)
     * @param userMessage  the original user message (must not be null)
     * @return a {@link ContextResult} containing both original and enriched messages
     */
    public ContextResult buildPrompt(String fileContext, String userMessage) {
        if (fileContext == null || fileContext.isBlank()) {
            log.debug("No file context to prepend, returning original message unchanged");
            return new ContextResult(userMessage, userMessage);
        }

        String truncatedContext = fileContext.length() > MAX_CONTEXT_LENGTH
                ? fileContext.substring(0, MAX_CONTEXT_LENGTH)
                : fileContext;

        String enrichedPrompt = CONTEXT_PREFIX + truncatedContext + "\n\n" + userMessage;

        log.debug("Built enriched prompt with {} chars of context (original: {} chars, truncated: {})",
                truncatedContext.length(), fileContext.length(), fileContext.length() > MAX_CONTEXT_LENGTH);

        return new ContextResult(userMessage, enrichedPrompt);
    }

    /**
     * Builds an enriched prompt from a list of chunks.
     * Chunks are joined with double newlines and then passed to {@link #buildPrompt(String, String)}.
     *
     * @param chunks       the list of text chunks (may be null or empty)
     * @param userMessage  the original user message
     * @return a {@link ContextResult} containing both original and enriched messages
     */
    public ContextResult buildPromptFromChunks(List<String> chunks, String userMessage) {
        if (chunks == null || chunks.isEmpty()) {
            return buildPrompt(null, userMessage);
        }

        String joinedContext = String.join("\n\n", chunks);
        return buildPrompt(joinedContext, userMessage);
    }
}