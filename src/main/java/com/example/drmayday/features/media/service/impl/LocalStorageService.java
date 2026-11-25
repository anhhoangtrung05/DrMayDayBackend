package com.example.drmayday.features.media.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.features.media.service.StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.UUID;

@Service
public class LocalStorageService implements StorageService {

    private static final Logger log = LoggerFactory.getLogger(LocalStorageService.class);
    private static final int MAX_ORIGINAL_NAME_LENGTH = 255;
    private static final int MAX_FORMAT_LENGTH = 50;

    private final Path rootLocation;
    private final String urlPrefix;

    public LocalStorageService(
            @Value("${app.storage.upload-dir:${java.io.tmpdir}/drmayday-uploads/attachments}") String uploadDir,
            @Value("${app.storage.url-prefix:http://localhost:9000/drmayday/attachments}") String urlPrefix) {
        this.rootLocation = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.urlPrefix = urlPrefix.endsWith("/") ? urlPrefix.substring(0, urlPrefix.length() - 1) : urlPrefix;
        try {
            Files.createDirectories(this.rootLocation);
        } catch (IOException e) {
            log.warn("Could not initialize storage directory {}: {}", this.rootLocation, e.getMessage());
        }
    }

    @Override
    public StoredFile store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.IMAGE_FILE_REQUIRED, "Tệp hình ảnh là bắt buộc");
        }

        String sanitizedName = sanitizeFilename(file.getOriginalFilename());
        String extension = extractExtension(sanitizedName, file.getContentType());

        String storedFileName = UUID.randomUUID() + (extension.isEmpty() ? "" : "." + extension);
        String relativeStoragePath = "attachments/" + storedFileName;
        String generatedUrl = urlPrefix + "/" + storedFileName;

        Path destinationFile = this.rootLocation.resolve(storedFileName).normalize();
        if (!destinationFile.startsWith(this.rootLocation)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên tệp không hợp lệ");
        }

        try {
            Files.createDirectories(this.rootLocation);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            log.error("Failed to store file {}: {}", destinationFile, e.getMessage(), e);
            throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR, "Không thể lưu tệp lên hệ thống lưu trữ");
        }

        String contentType = file.getContentType();
        String format = deriveFormat(extension, contentType);
        String mediaType = deriveMediaType(contentType, extension);

        return new StoredFile(sanitizedName, relativeStoragePath, generatedUrl, format, mediaType, file.getSize());
    }

    @Override
    public void delete(String storagePath) {
        if (storagePath == null || storagePath.isBlank()) {
            return;
        }
        try {
            Path fileNamePath = Paths.get(storagePath).getFileName();
            if (fileNamePath == null) {
                return;
            }
            Path target = this.rootLocation.resolve(fileNamePath.toString()).normalize();
            if (!target.startsWith(this.rootLocation)) {
                return;
            }
            Files.deleteIfExists(target);
        } catch (Exception e) {
            log.warn("Failed to delete physical file {}: {}", storagePath, e.getMessage());
        }
    }

    private String sanitizeFilename(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            return "attachment.bin";
        }
        String normalizedSlashes = rawName.replace('\\', '/');
        String baseName = normalizedSlashes.substring(normalizedSlashes.lastIndexOf('/') + 1).trim();
        baseName = baseName.replaceAll("[\\r\\n\\x00]", "");
        if (baseName.isEmpty() || ".".equals(baseName) || "..".equals(baseName)) {
            return "attachment.bin";
        }
        if (baseName.length() > MAX_ORIGINAL_NAME_LENGTH) {
            int dotIdx = baseName.lastIndexOf('.');
            if (dotIdx > 0 && baseName.length() - dotIdx <= 20) {
                String ext = baseName.substring(dotIdx);
                return baseName.substring(0, MAX_ORIGINAL_NAME_LENGTH - ext.length()) + ext;
            }
            return baseName.substring(0, MAX_ORIGINAL_NAME_LENGTH);
        }
        return baseName;
    }

    private String extractExtension(String filename, String contentType) {
        int dotIdx = filename.lastIndexOf('.');
        if (dotIdx >= 0 && dotIdx < filename.length() - 1) {
            String ext = filename.substring(dotIdx + 1).toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
            if (!ext.isEmpty()) {
                return truncate(ext, 20);
            }
        }
        if (contentType != null && contentType.contains("/")) {
            String subtype = contentType.substring(contentType.indexOf('/') + 1).toLowerCase(Locale.ROOT);
            int paramIdx = subtype.indexOf(';');
            if (paramIdx >= 0) {
                subtype = subtype.substring(0, paramIdx);
            }
            subtype = subtype.replaceAll("[^a-z0-9]", "");
            if (!subtype.isEmpty() && !"octetstream".equals(subtype)) {
                return truncate(subtype, 20);
            }
        }
        return "";
    }

    private String deriveFormat(String extension, String contentType) {
        if (!extension.isEmpty()) {
            return truncate(extension, MAX_FORMAT_LENGTH);
        }
        if (contentType != null && !contentType.isBlank()) {
            return truncate(contentType.trim(), MAX_FORMAT_LENGTH);
        }
        return "bin";
    }

    private String deriveMediaType(String contentType, String extension) {
        if (contentType != null) {
            String lower = contentType.toLowerCase(Locale.ROOT);
            if (lower.startsWith("image/")) {
                return "IMAGE";
            }
            if (lower.startsWith("video/")) {
                return "VIDEO";
            }
            if (lower.startsWith("audio/")) {
                return "AUDIO";
            }
        }
        if (extension.matches("jpg|jpeg|png|gif|webp|bmp|heic|heif")) {
            return "IMAGE";
        }
        if (extension.matches("mp4|mov|avi|mkv|webm")) {
            return "VIDEO";
        }
        return "DOCUMENT";
    }

    private String truncate(String value, int maxLen) {
        return value.length() > maxLen ? value.substring(0, maxLen) : value;
    }
}
