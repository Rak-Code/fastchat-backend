package com.rakeshgupta.fastchat_backend.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for UnsupportedFileTypeException.
 */
class UnsupportedFileTypeExceptionTest {
    
    @Test
    void shouldCreateExceptionWithFilenameAndExtension() {
        // Arrange
        String filename = "image.png";
        String extension = "png";
        
        // Act
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException(filename, extension);
        
        // Assert
        assertEquals(filename, exception.getFilename());
        assertEquals(extension, exception.getExtension());
        assertTrue(exception.getMessage().contains(filename));
        assertTrue(exception.getMessage().contains(extension));
    }
    
    @Test
    void shouldFormatMessageWithFilenameAndExtension() {
        // Arrange
        String filename = "document.docx";
        String extension = "docx";
        String expectedMessage = "Unsupported file type: document.docx (extension: docx)";
        
        // Act
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException(filename, extension);
        
        // Assert
        assertEquals(expectedMessage, exception.getMessage());
    }
    
    @Test
    void shouldBeRuntimeException() {
        // Arrange
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException("test.pdf", "pdf");
        
        // Assert
        assertTrue(exception instanceof RuntimeException);
    }
    
    @Test
    void shouldHandleNullFilename() {
        // Act
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException(null, "txt");
        
        // Assert
        assertNull(exception.getFilename());
        assertEquals("txt", exception.getExtension());
    }
    
    @Test
    void shouldHandleNullExtension() {
        // Act
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException("file.txt", null);
        
        // Assert
        assertEquals("file.txt", exception.getFilename());
        assertNull(exception.getExtension());
    }
    
    @Test
    void shouldHandleEmptyStrings() {
        // Act
        UnsupportedFileTypeException exception = new UnsupportedFileTypeException("", "");
        
        // Assert
        assertEquals("", exception.getFilename());
        assertEquals("", exception.getExtension());
    }
    
    @Test
    void shouldHandleCommonFileExtensions() {
        // Arrange & Act
        UnsupportedFileTypeException pdfException = new UnsupportedFileTypeException("doc.pdf", "pdf");
        UnsupportedFileTypeException jpgException = new UnsupportedFileTypeException("image.jpg", "jpg");
        UnsupportedFileTypeException zipException = new UnsupportedFileTypeException("archive.zip", "zip");
        
        // Assert
        assertEquals("pdf", pdfException.getExtension());
        assertEquals("jpg", jpgException.getExtension());
        assertEquals("zip", zipException.getExtension());
    }
}
