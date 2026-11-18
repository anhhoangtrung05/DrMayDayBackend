package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.medicalrecord.dto.AddLesionDetailsBulkRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailItemRequest;
import com.example.drmayday.features.medicalrecord.dto.LesionDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.UpdateLesionDetailRequest;
import com.example.drmayday.features.medicalrecord.service.LesionDetailsService;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LesionDetailsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LesionDetailsService lesionDetailsService;

    @InjectMocks
    private LesionDetailsController lesionDetailsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lesionDetailsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 54: POST /api/v1/lesion-details - Thêm bulk thành công (201 Created)")
    void addLesionDetailsBulk_success() throws Exception {
        AddLesionDetailsBulkRequest req = AddLesionDetailsBulkRequest.builder()
                .examId("1")
                .items(List.of(
                        LesionDetailItemRequest.builder()
                                .viTriTonThuong("Mặt")
                                .loaiTonThuong("San phu")
                                .kichThuocMm(BigDecimal.valueOf(15))
                                .build()
                ))
                .build();

        LesionDetailResponse res = LesionDetailResponse.builder()
                .lesionDetailId("101")
                .examId("1")
                .loaiBenhAn("CAP")
                .viTriTonThuong("Mặt")
                .build();

        when(lesionDetailsService.addLesionDetailsBulk(any(AddLesionDetailsBulkRequest.class)))
                .thenReturn(List.of(res));

        mockMvc.perform(post("/api/v1/lesion-details")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data[0].lesion_detail_id").value("101"));
    }

    @Test
    @DisplayName("API 55: GET /api/v1/lesion-details - Lấy danh sách thành công (200 OK)")
    void getListLesionDetails_success() throws Exception {
        LesionDetailResponse res = LesionDetailResponse.builder()
                .lesionDetailId("101")
                .examId("1")
                .loaiBenhAn("CAP")
                .build();

        when(lesionDetailsService.getListLesionDetails("1"))
                .thenReturn(List.of(res));

        mockMvc.perform(get("/api/v1/lesion-details").param("exam_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[0].lesion_detail_id").value("101"));
    }

    @Test
    @DisplayName("API 56: PUT /api/v1/lesion-details/{id} - Sửa thành công (200 OK)")
    void updateLesionDetail_success() throws Exception {
        UpdateLesionDetailRequest req = UpdateLesionDetailRequest.builder()
                .viTriTonThuong("Toàn thân")
                .kichThuocMm(BigDecimal.valueOf(25))
                .build();

        LesionDetailResponse res = LesionDetailResponse.builder()
                .lesionDetailId("101")
                .examId("1")
                .loaiBenhAn("CAP")
                .viTriTonThuong("Toàn thân")
                .build();

        when(lesionDetailsService.updateLesionDetail(eq("101"), eq("CAP"), any(UpdateLesionDetailRequest.class)))
                .thenReturn(res);

        mockMvc.perform(put("/api/v1/lesion-details/101")
                        .param("loai_benh_an", "CAP")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.vi_tri_ton_thuong").value("Toàn thân"));
    }

    @Test
    @DisplayName("API 57: DELETE /api/v1/lesion-details/{id} - Xóa thành công (200 OK)")
    void deleteLesionDetail_success() throws Exception {
        doNothing().when(lesionDetailsService).deleteLesionDetail("101", "CAP");

        mockMvc.perform(delete("/api/v1/lesion-details/101")
                        .param("loai_benh_an", "CAP"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));
    }

    @Test
    @DisplayName("API 57: DELETE /api/v1/lesion-details/{id} - Không tìm thấy bản ghi trả về 404 (Mã 7010)")
    void deleteLesionDetail_notFound() throws Exception {
        org.mockito.Mockito.doThrow(new AppException(ErrorCode.LESION_DETAIL_NOT_FOUND, "Lesion detail not found"))
                .when(lesionDetailsService).deleteLesionDetail("999", "CAP");

        mockMvc.perform(delete("/api/v1/lesion-details/999")
                        .param("loai_benh_an", "CAP"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("7010"));
    }
}
