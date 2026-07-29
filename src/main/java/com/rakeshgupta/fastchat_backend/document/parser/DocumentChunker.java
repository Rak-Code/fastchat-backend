package com.rakeshgupta.fastchat_backend.document.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Document chunking and selection service for the AI Context Engine.
 * <p>
 * This service implements a two-stage process for handling large documents:
 * </p>
 * <ol>
 *   <li><strong>Chunking:</strong> Splits text into approximately 500-character segments</li>
 *   <li><strong>Selection:</strong> Uses substring matching to find the most relevant chunks</li>
 * </ol>
 * 
 * <h3>Chunking Algorithm:</h3>
 * <ul>
 *   <li>Target chunk size: 500 characters</li>
 *   <li>Linear time complexity: O(n) where n = text length</li>
 *   <li>Preserves word boundaries when possible</li>
 *   <li>Maintains original text order</li>
 * </ul>
 * 
 * <h3>Selection Algorithm:</h3>
 * <ul>
 *   <li>Simple case-insensitive substring matching</li>
 *   <li>Returns top 10 most relevant chunks (TOP_K)</li>
 *   <li>Fallback to first 10 chunks if no matches found</li>
 *   <li>Preserves original chunk ordering (no reordering by relevance)</li>
 *   <li>Time complexity: O(n*m) where n = chunk count, m = average chunk length</li>
 * </ul>
 * 
 * <h3>Future Enhancements (Phase 2):</h3>
 * <ul>
 *   <li>Semantic search with vector embeddings</li>
 *   <li>TF-IDF scoring for better relevance</li>
 *   <li>Smart boundary detection (sentences, paragraphs)</li>
 *   <li>Configurable chunk sizes based on document type</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * @Autowired
 * private DocumentChunker chunker;
 * 
 * public List<String> getRelevantContent(String documentText, String userQuery) {
 *     return chunker.chunkAndSelect(documentText, userQuery);
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    /**
     * Target size for each text chunk in characters.
     * Chosen to balance context richness with processing efficiency.
     */
    public static final int CHUNK_SIZE = 500;

    /**
     * Number of top relevant chunks to return.
     * Limits context size while providing sufficient information.
     */
    public static final int TOP_K = 10;

    /**
     * Chunks text into approximately 500-character segments.
     * <p>
     * This method splits the input text into chunks of approximately CHUNK_SIZE
     * characters each. The algorithm preserves the original text structure and
     * handles edge cases gracefully.
     * </p>
     * 
     * <h4>Algorithm Details:</h4>
     * <ul>
     *   <li>Iterates through text in CHUNK_SIZE increments</li>
     *   <li>Last chunk may be smaller than CHUNK_SIZE</li>
     *   <li>Preserves all characters including whitespace and line breaks</li>
     *   <li>No word boundary detection in Phase 1 (simple character-based)</li>
     * </ul>
     * 
     * @param text the text to chunk (may be null or empty)
     * @return list of text chunks, each approximately CHUNK_SIZE characters (never null)
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isEmpty()) {
            log.debug("Input text is null or empty - returning empty chunk list");
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int textLength = text.length();
        
        log.debug("Chunking text of {} characters into ~{} character segments", textLength, CHUNK_SIZE);

        for (int i = 0; i < textLength; i += CHUNK_SIZE) {
            int endIndex = Math.min(i + CHUNK_SIZE, textLength);
            String chunk = text.substring(i, endIndex);
            chunks.add(chunk);
        }

        log.info("Text chunked into {} segments (original length: {} chars)", chunks.size(), textLength);
        
        if (log.isDebugEnabled()) {
            logChunkingStats(chunks);
        }

        return chunks;
    }

    /**
     * Selects top K chunks containing user message keywords.
     * <p>
     * This method uses simple substring matching to find chunks that are most
     * likely relevant to the user's query. The selection preserves original
     * ordering to maintain context flow.
     * </p>
     * 
     * <h4>Selection Strategy:</h4>
     * <ol>
     *   <li>Convert user message to lowercase for case-insensitive matching</li>
     *   <li>Find all chunks containing the user message as a substring</li>
     *   <li>If matches found, return up to TOP_K matches in original order</li>
     *   <li>If no matches found, return first TOP_K chunks as fallback</li>
     *   <li>If fewer than TOP_K chunks total, return all chunks</li>
     * </ol>
     * 
     * @param chunks all available chunks (may be null or empty)
     * @param userMessage the user's message for keyword matching (may be null or empty)
     * @return top K relevant chunks in original order (never null, max TOP_K elements)
     */
    public List<String> selectTopChunks(List<String> chunks, String userMessage) {
        if (chunks == null || chunks.isEmpty()) {
            log.debug("No chunks provided for selection - returning empty list");
            return Collections.emptyList();
        }

        if (userMessage == null || userMessage.trim().isEmpty()) {
            log.debug("No user message provided - returning first {} chunks", TOP_K);
            return chunks.stream().limit(TOP_K).collect(Collectors.toList());
        }

        String searchTerm = userMessage.toLowerCase().trim();
        log.debug("Selecting relevant chunks from {} total using search term: '{}'", chunks.size(), searchTerm);

        // Find chunks containing the user message (case-insensitive)
        List<String> matchingChunks = chunks.stream()
                .filter(chunk -> chunk.toLowerCase().contains(searchTerm))
                .collect(Collectors.toList());

        List<String> selectedChunks;
        if (!matchingChunks.isEmpty()) {
            // Use matching chunks, limited to TOP_K
            selectedChunks = matchingChunks.stream().limit(TOP_K).collect(Collectors.toList());
            log.info("Found {} matching chunks for '{}', selected {} chunks", 
                    matchingChunks.size(), searchTerm, selectedChunks.size());
        } else {
            // Fallback: use first TOP_K chunks
            selectedChunks = chunks.stream().limit(TOP_K).collect(Collectors.toList());
            log.info("No keyword matches found for '{}' - using first {} chunks as fallback", 
                    searchTerm, selectedChunks.size());
        }

        if (log.isDebugEnabled()) {
            logSelectionStats(chunks.size(), matchingChunks.size(), selectedChunks.size(), searchTerm);
        }

        return selectedChunks;
    }

    /**
     * Convenience method: chunks text and selects relevant pieces in one call.
     * <p>
     * This method combines chunking and selection into a single operation,
     * which is the most common use case in the AI Context Engine.
     * </p>
     * 
     * @param text the text to chunk and select from (may be null or empty)
     * @param userMessage the user's message for relevance matching (may be null or empty)
     * @return top K relevant chunks in original order (never null, max TOP_K elements)
     */
    public List<String> chunkAndSelect(String text, String userMessage) {
        log.debug("Performing combined chunk-and-select operation");
        
        List<String> chunks = chunkText(text);
        return selectTopChunks(chunks, userMessage);
    }

    /**
     * Logs detailed statistics about the chunking process for debugging.
     * 
     * @param chunks the list of chunks to analyze
     */
    private void logChunkingStats(List<String> chunks) {
        if (chunks.isEmpty()) {
            return;
        }

        int totalChars = chunks.stream().mapToInt(String::length).sum();
        double avgChunkSize = (double) totalChars / chunks.size();
        int minChunkSize = chunks.stream().mapToInt(String::length).min().orElse(0);
        int maxChunkSize = chunks.stream().mapToInt(String::length).max().orElse(0);

        log.debug("Chunking statistics: {} chunks, avg={:.1f} chars, min={} chars, max={} chars", 
                 chunks.size(), avgChunkSize, minChunkSize, maxChunkSize);
    }

    /**
     * Logs detailed statistics about the chunk selection process for debugging.
     * 
     * @param totalChunks total number of chunks available
     * @param matchingChunks number of chunks containing search term
     * @param selectedChunks number of chunks actually selected
     * @param searchTerm the search term used for matching
     */
    private void logSelectionStats(int totalChunks, int matchingChunks, int selectedChunks, String searchTerm) {
        double matchRate = totalChunks > 0 ? (double) matchingChunks / totalChunks * 100 : 0;
        log.debug("Selection statistics: {}/{} chunks matched '{}' ({:.1f}% match rate), {} selected", 
                 matchingChunks, totalChunks, searchTerm, matchRate, selectedChunks);
    }

    /**
     * Returns the configured chunk size.
     * 
     * @return the target chunk size in characters
     */
    public int getChunkSize() {
        return CHUNK_SIZE;
    }

    /**
     * Returns the configured top-K value.
     * 
     * @return the maximum number of chunks to select
     */
    public int getTopK() {
        return TOP_K;
    }
}