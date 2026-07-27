# Design Document: AI Context Engine

## Executive Summary

This document presents the architectural design for the AI Context Engine feature, which enables FastChat to process uploaded files as contextual input during conversations. The design follows SOLID principles and uses Strategy and Builder patterns to create a modular, extensible system that preserves existing conversation memory functionality while adding powerful file-based context capabilities.

**Key Design Decisions:**
- **Preservation**: Existing JdbcChatMemoryRepository and ChatService conversation memory remain unchanged
- **Strategy Pattern**: FileExtractor implementations for different file formats (TXT, PDF, DOCX, Markdown)
- **Builder Pattern**: ContextBuilder for composing context-enriched prompts
- **Single Responsibility**: Small, focused services with clear responsibilities
- **Extensibility**: Easy addition of new file formats through FileExtractorFactory
- **Future-Ready**: Architecture supports semantic search capability (Phase 2)

## Architecture Overview

### System Context

```
┌─────────────────────────────────────────────────────────────────┐
│                      FastChat Backend                            │
│                                                                   │
│  ┌────────────┐         ┌──────────────────────────────────┐   │
│  │            │         │    AI Context Engine              │   │
│  │  Chat      │         │                                    │   │
│  │  Controller├────────►│  ┌──────────────────────────┐    │   │
│  │            │         │  │  FileContextService       │    │   │
│  └────────────┘         │  └──────────┬───────────────┘    │   │
│                          │             │                     │   │
│                          │  ┌──────────▼───────────────┐    │   │
│                          │  │  FileExtractorFactory    │    │   │
│                          │  └──────────┬───────────────┘    │   │
│                          │             │                     │   │
│                          │     ┌───────┴───────┐            │   │
│                          │     │   Extractors  │            │   │
│                          │     │  TXT│PDF│DOCX │            │   │
│                          │     └─────────────────            │   │
│                          │             │                     │   │
│                          │  ┌──────────▼───────────────┐    │   │
│                          │  │  DocumentChunker         │    │   │
│                          │  └──────────────────────────┘    │   │
│                          │             │                     │   │
│                          │  ┌──────────▼───────────────┐    │   │
│                          │  │  ContextBuilder          │    │   │
│                          │  └──────────────────────────┘    │   │
│                          └──────────────────────────────────┘   │
│                                                                   │
│  ┌────────────┐         ┌──────────────────────────────────┐   │
│  │            │         │    Existing Components            │   │
│  │  Chat      │◄────────┤  (PRESERVED - NO CHANGES)        │   │
│  │  Service   │         │                                    │   │
│  │            │         │  - JdbcChatMemoryRepository       │   │
│  └────────────┘         │  - MessageWindowChatMemory        │   │
│                          │  - ChatClient with Advisors       │   │
│                          └──────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────┘
```

### Package Structure

```
com.rakeshgupta.fastchat_backend
├── context                           # AI Context Engine (NEW)
│   ├── builder
│   │   ├── ContextBuilder.java       # Builder for context-enriched prompts
│   │   └── ContextResult.java        # Record for builder output
│   ├── conversation
│   │   └── ConversationContextProvider.java  # Future: conversation context
│   ├── memory
│   │   └── MemoryContextProvider.java        # Future: memory-based context
│   └── file
│       ├── FileContextService.java           # Main orchestrator
│       ├── FileExtractor.java                # Strategy interface
│       ├── FileExtractorFactory.java         # Factory for extractors
│       ├── TxtFileExtractor.java             # Plain text implementation
│       ├── PdfFileExtractor.java             # PDF implementation
│       ├── DocxFileExtractor.java            # Word doc implementation
│       └── MarkdownFileExtractor.java        # Markdown implementation
├── document                          # Document processing (NEW)
│   ├── parser
│   │   ├── DocumentChunker.java              # 500 char chunking
│   │   └── ChunkingStrategy.java             # Interface for future strategies
├── common                            # Shared utilities (NEW)
│   ├── exception
│   │   ├── FileProcessingException.java
│   │   └── UnsupportedFileTypeException.java
│   └── config
│       └── FileContextConfig.java            # Spring configuration
├── controller                        # Existing controllers (MODIFIED)
│   └── ChatController.java                   # Add MultipartFile support
├── dto                               # Existing DTOs (MODIFIED)
│   └── ChatRequest.java                      # Add file field
├── service                           # Existing services (MODIFIED)
│   └── ChatService.java                      # Add file context integration
└── config                            # Existing config (UNCHANGED)
    ├── AiConfig.java
    ├── CorsConfig.java
    └── RestClientConfig.java
```


## Component Design

### 1. FileExtractor (Strategy Interface)

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Purpose:** Defines the strategy interface for extracting text from different file formats.

**Interface Definition:**
```java
public interface FileExtractor {
    /**
     * Extracts text content from the provided input stream.
     * 
     * @param inputStream the file content as an InputStream
     * @param filename the original filename (for context/logging)
     * @return extracted text content, or empty string if extraction fails
     * @throws FileProcessingException if critical extraction error occurs
     */
    String extractText(InputStream inputStream, String filename);
    
    /**
     * Returns the file extensions supported by this extractor.
     * 
     * @return list of supported extensions (e.g., ["txt", "text"])
     */
    List<String> getSupportedExtensions();
}
```

**Design Notes:**
- All implementations MUST handle encoding gracefully
- All implementations MUST NOT throw unchecked exceptions (wrap in FileProcessingException)
- Empty string return indicates extraction failure (logged, not thrown)
- Preserve line breaks and formatting characters

### 2. FileExtractor Implementations

#### 2.1 TxtFileExtractor

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Responsibilities:**
- Extract text from plain text files (.txt, .text)
- Handle multiple encodings (UTF-8, ISO-8859-1, Windows-1252)
- Preserve line breaks and whitespace

**Implementation Strategy:**
```java
@Component
public class TxtFileExtractor implements FileExtractor {
    private static final Logger log = LoggerFactory.getLogger(TxtFileExtractor.class);
    private static final List<Charset> FALLBACK_CHARSETS = List.of(
        StandardCharsets.UTF_8,
        StandardCharsets.ISO_8859_1,
        Charset.forName("Windows-1252")
    );
    
    @Override
    public String extractText(InputStream inputStream, String filename) {
        // Try UTF-8 first, then fallback charsets
        // Use BufferedReader for efficient reading
        // Log encoding detection
        // Return empty string on IOException, log error
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("txt", "text");
    }
}
```


#### 2.2 PdfFileExtractor

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Responsibilities:**
- Extract text from PDF files (.pdf)
- Handle multi-page documents
- Preserve paragraph structure

**Dependencies:**
- Apache PDFBox 3.0.x (add to pom.xml)

**Implementation Strategy:**
```java
@Component
public class PdfFileExtractor implements FileExtractor {
    private static final Logger log = LoggerFactory.getLogger(PdfFileExtractor.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) {
        // Use PDFBox PDDocument.load()
        // Use PDFTextStripper for text extraction
        // Close document in finally block
        // Log page count
        // Return empty string on IOException, log error
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("pdf");
    }
}
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.2</version>
</dependency>
```

#### 2.3 DocxFileExtractor

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Responsibilities:**
- Extract text from Microsoft Word documents (.docx)
- Handle paragraphs, tables, and headers
- Preserve document structure

**Dependencies:**
- Apache POI 5.x (add to pom.xml)

**Implementation Strategy:**
```java
@Component
public class DocxFileExtractor implements FileExtractor {
    private static final Logger log = LoggerFactory.getLogger(DocxFileExtractor.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) {
        // Use Apache POI XWPFDocument
        // Extract paragraphs with XWPFParagraph
        // Extract tables with XWPFTable
        // Join with line breaks
        // Close document in finally block
        // Return empty string on IOException, log error
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("docx");
    }
}
```

**Maven Dependency:**
```xml
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>
```


#### 2.4 MarkdownFileExtractor

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Responsibilities:**
- Extract text from Markdown files (.md, .markdown)
- Preserve Markdown formatting (headers, lists, code blocks)
- No rendering required (keep as plain Markdown)

