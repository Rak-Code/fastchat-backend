# Implementation Plan: Frontend-Backend Integration

## Overview

This plan implements the integration between the FastChat React frontend and Spring Boot backend for local development. Tasks are ordered to establish connectivity first, then align the API contract, and finally add session management.

## Tasks

- [x] 1. Configure Backend CORS
  - [x] 1.1 Create CorsConfig.java in the config package
    - Add `@Configuration` class implementing `WebMvcConfigurer`
    - Configure allowed origins from environment variable with localhost:5173 default
    - Allow methods: GET, POST, DELETE, OPTIONS
    - Allow Content-Type header
    - Enable credentials
    - _Requirements: 2.1, 2.2, 2.3, 2.4_

- [x] 2. Update Frontend Port Configuration
  - [x] 2.1 Update vite.config.ts to use port 5173
    - Change server.port from 8080 to 5173
    - _Requirements: 6.2, 6.3_

- [x] 3. Update Frontend API Client
  - [x] 3.1 Update api.ts with localhost URL configuration
    - Change API_URL to use environment variable with localhost:8080 fallback
    - Remove hardcoded production URL
    - _Requirements: 1.1, 1.2_
  
  - [x] 3.2 Add session management functions to api.ts
    - Add `getOrCreateSession()` function that checks sessionStorage first
    - Add `clearSession()` function that calls DELETE endpoint
    - Store/retrieve conversationId from sessionStorage
    - _Requirements: 5.1, 5.2, 5.3, 5.4_
  
  - [x] 3.3 Update sendMessage function in api.ts
    - Call getOrCreateSession() to get conversationId
    - Include conversationId in request body
    - Map backend `reply` to frontend `response`
    - Provide default values for model and tokenUsed
    - _Requirements: 3.1, 4.1, 4.2, 4.3_
  
  - [ ]* 3.4 Write property test for response mapping
    - **Property 4: Response Field Mapping**
    - **Validates: Requirements 4.1**

- [x] 4. Update Frontend Chat Container
  - [x] 4.1 Update ChatContainer.tsx to use clearSession on chat clear
    - Import clearSession from api.ts
    - Call clearSession() in handleClearChat before clearing messages
    - _Requirements: 3.4, 5.3_

- [ ] 5. Checkpoint - Verify Integration
  - Ensure backend starts on port 8080
  - Ensure frontend starts on port 5173
  - Test that CORS allows requests
  - Test message send/receive flow
  - Test chat clear creates new session

## Notes

- Tasks marked with `*` are optional and can be skipped for faster MVP
- The backend already has the required endpoints (`/api/chat`, `/api/session`)
- No changes needed to backend DTOs or controllers
- Property tests use fast-check library for TypeScript
