package com.sliit.ems.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.UUID;

/**
 * Stores uploaded CV/resume PDFs on local disk under backend/uploads/cv/.
 *
 * Design decision (documented per project owner's instructions, see
 * docs/database-design.md): com.sun.net.httpserver.HttpServer has no built-in
 * multipart/form-data parser, and hand-writing a correct one is error-prone
 * for a student project. Instead the frontend sends the file as a
 * base64-encoded string plus its original filename in an ordinary JSON POST
 * body (Json.java already supports flat string fields, no new dependency
 * needed). The server never trusts the client-supplied filename for storage -
 * it only validates the extension, generates a random UUID-based filename,
 * and - critically - checks the *decoded byte content* starts with the PDF
 * magic bytes "%PDF", not just the extension or any client-asserted MIME type.
 * Only the generated filename (never file bytes) is stored in
 * APPLICATION.CV_Resume.
 */
public class FileStorage {

    public static final long MAX_CV_SIZE_BYTES = 5L * 1024 * 1024; // 5MB
    private static final byte[] PDF_MAGIC = { '%', 'P', 'D', 'F' };
    private static final Path CV_DIR = Path.of("uploads", "cv");

    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) { super(message); }
    }

    /**
     * Decodes, validates and saves a base64-encoded PDF. Returns the generated
     * server-side filename to store in APPLICATION.CV_Resume.
     */
    public static String saveCv(String originalFilename, String base64Content) throws IOException {
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new ValidationException("A file name is required.");
        }
        if (!originalFilename.toLowerCase().endsWith(".pdf")) {
            throw new ValidationException("Only PDF files are accepted (file must end with .pdf).");
        }
        if (base64Content == null || base64Content.isBlank()) {
            throw new ValidationException("No file content was received.");
        }

        byte[] decoded;
        try {
            // Strip an optional data: URL prefix (e.g. "data:application/pdf;base64,...") if the
            // frontend sent one from a <input type=file> + FileReader.readAsDataURL() flow.
            String content = base64Content;
            int commaIdx = content.indexOf(",");
            if (content.startsWith("data:") && commaIdx > -1) {
                content = content.substring(commaIdx + 1);
            }
            decoded = Base64.getDecoder().decode(content);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("File content is not valid base64.");
        }

        if (decoded.length == 0) {
            throw new ValidationException("The uploaded file is empty.");
        }
        if (decoded.length > MAX_CV_SIZE_BYTES) {
            throw new ValidationException("File is too large. Maximum size is 5MB.");
        }
        if (!startsWithPdfMagic(decoded)) {
            throw new ValidationException("File content is not a valid PDF (missing PDF signature).");
        }

        Files.createDirectories(CV_DIR);
        String storedFilename = UUID.randomUUID() + ".pdf";
        Path target = CV_DIR.resolve(storedFilename);
        Files.write(target, decoded);
        return storedFilename;
    }

    /** Reads a previously stored CV by its server-generated filename. Returns null if it doesn't exist. */
    public static byte[] readCv(String storedFilename) throws IOException {
        if (storedFilename == null || storedFilename.isBlank()) return null;
        // Defense in depth: storedFilename always comes from our own DB column (set only by saveCv
        // above), but never resolve anything that isn't a bare filename within CV_DIR.
        Path target = CV_DIR.resolve(storedFilename).normalize();
        if (!target.startsWith(CV_DIR) || !Files.exists(target)) return null;
        return Files.readAllBytes(target);
    }

    private static boolean startsWithPdfMagic(byte[] data) {
        if (data.length < PDF_MAGIC.length) return false;
        for (int i = 0; i < PDF_MAGIC.length; i++) {
            if (data[i] != PDF_MAGIC[i]) return false;
        }
        return true;
    }
}
