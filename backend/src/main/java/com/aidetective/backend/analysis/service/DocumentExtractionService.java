package com.aidetective.backend.analysis.service;

import com.aidetective.backend.analysis.exception.BadRequestException;
import com.aidetective.backend.analysis.model.InputType;
import com.aidetective.backend.config.ExtractionProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Locale;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.tika.Tika;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;

@Service
public class DocumentExtractionService {

    private static final long MAX_UPLOAD_BYTES = 8L * 1024 * 1024;
    private static final long MAX_VIDEO_UPLOAD_BYTES = 5L * 1024 * 1024;

    private final ExtractionProperties extractionProperties;
    private final Tika tika = new Tika();

    public DocumentExtractionService(ExtractionProperties extractionProperties) {
        this.extractionProperties = extractionProperties;
    }

    public ExtractedUpload extract(MultipartFile file, boolean ocrEnabled) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Please upload an image, PDF, or video file.");
        }
        if (file.getSize() > MAX_UPLOAD_BYTES) {
            throw new BadRequestException("Uploaded files are limited to 8 MB.");
        }

        String filename = file.getOriginalFilename() == null || file.getOriginalFilename().isBlank()
            ? "uploaded-file"
            : file.getOriginalFilename().trim();

        try {
            byte[] bytes = file.getBytes();
            String detectedMimeType = detectMimeType(file, bytes);

            if (detectedMimeType.startsWith("image/")) {
                return extractImage(bytes, detectedMimeType, filename, ocrEnabled);
            }
            if ("application/pdf".equals(detectedMimeType)) {
                return extractPdf(bytes, filename, ocrEnabled);
            }
            if (detectedMimeType.startsWith("video/")) {
                if (file.getSize() > MAX_VIDEO_UPLOAD_BYTES) {
                    throw new BadRequestException("Uploaded video files are limited to 5 MB.");
                }
                return extractVideo(detectedMimeType, filename, file.getSize());
            }
            throw new BadRequestException("Only PDF, common image formats, and video formats are supported.");
        } catch (IOException exception) {
            throw new BadRequestException("The uploaded file could not be read.");
        }
    }

    private ExtractedUpload extractImage(byte[] bytes, String mimeType, String filename, boolean ocrEnabled) throws IOException {
        if (!ocrEnabled) {
            throw new BadRequestException("OCR must be enabled for image uploads sent as files.");
        }
        String extractedText = extractTextFromImage(bytes);
        if (extractedText.isBlank()) {
            throw new BadRequestException("No readable text could be extracted from the uploaded image.");
        }
        return new ExtractedUpload(InputType.IMAGE, filename, filename, mimeType, trimText(extractedText), true);
    }

    private ExtractedUpload extractPdf(byte[] bytes, String filename, boolean ocrEnabled) throws IOException {
        try (PDDocument document = Loader.loadPDF(bytes)) {
            String extractedText = new PDFTextStripper().getText(document);
            extractedText = trimText(extractedText);

            if (extractedText.isBlank() && ocrEnabled) {
                extractedText = trimText(extractTextFromRenderedPdf(document));
            }
            if (extractedText.isBlank()) {
                throw new BadRequestException("No readable text could be extracted from the uploaded PDF.");
            }

            return new ExtractedUpload(InputType.PDF, filename, filename, "application/pdf", extractedText, ocrEnabled);
        }
    }

    private ExtractedUpload extractVideo(String mimeType, String filename, long sizeBytes) {
        String summary = trimText("""
            Uploaded video file: %s
            MIME type: %s
            Approximate size: %d bytes.
            Investigate this video source based on the available file metadata. If the actual video transcript or frames are unavailable,
            explain the limitation clearly and focus on credibility signals from the file naming, source context, and any surrounding claims.
            """.formatted(filename, mimeType, sizeBytes));
        return new ExtractedUpload(InputType.VIDEO, filename, filename, mimeType, summary, false);
    }

    private String extractTextFromRenderedPdf(PDDocument document) throws IOException {
        PDFRenderer renderer = new PDFRenderer(document);
        StringBuilder builder = new StringBuilder();
        int pagesToScan = Math.min(document.getNumberOfPages(), 3);
        for (int pageIndex = 0; pageIndex < pagesToScan; pageIndex++) {
            BufferedImage image = renderer.renderImageWithDPI(pageIndex, 180);
            builder.append(runTesseract(image)).append('\n');
        }
        return builder.toString();
    }

    private String extractTextFromImage(byte[] bytes) throws IOException {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(bytes));
        if (image == null) {
            throw new BadRequestException("The uploaded image format is not readable.");
        }
        return runTesseract(image);
    }

    private String runTesseract(BufferedImage image) {
        try {
            Tesseract tesseract = new Tesseract();
            String datapath = System.getenv("TESSDATA_PREFIX");
            if (datapath != null && !datapath.isBlank()) {
                tesseract.setDatapath(datapath);
            }
            tesseract.setLanguage("eng");
            return tesseract.doOCR(image);
        } catch (UnsatisfiedLinkError | TesseractException exception) {
            throw new BadRequestException("OCR is unavailable on this backend. Install Tesseract and set TESSDATA_PREFIX.");
        }
    }

    private String detectMimeType(MultipartFile file, byte[] bytes) throws IOException {
        String detected = tika.detect(bytes, file.getOriginalFilename());
        if (detected == null || detected.isBlank() || "application/octet-stream".equals(detected)) {
            detected = file.getContentType();
        }
        return detected == null ? "application/octet-stream" : detected.toLowerCase(Locale.ROOT);
    }

    private String trimText(String text) {
        String normalized = text == null ? "" : text.replaceAll("\\s+", " ").trim();
        if (normalized.length() > extractionProperties.getMaxTextLength()) {
            return normalized.substring(0, extractionProperties.getMaxTextLength());
        }
        return normalized;
    }

    public record ExtractedUpload(
        InputType inputType,
        String sourceTitle,
        String sourceLabel,
        String mimeType,
        String normalizedText,
        boolean ocrUsed
    ) {
    }
}
