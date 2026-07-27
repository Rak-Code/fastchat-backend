package com.rakeshgupta.fastchat_backend.common.exception;

/**
 * Exception thrown when an error occurs during file text extraction.
 * Wraps the underlying cause (e.g., {@link java.io.IOException}) and
 * carries the filename for diagnostic purposes.
 */
public class FileProcessingException extends RuntimeException {

    private final String filename;

    /**
     * Creates a new FileProcessingException.
     *
     * @param filename the name of the file that caused the error
     * @param message  a descriptive error message
     * @param cause    the underlying cause of the error
     */
    public FileProcessingException(String filename, String message, Throwable cause) {
        super(message, cause);
        this.filename = filename;
    }

    /**
     * Creates a new FileProcessingException with no cause.
     *
     * @param filename the name of the file that caused the error
     * @param message  a descriptive error message
     */
    public FileProcessingException(String filename, String message) {
        super(message);
        this.filename = filename;
    }

    /**
     * Returns the name of the file that caused this exception.
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }
}