package com.rakeshgupta.fastchat_backend.common.exception;

/**
 * Exception thrown when file processing fails during text extraction.
 * <p>
 * This exception is used to wrap and report critical file processing errors
 * that occur during text extraction from uploaded files. It provides context
 * about the specific file that caused the failure.
 * </p>
 * 
 * <h3>When to Use:</h3>
 * <ul>
 *   <li>Critical I/O errors during file reading</li>
 *   <li>Corrupted file formats that cannot be processed</li>
 *   <li>Encoding issues that prevent text extraction</li>
 *   <li>Parser failures in PDF, DOCX, or other complex formats</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * try {
 *     // Attempt file processing
 *     return extractTextFromFile(inputStream);
 * } catch (IOException e) {
 *     throw new FileProcessingException(
 *         "Failed to read file content", 
 *         filename, 
 *         e
 *     );
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
public class FileProcessingException extends RuntimeException {

    /**
     * The filename that caused the processing failure.
     */
    private final String filename;

    /**
     * Creates a new FileProcessingException with a message and filename.
     * 
     * @param message the detail message explaining the failure
     * @param filename the name of the file that caused the failure (may be null)
     */
    public FileProcessingException(String message, String filename) {
        super(message);
        this.filename = filename;
    }

    /**
     * Creates a new FileProcessingException with a message, filename, and cause.
     * 
     * @param message the detail message explaining the failure
     * @param filename the name of the file that caused the failure (may be null)
     * @param cause the underlying cause of the failure
     */
    public FileProcessingException(String message, String filename, Throwable cause) {
        super(message, cause);
        this.filename = filename;
    }

    /**
     * Returns the filename that caused the processing failure.
     * 
     * @return the filename, or null if not available
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns a string representation of this exception including the filename.
     * 
     * @return a string representation of this exception
     */
    @Override
    public String toString() {
        String baseMessage = super.toString();
        if (filename != null && !filename.trim().isEmpty()) {
            return baseMessage + " (filename: " + filename + ")";
        }
        return baseMessage;
    }
}