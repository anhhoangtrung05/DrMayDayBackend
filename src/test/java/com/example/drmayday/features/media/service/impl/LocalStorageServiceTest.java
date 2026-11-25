package com.example.drmayday.features.media.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.features.media.service.StorageService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @Test
    @DisplayName("Luu anh hop le, sinh URL va xoa thanh cong")
    void storeAndDeleteImage() {
        LocalStorageService service = new LocalStorageService(tempDir.toString(), "http://localhost:9000/drmayday/attachments/");
        MockMultipartFile file = new MockMultipartFile("file", "urticaria.png", "image/png", new byte[]{1, 2, 3, 4});

        StorageService.StoredFile stored = service.store(file);

        assertEquals("urticaria.png", stored.sanitizedOriginalName());
        assertTrue(stored.storagePath().startsWith("attachments/"));
        assertTrue(stored.storagePath().endsWith(".png"));
        assertTrue(stored.url().startsWith("http://localhost:9000/drmayday/attachments/"));
        assertEquals("png", stored.format());
        assertEquals("IMAGE", stored.mediaType());
        assertEquals(4L, stored.size());

        Path storedFileOnDisk = tempDir.resolve(Path.of(stored.storagePath()).getFileName());
        assertTrue(Files.exists(storedFileOnDisk));

        service.delete(stored.storagePath());
        assertFalse(Files.exists(storedFileOnDisk));
    }

    @Test
    @DisplayName("Chong path traversal va cat ngan ten tep / dinh dang dai an toan")
    void sanitizesPathTraversalAndLongNames() {
        LocalStorageService service = new LocalStorageService(tempDir.toString(), "http://localhost:9000/drmayday/attachments");
        String longDocName = "../../" + "a".repeat(300) + ".docx";
        String longMime = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";

        MockMultipartFile docFile = new MockMultipartFile("file", longDocName, longMime, new byte[]{10, 20});
        StorageService.StoredFile storedDoc = service.store(docFile);

        assertFalse(storedDoc.sanitizedOriginalName().contains(".."));
        assertFalse(storedDoc.sanitizedOriginalName().contains("/"));
        assertTrue(storedDoc.sanitizedOriginalName().length() <= 255);
        assertTrue(storedDoc.sanitizedOriginalName().endsWith(".docx"));
        assertTrue(storedDoc.format().length() <= 50);
        assertEquals("DOCUMENT", storedDoc.mediaType());
        service.delete(storedDoc.storagePath());

        MockMultipartFile fallbackImg = new MockMultipartFile("file", "..\\..\\photo.jpeg", "application/octet-stream", new byte[]{1});
        StorageService.StoredFile storedImg = service.store(fallbackImg);
        assertEquals("photo.jpeg", storedImg.sanitizedOriginalName());
        assertEquals("IMAGE", storedImg.mediaType());
        service.delete(storedImg.storagePath());
    }

    @Test
    @DisplayName("Tu choi file null hoac 0 byte voi ma 6001")
    void rejectsEmptyOrNullFile() {
        LocalStorageService service = new LocalStorageService(tempDir.toString(), "http://localhost:9000/drmayday/attachments");
        MockMultipartFile empty = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        AppException ex1 = assertThrows(AppException.class, () -> service.store(empty));
        assertEquals(ErrorCode.IMAGE_FILE_REQUIRED, ex1.getErrorCode());

        AppException ex2 = assertThrows(AppException.class, () -> service.store(null));
        assertEquals(ErrorCode.IMAGE_FILE_REQUIRED, ex2.getErrorCode());
    }

    @Test
    @DisplayName("Delete bo qua duong dan null/rong/path-traversal an toan")
    void deleteIgnoresUnsafeOrBlankPaths() {
        LocalStorageService service = new LocalStorageService(tempDir.toString(), "http://localhost:9000/drmayday/attachments");
        assertDoesNotThrow(() -> service.delete(null));
        assertDoesNotThrow(() -> service.delete("   "));
        assertDoesNotThrow(() -> service.delete("attachments/does-not-exist.png"));
    }
}
