# Implementation Plan: AI Context Engine

## Overview

This implementation plan breaks down the AI Context Engine feature into discrete, testable coding tasks. The feature enables FastChat to process uploaded files (TXT, PDF, DOCX, Markdown) as contextual input during conversations. The implementation follows a modular, extensible architecture using Strategy and Builder patterns while preserving all existing conversation memory functionality.

**Key Principles:**
- Backward Compatibility: All existing chat functionality remains unchanged
- Graceful Degradation: File processing errors never break chat requests
- Extensibility: Easy addition of new file formats through Strategy pattern
- Testing: Property-based tests validate universal correctness properties

**Implementation Language:** Java 21 with Spring Boot 3.5.9

**Implementation Order:** Follow the 5-phase structure below to ensure incremental progress with early validation.

## Tasks

### Phase 1: Core Infrastructure

- [ ] 1. Set up project structure and core interfaces
  - [-] 1.1 Create FileExtractor interface and package structure
    - Create package `com.rakeshgupta.fastchat_backend.context.file`
    - Define FileExtractor interface with `extractText(InputStream, String)` and `getSupportedExtensions()` methods
    - Add comprehensive JavaDoc documenting contract and expected behaviors
    - _Requirements: FR1.4, FR1.5_

  - [-] 1.2 Create exception classes for file processing
    - Create package `com.rakeshgupta.fastchat_backend.common.exception`
    - Implement FileProcessingException with filename field
    - Implement UnsupportedFileTypeException with filename and extension fields
    - Add constructors with message and cause parameters
    - _Requirements: FR1.3, FR2.2_

  - [ ] 1.3 Implement TxtFileExtractor with encoding handling
    - Create TxtFileExtractor class implementing FileExtractor
    - Add @Component annotation for Spring auto-discovery
    - Implement multi-charset fallback (UTF-8, ISO-8859-1, Windows-1252)
    - Use BufferedReader for efficient text reading
    - Preserve line breaks and formatting characters
    - Handle IOException gracefully, return empty string and log errors
    - Return supported extensions: ["txt", "text"]
    - _Requirements: FR1.1, FR1.5, FR2.1, FR2.5_

  - [ ]* 1.4 Write property test for TxtFileExtractor encoding handling
    - **Property 5: Formatting Preservation**
    - **Validates: Requirements FR2.5, AC1.4**
    - Generate random text with line breaks (\n, \r\n), tabs (\t), and formatting characters
    - Write to temporary file, extract text, verify all formatting preserved
    - Use jqwik for property-based testing with minimum 100 iterations
    - Tag with @Tag("property") and @Tag("ai-context-engine")

  - [ ]* 1.5 Write unit tests for TxtFileExtractor
    - Test text extraction with valid UTF-8 files
    - Test encoding fallback with ISO-8859-1 and Windows-1252 files
    - Test error handling with corrupted/invalid files
    - Test empty file handling
    - Test large file handling (up to 5MB)
    - _Requirements: FR1.5, FR2.2_

  - [ ] 1.6 Implement MarkdownFileExtractor
    - Create MarkdownFileExtractor class implementing FileExtractor
    - Reuse TxtFileExtractor logic (Markdown is plain text)
    - Return supported extensions: ["md", "markdown"]
    - Preserve Markdown formatting without rendering
    - _Requirements: FR1.1, FR2.5_

  - [ ]* 1.7 Write unit tests for MarkdownFileExtractor
    - Test Markdown syntax preservation (headers, lists, code blocks, links)
    - Test encoding handling
    - Test error handling
    - _Requirements: FR1.5, FR2.5_