**Implementation Strategy:**
```java
@Component
public class MarkdownFileExtractor implements FileExtractor {
    private static final Logger log = LoggerFactory.getLogger(MarkdownFileExtractor.class);
    
    @Override
    public String extractText(InputStream inputStream, String filename) {
        // Use same logic as TxtFileExtractor
        // Markdown is plain text, preserve as-is
        // No special parsing needed
    }
    
    @Override
    public List<String> getSupportedExtensions() {
        return List.of("md", "markdown");
    }
}
```

### 3. FileExtractorFactory

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Purpose:** Factory for selecting the appropriate FileExtractor based on file extension.

**Responsibilities:**
- Register all FileExtractor implementations via dependency injection
- Select extractor by file extension
- Return Optional<FileExtractor> for unsupported types
- Log available extractors at startup

**Class Design:**
```java
@Component
public class FileExtractorFactory {
    private static final Logger log = LoggerFactory.getLogger(FileExtractorFactory.class);
    private final Map<String, FileExtractor> extractorMap;
    
    public FileExtractorFactory(List<FileExtractor> extractors) {
        // Build map: extension -> extractor
        // Log registered extractors
    }
    
    public Optional<FileExtractor> getExtractor(String filename) {
        // Extract extension from filename
        // Convert to lowercase
        // Return Optional.ofNullable(extractorMap.get(extension))
    }
    
    public Set<String> getSupportedExtensions() {
        // Return all registered extensions
    }
}
```

**Design Notes:**
- Uses Spring's List<FileExtractor> injection to auto-discover all implementations
- Thread-safe (immutable map)
- Case-insensitive extension matching
- O(1) lookup performance


### 4. DocumentChunker

**Package:** `com.rakeshgupta.fastchat_backend.document.parser`

**Purpose:** Chunks extracted text into ~500 character segments and selects top 10 relevant chunks.

**Responsibilities:**
- Split text into approximately 500-character chunks
- Select top 10 chunks via substring matching
- Preserve original chunk ordering
- Handle edge cases (text < 500 chars, no matches)

**Class Design:**
```java
@Component
public class DocumentChunker {
    private static final Logger log = LoggerFactory.getLogger(DocumentChunker.class);
    private static final int CHUNK_SIZE = 500;
    private static final int TOP_K = 10;
    
    /**
     * Chunks text into approximately 500-character segments.
     * 
     * @param text the text to chunk
     * @return list of chunks, each approximately 500 characters
     */
    public List<String> chunkText(String text) {
        // Handle null/empty text -> empty list
        // Split into chunks of CHUNK_SIZE
        // Last chunk may be smaller
        // Preserve line breaks within chunks
    }
    
    /**
     * Selects top 10 chunks containing user message keywords.
     * Uses simple substring matching (case-insensitive).
     * Falls back to first 10 chunks if no matches found.
     * Preserves original ordering.
     * 
     * @param chunks all available chunks
     * @param userMessage the user's message for keyword matching
     * @return top 10 relevant chunks in original order
     */
    public List<String> selectTopChunks(List<String> chunks, String userMessage) {
        // Handle null/empty inputs -> empty list
        // Convert userMessage to lowercase for matching
        // Filter chunks containing any word from userMessage
        // If matches.size() >= TOP_K, take first TOP_K matches
        // If matches.size() < TOP_K, return all matches
        // If no matches, return first TOP_K chunks (or all if fewer)
        // Maintain original chunk ordering (no reordering)
    }
    
    /**
     * Convenience method: chunk and select in one call.
     * 
     * @param text the text to chunk
     * @param userMessage the user's message for keyword matching
     * @return top 10 relevant chunks in original order
     */
    public List<String> chunkAndSelect(String text, String userMessage) {
        List<String> chunks = chunkText(text);
        return selectTopChunks(chunks, userMessage);
    }
}
```

**Algorithm Details:**

**Chunking Algorithm:**
```
Input: text (String)
Output: List<String> chunks

1. If text is null or empty, return empty list
2. chunks = new ArrayList<>()
3. For i = 0 to text.length() step CHUNK_SIZE:
     chunk = text.substring(i, min(i + CHUNK_SIZE, text.length()))
     chunks.add(chunk)
4. Return chunks

Complexity: O(n) where n = text.length()
```


**Selection Algorithm:**
```
Input: chunks (List<String>), userMessage (String)
Output: List<String> topChunks

1. If chunks is null or empty, return empty list
2. If userMessage is null or empty, return first min(TOP_K, chunks.size()) chunks
3. messageLower = userMessage.toLowerCase()
4. matches = new ArrayList<>()
5. For each chunk in chunks:
     chunkLower = chunk.toLowerCase()
     if chunkLower.contains(messageLower):
         matches.add(chunk)
6. If matches.size() >= TOP_K:
     return matches.subList(0, TOP_K)
7. If matches.size() > 0:
     return matches
8. Else:
     return chunks.subList(0, min(TOP_K, chunks.size()))

Complexity: O(n * m) where n = chunks.size(), m = average chunk length
```

**Design Notes:**
- Simple substring matching (Phase 1)
- Future enhancement: semantic search with vector embeddings (Phase 2)
- No chunk reordering to maintain context flow
- Fallback to first 10 chunks ensures consistent behavior

### 5. ContextBuilder

**Package:** `com.rakeshgupta.fastchat_backend.context.builder`

**Purpose:** Builds context-enriched prompts using the Builder pattern.

**Responsibilities:**
- Prepend file context to user messages
- Truncate context to 5000 characters
- Format context with "File Context:\n" prefix
- Return both original message and enriched prompt

**Class Design:**
```java
@Component
public class ContextBuilder {
    private static final Logger log = LoggerFactory.getLogger(ContextBuilder.class);
    private static final int MAX_CONTEXT_LENGTH = 5000;
    private static final String CONTEXT_PREFIX = "File Context:\n";
    
    /**
     * Builds a context-enriched prompt from file context and user message.
     * 
     * @param fileContext extracted file context (may be null or empty)
     * @param userMessage the user's original message
     * @return ContextResult containing original message and enriched prompt
     */
    public ContextResult buildPrompt(String fileContext, String userMessage) {
        // If fileContext is null or blank, return unchanged message
        // Truncate fileContext to MAX_CONTEXT_LENGTH if needed
        // Build enriched prompt: CONTEXT_PREFIX + truncatedContext + "\n\n" + userMessage
        // Return new ContextResult(userMessage, enrichedPrompt)
    }
    
    /**
     * Builds a context-enriched prompt from chunks and user message.
     * Joins chunks with double newlines.
     * 
     * @param chunks selected context chunks
     * @param userMessage the user's original message
     * @return ContextResult containing original message and enriched prompt
     */
    public ContextResult buildPromptFromChunks(List<String> chunks, String userMessage) {
        // If chunks is null or empty, return unchanged message
        // Join chunks with "\n\n"
        // Delegate to buildPrompt(joinedChunks, userMessage)
    }
}
```


**ContextResult Record:**
```java
package com.rakeshgupta.fastchat_backend.context.builder;

/**
 * Result of context building containing both original message and enriched prompt.
 * 
 * @param originalMessage the user's original message (unchanged)
 * @param enrichedPrompt the context-enriched prompt to send to AI
 */
public record ContextResult(
    String originalMessage,
    String enrichedPrompt
) {
    /**
     * Returns true if context was added (enriched differs from original).
     */
    public boolean hasContext() {
        return !originalMessage.equals(enrichedPrompt);
    }
}
```

**Design Notes:**
- Immutable record for thread-safety
- Preserves original message for storage in conversation memory
- Enriched prompt used for AI request
- hasContext() helper for logging/debugging

### 6. FileContextService

**Package:** `com.rakeshgupta.fastchat_backend.context.file`

**Purpose:** Main orchestrator for file context processing. Coordinates extraction, chunking, and context building.

**Responsibilities:**
- Validate MultipartFile input
- Delegate extraction to FileExtractorFactory
- Coordinate chunking via DocumentChunker
- Build enriched prompts via ContextBuilder
- Handle all errors gracefully
- Log all operations

