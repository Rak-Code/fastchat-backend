package com.rakeshgupta.fastchat_backend.service;

import com.rakeshgupta.fastchat_backend.context.builder.ContextResult;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ChatService {

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

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(message)
                .call()
                .content();
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

        // Process file context
        ContextResult contextResult = fileContextService.processFileContext(file, message);
        String promptToSend = contextResult.enrichedPrompt();

        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(promptToSend)
                .call()
                .content();
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
        chatMemory.clear(conversationId);
    }
}
