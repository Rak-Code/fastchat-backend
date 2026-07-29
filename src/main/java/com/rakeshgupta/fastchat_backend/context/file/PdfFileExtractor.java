package com.rakeshgupta.fastchat_backend.context.file;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

/**
 * File extractor implementation for PDF files (.pdf).
 * <p>
 * Uses Apache PDFBox to load PDF documents from input streams and extract
 * text content using {@link PDFTextStripper}. Handles multi-page documents
 * and preserves paragraph structure with line breaks.
 * <p>
 * Resources are managed carefully: {@link PDDocument} is always closed in
 * a finally block to prevent resource leaks.
 */
@Component
public class PdfFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(PdfFileExtractor.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("pdf");

    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }

        PDDocument document = null;
        try {
            // Read InputStream into byte array (PDFBox 3.x requires byte[] or File)
            byte[] pdfBytes = toByteArray(inputStream);
            document = Loader.loadPDF(pdfBytes);
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            String text = stripper.getText(document);

            int pageCount = document.getNumberOfPages();
            log.debug("Successfully extracted text from PDF '{}' ({} pages, {} chars)",
                    filename, pageCount, text.length());

            return text.trim();
        } catch (IOException e) {
            log.error("Failed to extract text from PDF '{}': {}", filename, e.getMessage(), e);
            return "";
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.warn("Error closing PDF document '{}': {}", filename, e.getMessage());
                }
            }
        }
    }

    /**
     * Reads an InputStream into a byte array.
     */
    private byte[] toByteArray(InputStream inputStream) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] data = new byte[8192];
        int n;
        while ((n = inputStream.read(data, 0, data.length)) != -1) {
            buffer.write(data, 0, n);
        }
        return buffer.toByteArray();
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}