- [ ] 2. Implement FileExtractor factory and registration
  - [ ] 2.1 Create FileExtractorFactory with auto-discovery
    - Create FileExtractorFactory class with @Component annotation
    - Use constructor injection with List<FileExtractor> parameter
    - Build immutable Map<String, FileExtractor> in constructor (extension -> extractor)
    - Handle multiple extensions per extractor
    - Implement getExtractor(filename) returning Optional<FileExtractor>
    - Implement getSupportedExtensions() returning Set<String>
    - Add @PostConstruct method to log registered extractors at startup
    - Use case-insensitive extension matching
    - _Requirements: FR1.1, FR1.2_

  - [ ]* 2.2 Write property test for provider selection by file extension
    - **Property 1: Provider Selection by File Extension**
    - **Validates: Requirements FR1.1, FR1.2**
    - Generate random filenames with registered extensions (txt, text, md, markdown)
    - Verify getExtractor() returns correct FileExtractor type
    - Verify case-insensitive matching works (TXT, txt, Txt all match)
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR1.1, FR1.2_

  - [ ]* 2.3 Write property test for graceful degradation with unsupported files
    - **Property 2: Graceful Degradation for Unsupported Files**
    - **Validates: Requirements FR1.3, AC1.2**
    - Generate random filenames with unsupported extensions (png, jpg, exe, etc.)
    - Verify getExtractor() returns Optional.empty()
    - Tag with @Tag("property") and @Tag("ai-context-engine")

    - _Requirements: FR1.3, AC1.2_

  - [ ]* 2.4 Write unit tests for FileExtractorFactory
    - Test factory initialization with multiple extractors
    - Test extension lookup (case-sensitive and insensitive)
    - Test unknown extension returns Optional.empty()
    - Test getSupportedExtensions() includes all registered extensions
    - _Requirements: FR1.1, FR1.2, FR1.3_

- [ ] 3. Implement document chunking and selection
  - [ ] 3.1 Create DocumentChunker class
    - Create package `com.rakeshgupta.fastchat_backend.document.parser`
    - Implement DocumentChunker with @Component annotation
    - Define constants: CHUNK_SIZE = 500, TOP_K = 10
    - Implement chunkText(String) method with linear algorithm
    - Implement selectTopChunks(List<String>, String) with substring matching
    - Implement chunkAndSelect(String, String) convenience method
    - Handle null/empty inputs gracefully
    - Preserve original chunk ordering (no reordering by relevance)
    - Add comprehensive JavaDoc with algorithm complexity notes
    - _Requirements: AC2.1, AC2.3, AC2.5_

  - [ ]* 3.2 Write property test for chunking size consistency
    - **Property 12: Chunking Size Consistency**
    - **Validates: Requirements AC2.1**
    - Generate random text of varying lengths (600, 1500, 5000, 10000 chars)
    - Call chunkText() and verify all chunks except last are 500 characters
    - Verify last chunk is <= 500 characters
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: AC2.1_

  - [ ]* 3.3 Write property test for chunk selection by substring matching
    - **Property 13: Chunk Selection by Substring Matching**
    - **Validates: Requirements AC2.3**
    - Generate random chunks where some contain keywords from user message
    - Call selectTopChunks() and verify returned chunks contain user message as substring
    - Use case-insensitive matching
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: AC2.3_

  - [ ]* 3.4 Write property test for chunk ordering preservation
    - **Property 14: Chunk Ordering Preservation**
    - **Validates: Requirements AC2.5**
    - Generate random chunks with matches at specific positions (2, 5, 8, etc.)
    - Call selectTopChunks() and verify returned chunks maintain original order

    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: AC2.5_

  - [ ]* 3.5 Write unit tests for DocumentChunker edge cases
    - Test chunking with empty text (returns empty list)
    - Test chunking with text < 500 chars (returns single chunk)
    - Test chunking with text exactly 500 chars (returns single chunk)
    - Test chunking with text exactly 1000 chars (returns two 500-char chunks)
    - Test selection with no matching chunks (returns first 10 chunks)
    - Test selection with fewer than 10 chunks total
    - Test selection with null/empty userMessage (returns first 10 chunks)
    - _Requirements: AC2.1, AC2.3, AC2.4, AC2.5_

