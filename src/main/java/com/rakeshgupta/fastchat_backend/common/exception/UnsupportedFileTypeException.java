package com.rakeshgupta.fastchat_backend.common.exception;

/**
 * Exception thrown when an uploaded file type is not supported for text extraction.
 * <p>
 * This exception is thrown when a user uploads a file with an extension that
 * is not supported by any registered FileExtractor implementation. It provides
 * context about the specific file and extension that was rejected.
 * </p>
 * 
 * <h3>Common Unsupported File Types:</h3>
 * <ul>
 *   <li>Image files: jpg, png, gif, bmp</li>
 *   <li>Video files: mp4, avi, mov</li>
 *   <li>Audio files: mp3, wav, flac</li>
 *   <li>Binary files: exe, dll, so</li>
 *   <li>Archive files: zip, rar, 7z</li>
 * </ul>
 * 
 * <h3>Usage Example:</h3>
 * <pre>{@code
 * Optional<FileExtractor> extractor = factory.getExtractor(filename);
 * if (extractor.isEmpty()) {
 *     String extension = getFileExtension(filename);
 *     throw new UnsupportedFileTypeException(filename, extension);
 * }
 * }</pre>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
public class UnsupportedFileTypeException extends RuntimeException {

    /**
     * The filename that has the unsupported type.
     */
    private final String filename;

    /**
     * The file extension that is not supported.
     */
    private final String extension;

    /**
     * Creates a new UnsupportedFileTypeException with filename and extension.
     * 
     * @param filename the name of the file with unsupported type
     * @param extension the unsupported file extension (without the dot)
     */
    public UnsupportedFileTypeException(String filename, String extension) {
        super(String.format("Unsupported file type: %s (extension: %s)", 
              filename != null ? filename : "unknown", 
              extension != null ? extension : "unknown"));
        this.filename = filename;
        this.extension = extension;
    }

    /**
     * Creates a new UnsupportedFileTypeException with filename, extension, and custom message.
     * 
     * @param message custom error message
     * @param filename the name of the file with unsupported type
     * @param extension the unsupported file extension (without the dot)
     */
    public UnsupportedFileTypeException(String message, String filename, String extension) {
        super(message);
        this.filename = filename;
        this.extension = extension;
    }

    /**
     * Returns the filename that has the unsupported type.
     * 
     * @return the filename, or null if not available
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the unsupported file extension.
     * 
     * @return the file extension (without dot), or null if not available
     */
    public String getExtension() {
        return extension;
    }

    /**
     * Returns a string representation of this exception including filename and extension.
     * 
     * @return a string representation of this exception
     */
    @Override
    public String toString() {
        String baseMessage = super.toString();
        if (filename != null || extension != null) {
            return String.format("%s (filename: %s, extension: %s)", 
                   baseMessage, 
                   filename != null ? filename : "unknown",
                   extension != null ? extension : "unknown");
        }
        return baseMessage;
    }
}