**Class Design:**
```java
@Service
public class FileContextService {
    private static final Logger log = LoggerFactory.getLogger(FileContextService.class);
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024; // 10MB
    
    private final FileExtractorFactory extractorFactory;
    private final DocumentChunker documentChunker;
    private final ContextBuilder contextBuilder;
    
    public FileContextService(
        FileExtractorFactory extractorFactory,
        DocumentChunker documentChunker,
        ContextBuilder contextBuilder
    ) {
        this.extractorFactory = extractorFactory;
        this.documentChunker = documentChunker;
        this.contextBuilder = contextBuilder;
    }
    
    /**
     * Processes uploaded file and builds context-enriched prompt.
     * 
     * @param file the uploaded file (may be null)
     * @param userMessage the user's original message
     * @return ContextResult with enriched prompt, or unchanged message if no file
     */
    public ContextResult processFileContext(MultipartFile file, String userMessage) {
        // If file is null or empty, return unchanged message
        // Validate file size <= MAX_FILE_SIZE
        // Get filename and extract extension
        // Get extractor from factory
        // If no extractor found, log warning and return unchanged message
        // Extract text using extractor
        // If extraction returns empty, log info and return unchanged message
        // Chunk and select top chunks
        // Build enriched prompt
        // Log success with file info
        // Return ContextResult
        
        // Catch all exceptions, log error, return unchanged message (graceful degradation)
    }
}
```


**Error Handling Strategy:**
- File size exceeds limit → log warning, return unchanged message
- Unsupported file type → log warning, return unchanged message
- Extraction fails → log error, return unchanged message
- Any unexpected exception → log error with stack trace, return unchanged message
- **Goal:** Never fail the chat request due to file processing errors

**Design Notes:**
- All dependencies injected via constructor for testability
- Graceful degradation: errors never break the chat flow
- Extensive logging for debugging
- Single responsibility: orchestration only, delegates all work

### 7. ChatService Enhancement

**Package:** `com.rakeshgupta.fastchat_backend.service`

**Purpose:** Enhanced to support file context while preserving existing conversation memory.

**Modifications:**
```java
@Service
public class ChatService {
    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    
    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final FileContextService fileContextService; // NEW
    
    public ChatService(
        ChatClient chatClient,
        ChatMemory chatMemory,
        FileContextService fileContextService // NEW
    ) {
        this.chatClient = chatClient;
        this.chatMemory = chatMemory;
        this.fileContextService = fileContextService;
    }
    
    /**
     * Enhanced chat method with optional file context support.
     * 
     * @param conversationId the conversation ID for memory isolation
     * @param message the user's message
     * @param file optional file upload for context (may be null)
     * @return AI response
     */
    public String chat(String conversationId, String message, MultipartFile file) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        if (!StringUtils.hasText(message)) {
            throw new IllegalArgumentException("message must not be blank");
        }
        
        // NEW: Process file context
        ContextResult contextResult = fileContextService.processFileContext(file, message);
        
        // Use enriched prompt for AI, but store original message in memory
        return chatClient.prompt()
                .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, conversationId))
                .user(contextResult.enrichedPrompt()) // NEW: use enriched prompt
                .call()
                .content();
    }
    
    /**
     * Backward compatible chat method without file support.
     */
    public String chat(String conversationId, String message) {
        return chat(conversationId, message, null);
    }
    
    // clearConversation() remains unchanged
    public void clearConversation(String conversationId) {
        if (!StringUtils.hasText(conversationId)) {
            throw new IllegalArgumentException("conversationId must not be blank");
        }
        chatMemory.clear(conversationId);
    }
}
```


**Key Design Notes:**
- Overloaded chat() method: new signature with file, old signature for backward compatibility
- File context processed BEFORE ChatClient call
- Original message stored in conversation memory (not enriched prompt)
- Existing conversation memory flow unchanged
- Zero breaking changes to existing API

### 8. Controller Enhancement

**Package:** `com.rakeshgupta.fastchat_backend.controller`

**Purpose:** Enhanced to accept file uploads via multipart/form-data.

**Modifications:**
```java
@RestController
@RequestMapping("/api")
public class ChatController {
    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    
    private final ChatService chatService;
    
    public ChatController(ChatService chatService) {
        this.chatService = chatService;
    }
    
    /**
     * Enhanced chat endpoint with optional file upload.
     * Accepts multipart/form-data with fields:
     * - conversationId (String, required)
     * - message (String, required)
     * - file (MultipartFile, optional)
     */
    @PostMapping(value = "/chat", consumes = {
        MediaType.APPLICATION_JSON_VALUE,
        MediaType.MULTIPART_FORM_DATA_VALUE
    })
    public ChatResponse chat(
        @RequestParam("conversationId") @NotBlank String conversationId,
        @RequestParam("message") @NotBlank @Size(max = 4000) String message,
        @RequestParam(value = "file", required = false) MultipartFile file
    ) {
        log.info("Chat request: conversationId={}, hasFile={}", 
                 conversationId, file != null && !file.isEmpty());
        
        String reply = chatService.chat(conversationId, message, file);
        return new ChatResponse(conversationId, reply);
    }
}
```

**Design Notes:**
- Dual content type support: JSON (backward compatible) and multipart/form-data (new)
- File parameter is optional (required = false)
- Validation via Jakarta annotations
- Spring automatically handles multipart parsing
- File size limit enforced by Spring Boot configuration (application.yml)

**Configuration Required (application.yml):**
```yaml
spring:
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
```


### 9. Exception Handling

**Package:** `com.rakeshgupta.fastchat_backend.common.exception`

**FileProcessingException:**
```java
/**
 * Exception thrown when file processing fails.
 * This is a checked exception to force explicit error handling.
 */
public class FileProcessingException extends RuntimeException {
    private final String filename;
    
    public FileProcessingException(String message, String filename) {
        super(message);
        this.filename = filename;
    }
    
    public FileProcessingException(String message, String filename, Throwable cause) {
        super(message, cause);
        this.filename = filename;
    }
    
    public String getFilename() {
        return filename;
    }
}
```

**UnsupportedFileTypeException:**
```java
/**
 * Exception thrown when uploaded file type is not supported.
 */
public class UnsupportedFileTypeException extends RuntimeException {
    private final String filename;
    private final String extension;
    
    public UnsupportedFileTypeException(String filename, String extension) {
        super(String.format("Unsupported file type: %s (extension: %s)", filename, extension));
        this.filename = filename;
        this.extension = extension;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public String getExtension() {
        return extension;
    }
}
```

**GlobalExceptionHandler Enhancement:**
```java
@RestControllerAdvice
public class GlobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);
    
    // Existing handlers remain unchanged
    
    // NEW: Handle file size exceeded
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Map<String, String>> handleMaxUploadSize(
        MaxUploadSizeExceededException ex
    ) {
        log.warn("File upload size exceeded: {}", ex.getMessage());
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                .body(Map.of("error", "File size exceeds maximum limit of 10MB"));
    }
    
    // NEW: Handle file processing errors (should rarely occur due to graceful degradation)
    @ExceptionHandler(FileProcessingException.class)
    public ResponseEntity<Map<String, String>> handleFileProcessing(
        FileProcessingException ex
    ) {
        log.error("File processing failed: {}", ex.getMessage(), ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", "Failed to process uploaded file: " + ex.getMessage()));
    }
}
```


### 10. Spring Configuration

**Package:** `com.rakeshgupta.fastchat_backend.common.config`

**FileContextConfig:**
```java
@Configuration
public class FileContextConfig {
    private static final Logger log = LoggerFactory.getLogger(FileContextConfig.class);
    
    /**
     * All FileExtractor beans are automatically discovered via @Component scanning.
     * This configuration class is for future customization if needed.
     */
    
    @PostConstruct
    public void logConfiguration() {
        log.info("File Context Engine initialized");
    }
}
```

**Existing AiConfig (No Changes Required):**
- ChatMemory and ChatClient configuration remains unchanged
- JdbcChatMemoryRepository auto-configured by Spring AI
- Conversation memory flow preserved

