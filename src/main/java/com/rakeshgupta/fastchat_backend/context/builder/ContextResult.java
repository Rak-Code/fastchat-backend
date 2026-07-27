package com.rakeshgupta.fastchat_backend.context.builder;

/**
 * Record representing the result of building an enriched prompt with file context.
 * <p>
 * Contains both the original user message and the enriched prompt that includes
 * file context. The {@link #hasContext()} method can be used to check whether
 * file context was actually added.
 *
 * @param originalMessage the original user message, unchanged
 * @param enrichedPrompt  the prompt enriched with file context (may equal originalMessage if no context)
 */
public record ContextResult(String originalMessage, String enrichedPrompt) {

    /**
     * Returns whether this result contains file context beyond the original message.
     *
     * @return true if enrichedPrompt differs from originalMessage
     */
    public boolean hasContext() {
        return !originalMessage.equals(enrichedPrompt);
    }
}