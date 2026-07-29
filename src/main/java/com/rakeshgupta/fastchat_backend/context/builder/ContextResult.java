package com.rakeshgupta.fastchat_backend.context.builder;

/**
 * Immutable record containing both original message and context-enriched prompt.
 * <p>
 * This record represents the result of context building operations, preserving both
 * the user's original message and the AI-ready enriched prompt that includes file
 * context. This separation is crucial for proper conversation memory management.
 * </p>
 * 
 * <h3>Key Design Principles:</h3>
 * <ul>
 *   <li><strong>Immutability:</strong> Thread-safe record with final fields</li>
 *   <li><strong>Separation of Concerns:</strong> Original vs enriched content clearly separated</li>
 *   <li><strong>Memory Preservation:</strong> Original message stored in conversation history</li>
 *   <li><strong>AI Processing:</strong> Enriched prompt sent to ChatClient for better responses</li>
 * </ul>
 * 
 * <h3>Usage in Context Flow:</h3>
 * <ol>
 *   <li>User uploads file + message: "What are my key skills?"</li>
 *   <li>ContextBuilder creates ContextResult with:</li>
 *   <ul>
 *     <li>originalMessage: "What are my key skills?"</li>
 *     <li>enrichedPrompt: "File Context:\n[CV content]\n\nWhat are my key skills?"</li>
 *   </ul>
 *   <li>ChatService uses enrichedPrompt for AI request</li>
 *   <li>ChatMemory stores originalMessage (not enriched) for conversation history</li>
 * </ol>
 * 
 * <h3>Memory Management Benefits:</h3>
 * <ul>
 *   <li>Conversation history stays clean (no file context pollution)</li>
 *   <li>File context is ephemeral (only for current request)</li>
 *   <li>Subsequent messages don't carry forward stale file context</li>
 *   <li>Token usage optimized (context not repeated in history)</li>
 * </ul>
 * 
 * <h3>Example Usage:</h3>
 * <pre>{@code
 * // Building context
 * ContextResult result = contextBuilder.buildPrompt(fileContent, userMessage);
 * 
 * // Using in ChatService
 * String aiResponse = chatClient.prompt()
 *     .user(result.enrichedPrompt())  // Send enriched prompt to AI
 *     .call()
 *     .content();
 * 
 * // Conversation memory gets original message only
 * // (handled automatically by Spring AI ChatMemory)
 * }</pre>
 * 
 * @param originalMessage the user's original message exactly as provided (never null)
 * @param enrichedPrompt the context-enriched prompt ready for AI processing (never null)
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
public record ContextResult(
    String originalMessage,
    String enrichedPrompt
) {
    
    /**
     * Compact constructor with validation and normalization.
     * Ensures both fields are non-null and handles edge cases gracefully.
     * 
     * @param originalMessage the user's original message
     * @param enrichedPrompt the context-enriched prompt
     * @throws IllegalArgumentException if either parameter is null
     */
    public ContextResult {
        if (originalMessage == null) {
            throw new IllegalArgumentException("originalMessage cannot be null");
        }
        if (enrichedPrompt == null) {
            throw new IllegalArgumentException("enrichedPrompt cannot be null");
        }
        
        // Normalize empty strings to ensure consistent behavior
        originalMessage = originalMessage.trim();
        enrichedPrompt = enrichedPrompt.trim();
        
        // Additional validation for empty strings
        if (originalMessage.isEmpty()) {
            throw new IllegalArgumentException("originalMessage cannot be empty");
        }
        if (enrichedPrompt.isEmpty()) {
            throw new IllegalArgumentException("enrichedPrompt cannot be empty");
        }
    }
    
    /**
     * Returns true if context was added to the original message.
     * <p>
     * This method compares the original message with the enriched prompt to
     * determine if any file context was actually added. It's useful for:
     * </p>
     * <ul>
     *   <li>Logging and debugging context processing</li>
     *   <li>Metrics and monitoring (context utilization rates)</li>
     *   <li>Conditional processing based on context presence</li>
     *   <li>Testing and validation</li>
     * </ul>
     * 
     * @return true if enriched prompt differs from original message, false otherwise
     */
    public boolean hasContext() {
        return !originalMessage.equals(enrichedPrompt);
    }
    
    /**
     * Returns the length difference between enriched prompt and original message.
     * <p>
     * This method calculates how much context content was added, which is useful for:
     * </p>
     * <ul>
     *   <li>Monitoring context size impact</li>
     *   <li>Token usage estimation</li>
     *   <li>Performance metrics</li>
     *   <li>Context truncation analysis</li>
     * </ul>
     * 
     * @return the number of additional characters in enriched prompt (0 if no context added)
     */
    public int getContextLength() {
        return hasContext() ? enrichedPrompt.length() - originalMessage.length() : 0;
    }
    
    /**
     * Returns a summary of this ContextResult for logging and debugging.
     * <p>
     * The summary includes key metrics without exposing sensitive content:
     * </p>
     * <ul>
     *   <li>Whether context was added</li>
 f    *   <li>Character counts for both messages</li>
     *   <li>Context size impact</li>
     * </ul>
     * 
     * @return a concise summary string suitable for logging
     */
    public String getSummary() {
        return String.format("ContextResult[hasContext=%s, original=%d chars, enriched=%d chars, context=+%d chars]",
                hasContext(), originalMessage.length(), enrichedPrompt.length(), getContextLength());
    }
    
    /**
     * Creates a ContextResult with no context added (original message unchanged).
     * <p>
     * This factory method is used when no file context is available or when
     * context processing fails. It ensures consistent API usage even when
     * context enhancement isn't possible.
     * </p>
     * 
     * @param message the user's message (will be used for both original and enriched)
     * @return a ContextResult where both fields contain the same message
     * @throws IllegalArgumentException if message is null or empty
     */
    public static ContextResult withoutContext(String message) {
        return new ContextResult(message, message);
    }
    
    /**
     * Creates a ContextResult with context enhancement.
     * <p>
     * This factory method is the standard way to create context-enriched results.
     * It clearly separates the original message from the enriched version.
     * </p>
     * 
     * @param original the user's original message
     * @param enriched the context-enriched prompt
     * @return a new ContextResult with both messages
     * @throws IllegalArgumentException if either parameter is null or empty
     */
    public static ContextResult withContext(String original, String enriched) {
        return new ContextResult(original, enriched);
    }
    
    /**
     * Enhanced toString() method that provides detailed information for debugging.
     * <p>
     * Shows both messages with clear labeling, but truncates long content to
     * prevent log pollution while still providing useful debugging information.
     * </p>
     * 
     * @return a detailed string representation suitable for debugging
     */
    @Override
    public String toString() {
        String originalPreview = originalMessage.length() > 100 
            ? originalMessage.substring(0, 100) + "..." 
            : originalMessage;
        String enrichedPreview = enrichedPrompt.length() > 100 
            ? enrichedPrompt.substring(0, 100) + "..." 
            : enrichedPrompt;
            
        return String.format("ContextResult{\n  original: \"%s\" (%d chars)\n  enriched: \"%s\" (%d chars)\n  hasContext: %s\n}",
                originalPreview, originalMessage.length(), 
                enrichedPreview, enrichedPrompt.length(), 
                hasContext());
    }
}