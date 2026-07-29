package com.rakeshgupta.fastchat_backend.context.file;

import com.rakeshgupta.fastchat_backend.common.exception.FileProcessingException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * FileExtractor implementation for PDF files (.pdf).
 * <p>
 * This extractor uses Apache PDFBox to extract text content from PDF documents.
 * It handles multi-page documents, preserves paragraph structure, and provides
 * robust error handling with resource management.
 * </p>
 * 
 * <h3>Supported Features:</h3>
 * <ul>
 *   <li>Multi-page PDF processing</li>
 *   <li>Text positioning and ordering preservation</li>
 *   <li>Paragraph structure preservation with line breaks</li>
 *   <li>Resource cleanup with proper PDDocument management</li>
 *   <li>Comprehensive error handling and logging</li>
 * </ul>
 * 
 * <h3>Limitations:</h3>
 * <ul>
 *   <li>Image-based PDFs (scanned documents) may have poor text extraction</li>
 *   <li>Complex layouts may have text ordering issues</li>
 *   <li>Password-protected PDFs are not supported</li>
 *   <li>Embedded fonts may cause extraction issues</li>
 * </ul>
 * 
 * <h3>Performance Characteristics:</h3>
 * <ul>
 *   <li>Memory usage: ~2x file size during processing</li>
 *   <li>Processing time: ~100-500ms per MB depending on complexity</li>
 *   <li>Recommended maximum file size: 10MB</li>
 * </ul>
 * 
 * @author FastChat AI Context Engine
 * @version 1.0
 * @since 1.0
 */
@Component
public class PdfFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfFileExtractor.class);

    /**
     * Supported file extensions for PDF files.
     */
    private static final List<String> SUPPORTED_EXTENSIONS = List.of("pdf");

    /**
     * Extracts text content from PDF files with comprehensive error handling.
     * <p>
     * This method loads the PDF document using Apache PDFBox, configures the
     * text stripper for optimal text extraction, and handles all potential
     * errors gracefully with detailed logging.
     * </p>
     * 
     * <h4>Processing Steps:</h4>
     * <ol>
     *   <li>Read InputStream into byte array (PDFBox requirement)</li>
     *   <li>Load PDF document with Loader.loadPDF()</li>
     *   <li>Configure PDFTextStripper with position-based sorting</li>
     *   <li>Extract text from all pages</li>
     *   <li>Clean up resources and return extracted text</li>
     * </ol>
     * 
     * @param inputStream the PDF file content as an InputStream (must not be null)
     * @param filename the original filename for logging purposes (may be null)
     * @return extracted text content, or empty string if extraction fails
     * @throws IllegalArgumentException if inputStream is null
     * @throws FileProcessingException if critical PDF processing error occurs
     */
    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("InputStream cannot be null");
        }

        String displayName = filename != null ? filename : "unknown PDF file";
        log.debug("Starting PDF text extraction for: {}", displayName);

        PDDocument document = null;
        try {
            // Read InputStream into byte array (PDFBox 3.x requires byte[] or File)
            byte[] pdfBytes = readInputStreamToByteArray(inputStream);
            if (pdfBytes.length == 0) {
                log.warn("PDF file {} is empty", displayName);
                return "";
            }

            // Load PDF document
            document = Loader.loadPDF(pdfBytes);
            int pageCount = document.getNumberOfPages();
            
            if (pageCount == 0) {
                log.warn("PDF file {} has no pages", displayName);
                return "";
            }

            // Configure text stripper
            PDFTextStripper textStripper = new PDFTextStripper();
            textStripper.setSortByPosition(true); // Preserve text positioning
            textStripper.setLineSeparator("\n"); // Use consistent line breaks
            textStripper.setWordSeparator(" "); // Use consistent word separation
            
            // Extract text from all pages
            String extractedText = textStripper.getText(document);

            log.info("Successfully extracted text from PDF {}: {} pages, {} characters",
                    displayName, pageCount, extractedText.length());
            
            if (log.isDebugEnabled()) {
                logPdfStructure(extractedText, pageCount, displayName);
            }

            return extractedText.trim();

        } catch (IOException e) {
            log.error("Failed to extract text from PDF {}: {}", displayName, e.getMessage());
            throw new FileProcessingException("PDF text extraction failed", displayName, e);
        } catch (Exception e) {
            log.error("Unexpected error extracting text from PDF {}: {}", displayName, e.getMessage(), e);
            return "";
        } finally {
            // Always close the PDF document to prevent resource leaks
            if (document != null) {
                try {
                    document.close();
                    log.debug("PDF document closed successfully: {}", displayName);
                } catch (IOException e) {
                    log.warn("Error closing PDF document {}: {}", displayName, e.getMessage());
                }
            }
        }
    }

    /**
     * Reads an InputStream into a byte array efficiently.
     * Uses a buffer to read chunks and automatically grows the output buffer.
     * 
     * @param inputStream the input stream to read
     * @return byte array containing all data from the stream
     * @throws IOException if reading fails
     */
    private byte[] readInputStreamToByteArray(InputStream inputStream) throws IOException {
        try (ByteArrayOutputStream buffer = new ByteArrayOutputStream()) {
            byte[] data = new byte[8192]; // 8KB buffer for efficient reading
            int bytesRead;
            
            while ((bytesRead = inputStream.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, bytesRead);
            }
            
            return buffer.toByteArray();
        }
    }

    /**
     * Logs basic structural information about the PDF content for debugging.
     * 
     * @param content the extracted text content
     * @param pageCount the number of pages in the PDF
     * @param displayName the filename for logging
     */
    private void logPdfStructure(String content, int pageCount, String displayName) {
        String[] lines = content.split("\n");
        long nonEmptyLines = java.util.Arrays.stream(lines)
                .filter(line -> !line.trim().isEmpty())
                .count();
        
        // Estimate paragraphs (lines followed by empty lines or document end)
        long estimatedParagraphs = java.util.Arrays.stream(lines)
                .filter(line -> line.trim().length() > 50) // Assume substantial lines are paragraph starts
                .count();
        
        log.debug("PDF structure for {}: {} pages, {} lines ({} non-empty), ~{} paragraphs", 
                 displayName, pageCount, lines.length, nonEmptyLines, estimatedParagraphs);
    }

    /**
     * Returns the file extensions supported by this extractor.
     * 
     * @return list of supported extensions: ["pdf"]
     */
    @Override
    public List<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}
