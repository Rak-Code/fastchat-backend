package com.rakeshgupta.fastchat_backend.common.exception;

/**
 * Exception thrown when a file with an unsupported extension is provided.
 * Carries the filename and the unsupported extension for diagnostic purposes.
 */
public class UnsupportedFileTypeException extends RuntimeException {

    private final String filename;
    private final String extension;

    /**
     * Creates a new UnsupportedFileTypeException.
     *
     * @param filename  the name of the file with an unsupported type
     * @param extension the unsupported file extension (without dot)
     */
    public UnsupportedFileTypeException(String filename, String extension) {
        super("Unsupported file type '" + extension + "' for file: " + filename);
        this.filename = filename;
        this.extension = extension;
    }

    /**
     * Returns the name of the file that caused this exception.
     *
     * @return the filename
     */
    public String getFilename() {
        return filename;
    }

    /**
     * Returns the unsupported file extension.
     *
     * @return the extension (without dot)
     */
    public String getExtension() {
        return extension;
    }
}