package com.example.drmayday.features.media.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * Abstraction for storing and removing physical attachment objects (e.g. MinIO/local disk).
 */
public interface StorageService {

    StoredFile store(MultipartFile file);

    void delete(String storagePath);

    record StoredFile(
            String sanitizedOriginalName,
            String storagePath,
            String url,
            String format,
            String mediaType,
            long size
    ) {}
}
