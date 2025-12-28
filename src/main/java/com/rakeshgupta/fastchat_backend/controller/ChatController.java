package com.rakeshgupta.fastchat_backend.controller;

import com.rakeshgupta.fastchat_backend.dto.ChatRequest;
import com.rakeshgupta.fastchat_backend.dto.ChatResponse;
import com.rakeshgupta.fastchat_backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Chat endpoint: uses conversationId to isolate memory via advisor parameters.
     */
    @PostMapping("/chat")
    public ChatResponse chat(@Valid @RequestBody ChatRequest request) {
        String reply = chatService.chat(request.conversationId(), request.message());
        return new ChatResponse(request.conversationId(), reply);
    }

    /**
     * Streaming chat endpoint using Server-Sent Events (SSE) - DISABLED
     * Returns a reactive stream of tokens for real-time chat responses
     */
    /*
    @PostMapping(
            value = "/chat/stream",
            produces = MediaType.TEXT_EVENT_STREAM_VALUE
    )
    public Flux<String> streamChat(@Valid @RequestBody ChatRequest request) {
        return chatService.streamChat(
                request.conversationId(),
                request.message()
        );
    }
    */
}