- [ ] 4. Implement context building with Builder pattern
  - [ ] 4.1 Create ContextResult record
    - Create package `com.rakeshgupta.fastchat_backend.context.builder`
    - Define ContextResult record with fields: originalMessage, enrichedPrompt
    - Add hasContext() helper method returning boolean
    - Add comprehensive JavaDoc explaining the purpose of each field
    - _Requirements: FR3.5_

  - [ ] 4.2 Implement ContextBuilder class
    - Create ContextBuilder with @Component annotation
    - Define constants: MAX_CONTEXT_LENGTH = 5000, CONTEXT_PREFIX = "File Context:\n"
    - Implement buildPrompt(String fileContext, String userMessage) method
    - Handle null/blank fileContext by returning unchanged message
    - Truncate fileContext to MAX_CONTEXT_LENGTH if needed
    - Build enriched prompt: CONTEXT_PREFIX + truncatedContext + "\n\n" + userMessage
    - Return ContextResult with both original and enriched messages
    - Implement buildPromptFromChunks(List<String> chunks, String userMessage)
    - Join chunks with "\n\n" separator
    - Delegate to buildPrompt() for consistency
    - _Requirements: FR3.1, FR3.2, FR3.3, FR3.4, FR3.5_

  - [ ]* 4.3 Write property test for context prepending structure
    - **Property 6: Context Prepending Structure**
    - **Validates: Requirements FR3.1, FR3.3**
    - Generate random file contexts and user messages
    - Verify enrichedPrompt starts with "File Context:\n"
    - Verify enrichedPrompt contains user message at the end
    - Verify structure: prefix + context + "\n\n" + userMessage

    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR3.1, FR3.3_

  - [ ]* 4.4 Write property test for context truncation
    - **Property 7: Context Truncation**
    - **Validates: Requirements FR3.2**
    - Generate random file contexts exceeding 5000 characters (6000, 10000, 50000)
    - Call buildPrompt() and extract context portion
    - Verify context portion is exactly 5000 characters (excluding prefix and user message)
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR3.2_

  - [ ]* 4.5 Write property test for empty context passthrough
    - **Property 8: Empty Context Passthrough**
    - **Validates: Requirements FR3.4**
    - Generate random user messages
    - Call buildPrompt() with null or empty context
    - Verify enrichedPrompt equals originalMessage (unchanged)
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR3.4_

  - [ ]* 4.6 Write property test for context result completeness
    - **Property 9: Context Result Completeness**
    - **Validates: Requirements FR3.5**
    - Generate random contexts and messages
    - Verify ContextResult contains both fields
    - Verify originalMessage is unchanged from input
    - Verify hasContext() returns correct boolean
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR3.5_

  - [ ]* 4.7 Write unit tests for ContextBuilder
    - Test prompt building with various context sizes (100, 500, 5000, 10000 chars)
    - Test prompt building with chunks (multiple chunks joined correctly)
    - Test null/empty context handling
    - Test prefix formatting
    - Test ContextResult field values
    - _Requirements: FR3.1, FR3.2, FR3.3, FR3.4, FR3.5_

- [ ] 5. Checkpoint - Core infrastructure complete
  - Ensure all tests pass for Phase 1 components
  - Verify FileExtractorFactory discovers all registered extractors
  - Verify DocumentChunker chunking and selection algorithms work correctly

  - Verify ContextBuilder produces correct enriched prompts
  - Ask the user if any questions arise

### Phase 2: Service Integration

- [ ] 6. Implement FileContextService orchestrator
  - [ ] 6.1 Create FileContextService class
    - Create FileContextService with @Service annotation
    - Define constant: MAX_FILE_SIZE = 10 * 1024 * 1024 (10MB)
    - Add constructor injection for FileExtractorFactory, DocumentChunker, ContextBuilder
    - Implement processFileContext(MultipartFile file, String userMessage) method
    - Handle null/empty file by returning unchanged message
    - Validate file size <= MAX_FILE_SIZE, log warning if exceeded
    - Extract filename and file extension
    - Get appropriate extractor from factory
    - Handle unsupported file type by logging warning and returning unchanged message
    - Extract text using extractor, handle extraction failures gracefully
    - Call documentChunker.chunkAndSelect() to get top chunks
    - Build enriched prompt using contextBuilder.buildPromptFromChunks()
    - Wrap all processing in try-catch, log errors, return unchanged message on any exception
    - Add comprehensive logging for success/warning/error cases
    - _Requirements: FR2.1, FR2.2, NFR1.1, NFR2.1_

  - [ ]* 6.2 Write property test for extraction error handling
    - **Property 4: Extraction Error Handling**
    - **Validates: Requirements FR2.2**
    - Create corrupted/malformed files that cause extraction errors
    - Call processFileContext() and verify it returns unchanged message
    - Verify error is logged without propagating exception
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR2.2_

  - [ ]* 6.3 Write property test for exception safety in text extraction
    - **Property 3: Exception Safety in Text Extraction**
    - **Validates: Requirements FR1.5**
    - Generate random byte arrays and corrupt InputStreams
    - Pass to each FileExtractor implementation
    - Verify no unchecked exceptions are thrown
    - Verify empty string is returned on error
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR1.5_

  - [ ]* 6.4 Write unit tests for FileContextService
    - Test processFileContext with null file (returns unchanged message)

    - Test processFileContext with empty file (returns unchanged message)
    - Test processFileContext with file exceeding MAX_FILE_SIZE (logs warning, returns unchanged message)
    - Test processFileContext with unsupported file type (logs warning, returns unchanged message)
    - Test processFileContext with valid text file (returns enriched prompt)
    - Test processFileContext with extraction failure (logs error, returns unchanged message)
    - Mock FileExtractorFactory, DocumentChunker, ContextBuilder for isolation
    - _Requirements: FR2.1, FR2.2, FR2.3, FR2.4_

