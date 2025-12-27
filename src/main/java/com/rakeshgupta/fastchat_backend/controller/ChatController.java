package com.rakeshgupta.fastchat_backend.controller;

import com.rakeshgupta.fastchat_backend.dto.ChatRequest;
import com.rakeshgupta.fastchat_backend.dto.ChatResponse;
import com.rakeshgupta.fastchat_backend.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

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
}
