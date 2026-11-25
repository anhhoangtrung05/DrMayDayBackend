package com.example.drmayday.features.media.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.media.dto.AttachmentResponse;
import com.example.drmayday.features.media.service.AttachmentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AttachmentControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AttachmentService attachmentService;

    @InjectMocks
    private AttachmentController attachmentController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(attachmentController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 84: Upload anh hop le (<5MB) kem record_id -> 201 Created (1001)")
    void createAttachment_success() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "lesion.jpg", "image/jpeg", new byte[]{1, 2, 3});
        AttachmentResponse response = AttachmentResponse.builder()
                .attachmentId("101")
                .recordId("10")
                .originalFileName("lesion.jpg")
                .url("http://localhost:9000/drmayday/attachments/lesion.jpg")
                .fileFormat("jpg")
                .mediaType("IMAGE")
                .sizeBytes(3L)
                .purposeCategory("Tổn thương da")
                .fileDescription("Ảnh cận cảnh")
                .createdAt(OffsetDateTime.now())
                .build();

        when(attachmentService.createAttachment(any(), eq("10"), eq("Tổn thương da"), eq("Ảnh cận cảnh")))
                .thenReturn(response);

        mockMvc.perform(multipart("/api/v1/attachments")
                        .file(file)
                        .param("record_id", "10")
                        .param("phan_loai_muc_dich", "Tổn thương da")
                        .param("mo_ta_tep", "Ảnh cận cảnh"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.attachment_id").value("101"))
                .andExpect(jsonPath("$.data.record_id").value("10"))
                .andExpect(jsonPath("$.data.ten_tep_goc").value("lesion.jpg"));
    }

    @Test
    @DisplayName("API 84: Khong gui file -> 400 Bad Request (6001)")
    void createAttachment_missingFile() throws Exception {
        when(attachmentService.createAttachment(eq(null), eq("10"), any(), any()))
                .thenThrow(new AppException(ErrorCode.IMAGE_FILE_REQUIRED, "Tệp hình ảnh là bắt buộc"));

        mockMvc.perform(multipart("/api/v1/attachments").param("record_id", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("6001"));
    }

    @Test
    @DisplayName("API 84: Thieu record_id -> 400 Bad Request (2001)")
    void createAttachment_missingRecordId() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        when(attachmentService.createAttachment(any(), eq(null), any(), any()))
                .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "record_id is required"));

        mockMvc.perform(multipart("/api/v1/attachments").file(file))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("API 84: Patient gui record_id cua nguoi khac -> 403 Forbidden (3106)")
    void createAttachment_otherPatient() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        when(attachmentService.createAttachment(any(), eq("99"), any(), any()))
                .thenThrow(new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA));

        mockMvc.perform(multipart("/api/v1/attachments").file(file).param("record_id", "99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));
    }

    @Test
    @DisplayName("API 84: record_id khong ton tai -> 404 Not Found (4001)")
    void createAttachment_recordNotFound() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", new byte[]{1});
        when(attachmentService.createAttachment(any(), eq("999"), any(), any()))
                .thenThrow(new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        mockMvc.perform(multipart("/api/v1/attachments").file(file).param("record_id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("API 84: Tep > 5MB hoac MaxUploadSizeExceededException -> 413 Payload Too Large (6003)")
    void createAttachment_fileTooLarge() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "large.jpg", "image/jpeg", new byte[]{1});
        when(attachmentService.createAttachment(any(), eq("10"), any(), any()))
                .thenThrow(new MaxUploadSizeExceededException(5 * 1024 * 1024L));

        mockMvc.perform(multipart("/api/v1/attachments").file(file).param("record_id", "10"))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.code").value("6003"));
    }

    @Test
    @DisplayName("API 85: Lay danh sach tep dinh kem thanh cong -> 200 OK (1000)")
    void getAttachments_success() throws Exception {
        AttachmentResponse a = AttachmentResponse.builder()
                .attachmentId("1").recordId("10").originalFileName("img1.jpg").build();
        when(attachmentService.getAttachments("10")).thenReturn(List.of(a));

        mockMvc.perform(get("/api/v1/attachments").param("record_id", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[0].attachment_id").value("1"))
                .andExpect(jsonPath("$.data[0].ten_tep_goc").value("img1.jpg"));
    }

    @Test
    @DisplayName("API 85: Thieu record_id (2001), 403 (3106), 404 (4001)")
    void getAttachments_errors() throws Exception {
        when(attachmentService.getAttachments(null))
                .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER));
        mockMvc.perform(get("/api/v1/attachments"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));

        when(attachmentService.getAttachments("99"))
                .thenThrow(new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA));
        mockMvc.perform(get("/api/v1/attachments").param("record_id", "99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));

        when(attachmentService.getAttachments("999"))
                .thenThrow(new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        mockMvc.perform(get("/api/v1/attachments").param("record_id", "999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("API 86: Xoa tep dinh kem -> 200 OK (1000) va cac ma loi 6005, 3106, 6007")
    void deleteAttachment_successAndErrors() throws Exception {
        mockMvc.perform(delete("/api/v1/attachments/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));

        doThrow(new AppException(ErrorCode.ATTACHMENT_NOT_FOUND)).when(attachmentService).deleteAttachment("999");
        mockMvc.perform(delete("/api/v1/attachments/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("6005"));

        doThrow(new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA)).when(attachmentService).deleteAttachment("99");
        mockMvc.perform(delete("/api/v1/attachments/99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));

        doThrow(new AppException(ErrorCode.ATTACHMENT_IN_USE)).when(attachmentService).deleteAttachment("100");
        mockMvc.perform(delete("/api/v1/attachments/100"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("6007"));
    }
}