**application.yml Additions:**
```yaml
spring:
  # Existing configuration remains unchanged
  
  # NEW: Multipart file upload configuration
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
      file-size-threshold: 1MB
      location: ${java.io.tmpdir}

# NEW: File context configuration (for future enhancements)
app:
  file-context:
    chunk-size: 500
    top-k-chunks: 10
    max-context-length: 5000
    supported-formats: txt,pdf,docx,md,markdown
```

## Sequence Diagrams

### Sequence 1: Chat with File Upload

```
┌────────┐      ┌──────────────┐      ┌─────────────┐      ┌────────────────────┐      ┌──────────────┐      ┌─────────────┐
│ Client │      │ChatController│      │ ChatService │      │FileContextService │      │   Extractor  │      │ ChatClient  │
└───┬────┘      └──────┬───────┘      └──────┬──────┘      └─────────┬──────────┘      └──────┬───────┘      └──────┬──────┘
    │                   │                     │                       │                        │                     │
    │ POST /api/chat    │                     │                       │                        │                     │
    │ (multipart/form)  │                     │                       │                        │                     │
    ├──────────────────>│                     │                       │                        │                     │
    │                   │                     │                       │                        │                     │
    │                   │ chat(id, msg, file) │                       │                        │                     │
    │                   ├────────────────────>│                       │                        │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │ processFileContext()  │                        │                     │
    │                   │                     ├──────────────────────>│                        │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │ getExtractor(filename) │                     │
    │                   │                     │                       ├───────────────────────>│                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │<───────────────────────┤                     │
    │                   │                     │                       │  TxtFileExtractor      │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │ extractText()          │                     │
    │                   │                     │                       ├───────────────────────>│                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │<───────────────────────┤                     │
    │                   │                     │                       │  "extracted text..."   │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │ [chunk and select]     │                     │
    │                   │                     │                       │────────┐               │                     │
    │                   │                     │                       │        │               │                     │
    │                   │                     │                       │<───────┘               │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │ [build enriched prompt]│                     │
    │                   │                     │                       │────────┐               │                     │
    │                   │                     │                       │        │               │                     │
    │                   │                     │                       │<───────┘               │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │<──────────────────────┤                        │                     │
    │                   │                     │  ContextResult        │                        │                     │
    │                   │                     │                       │                        │                     │
    │                   │                     │ prompt().user(enrichedPrompt).call()           │                     │
    │                   │                     ├────────────────────────────────────────────────────────────────────>│
    │                   │                     │                       │                        │                     │
    │                   │                     │                       │                        │  [ChatMemory stores │
    │                   │                     │                       │                        │   original message] │
    │                   │                     │                       │                        │                     │
    │                   │                     │<────────────────────────────────────────────────────────────────────┤
    │                   │                     │  "AI response..."     │                        │                     │
    │                   │                     │                       │                        │                     │
    │                   │<────────────────────┤                       │                        │                     │
    │                   │  "AI response..."   │                       │                        │                     │
    │                   │                     │                       │                        │                     │
    │<──────────────────┤                     │                       │                        │                     │
    │ ChatResponse      │                     │                       │                        │                     │
    │                   │                     │                       │                        │                     │
```


### Sequence 2: File Extraction Flow (Detailed)

```
┌──────────────────────┐    ┌─────────────────────┐    ┌────────────────┐    ┌─────────────────┐
│FileContextService    │    │FileExtractorFactory │    │  FileExtractor  │    │DocumentChunker  │
└──────────┬───────────┘    └──────────┬──────────┘    └────────┬───────┘    └────────┬────────┘
           │                           │                         │                     │
           │ processFileContext()      │                         │                     │
           ├────────┐                  │                         │                     │
           │        │ validate file    │                         │                     │
           │<───────┘                  │                         │                     │
           │                           │                         │                     │
           │ getExtractor("test.txt")  │                         │                     │
           ├──────────────────────────>│                         │                     │
           │                           │                         │                     │
           │                           ├────────┐                │                     │
           │                           │        │ lookup by ext  │                     │
           │                           │<───────┘                │                     │
           │                           │                         │                     │
           │<──────────────────────────┤                         │                     │
           │  Optional<TxtFileExtractor>│                        │                     │
           │                           │                         │                     │
           │ extractText(inputStream, filename)                  │                     │
           ├─────────────────────────────────────────────────────>│                     │
           │                           │                         │                     │
           │                           │                         ├────────┐            │
           │                           │                         │        │ read text  │
           │                           │                         │<───────┘            │
           │                           │                         │                     │
           │<─────────────────────────────────────────────────────┤                     │
           │  "extracted text content..."                        │                     │
           │                           │                         │                     │
           │ chunkAndSelect(text, userMessage)                   │                     │
           ├─────────────────────────────────────────────────────────────────────────>│
           │                           │                         │                     │
           │                           │                         │                     ├────────┐
           │                           │                         │                     │        │ chunk
           │                           │                         │                     │<───────┘
           │                           │                         │                     │
           │                           │                         │                     ├────────┐
           │                           │                         │                     │        │ select
           │                           │                         │                     │<───────┘
           │                           │                         │                     │
           │<─────────────────────────────────────────────────────────────────────────┤
           │  List<String> topChunks   │                         │                     │
           │                           │                         │                     │
           │ [build enriched prompt]   │                         │                     │
           ├────────┐                  │                         │                     │
           │        │                  │                         │                     │
           │<───────┘                  │                         │                     │
           │                           │                         │                     │
           │ return ContextResult      │                         │                     │
           │                           │                         │                     │
```

### Sequence 3: Chat without File (Backward Compatible)

```
┌────────┐      ┌──────────────┐      ┌─────────────┐      ┌────────────────────┐      ┌─────────────┐
│ Client │      │ChatController│      │ ChatService │      │FileContextService │      │ ChatClient  │
└───┬────┘      └──────┬───────┘      └──────┬──────┘      └─────────┬──────────┘      └──────┬──────┘
    │                   │                     │                       │                        │
    │ POST /api/chat    │                     │                       │                        │
    │ (JSON, no file)   │                     │                       │                        │
    ├──────────────────>│                     │                       │                        │
    │                   │                     │                       │                        │
    │                   │ chat(id, msg, null) │                       │                        │
    │                   ├────────────────────>│                       │                        │
    │                   │                     │                       │                        │
    │                   │                     │ processFileContext(null, msg)                  │
    │                   │                     ├──────────────────────>│                        │
    │                   │                     │                       │                        │
    │                   │                     │                       ├────────┐               │
    │                   │                     │                       │        │ file is null  │
    │                   │                     │                       │<───────┘ return unchanged
    │                   │                     │                       │                        │
    │                   │                     │<──────────────────────┤                        │
    │                   │                     │  ContextResult        │                        │
    │                   │                     │  (original message)   │                        │
    │                   │                     │                       │                        │
    │                   │                     │ prompt().user(message).call()                  │
    │                   │                     ├───────────────────────────────────────────────>│
    │                   │                     │                       │                        │
    │                   │                     │                       │  [Same as before -     │
    │                   │                     │                       │   no file context]     │
    │                   │                     │                       │                        │
    │                   │                     │<───────────────────────────────────────────────┤
    │                   │                     │  "AI response..."     │                        │
    │                   │                     │                       │                        │
    │                   │<────────────────────┤                       │                        │
    │                   │  "AI response..."   │                       │                        │
    │                   │                     │                       │                        │
    │<──────────────────┤                     │                       │                        │
    │ ChatResponse      │                     │                       │                        │
    │                   │                     │                       │                        │
```


## Correctness Properties

*A property is a characteristic or behavior that should hold true across all valid executions of a system—essentially, a formal statement about what the system should do. Properties serve as the bridge between human-readable specifications and machine-verifiable correctness guarantees.*

### Property Reflection

After analyzing all acceptance criteria in the requirements document, the following properties were identified. Redundant properties have been eliminated:

**Eliminated Redundancies:**
- FR1.2 combined with FR1.1 (both test provider selection)
- FR2.1 classified as integration test (end-to-end behavior)
- AC1.2 is duplicate of FR1.3 (unsupported file handling)
- AC1.4 is duplicate of FR2.5 (character preservation)
- AC3.1 is duplicate of FR4.3 (backward compatibility)
- FR5.3 is duplicate of FR4.4 (validation at different layer)