- [ ] 7. Enhance ChatService with file context support
  - [ ] 7.1 Add FileContextService to ChatService
    - Add FileContextService field to ChatService
    - Update constructor to inject FileContextService
    - Create new chat() method signature with MultipartFile parameter
    - Implement overloaded chat(String conversationId, String message, MultipartFile file)
    - Add conversationId validation (throw IllegalArgumentException if blank)
    - Add message validation (throw IllegalArgumentException if blank)
    - Call fileContextService.processFileContext() to get ContextResult
    - Use contextResult.enrichedPrompt() in ChatClient.prompt().user() call
    - Preserve existing chat() method without file parameter for backward compatibility
    - Keep clearConversation() method unchanged
    - _Requirements: FR4.1, FR4.2, FR4.3, FR4.4, FR4.5_

  - [ ]* 7.2 Write property test for conversationId validation
    - **Property 10: ConversationId Validation**
    - **Validates: Requirements FR4.4, FR5.3**
    - Generate random invalid conversationIds (null, "", "   ", "\t\n")
    - Call chat() method and verify IllegalArgumentException is thrown
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR4.4, FR5.3_

  - [ ]* 7.3 Write property test for backward compatibility
    - **Property 15: Backward Compatibility**
    - **Validates: Requirements FR4.3, AC3.1, FR5.5**
    - Make chat requests with file=null
    - Verify behavior is identical to original implementation
    - Verify same prompt sent to ChatClient (no file context processing)
    - Verify conversation memory updated correctly
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR4.3, AC3.1, FR5.5_

  - [ ]* 7.4 Write unit tests for enhanced ChatService

    - Test chat() with file parameter (verify enriched prompt used)
    - Test chat() without file parameter (verify backward compatibility)
    - Test conversationId validation (null, blank, whitespace)
    - Test message validation (null, blank, whitespace)
    - Verify original message stored in conversation memory (not enriched prompt)
    - Mock FileContextService and ChatClient for isolation
    - _Requirements: FR4.1, FR4.2, FR4.3, FR4.4, FR4.5_

- [ ] 8. Checkpoint - Service integration complete
  - Ensure all tests pass for Phase 2 components
  - Verify FileContextService orchestrates all dependencies correctly
  - Verify ChatService integrates file context without breaking existing functionality
  - Test end-to-end flow: file upload → extraction → chunking → context building → chat
  - Ask the user if any questions arise

### Phase 3: API Layer

- [ ] 9. Enhance ChatController with multipart support
  - [ ] 9.1 Update ChatController to accept file uploads
    - Modify @PostMapping to accept both JSON and multipart/form-data content types
    - Update chat() method to accept @RequestParam("file") MultipartFile file parameter
    - Set file parameter as optional (required = false)
    - Keep existing @RequestParam validations for conversationId and message
    - Add @NotBlank validation for conversationId
    - Add @Size(max = 4000) validation for message
    - Log incoming requests with file presence indicator
    - Pass file parameter to chatService.chat()
    - _Requirements: FR5.1, FR5.2, FR5.3, FR5.4, FR5.5_

  - [ ]* 9.2 Write property test for file upload validation
    - **Property 11: File Upload Validation**
    - **Validates: Requirements FR5.4**
    - Generate random invalid requests (missing conversationId, message too long, etc.)
    - Make POST requests and verify 400 Bad Request responses with error messages
    - Tag with @Tag("property") and @Tag("ai-context-engine")
    - _Requirements: FR5.4_

  - [ ]* 9.3 Write unit tests for enhanced ChatController
    - Test POST with JSON (no file) - backward compatibility
    - Test POST with multipart/form-data and valid file
    - Test POST with multipart/form-data without file (file=null)
    - Test validation errors (missing conversationId, blank message, message too long)
    - Mock ChatService to verify correct parameters passed

    - _Requirements: FR5.1, FR5.2, FR5.3, FR5.4, FR5.5_

