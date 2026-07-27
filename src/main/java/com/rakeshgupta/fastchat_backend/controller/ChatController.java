package com.rakeshgupta.fastchat_backend.controller;

import com.rakeshgupta.fastchat_backend.dto.ChatRequest;
import com.rakeshgupta.fastchat_backend.dto.ChatResponse;
import com.rakeshgupta.fastchat_backend.service.ChatService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);

    private final ChatService chatService;

    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }

    /**
     * Chat endpoint for JSON requests (backward compatible).
     * This method handles standard JSON POST requests without file uploads.
     */
    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ChatResponse chatJson(@Valid @RequestBody ChatRequest request) {
        log.info("Chat request (JSON) - conversationId: {}, message length: {}",
                request.conversationId(), request.message().length());

        String reply = chatService.chat(request.conversationId(), request.message());
        return new ChatResponse(request.conversationId(), reply);
    }

    /**
     * Chat endpoint for multipart/form-data requests (supports file uploads).
     * <p>
     * When a file is provided, the file content is extracted and used as context
     * for the AI response. When no file is provided, behaves identically to JSON endpoint.
     *
     * @param conversationId the conversation identifier (required)
     * @param message        the user message (required, max 4000 chars)
     * @param file           the uploaded file (optional; supports txt, text, md, markdown, pdf, docx)
     * @return the AI response
     */
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatMultipart(
            @RequestParam("conversationId") String conversationId,
            @RequestParam("message") String message,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        log.info("Chat request (multipart) - conversationId: {}, message length: {}, file present: {}",
                conversationId, message.length(), file != null && !file.isEmpty());

        String reply = chatService.chat(conversationId, message, file);
        return new ChatResponse(conversationId, reply);
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
