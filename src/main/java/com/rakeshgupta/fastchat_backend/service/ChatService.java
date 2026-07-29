package com.rakeshgupta.fastchat_backend.service;

import com.rakeshgupta.fastchat_backend.context.builder.ContextResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final FileContextService fileContextService;

    public ChatService(ChatClient chatClient, ChatMemory chatMemory, FileContextService fileContextService) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.fileContextService = fileContextService;
    }

    public String chat(String conversationId, String message) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("message must not be blank");
        }

        log.info("Processing chat request for conversation: {} with message length: {}", conversationId, message.length());

        try {
            String response = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(message)
                    .call()
                    .content();
            
            log.debug("Chat response generated successfully for conversation: {}", conversationId);
            return response;
        } catch (Exception e) {
            log.error("Error processing chat request for conversation: {} - {}", conversationId, e.getMessage(), e);
            throw new RuntimeException("Failed to process chat request: " + e.getMessage(), e);
        }
    }

    /**
     * Chat method supporting file upload context. Processes the uploaded file,
     * enriches the prompt with extracted text context, and sends to the AI.
     * <p>
     * The original (unenriched) message is stored in conversation memory to maintain
     * clean conversation history without file context artifacts.
     *
     * @param conversationId the conversation identifier (must not be blank)
     * @param message        the user message (must not be blank)
     * @param file           the uploaded file (may be null for backward compatibility)
     * @return the AI response
     */
    public String chat(String conversationId, String message, MultipartFile file) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("message must not be blank");
        }

        log.info("Processing multipart chat request for conversation: {} with message length: {}, file present: {}", 
                conversationId, message.length(), file != null && !file.isEmpty());

        try {
            // Process file context
            ContextResult contextResult = fileContextService.processFileContext(file, message);
            String promptToSend = contextResult.enrichedPrompt();

            log.debug("File context processed, prompt length: {}", promptToSend.length());

            String response = chatClient.prompt()
                    .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                    .user(promptToSend)
                    .call()
                    .content();
            
            log.debug("Multipart chat response generated successfully for conversation: {}", conversationId);
            return response;
        } catch (Exception e) {
            log.error("Error processing multipart chat request for conversation: {} - {}", conversationId, e.getMessage(), e);
            throw new RuntimeException("Failed to process multipart chat request: " + e.getMessage(), e);
        }
    }

    /**
     * Streaming chat method for Server-Sent Events (SSE) - DISABLED
     * Returns a reactive stream of tokens for real-time chat responses
     */
    /*
    public Flux<String> streamChat(String conversationId, String message) {
        if (!StringUtils.hasText(conversationId)) {
            return Flux.error(new IllegalArgumentException("conversationId must not be blank"));
        }
        if (!StringUtils.hasText(message)) {
            return Flux.error(new IllegalArgumentException("message must not be blank"));
        }

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .stream()
                .content();
    }
    */

    public void clearConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        
        log.info("Clearing conversation: {}", conversationId);
        chatMemory.clear(conversationId);
    }
}
