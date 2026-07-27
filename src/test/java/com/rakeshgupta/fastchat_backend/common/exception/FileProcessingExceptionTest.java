package com.rakeshgupta.fastchat_backend.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for FileProcessingException.
 */
class FileProcessingExceptionTest {
    
    @Test
    void shouldCreateExceptionWithMessageAndFilename() {
        // Arrange
        String message = "Failed to extract text";
        String filename = "document.txt";
        
        // Act
        FileProcessingException exception = new FileProcessingException(message, filename);
        
        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(filename, exception.getFilename());
        assertNull(exception.getCause());
    }
    
    @Test
    void shouldCreateExceptionWithMessageFilenameAndCause() {
        // Arrange
        String message = "Failed to extract text";
        String filename = "document.txt";
        Throwable cause = new RuntimeException("IO error");
        
        // Act
        FileProcessingException exception = new FileProcessingException(message, filename, cause);
        
        // Assert
        assertEquals(message, exception.getMessage());
        assertEquals(filename, exception.getFilename());
        assertEquals(cause, exception.getCause());
    }
    
    @Test
    void shouldBeRuntimeException() {
        // Arrange
        FileProcessingException exception = new FileProcessingException("test", "test.txt");
        
        // Assert
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    void shouldHandleNullFilename() {
        // Act
        FileProcessingException exception = new FileProcessingException("test", null);
        
        // Assert
        assertNull(exception.getFilename());
    }
    
    @Test
    void shouldHandleEmptyFilename() {
        // Act
        FileProcessingException exception = new FileProcessingException("test", "");
        
        // Assert
        assertEquals("", exception.getFilename());
    }
}