**Remaining Properties:**
The following universal properties comprehensively validate the AI Context Engine:

### Property 1: Provider Selection by File Extension

*For any* file with a registered extension, the FileExtractorFactory SHALL select the correct FileExtractor implementation based on the file extension (case-insensitive).

**Validates: Requirements FR1.1, FR1.2**

**Test Strategy:** Generate random file extensions that are registered in the factory, verify that getExtractor() returns the correct FileExtractor type.

### Property 2: Graceful Degradation for Unsupported Files

*For any* file with an unsupported extension, the FileContextService SHALL log a warning and return an unchanged ContextResult (no context added) without throwing exceptions.

**Validates: Requirements FR1.3, AC1.2**

**Test Strategy:** Generate random unsupported file extensions, verify that processFileContext() returns a ContextResult where enrichedPrompt equals originalMessage, and verify warning is logged.

### Property 3: Exception Safety in Text Extraction

*For any* InputStream (including malformed or corrupted data), the FileExtractor implementations SHALL NOT throw unchecked exceptions, but instead return empty string and log the error.

**Validates: Requirements FR1.5**

**Test Strategy:** Generate random byte arrays and corrupt InputStreams, pass to each FileExtractor implementation, verify no unchecked exceptions are thrown and empty string is returned.


### Property 4: Extraction Error Handling

*For any* file that causes extraction failure, the FileContextService SHALL log the error and return an unchanged ContextResult (graceful degradation) without propagating the exception to the caller.

**Validates: Requirements FR2.2**

**Test Strategy:** Create files that cause extraction errors (corrupted, wrong encoding, etc.), verify that processFileContext() returns unchanged message and error is logged.

### Property 5: Formatting Preservation

*For any* text file containing line breaks, tabs, and formatting characters, the FileExtractor SHALL preserve these characters in the extracted text.

**Validates: Requirements FR2.5, AC1.4**

**Test Strategy:** Generate random text with various line breaks (\n, \r\n), tabs (\t), and formatting characters, write to file, extract, and verify all formatting is preserved in output.

### Property 6: Context Prepending Structure

*For any* non-empty file context, the ContextBuilder SHALL prepend the context with "File Context:\n" prefix followed by the context text, then "\n\n", then the user message.

**Validates: Requirements FR3.1, FR3.3**

**Test Strategy:** Generate random file contexts and user messages, verify that buildPrompt() returns a ContextResult where enrichedPrompt starts with "File Context:\n" and contains the user message at the end.

### Property 7: Context Truncation

*For any* file context exceeding 5000 characters, the ContextBuilder SHALL truncate the context to exactly 5000 characters before building the enriched prompt.

**Validates: Requirements FR3.2**

**Test Strategy:** Generate random file contexts over 5000 characters (e.g., 6000, 10000, 50000), verify that the context portion in enrichedPrompt is exactly 5000 characters (excluding prefix and user message).

### Property 8: Empty Context Passthrough

*For any* request with null or empty file context, the ContextBuilder SHALL return a ContextResult where enrichedPrompt equals the original user message (unchanged).

**Validates: Requirements FR3.4**

**Test Strategy:** Generate random user messages, call buildPrompt() with null or empty context, verify that enrichedPrompt equals originalMessage.

### Property 9: Context Result Completeness

*For any* call to buildPrompt(), the ContextBuilder SHALL return a ContextResult containing both the original user message and the context-enriched prompt.

**Validates: Requirements FR3.5**

**Test Strategy:** Generate random contexts and messages, verify that ContextResult contains both fields and originalMessage is unchanged from input.


### Property 10: ConversationId Validation

*For any* conversationId that is null, blank, or contains only whitespace, the ChatService SHALL throw IllegalArgumentException before processing the request.

**Validates: Requirements FR4.4, FR5.3**

**Test Strategy:** Generate random invalid conversationIds (null, "", "   ", "\t\n"), call chat() method, verify IllegalArgumentException is thrown.

### Property 11: File Upload Validation

*For any* invalid file upload request (wrong content type, missing parameters, validation errors), the ChatController SHALL return HTTP 400 Bad Request with descriptive error details.

**Validates: Requirements FR5.4**

**Test Strategy:** Generate random invalid requests (missing conversationId, message too long, etc.), make POST request, verify 400 response with error message.

### Property 12: Chunking Size Consistency

*For any* text content, the DocumentChunker SHALL produce chunks where each chunk (except possibly the last) is approximately 500 characters.

**Validates: Requirements AC2.1**

**Test Strategy:** Generate random text of varying lengths (600, 1500, 5000, 10000 chars), call chunkText(), verify all chunks except last are 500 characters, and last chunk is <= 500.

### Property 13: Chunk Selection by Substring Matching

*For any* set of chunks and user message, the selectTopChunks() method SHALL use simple substring matching (case-insensitive) to find chunks containing the user message keywords.

**Validates: Requirements AC2.3**

**Test Strategy:** Generate random chunks where some contain keywords from the user message, call selectTopChunks(), verify that returned chunks contain the user message as a substring (case-insensitive).

### Property 14: Chunk Ordering Preservation

*For any* set of chunks selected by selectTopChunks(), the returned chunks SHALL maintain their original ordering from the source text without reordering.

**Validates: Requirements AC2.5**

**Test Strategy:** Generate random chunks with matches at positions 2, 5, 8, etc., call selectTopChunks(), verify returned chunks are in order [2, 5, 8, ...] not reordered by relevance score.

### Property 15: Backward Compatibility

*For any* chat request without a file upload (file parameter is null), the ChatService SHALL behave identically to the original implementation, using only the user message without file context processing.

**Validates: Requirements FR4.3, AC3.1, FR5.5**

**Test Strategy:** Make chat requests with file=null, compare behavior to original ChatService (same prompt sent to ChatClient, same conversation memory updates).


## Data Models

### ContextResult
```java
package com.rakeshgupta.fastchat_backend.context.builder;

/**
 * Immutable record containing both original message and context-enriched prompt.
 */
public record ContextResult(
    String originalMessage,
    String enrichedPrompt
) {
    public boolean hasContext() {
        return !originalMessage.equals(enrichedPrompt);
    }
}
```

### ChatRequest (Modified)
```java
package com.rakeshgupta.fastchat_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Note: For multipart requests, use @RequestParam in controller instead of this DTO.
 * This DTO remains for JSON-only backward compatible requests.
 */
public record ChatRequest(
        @NotBlank(message = "conversationId is required")
        String conversationId,

        @NotBlank(message = "message is required")
        @Size(max = 4000, message = "message must be <= 4000 characters")
        String message
) { }
```

### ChatResponse (Unchanged)
```java
package com.rakeshgupta.fastchat_backend.dto;

public record ChatResponse(String conversationId, String reply) { }
```

## Database Schema

**No changes required to database schema.** The existing JdbcChatMemoryRepository schema remains unchanged:

```sql
-- Existing Spring AI chat_memory_store table (auto-created)
CREATE TABLE IF NOT EXISTS chat_memory_store (
    id VARCHAR(255) PRIMARY KEY,
    conversation_id VARCHAR(255) NOT NULL,
    message_type VARCHAR(50) NOT NULL,
    content TEXT NOT NULL,
    metadata TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_conversation_id ON chat_memory_store(conversation_id);
```

**Design Note:** File context is NOT stored in the database. Only the original user message and AI response are persisted. File context is ephemeral and used only for the current request.


## Testing Strategy

### Unit Testing Approach

**FileExtractor Implementations:**
- Test text extraction with valid files
- Test encoding handling (UTF-8, ISO-8859-1, etc.)
- Test error handling with corrupted files
- Test formatting preservation (line breaks, tabs)
- Mock InputStreams for isolated testing

**FileExtractorFactory:**
- Test extractor registration and lookup
- Test case-insensitive extension matching
- Test unknown extension handling
- Verify all extractors are discovered

