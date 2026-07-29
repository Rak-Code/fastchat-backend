package com.rakeshgupta.fastchat_backend.controller;

import com.rakeshgupta.fastchat_backend.dto.ChatRequest;
import com.rakeshgupta.fastchat_backend.dto.ChatResponse;
import com.rakeshgupta.fastchat_backend.service.ChatService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
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
     * This endpoint supports file uploads for context-enhanced AI responses. When a file
     * is provided, its content is extracted and used to enrich the AI prompt. The system
     * supports various file formats including plain text, Markdown, PDF, and Word documents.
     * </p>
     * 
     * <h3>Supported File Types:</h3>
     * <ul>
     *   <li>Plain text: .txt, .text</li>
     *   <li>Markdown: .md, .markdown</li>
     *   <li>PDF: .pdf (future enhancement)</li>
     *   <li>Word: .docx (future enhancement)</li>
     * </ul>
     * 
     * <h3>File Size Limits:</h3>
     * <ul>
     *   <li>Maximum file size: 10MB</li>
     *   <li>Files exceeding limit are rejected with HTTP 413</li>
     *   <li>Empty files are accepted but ignored</li>
     * </ul>
     * 
     * <h3>Example Usage:</h3>
     * <pre>
     * curl -X POST http://localhost:8080/api/chat \
     *   -F "conversationId=user-123" \
     *   -F "message=What are my key skills?" \
     *   -F "file=@resume.txt"
     * </pre>
     *
     * @param conversationId the conversation identifier (required, not blank)
     * @param message the user message (required, not blank, max 4000 chars)
     * @param file the uploaded file (optional; null/empty files are handled gracefully)
     * @return ChatResponse containing the AI's response with file context if applicable
     */
    @PostMapping(value = "/chat", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ChatResponse chatMultipart(
            @RequestParam("conversationId") @NotBlank(message = "conversationId is required") String conversationId,
            @RequestParam("message") @NotBlank(message = "message is required") 
            @Size(max = 4000, message = "message must be <= 4000 characters") String message,
            @RequestParam(value = "file", required = false) MultipartFile file) {

        log.info("Chat request (multipart) - conversationId: {}, message length: {}, file: {}",
                conversationId, message.length(), 
                file != null && !file.isEmpty() 
                    ? String.format("%s (%d bytes)", file.getOriginalFilename(), file.getSize())
                    : "none");

        String reply = chatService.chat(conversationId, message, file);
        return new ChatResponse(conversationId, reply);
    }

    /**
     * Clears conversation history for a specific conversation ID.
     * <p>
     * This endpoint removes all stored messages and context for the specified
     * conversation. This is useful for starting fresh conversations or clearing
     * sensitive information from memory.
     * </p>
     * 
     * @param conversationId the conversation identifier to clear (required, not blank)
     */
    @DeleteMapping("/conversation/{conversationId}")
    public void clearConversation(
            @PathVariable @NotBlank(message = "conversationId is required") String conversationId) {
        
        log.info("Clearing conversation: {}", conversationId);
        chatService.clearConversation(conversationId);
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
