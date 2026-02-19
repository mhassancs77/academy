package com.academy.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.List;

@Service
public class FileValidationService {

    private static final Logger log = LoggerFactory.getLogger(FileValidationService.class);

    @Value("${app.upload.max-size-mb:500}")
    private int maxSizeMB;

    @Value("${app.upload.allowed-video-types:video/mp4,video/avi,video/quicktime,video/x-msvideo}")
    private String allowedVideoTypes;

    /**
     * Validate video file
     * @return null if valid, error message if invalid
     */
    public String validateVideoFile(MultipartFile file) {
        // Check if file is empty
        if (file == null || file.isEmpty()) {
            return "لم يتم اختيار ملف";
        }

        // Check file size
        long maxSizeBytes = maxSizeMB * 1024L * 1024L;
        if (file.getSize() > maxSizeBytes) {
            double fileSizeMB = file.getSize() / (1024.0 * 1024.0);
            return String.format("حجم الملف كبير جداً (%.2f ميجابايت). الحد الأقصى %d ميجابايت",
                    fileSizeMB, maxSizeMB);
        }

        // Check file type
        String contentType = file.getContentType();
        if (contentType == null || !isAllowedVideoType(contentType)) {
            return "نوع الملف غير مدعوم. الصيغ المدعومة: MP4, AVI, MOV";
        }

        // Check filename
        String filename = file.getOriginalFilename();
        if (filename == null || filename.trim().isEmpty()) {
            return "اسم الملف غير صالح";
        }

        // Check for dangerous extensions
        if (isDangerousExtension(filename)) {
            return "امتداد الملف غير مسموح به";
        }

        // All validations passed
        log.info("File validation passed: {} ({} bytes)", filename, file.getSize());
        return null;
    }

    /**
     * Validate text input (title, description, etc.)
     */
    public String validateText(String text, String fieldName, int maxLength, boolean required) {
        if (text == null || text.trim().isEmpty()) {
            if (required) {
                return fieldName + " مطلوب";
            }
            return null;
        }

        // Trim text
        text = text.trim();

        // Check length
        if (text.length() > maxLength) {
            return fieldName + " طويل جداً (الحد الأقصى " + maxLength + " حرف)";
        }

        // Check for SQL injection patterns (basic)
        if (containsSQLInjection(text)) {
            return fieldName + " يحتوي على محارف غير مسموحة";
        }

        return null;
    }

    /**
     * Check if content type is allowed for videos
     */
    private boolean isAllowedVideoType(String contentType) {
        List<String> allowed = Arrays.asList(allowedVideoTypes.split(","));
        return allowed.stream().anyMatch(type -> contentType.toLowerCase().contains(type.toLowerCase()));
    }

    /**
     * Check for dangerous file extensions
     */
    private boolean isDangerousExtension(String filename) {
        String lower = filename.toLowerCase();
        String[] dangerous = {".exe", ".bat", ".cmd", ".sh", ".php", ".jsp", ".js", ".jar"};

        for (String ext : dangerous) {
            if (lower.endsWith(ext)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Basic SQL injection check
     */
    private boolean containsSQLInjection(String text) {
        String lower = text.toLowerCase();
        String[] patterns = {"drop table", "delete from", "insert into", "update ", "select *", "--", "/*", "*/"};

        for (String pattern : patterns) {
            if (lower.contains(pattern)) {
                log.warn("Potential SQL injection detected: {}", text);
                return true;
            }
        }

        return false;
    }

    /**
     * Sanitize filename to prevent path traversal
     */
    public String sanitizeFilename(String filename) {
        if (filename == null) {
            return "unnamed";
        }

        // Remove path separators
        filename = filename.replace("../", "")
                .replace("..\\", "")
                .replace("/", "_")
                .replace("\\", "_");

        // Remove special characters except dots, hyphens, underscores
        filename = filename.replaceAll("[^a-zA-Z0-9._\\-\u0600-\u06FF]", "_");

        return filename;
    }
}