**DocumentChunker:**
- Test chunking with various text lengths
- Test edge cases (empty text, text < 500 chars, exact multiples of 500)
- Test chunk selection with matching keywords
- Test fallback to first 10 chunks when no matches
- Test ordering preservation

**ContextBuilder:**
- Test prompt building with various context sizes
- Test truncation at 5000 characters
- Test prefix formatting
- Test empty context passthrough
- Test ContextResult creation

**FileContextService:**
- Test end-to-end file processing (mock dependencies)
- Test error handling and graceful degradation
- Test file size validation
- Test unsupported file type handling
- Mock FileExtractorFactory, DocumentChunker, ContextBuilder

**ChatService:**
- Test chat with file context (mock FileContextService)
- Test chat without file (backward compatibility)
- Test conversationId validation
- Test message validation
- Verify ChatClient receives enriched prompt

### Property-Based Testing

**Configuration:**
- Minimum 100 iterations per property test
- Use libraries: JUnit 5 + jqwik (Java property-based testing)
- Tag format: `@Tag("property")` and `@Tag("ai-context-engine")`

**Property Test Examples:**

```java
@Property
@Tag("property")
@Tag("ai-context-engine")
@Label("Property 1: Provider Selection by File Extension")
void providerSelectionByExtension(@ForAll("registeredExtensions") String extension) {
    // Feature: ai-context-engine, Property 1: Provider Selection by File Extension
    String filename = "test." + extension;
    Optional<FileExtractor> extractor = factory.getExtractor(filename);
    
    assertTrue(extractor.isPresent());
    assertTrue(extractor.get().getSupportedExtensions().contains(extension.toLowerCase()));
}

@Property
@Tag("property")
@Tag("ai-context-engine")
@Label("Property 7: Context Truncation")
void contextTruncation(@ForAll @StringLength(min = 5001, max = 50000) String longContext,
                       @ForAll @StringLength(min = 1, max = 100) String userMessage) {
    // Feature: ai-context-engine, Property 7: Context Truncation
    ContextResult result = contextBuilder.buildPrompt(longContext, userMessage);
    
    String enriched = result.enrichedPrompt();
    String prefix = "File Context:\n";
    assertTrue(enriched.startsWith(prefix));
    
    // Extract context portion (between prefix and user message)
    String contextPortion = enriched.substring(prefix.length(), 
                                                enriched.length() - userMessage.length() - 2);
    assertEquals(5000, contextPortion.length());
}
```


### Integration Testing

**Scope:** Integration tests verify end-to-end behavior with real components but mocked external dependencies (ChatClient, database).

**Test Scenarios:**
1. **Chat with file upload** - Verify entire flow from controller to ChatClient
2. **Chat without file** - Verify backward compatibility
3. **Unsupported file type** - Verify graceful degradation
4. **File size limit** - Verify 10MB limit enforcement
5. **Conversation memory** - Verify original message stored, not enriched prompt
6. **Multiple conversations** - Verify conversationId isolation

**Integration Test Example:**
```java
@SpringBootTest
@AutoConfigureMockMvc
@Tag("integration")
@Tag("ai-context-engine")
class FileContextIntegrationTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private ChatClient chatClient;
    
    @Test
    void chatWithTextFileUpload() throws Exception {
        // Arrange
        MockMultipartFile file = new MockMultipartFile(
            "file", "test.txt", "text/plain", 
            "This is test content.".getBytes()
        );
        
        when(chatClient.prompt()).thenReturn(/* mock prompt builder */);
        
        // Act
        mockMvc.perform(multipart("/api/chat")
                .file(file)
                .param("conversationId", "test-123")
                .param("message", "What is this about?"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.conversationId").value("test-123"))
                .andExpect(jsonPath("$.reply").exists());
        
        // Assert
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(chatClient).prompt().user(promptCaptor.capture());
        
        String capturedPrompt = promptCaptor.getValue();
        assertTrue(capturedPrompt.contains("File Context:"));
        assertTrue(capturedPrompt.contains("This is test content."));
        assertTrue(capturedPrompt.contains("What is this about?"));
    }
}
```

### Test Coverage Targets

- **Unit Test Coverage:** 80% minimum for all new components
- **Property Test Coverage:** All 15 correctness properties implemented
- **Integration Test Coverage:** All critical end-to-end flows
- **Edge Case Coverage:** All edge cases from requirements

## Performance Considerations

### Expected Performance Characteristics

**File Extraction:**
- TXT files: < 100ms for files up to 5MB
- PDF files: < 500ms for files up to 5MB (depends on page count)
- DOCX files: < 300ms for files up to 5MB
- Markdown files: < 100ms for files up to 5MB (same as TXT)

**Chunking:**
- Linear time complexity: O(n) where n = text length
- Expected: < 50ms for 10MB text

**Chunk Selection:**
- Substring matching: O(n * m) where n = chunk count, m = chunk length
- Expected: < 100ms for 1000 chunks

**Total Processing Time:**
- Target: < 2 seconds for 5MB files (per NFR2.1)
- Logging if processing exceeds 2 seconds


### Memory Usage

**File Storage:**
- Files stored in memory only during request processing
- No persistent file storage
- Files garbage collected after response sent

**Context Storage:**
- Extracted text stored temporarily during request
- Maximum context: 5000 characters in enriched prompt
- Cleared after ChatClient call completes

**Peak Memory Estimate:**
- 10MB file upload: ~20MB peak (file + extracted text)
- Acceptable for JVM with 512MB+ heap

### Scalability Considerations

**Current Design (Phase 1):**
- Synchronous file processing
- One file per request
- In-memory processing only

**Future Enhancements (Phase 2+):**
- Async file processing with CompletableFuture
- Multiple file support with parallel processing
- File caching with Redis/database
- Vector embeddings for semantic search
- Streaming file uploads for large files

## Security Considerations

### Input Validation

1. **File Size:** Limited to 10MB via Spring Boot configuration
2. **File Type:** Validated via FileExtractorFactory (whitelist approach)
3. **ConversationId:** Validated as non-blank
4. **Message:** Validated with max length 4000 characters

### File Processing Security

1. **No Code Execution:** File extractors only read content, never execute
2. **InputStream Handling:** All streams closed properly in finally blocks
3. **Encoding Safety:** Multiple encoding fallbacks for text files
4. **Error Isolation:** Extraction errors don't propagate to caller

### Data Privacy

1. **No File Persistence:** Files not stored on disk or database
2. **Memory Cleanup:** Files cleared from memory after processing
3. **Conversation Isolation:** conversationId ensures memory isolation
4. **No Context Logging:** File content not logged (only metadata)

### Potential Vulnerabilities & Mitigations

| Vulnerability | Mitigation |
|--------------|------------|
| XML External Entity (XXE) in DOCX | Apache POI configured with XXE protection |
| Zip Bomb in DOCX/PDF | File size limit + timeout on extraction |
| Malformed PDF DoS | Try-catch around PDFBox, 2-second timeout |
| Memory exhaustion | File size limit + max context truncation |
| Path traversal in filename | Filename only used for logging, not file operations |


## Error Handling Strategy

### Error Categories

**1. Validation Errors (HTTP 400)**
- Missing conversationId or message
- Message exceeds 4000 characters
- Invalid request format
- **Action:** Return error to client immediately

**2. File Size Errors (HTTP 413)**
- File exceeds 10MB limit
- **Action:** Return 413 Payload Too Large

**3. Unsupported File Type (Graceful Degradation)**
- File extension not registered
- **Action:** Log warning, continue without context

**4. Extraction Errors (Graceful Degradation)**
- Corrupted file
- Encoding issues
- PDF/DOCX parsing failures
- **Action:** Log error, continue without context

**5. Processing Timeout (Graceful Degradation)**
- Extraction exceeds 2 seconds
- **Action:** Log warning, continue with partial or no context

**6. Unexpected Errors (HTTP 500)**
- Rare: should not occur due to extensive error handling
- **Action:** Log error with stack trace, return generic error

### Logging Strategy

**Log Levels:**
- **INFO:** Successful file processing, context added
- **WARN:** Unsupported file type, file too large, processing timeout
- **ERROR:** Extraction failures, unexpected exceptions