- [ ] 10. Implement global exception handling for file operations
  - [ ] 10.1 Enhance GlobalExceptionHandler
    - Add @ExceptionHandler for MaxUploadSizeExceededException
    - Return HTTP 413 Payload Too Large with error message
    - Add @ExceptionHandler for FileProcessingException
    - Return HTTP 500 Internal Server Error with error message
    - Log all file-related exceptions with appropriate levels
    - _Requirements: FR5.2, FR5.4_

  - [ ]* 10.2 Write unit tests for GlobalExceptionHandler
    - Test MaxUploadSizeExceededException handling (verify 413 response)
    - Test FileProcessingException handling (verify 500 response)
    - Verify error messages are descriptive
    - Verify errors are logged appropriately
    - _Requirements: FR5.2, FR5.4_

- [ ] 11. Add Spring configuration for multipart and file context
  - [ ] 11.1 Update application.yml with multipart configuration
    - Add spring.servlet.multipart.enabled: true
    - Add spring.servlet.multipart.max-file-size: 10MB
    - Add spring.servlet.multipart.max-request-size: 10MB
    - Add spring.servlet.multipart.file-size-threshold: 1MB
    - Add spring.servlet.multipart.location: ${java.io.tmpdir}
    - Add app.file-context configuration section
    - Set app.file-context.chunk-size: 500
    - Set app.file-context.top-k-chunks: 10
    - Set app.file-context.max-context-length: 5000
    - Set app.file-context.supported-formats: txt,pdf,docx,md,markdown
    - _Requirements: FR5.2, NFR2.1_

  - [ ] 11.2 Create FileContextConfig configuration class
    - Create package `com.rakeshgupta.fastchat_backend.common.config`
    - Create FileContextConfig with @Configuration annotation
    - Add @PostConstruct method to log initialization message
    - Add JavaDoc explaining configuration purpose
    - _Requirements: NFR1.1_

- [ ] 12. Checkpoint - API layer complete
  - Ensure all tests pass for Phase 3 components
  - Verify ChatController accepts both JSON and multipart requests
  - Verify file size limits are enforced (10MB)
  - Verify validation errors return appropriate HTTP status codes

  - Test end-to-end with curl or Postman
  - Ask the user if any questions arise

### Phase 4: Advanced Extractors

- [ ] 13. Add Maven dependencies for PDF and DOCX support
  - [ ] 13.1 Update pom.xml with Apache PDFBox dependency
    - Add org.apache.pdfbox:pdfbox:3.0.2 dependency
    - Update Maven dependencies
    - _Requirements: NFR1.2_

  - [ ] 13.2 Update pom.xml with Apache POI dependency
    - Add org.apache.poi:poi-ooxml:5.2.5 dependency
    - Update Maven dependencies
    - _Requirements: NFR1.2_

  - [ ] 13.3 Add jqwik dependency for property-based testing
    - Add net.jqwik:jqwik:1.8.2 dependency with test scope
    - Update Maven dependencies
    - _Requirements: Testing_

- [ ] 14. Implement PDF file extractor
  - [ ] 14.1 Create PdfFileExtractor class
    - Create PdfFileExtractor implementing FileExtractor
    - Add @Component annotation
    - Use Apache PDFBox PDDocument.load() to load PDF from InputStream
    - Use PDFTextStripper to extract text content
    - Close PDDocument in finally block (resource management)
    - Handle multi-page documents (extract all pages)
    - Preserve paragraph structure with line breaks
    - Log page count after successful extraction
    - Return empty string on IOException, log error details
    - Handle corrupted PDFs gracefully
    - Return supported extensions: ["pdf"]
    - _Requirements: FR1.1, FR1.5, FR2.1, FR2.5_

  - [ ]* 14.2 Write unit tests for PdfFileExtractor
    - Test text extraction from valid single-page PDF
    - Test text extraction from multi-page PDF
    - Test error handling with corrupted PDF
    - Test empty PDF handling
    - Test PDF with special characters and formatting
    - Test resource cleanup (PDDocument closed properly)
    - _Requirements: FR1.5, FR2.2, FR2.5_

  - [ ]* 14.3 Write integration tests for PDF extraction
    - Create sample PDF files for testing (single-page, multi-page, with tables)
    - Test end-to-end: PDF upload → extraction → chunking → context building
    - Verify extracted text contains expected content
    - Verify formatting is preserved

    - _Requirements: FR2.1, FR2.5_

