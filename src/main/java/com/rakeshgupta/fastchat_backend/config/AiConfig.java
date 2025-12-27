package com.rakeshgupta.fastchat_backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.memory.ChatMemoryRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * AI configuration:
 * - ChatMemory backed by JDBC ChatMemoryRepository (PostgreSQL)
 * - MessageWindowChatMemory strategy (keep last N messages; system preserved by design)
 * - MessageChatMemoryAdvisor attached as default advisor to ChatClient
 */
@Configuration
public class AiConfig {

    @Bean
    public ChatMemory chatMemory(
            ChatMemoryRepository chatMemoryRepository,
            @Value("${MEMORY_MAX_MESSAGES:20}") int maxMessages
    ) {
        return MessageWindowChatMemory.builder()
                .chatMemoryRepository(chatMemoryRepository)
                .maxMessages(maxMessages)
                .build();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder, ChatMemory chatMemory) {
        return builder
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
                .defaultSystem("""
                        You are a helpful, concise chatbot.
                        Answer clearly and directly. If you don't know, say so.
                        """)
                .build();
    }
}
