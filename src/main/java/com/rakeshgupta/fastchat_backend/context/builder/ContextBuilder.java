package com.rakeshgupta.fastchat_backend.context.builder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builder for creating context-enriched prompts using the Builder pattern.
 * <p>
 * This service constructs AI-ready prompts by combining file context with user messages.
 * It handles context formatting, truncation, and provides a clean separation between
 * original messages and enriched prompts for conversation memory management.
 * </p>
 * 
 * <h3>Key Features:</h3>
 * <ul>
 *   <li><strong>Context Formatting:</strong> Standardized "File Context:" prefix</li>
 *   <li><strong>Truncation:</strong> Limits context to 5000 characters to prevent token overflow</li>
 *   <li><strong>Graceful Fallback:</strong> Returns unchanged message when no context available</li>
 *   <li><strong>Memory Separation:</strong> Preserves original message for conversation history</li>
 * </ul>
 * 
 * <h3>Prompt Structure:</h3>
 * <pre>
 * File Context:
 * [Extracted and selected file content, up to 5000 characters]
 * 
 * [User's original message]
 * </pre>
 * 
 * <h3>Truncation Strategy:</h3>
 * <ul>
 *   <li>Context truncated to MAX_CONTEXT_LENGTH (5000 chars) if needed</li>
 *   <li>Truncation preserves beginning of context (most important)</li>
 *   <li>User message never truncated (always preserved completely)</li>
 *   <li>Warning logged when truncation occurs for monitoring</li>
 * </ul>
 * 
 * <h3>Usage Examples:</h3>
 * <pre>{@code
 * // Basic context building
 * ContextResult result = contextBuilder.buildPrompt(extractedText, "What are my skills?");
 * 
 * // Building from selected chunks
 * List<String> relevantChunks = chunker.selectTopChunks(chunks, userMessage);
 * ContextResult result = contextBuilder.buildPromptFromChunks(relevantChunks, userMessage);
 * 
 * // Usage in chat flow
 * String aiResponse = chatClient.prompt()
 *     .user(result.enrichedPrompt())  // Send enriched prompt to AI
 *     .call()
 *     .content();
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class ContextBuilder {

    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);

    /**
     * Maximum allowed context length in characters.
     * Chosen to balance context richness with token limits and processing efficiency.
     */
    public static final int MAX_CONTEXT_LENGTH = 5000;

    /**
     * Standard prefix for file context in enriched prompts.
     * Makes it clear to the AI that context is provided from uploaded files.
     */
    public static final String CONTEXT_PREFIX = "File Context:\n";

    /**
     * Separator between context and user message.
     * Provides clear visual separation for better AI understanding.
     */
    private static final String CONTEXT_SEPARATOR = "\n\n";

    /**
     * Builds a context-enriched prompt from file context and user message.
     * <p>
     * This method is the primary entry point for context building. It handles
     * all aspects of prompt construction including formatting, truncation,
     * and fallback scenarios.
     * </p>
     * 
     * <h4>Processing Steps:</h4>
     * <ol>
     *   <li>Validate inputs (null/empty checks)</li>
     *   <li>Truncate context if exceeds MAX_CONTEXT_LENGTH</li>
     *   <li>Format context with standard prefix</li>
     *   <li>Combine context with user message</li>
     *   <li>Return ContextResult with both original and enriched messages</li>
     * </ol>
     * 
     * @param fileContext extracted file context (may be null or empty)
     * @param userMessage the user's original message (must not be null or empty)
     * @return ContextResult containing original message and enriched prompt
     * @throws IllegalArgumentException if userMessage is null or empty
     */
    public ContextResult buildPrompt(String fileContext, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }

        String normalizedMessage = userMessage.trim();
        
        // If no context available, return unchanged message
        if (fileContext == null || fileContext.trim().isEmpty()) {
            log.debug("No file context provided - returning unchanged message");
            return ContextResult.withoutContext(normalizedMessage);
        }

        String normalizedContext = fileContext.trim();
        
        // Apply context length limits
        String truncatedContext = truncateContext(normalizedContext);
        
        // Build the enriched prompt
        String enrichedPrompt = formatEnrichedPrompt(truncatedContext, normalizedMessage);
        
        ContextResult result = ContextResult.withContext(normalizedMessage, enrichedPrompt);
        
        log.info("Built context-enriched prompt: original={} chars, enriched={} chars, context={} chars",
                normalizedMessage.length(), enrichedPrompt.length(), truncatedContext.length());
        
        if (log.isDebugEnabled()) {
            log.debug("Context building result: {}", result.getSummary());
        }
        
        return result;
    }

    /**
     * Builds a context-enriched prompt from selected chunks and user message.
     * <p>
     * This method is a convenience wrapper that joins chunks with double newlines
     * and delegates to the main buildPrompt method. It's commonly used with
     * the output from DocumentChunker.selectTopChunks().
     * </p>
     * 
     * @param chunks selected context chunks (may be null or empty)
     * @param userMessage the user's original message (must not be null or empty)
     * @return ContextResult containing original message and enriched prompt
     * @throws IllegalArgumentException if userMessage is null or empty
     */
    public ContextResult buildPromptFromChunks(List<String> chunks, String userMessage) {
        if (userMessage == null || userMessage.trim().isEmpty()) {
            throw new IllegalArgumentException("User message cannot be null or empty");
        }

        // Handle null or empty chunks
        if (chunks == null || chunks.isEmpty()) {
            log.debug("No chunks provided - returning unchanged message");
            return ContextResult.withoutContext(userMessage.trim());
        }

        // Join chunks with double newlines for readability
        String joinedContext = String.join(CONTEXT_SEPARATOR, chunks);
        
        log.debug("Joined {} chunks into context of {} characters", chunks.size(), joinedContext.length());
        
        return buildPrompt(joinedContext, userMessage);
    }

    /**
     * Truncates context to maximum allowed length with logging.
     * 
     * @param context the context to potentially truncate
     * @return truncated context (may be original if under limit)
     */
    private String truncateContext(String context) {
        if (context.length() <= MAX_CONTEXT_LENGTH) {
            return context;
        }

        String truncated = context.substring(0, MAX_CONTEXT_LENGTH);
        
        log.warn("Context truncated from {} to {} characters (limit: {})", 
                context.length(), truncated.length(), MAX_CONTEXT_LENGTH);
        
        return truncated;
    }

    /**
     * Formats the enriched prompt by combining context and user message.
     * 
     * @param context the (potentially truncated) file context
     * @param userMessage the user's original message
     * @return the formatted enriched prompt
     */
    private String formatEnrichedPrompt(String context, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        
        // Add context prefix
        prompt.append(CONTEXT_PREFIX);
        
        // Add context content
        prompt.append(context);
        
        // Add separator
        prompt.append(CONTEXT_SEPARATOR);
        
        // Add user message
        prompt.append(userMessage);
        
        return prompt.toString();
    }

    /**
     * Gets the maximum context length configuration.
     * 
     * @return the maximum allowed context length in characters
     */
    public int getMaxContextLength() {
        return MAX_CONTEXT_LENGTH;
    }

    /**
     * Gets the context prefix used in enriched prompts.
     * 
     * @return the standard context prefix string
     */
    public String getContextPrefix() {
        return CONTEXT_PREFIX;
    }

    /**
     * Estimates the token count for a given text.
     * <p>
     * This is a rough estimation using the common rule of ~4 characters per token.
     * Actual token count depends on the specific tokenizer used by the AI model.
     * </p>
     * 
     * @param text the text to estimate tokens for
     * @return estimated token count
     */
    public int estimateTokenCount(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / 4.0);
    }

    /**
     * Estimates token usage for a ContextResult.
     * <p>
     * Provides breakdown of token usage between original message and added context.
     * Useful for monitoring and optimization.
     * </p>
     * 
     * @param result the ContextResult to analyze
     * @return formatted string with token usage breakdown
     */
    public String getTokenUsageEstimate(ContextResult result) {
        if (result == null) {
            return "No result provided";
        }

        int originalTokens = estimateTokenCount(result.originalMessage());
        int enrichedTokens = estimateTokenCount(result.enrichedPrompt());
        int contextTokens = enrichedTokens - originalTokens;

        return String.format("Token estimate: original=%d, context=+%d, total=%d", 
                           originalTokens, contextTokens, enrichedTokens);
    }
}