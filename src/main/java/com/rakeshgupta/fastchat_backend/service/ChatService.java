package com.rakeshgupta.fastchat_backend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;

@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;

    public ChatService(ChatClient chatClient, ChatMemory chatMemory) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
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
     * Streaming chat method for Server-Sent Events (SSE)
     * Returns a reactive stream of tokens for real-time chat responses
     */
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

    public void clearConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        chatMemory.clear(conversationId);
    }
}