- [ ] 15. Implement DOCX file extractor
  - [ ] 15.1 Create DocxFileExtractor class
    - Create DocxFileExtractor implementing FileExtractor
    - Add @Component annotation
    - Use Apache POI XWPFDocument to load DOCX from InputStream
    - Extract paragraphs using XWPFParagraph.getText()
    - Extract tables using XWPFTable (iterate rows and cells)
    - Join paragraphs and table content with line breaks
    - Close XWPFDocument in finally block (resource management)
    - Handle headers and footers if present
    - Log paragraph and table count after successful extraction
    - Return empty string on IOException, log error details
    - Handle corrupted DOCX gracefully
    - Return supported extensions: ["docx"]
    - _Requirements: FR1.1, FR1.5, FR2.1, FR2.5_

  - [ ]* 15.2 Write unit tests for DocxFileExtractor
    - Test text extraction from valid DOCX with paragraphs only
    - Test text extraction from DOCX with tables
    - Test text extraction from DOCX with headers and footers
    - Test error handling with corrupted DOCX
    - Test empty DOCX handling
    - Test DOCX with special characters and formatting
    - Test resource cleanup (XWPFDocument closed properly)
    - _Requirements: FR1.5, FR2.2, FR2.5_

  - [ ]* 15.3 Write integration tests for DOCX extraction
    - Create sample DOCX files for testing (paragraphs, tables, headers)
    - Test end-to-end: DOCX upload → extraction → chunking → context building
    - Verify extracted text contains expected content
    - Verify table content is extracted correctly
    - _Requirements: FR2.1, FR2.5_

- [ ] 16. Checkpoint - Advanced extractors complete
  - Ensure all tests pass for Phase 4 components
  - Verify FileExtractorFactory discovers PDF and DOCX extractors
  - Test file uploads with TXT, Markdown, PDF, and DOCX files
  - Verify all supported formats listed in getSupportedExtensions()
  - Ask the user if any questions arise

### Phase 5: Testing & Refinement

