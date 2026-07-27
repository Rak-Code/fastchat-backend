package com.rakeshgupta.fastchat_backend.document.parser;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Splits extracted document text into fixed-size chunks and selects the most
 * relevant chunks based on substring matching with the user's message.
 * <p>
 * <b>Algorithm Complexity:</b>
 * <ul>
 *   <li>chunkText(): O(n) where n is the text length (single pass)</li>
 *   <li>selectTopChunks(): O(c * m) where c = chunk count, m = message length</li>
 * </ul>
 * <p>
 * Chunk ordering is always preserved from the original document (no reordering
 * by relevance), ensuring the AI receives context in document order.
 */
@Component
public class DocumentChunker {

    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);

    /** Default chunk size in characters. */
    public static final int CHUNK_SIZE = 500;

    /** Default maximum number of chunks to return. */
    public static final int TOP_K = 10;

    /**
     * Splits text into fixed-size chunks of {@value #CHUNK_SIZE} characters.
     * The last chunk may be shorter than the chunk size.
     *
     * @param text the text to chunk (may be null or empty)
     * @return a list of chunks, never null; empty if text is null/blank
     */
    public List<String> chunkText(String text) {
        if (text == null || text.isBlank()) {
            return Collections.emptyList();
        }

        List<String> chunks = new ArrayList<>();
        int length = text.length();

        for (int start = 0; start < length; start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, length);
            chunks.add(text.substring(start, end));
        }

        log.debug("Chunked text of {} characters into {} chunks", length, chunks.size());
        return chunks;
    }

    /**
     * Selects the top-k chunks that contain the user message as a substring
     * (case-insensitive). If fewer than k chunks match, the remaining slots
     * are filled with the first chunks from the document.
     * <p>
     * Results maintain original document order.
     *
     * @param chunks       the list of chunks (may be null or empty)
     * @param userMessage  the user's message to match against (may be null or empty)
     * @return a list of selected chunks, never null; empty if chunks is null/empty
     */
    public List<String> selectTopChunks(List<String> chunks, String userMessage) {
        if (chunks == null || chunks.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> matchingChunks = new ArrayList<>();
        List<String> nonMatchingChunks = new ArrayList<>();
        String lowerMessage = (userMessage != null) ? userMessage.toLowerCase() : "";

        for (String chunk : chunks) {
            if (!lowerMessage.isEmpty() && chunk.toLowerCase().contains(lowerMessage)) {
                matchingChunks.add(chunk);
            } else {
                nonMatchingChunks.add(chunk);
            }
        }

        // Combine: matching chunks first (maintaining order), then fill remaining slots
        List<String> result = new ArrayList<>(matchingChunks);
        int remaining = TOP_K - result.size();
        if (remaining > 0) {
            result.addAll(nonMatchingChunks.subList(0, Math.min(remaining, nonMatchingChunks.size())));
        } else if (result.size() > TOP_K) {
            result = new ArrayList<>(result.subList(0, TOP_K));
        }

        log.debug("Selected {} chunks ({} matching) out of {}", result.size(), matchingChunks.size(), chunks.size());
        return result;
    }

    /**
     * Convenience method that chains {@link #chunkText(String)} and
     * {@link #selectTopChunks(List, String)}.
     *
     * @param text         the document text to chunk and select from
     * @param userMessage  the user's message to match against
     * @return a list of selected chunks
     */
    public List<String> chunkAndSelect(String text, String userMessage) {
        List<String> chunks = chunkText(text);
        return selectTopChunks(chunks, userMessage);
    }
}