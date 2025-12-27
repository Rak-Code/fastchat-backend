package com.rakeshgupta.fastchat_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChatRequest(
        @NotBlank(message = "conversationId is required")
        String conversationId,

        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be <= 4000 characters")
        String message
) { }
