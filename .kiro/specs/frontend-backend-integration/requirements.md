# Requirements Document

## Introduction

This document specifies the requirements for integrating the FastChat React frontend with the Spring Boot backend for local development. The integration involves aligning API contracts, configuring CORS, resolving port conflicts, and ensuring seamless communication between the two applications running on localhost.

## Glossary

- **Frontend**: The React/TypeScript application (fastchat-ui) that provides the user interface for the chat application
- **Backend**: The Spring Boot Java application (fastchat-backend) that handles chat requests and AI interactions
- **API_Client**: The frontend module responsible for making HTTP requests to the backend
- **CORS_Config**: Cross-Origin Resource Sharing configuration that allows the frontend to communicate with the backend
- **Session_Manager**: The component responsible for managing conversation session IDs

## Requirements

### Requirement 1: API URL Configuration

**User Story:** As a developer, I want to configure the frontend to connect to localhost, so that I can test the integration locally without deploying to production.

#### Acceptance Criteria

1. WHEN the frontend application starts in development mode, THE API_Client SHALL use `http://localhost:8080` as the base URL
2. WHEN the `VITE_API_URL` environment variable is set, THE API_Client SHALL use that value as the base URL
3. THE Frontend SHALL run on a different port than the backend to avoid conflicts

### Requirement 2: CORS Configuration

**User Story:** As a developer, I want the backend to accept requests from the frontend running on localhost, so that cross-origin requests are not blocked.

#### Acceptance Criteria

1. WHEN a request originates from `http://localhost:5173`, THE CORS_Config SHALL allow the request
2. THE CORS_Config SHALL allow the following HTTP methods: GET, POST, DELETE, OPTIONS
3. THE CORS_Config SHALL allow the `Content-Type` header in requests
4. THE CORS_Config SHALL expose necessary response headers to the frontend

### Requirement 3: API Contract Alignment - Request

**User Story:** As a developer, I want the frontend to send requests in the format expected by the backend, so that chat messages are processed correctly.

#### Acceptance Criteria

1. WHEN sending a chat message, THE API_Client SHALL include both `conversationId` and `message` fields in the request body
2. THE Session_Manager SHALL generate a unique conversation ID when the chat session starts
3. THE Session_Manager SHALL persist the conversation ID for the duration of the browser session
4. WHEN the user clears the chat, THE Session_Manager SHALL generate a new conversation ID

### Requirement 4: API Contract Alignment - Response

**User Story:** As a developer, I want the frontend to correctly parse backend responses, so that chat messages are displayed properly.

#### Acceptance Criteria

1. WHEN receiving a chat response, THE API_Client SHALL map the backend `reply` field to the frontend `response` field
2. THE Frontend SHALL handle responses that may not include `model` or `tokenUsed` fields gracefully
3. IF the backend response is missing expected fields, THEN THE Frontend SHALL display the message content without metadata

### Requirement 5: Session Management

**User Story:** As a developer, I want the frontend to manage conversation sessions, so that chat history is maintained correctly.

#### Acceptance Criteria

1. WHEN the application loads, THE Session_Manager SHALL check for an existing session ID in session storage
2. IF no session ID exists, THEN THE Session_Manager SHALL create a new session by calling the `/api/session` endpoint
3. WHEN the user clears the chat, THE Session_Manager SHALL call the `/api/session/{conversationId}` DELETE endpoint
4. THE Session_Manager SHALL store the conversation ID in browser session storage

### Requirement 6: Port Configuration

**User Story:** As a developer, I want the frontend and backend to run on different ports, so that both applications can run simultaneously.

#### Acceptance Criteria

1. THE Backend SHALL run on port 8080
2. THE Frontend SHALL run on port 5173 (Vite default)
3. WHEN configuring the frontend dev server, THE Frontend SHALL not conflict with the backend port