**Log Format:**
```
INFO  FileContextService - File processed successfully: filename=doc.txt, size=1234, chunks=5, hasContext=true
WARN  FileContextService - Unsupported file type: filename=image.png, extension=png
ERROR FileContextService - File extraction failed: filename=corrupt.pdf, error=Invalid PDF format
```

**Sensitive Data:**
- Never log file content
- Log only metadata: filename, size, extension, chunk count
- Sanitize filenames in logs (remove special characters)

## Dependencies & Maven Configuration

### Required Dependencies

**New Dependencies to Add:**
```xml
<!-- Apache PDFBox for PDF extraction -->
<dependency>
    <groupId>org.apache.pdfbox</groupId>
    <artifactId>pdfbox</artifactId>
    <version>3.0.2</version>
</dependency>

<!-- Apache POI for DOCX extraction -->
<dependency>
    <groupId>org.apache.poi</groupId>
    <artifactId>poi-ooxml</artifactId>
    <version>5.2.5</version>
</dependency>

<!-- Property-based testing -->
<dependency>
    <groupId>net.jqwik</groupId>
    <artifactId>jqwik</artifactId>
    <version>1.8.2</version>
    <scope>test</scope>
</dependency>
```

**Existing Dependencies (No Changes):**
- Spring Boot 3.5.9
- Spring AI 1.1.2
- PostgreSQL JDBC Driver
- Jakarta Validation API
- JUnit 5


## Implementation Plan

### Phase 1: Core Infrastructure (Priority 1)

**Sprint 1.1: Strategy Pattern & Factory**
- [ ] Create FileExtractor interface
- [ ] Implement TxtFileExtractor with encoding handling
- [ ] Implement MarkdownFileExtractor (reuse TXT logic)
- [ ] Create FileExtractorFactory with auto-discovery
- [ ] Unit tests for extractors and factory
- [ ] Property tests for provider selection (Properties 1, 2, 3)

**Sprint 1.2: Document Processing**
- [ ] Implement DocumentChunker with chunking algorithm
- [ ] Implement chunk selection with substring matching
- [ ] Unit tests for chunking and selection
- [ ] Property tests for chunking (Properties 12, 13, 14)
- [ ] Edge case tests (empty text, exact multiples, no matches)

**Sprint 1.3: Context Building**
- [ ] Create ContextResult record
- [ ] Implement ContextBuilder with truncation
- [ ] Unit tests for context building
- [ ] Property tests for context building (Properties 6, 7, 8, 9)

### Phase 2: Service Integration (Priority 1)

**Sprint 2.1: File Context Service**
- [ ] Implement FileContextService orchestrator
- [ ] Add file size validation
- [ ] Implement error handling and logging
- [ ] Unit tests with mocked dependencies
- [ ] Property tests for error handling (Property 4)

**Sprint 2.2: ChatService Enhancement**
- [ ] Add FileContextService injection to ChatService
- [ ] Implement overloaded chat() method with file parameter
- [ ] Add conversationId validation
- [ ] Unit tests for enhanced ChatService
- [ ] Property tests for validation (Property 10, 15)

### Phase 3: API Layer (Priority 1)

**Sprint 3.1: Controller Enhancement**
- [ ] Modify ChatController to accept MultipartFile
- [ ] Add multipart/form-data support
- [ ] Update GlobalExceptionHandler for file errors
- [ ] Controller unit tests
- [ ] Property tests for validation (Property 11)

**Sprint 3.2: Configuration**
- [ ] Add multipart configuration to application.yml
- [ ] Create FileContextConfig
- [ ] Update pom.xml with new dependencies
- [ ] Integration tests for full flow

### Phase 4: Advanced Extractors (Priority 2)

**Sprint 4.1: PDF Support**
- [ ] Add Apache PDFBox dependency
- [ ] Implement PdfFileExtractor
- [ ] Handle multi-page documents
- [ ] Unit tests for PDF extraction
- [ ] Integration tests with real PDFs

**Sprint 4.2: DOCX Support**
- [ ] Add Apache POI dependency
- [ ] Implement DocxFileExtractor
- [ ] Handle paragraphs and tables
- [ ] Unit tests for DOCX extraction
- [ ] Integration tests with real DOCX files

### Phase 5: Testing & Refinement (Priority 1)

**Sprint 5.1: Comprehensive Testing**
- [ ] Complete all 15 property-based tests
- [ ] Integration tests for all file types
- [ ] Performance tests (verify < 2 second processing)
- [ ] Security tests (file size limits, malformed files)
- [ ] Backward compatibility tests

**Sprint 5.2: Documentation & Deployment**
- [ ] JavaDoc for all public APIs
- [ ] Update API documentation
- [ ] Deployment guide
- [ ] Monitoring and logging setup


## Migration & Deployment

### Database Migration

**No database migration required.** The existing `chat_memory_store` table schema remains unchanged.

### Application Configuration

**Changes to application.yml:**
```yaml
spring:
  # Add multipart configuration
  servlet:
    multipart:
      enabled: true
      max-file-size: 10MB
      max-request-size: 10MB
      file-size-threshold: 1MB
      location: ${java.io.tmpdir}

# Add file context configuration
app:
  file-context:
    chunk-size: 500
    top-k-chunks: 10
    max-context-length: 5000
    supported-formats: txt,pdf,docx,md,markdown
```

### Deployment Strategy

**Zero-Downtime Deployment:**
1. Deploy new version with file context feature
2. Existing endpoints remain backward compatible
3. Clients can start using file upload immediately
4. No breaking changes to existing API

**Rollback Plan:**
- Remove multipart configuration
- Existing chat functionality continues to work
- File upload requests will fail gracefully (no file context)

### Monitoring

**Metrics to Monitor:**
- File upload rate (requests/min with files)
- File processing time (avg, p95, p99)
- File extraction failures (count by file type)
- Context truncation frequency
- Memory usage during file processing

**Alerts to Configure:**
- File processing time > 2 seconds (warning)
- File extraction failure rate > 5% (critical)
- Memory usage > 80% (warning)

## Future Enhancements (Phase 2+)

### Semantic Search (Vector Embeddings)

**Goal:** Replace substring matching with semantic similarity

**Design Changes:**
- Add vector embedding service (OpenAI Embeddings API)
- Store chunk embeddings in PostgreSQL with pgvector extension
- Implement cosine similarity for chunk selection
- Cache embeddings for repeated file uploads

**Benefits:**
- Better context relevance (semantic vs. keyword matching)
- Support for paraphrased queries
- Improved AI response quality

### Multi-File Support

**Goal:** Process multiple files in one request

**Design Changes:**
- Change MultipartFile to List<MultipartFile> in controller
- Process files in parallel using CompletableFuture
- Merge contexts from all files
- Add file source metadata to context

### File Caching

**Goal:** Cache extracted content for repeated uploads

**Design Changes:**
- Add file hash calculation (SHA-256)
- Store extracted text in Redis with TTL
- Check cache before extraction
- Eviction policy: LRU with 1 hour TTL

### Context Summarization

**Goal:** Reduce token usage for long files

**Design Changes:**
- Add summarization service using ChatClient
- Summarize chunks before building context
- Configurable: use full context vs. summarized
- Target: 50% token reduction for files > 5000 chars


## Appendix

### A. Technology Choices

**Why Strategy Pattern for FileExtractor?**
- Easy to add new file formats without modifying existing code
- Each extractor encapsulates format-specific logic
- Open/Closed Principle: open for extension, closed for modification

**Why Builder Pattern for ContextBuilder?**
- Clear separation of context construction logic
- Immutable ContextResult output
- Future extensibility for complex context composition

**Why Apache PDFBox and POI?**
- Industry-standard libraries for PDF and Office formats
- Well-maintained with active communities
- Good performance and memory efficiency
- Comprehensive format support

**Why Substring Matching for Phase 1?**
- Simple to implement and test
- O(n*m) complexity acceptable for Phase 1
- Provides baseline functionality
- Easy to replace with semantic search in Phase 2

### B. Design Alternatives Considered

