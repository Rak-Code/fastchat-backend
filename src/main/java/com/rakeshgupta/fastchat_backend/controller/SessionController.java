package com.rakeshgupta.fastchat_backend.controller;

import com.rakeshgupta.fastchat_backend.dto.SessionResponse;
import com.rakeshgupta.fastchat_backend.service.ChatService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@Validated
@RestController
@RequestMapping("/api")
public class SessionController {

    private final ChatService chatService;

    public SessionController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Create a new conversation session ID (no auth).
     */
    @PostMapping("/session")
    public SessionResponse createSession() {
        return new SessionResponse(UUID.randomUUID().toString());
    }

    /**
     * Clear memory for a conversation ID.
     */
    @DeleteMapping("/session/{conversationId}")
    public ResponseEntity<Void> deleteSession(@PathVariable @NotBlank String conversationId) {
        chatService.clearConversation(conversationId);
        return ResponseEntity.noContent().build();
    }
}