- [ ] 17. Comprehensive integration testing
  - [ ]* 17.1 Write end-to-end integration tests for all file types

    - Test chat with TXT file upload (full flow: controller → service → ChatClient)
    - Test chat with Markdown file upload
    - Test chat with PDF file upload
    - Test chat with DOCX file upload
    - Test chat without file (backward compatibility)
    - Use @SpringBootTest and @AutoConfigureMockMvc for integration tests
    - Mock ChatClient to capture enriched prompts
    - Verify enriched prompts contain "File Context:" prefix
    - Verify original messages stored in conversation memory
    - Tag with @Tag("integration") and @Tag("ai-context-engine")
    - _Requirements: FR4.5, AC3.2, AC3.3_

  - [ ]* 17.2 Write integration tests for error scenarios
    - Test chat with unsupported file type (image, video) - graceful degradation
    - Test chat with file exceeding 10MB - HTTP 413 response
    - Test chat with corrupted file - graceful degradation
    - Test chat with empty file - graceful degradation
    - Test chat with missing conversationId - HTTP 400 response
    - Test chat with missing message - HTTP 400 response
    - Tag with @Tag("integration") and @Tag("ai-context-engine")
    - _Requirements: FR1.3, FR2.2, FR5.2, FR5.4_

  - [ ]* 17.3 Write integration tests for conversation memory
    - Test multiple chat requests in same conversation (with and without files)
    - Verify conversation memory stores only original messages (not enriched prompts)
    - Verify conversationId isolation (different conversations don't mix)
    - Test clearConversation() works correctly after file uploads
    - Tag with @Tag("integration") and @Tag("ai-context-engine")
    - _Requirements: FR4.2, FR4.5, AC3.2, AC3.3, AC3.4, AC3.5_

- [ ] 18. Performance and security testing
  - [ ]* 18.1 Write performance tests for file processing
    - Test file processing time for 1MB TXT file (target: < 100ms)
    - Test file processing time for 5MB TXT file (target: < 500ms)
    - Test file processing time for 5MB PDF file (target: < 500ms)
    - Test file processing time for 5MB DOCX file (target: < 300ms)
    - Verify total processing time < 2 seconds per NFR2.1
    - Verify timeout logging if processing exceeds 2 seconds
    - Tag with @Tag("performance") and @Tag("ai-context-engine")
    - _Requirements: NFR2.1, NFR2.2, NFR2.3_

  - [ ]* 18.2 Write security tests for file validation

    - Test file size validation (reject files > 10MB)
    - Test file type whitelist (only TXT, MD, PDF, DOCX accepted)
    - Test malformed filename handling (special characters, path traversal attempts)
    - Test memory cleanup (files not persisted, cleared after processing)
    - Verify no code execution from file content
    - Tag with @Tag("security") and @Tag("ai-context-engine")
    - _Requirements: Security, NFR2.4_

  - [ ]* 18.3 Write memory usage tests
    - Test memory usage for 10MB file upload (estimate: ~20MB peak)
    - Verify files are garbage collected after request completes
    - Verify extracted text is not retained after context building
    - Verify context truncation limits memory (max 5000 chars)
    - Tag with @Tag("performance") and @Tag("ai-context-engine")
    - _Requirements: NFR2.4_

- [ ] 19. Documentation and code quality
  - [ ] 19.1 Add comprehensive JavaDoc to all public APIs
    - Add class-level JavaDoc for all components
    - Add method-level JavaDoc for all public methods
    - Document parameters, return values, and exceptions
    - Add usage examples in JavaDoc
    - Document algorithm complexity where relevant
    - _Requirements: NFR3.1, NFR3.3_

  - [ ] 19.2 Update README or API documentation
    - Document file upload API endpoint
    - Provide curl examples for file uploads
    - Document supported file formats
    - Document file size limits
    - Document error responses and status codes
    - Add troubleshooting section
    - _Requirements: Documentation_

  - [ ] 19.3 Add logging and monitoring hooks
    - Verify all components log at appropriate levels (INFO, WARN, ERROR)
    - Add metrics for file upload rate (if using Micrometer/Prometheus)
    - Add metrics for file processing time
    - Add metrics for extraction failures by file type
    - Add health check for file context feature
    - _Requirements: Monitoring_

- [ ] 20. Final checkpoint and deployment preparation
  - Run all tests (unit, property-based, integration)
  - Verify test coverage meets 80% minimum for new components
  - Verify all 15 correctness properties pass with 100+ iterations
  - Run Maven build and verify no compilation errors
  - Test with real file uploads using curl or Postman
  - Verify backward compatibility (existing chat without files works)

  - Verify JdbcChatMemoryRepository unchanged and working
  - Review code for SOLID principles adherence
  - Review error handling for graceful degradation
  - Prepare deployment checklist
  - Ask the user if ready for deployment or if any refinements needed

## Notes

- **Optional Tasks**: Tasks marked with `*` are test-related sub-tasks and can be skipped for faster MVP delivery, though they are strongly recommended for production quality
- **Property-Based Tests**: All 15 correctness properties from the design document are included as dedicated test tasks
- **Incremental Validation**: Checkpoints ensure early detection of issues and allow user feedback
- **Backward Compatibility**: All existing ChatService functionality preserved without breaking changes
- **Graceful Degradation**: File processing errors never break chat requests
- **Extensibility**: New file formats can be added by implementing FileExtractor interface

## Task Dependency Graph

```json
{
  "waves": [
    { "id": 0, "tasks": ["1.1", "1.2"] },
    { "id": 1, "tasks": ["1.3", "1.6", "2.1", "3.1", "4.1"] },
    { "id": 2, "tasks": ["1.4", "1.5", "1.7", "2.2", "2.3", "2.4", "3.2", "3.3", "3.4", "3.5", "4.2"] },
    { "id": 3, "tasks": ["4.3", "4.4", "4.5", "4.6", "4.7", "6.1"] },
    { "id": 4, "tasks": ["6.2", "6.3", "6.4", "7.1"] },
    { "id": 5, "tasks": ["7.2", "7.3", "7.4", "9.1", "10.1", "11.1", "11.2"] },
    { "id": 6, "tasks": ["9.2", "9.3", "10.2", "13.1", "13.2", "13.3"] },
    { "id": 7, "tasks": ["14.1", "15.1"] },
    { "id": 8, "tasks": ["14.2", "14.3", "15.2", "15.3"] },
    { "id": 9, "tasks": ["17.1", "17.2", "17.3"] },
    { "id": 10, "tasks": ["18.1", "18.2", "18.3", "19.1"] },
    { "id": 11, "tasks": ["19.2", "19.3"] }
  ]
}
```