**Alternative 1: Store File Context in Database**
- **Pros:** Persistent context, reusable across sessions
- **Cons:** Schema changes, storage costs, complexity
- **Decision:** Use in-memory for Phase 1, consider for Phase 2

**Alternative 2: Modify ChatRequest DTO Instead of @RequestParam**
- **Pros:** Type-safe DTO, validation at DTO level
- **Cons:** Breaking change to existing API, requires clients to update
- **Decision:** Use @RequestParam for backward compatibility

**Alternative 3: Use Apache Tika for Universal Extraction**
- **Pros:** Single library for all formats
- **Cons:** Large dependency, less control, overkill for 4 formats
- **Decision:** Use format-specific libraries for better control

**Alternative 4: Store Enriched Prompt in Conversation Memory**
- **Pros:** Complete context preserved in history
- **Cons:** File context pollutes conversation history, increases token usage in subsequent requests
- **Decision:** Store only original message, not enriched prompt

### C. Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    FileContextService                       │
├─────────────────────────────────────────────────────────────┤
│ - extractorFactory: FileExtractorFactory                    │
│ - documentChunker: DocumentChunker                          │
│ - contextBuilder: ContextBuilder                            │
├─────────────────────────────────────────────────────────────┤
│ + processFileContext(file, message): ContextResult          │
└──────────────┬──────────────────────────────────────────────┘
               │
               │ uses
               ▼
┌─────────────────────────────────────────────────────────────┐
│              FileExtractorFactory                           │
├─────────────────────────────────────────────────────────────┤
│ - extractorMap: Map<String, FileExtractor>                  │
├─────────────────────────────────────────────────────────────┤
│ + getExtractor(filename): Optional<FileExtractor>           │
│ + getSupportedExtensions(): Set<String>                     │
└──────────────┬──────────────────────────────────────────────┘
               │
               │ manages
               ▼
┌─────────────────────────────────────────────────────────────┐
│                    <<interface>>                            │
│                    FileExtractor                            │
├─────────────────────────────────────────────────────────────┤
│ + extractText(inputStream, filename): String                │
│ + getSupportedExtensions(): List<String>                    │
└──────────────┬──────────────────────────────────────────────┘
               │
               │ implemented by
               │
       ┌───────┴───────────────┬────────────────┬──────────────┐
       │                       │                │              │
       ▼                       ▼                ▼              ▼
┌──────────────┐   ┌────────────────┐   ┌──────────────┐   ┌─────────────────┐
│TxtFileEx-    │   │PdfFileEx-      │   │DocxFileEx-   │   │MarkdownFileEx-  │
│tractor       │   │tractor         │   │tractor       │   │tractor          │
└──────────────┘   └────────────────┘   └──────────────┘   └─────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    DocumentChunker                          │
├─────────────────────────────────────────────────────────────┤
│ - CHUNK_SIZE: int = 500                                     │
│ - TOP_K: int = 10                                           │
├─────────────────────────────────────────────────────────────┤
│ + chunkText(text): List<String>                             │
│ + selectTopChunks(chunks, userMessage): List<String>        │
│ + chunkAndSelect(text, userMessage): List<String>           │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    ContextBuilder                           │
├─────────────────────────────────────────────────────────────┤
│ - MAX_CONTEXT_LENGTH: int = 5000                            │
│ - CONTEXT_PREFIX: String = "File Context:\n"                │
├─────────────────────────────────────────────────────────────┤
│ + buildPrompt(fileContext, userMessage): ContextResult      │
│ + buildPromptFromChunks(chunks, userMessage): ContextResult │
└─────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────┐
│                    ContextResult                            │
├─────────────────────────────────────────────────────────────┤
│ + originalMessage: String                                   │
│ + enrichedPrompt: String                                    │
├─────────────────────────────────────────────────────────────┤
│ + hasContext(): boolean                                     │
└─────────────────────────────────────────────────────────────┘
```


### D. API Examples

**Example 1: Chat without File (Backward Compatible)**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{
    "conversationId": "user-123",
    "message": "What is Spring Boot?"
  }'
```

**Response:**
```json
{
  "conversationId": "user-123",
  "reply": "Spring Boot is a framework that simplifies Java application development..."
}
```

**Example 2: Chat with Text File Upload**
```bash
curl -X POST http://localhost:8080/api/chat \
  -F "conversationId=user-123" \
  -F "message=Summarize this document" \
  -F "file=@document.txt"
```

**Response:**
```json
{
  "conversationId": "user-123",
  "reply": "Based on the provided document, here's a summary: The document discusses..."
}
```

**Example 3: Chat with PDF File Upload**
```bash
curl -X POST http://localhost:8080/api/chat \
  -F "conversationId=user-456" \
  -F "message=What are the key findings?" \
  -F "file=@research_paper.pdf"
```

**Response:**
```json
{
  "conversationId": "user-456",
  "reply": "The key findings from the research paper include: 1) ..."
}
```

**Example 4: Error - File Too Large**
```bash
curl -X POST http://localhost:8080/api/chat \
  -F "conversationId=user-789" \
  -F "message=Analyze this" \
  -F "file=@huge_file.pdf"  # > 10MB
```

**Response (HTTP 413):**
```json
{
  "error": "File size exceeds maximum limit of 10MB"
}
```

**Example 5: Error - Missing Parameters**
```bash
curl -X POST http://localhost:8080/api/chat \
  -F "message=Hello"  # Missing conversationId
```

**Response (HTTP 400):**
```json
{
  "error": "conversationId is required"
}
```

### E. Example File Context Flow

**Input File (document.txt):**
```
Spring Boot is a powerful framework for building Java applications.
It provides auto-configuration, embedded servers, and production-ready features.
Developers can create stand-alone applications with minimal configuration.

Key Features:
- Auto-configuration
- Embedded Tomcat/Jetty
- Production metrics
- Health checks

Spring Boot simplifies dependency management through starter dependencies.
```

**User Message:**
```
What are the key features?
```

**Processing Steps:**

1. **Extraction:** TxtFileExtractor extracts full text (287 chars)

2. **Chunking:** DocumentChunker splits into chunks of ~500 chars
   - Chunk 1: "Spring Boot is a powerful framework..." (287 chars - entire text)

3. **Selection:** selectTopChunks finds chunks containing "key features"
   - Match found: Chunk 1 contains "Key Features:"
   - Selected: [Chunk 1]

4. **Context Building:** ContextBuilder creates enriched prompt
```
File Context:
Spring Boot is a powerful framework for building Java applications.
It provides auto-configuration, embedded servers, and production-ready features.
Developers can create stand-alone applications with minimal configuration.

Key Features:
- Auto-configuration
- Embedded Tomcat/Jetty
- Production metrics
- Health checks

Spring Boot simplifies dependency management through starter dependencies.

What are the key features?
```

5. **AI Processing:** ChatClient receives enriched prompt and generates response

6. **Storage:** JdbcChatMemoryRepository stores:
   - User message: "What are the key features?" (original, NOT enriched)
   - AI response: "The key features of Spring Boot include..."

---

## Summary

This design document presents a comprehensive, production-ready architecture for the AI Context Engine. The design follows SOLID principles, uses proven design patterns (Strategy, Builder), and maintains complete backward compatibility with the existing ChatService.

**Key Strengths:**
- **Modularity:** Clear separation of concerns with focused components
- **Extensibility:** Easy to add new file formats and chunking strategies
- **Robustness:** Comprehensive error handling with graceful degradation
- **Testability:** All components designed for unit and property-based testing
- **Performance:** Efficient algorithms with clear complexity guarantees
- **Security:** Input validation, error isolation, no code execution risks

**Next Steps:**
1. Review and approve this design document
2. Begin implementation following the phased plan
3. Implement all 15 correctness properties as property-based tests
4. Conduct integration testing with real file uploads
5. Deploy to staging environment for user acceptance testing

**Questions or Concerns:**
- Should file context be cached for repeated uploads? (Future enhancement)
- Should we support more than 10MB file uploads? (Current limit based on requirements)
- Should chunk selection use TF-IDF instead of substring matching? (Future semantic search)
