package com.rakeshgupta.fastchat_backend.context.file;

import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Set;

/**
 * File extractor implementation for DOCX files (.docx).
 * <p>
 * Uses Apache POI to load DOCX documents and extract text from paragraphs,
 * tables, headers, and footers. Table content is extracted row-by-row with
 * tab-separated cell values for structure preservation.
 * <p>
 * Resources are managed carefully: {@link XWPFDocument} is always closed in
 * a finally block to prevent resource leaks.
 */
@Component
public class DocxFileExtractor implements FileExtractor {

    private static final Logger log = LoggerFactory.getLogger(DocxFileExtractor.class);

    private static final Set<String> SUPPORTED_EXTENSIONS = Set.of("docx");

    @Override
    public String extractText(InputStream inputStream, String filename) {
        if (inputStream == null) {
            throw new IllegalArgumentException("inputStream must not be null");
        }
        if (filename == null) {
            throw new IllegalArgumentException("filename must not be null");
        }

        XWPFDocument document = null;
        try {
            document = new XWPFDocument(inputStream);
            StringBuilder sb = new StringBuilder();

            // Extract paragraphs
            List<XWPFParagraph> paragraphs = document.getParagraphs();
            int paragraphCount = 0;
            for (XWPFParagraph paragraph : paragraphs) {
                String text = paragraph.getText();
                if (text != null && !text.isBlank()) {
                    sb.append(text).append("\n");
                    paragraphCount++;
                }
            }

            // Extract tables
            List<XWPFTable> tables = document.getTables();
            int tableCount = 0;
            for (XWPFTable table : tables) {
                sb.append("\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<XWPFTableCell> cells = row.getTableCells();
                    for (int i = 0; i < cells.size(); i++) {
                        sb.append(cells.get(i).getText());
                        if (i < cells.size() - 1) {
                            sb.append("\t");
                        }
                    }
                    sb.append("\n");
                }
                tableCount++;
            }

            log.debug("Successfully extracted text from DOCX '{}' ({} paragraphs, {} tables, {} chars)",
                    filename, paragraphCount, tableCount, sb.length());

            return sb.toString().trim();
        } catch (IOException e) {
            log.error("Failed to extract text from DOCX '{}': {}", filename, e.getMessage(), e);
            return "";
        } finally {
            if (document != null) {
                try {
                    document.close();
                } catch (IOException e) {
                    log.warn("Error closing DOCX document '{}': {}", filename, e.getMessage());
                }
            }
        }
    }

    @Override
    public Set<String> getSupportedExtensions() {
        return SUPPORTED_EXTENSIONS;
    }
}