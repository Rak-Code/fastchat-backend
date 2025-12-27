package com.rakeshgupta.fastchat_backend.service;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    public void clearConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        chatMemory.clear(conversationId);
    }
}
