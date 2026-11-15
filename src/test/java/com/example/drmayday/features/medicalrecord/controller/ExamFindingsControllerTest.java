package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.medicalrecord.dto.AcuteExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.ChronicExamFindingsResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicExamFindingsRequest;
import com.example.drmayday.features.medicalrecord.service.ExamFindingsService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ExamFindingsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private ExamFindingsService examFindingsService;

    @InjectMocks
    private ExamFindingsController examFindingsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(examFindingsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 50: PUT /api/v1/acute-exam-findings - Thành công (200 OK)")
    void upsertAcuteExamFindings_success() throws Exception {
        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("1")
                .coSanPhu(true)
                .coAnhSanPhu(false)
                .trieuChungDaNiemMac(true)
                .soLanNon(0)
                .build();

        AcuteExamFindingsResponse response = AcuteExamFindingsResponse.builder()
                .examId("1")
                .loaiBenhAn("CAP")
                .coSanPhu(true)
                .coAnhSanPhu(false)
                .trieuChungDaNiemMac(true)
                .soLanNon(0)
                .build();

        when(examFindingsService.upsertAcuteExamFindings(any(UpsertAcuteExamFindingsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/acute-exam-findings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Cập nhật triệu chứng phiên khám cấp thành công"))
                .andExpect(jsonPath("$.data.exam_id").value("1"))
                .andExpect(jsonPath("$.data.loai_benh_an").value("CAP"))
                .andExpect(jsonPath("$.data.co_san_phu").value(true));
    }

    @Test
    @DisplayName("API 50: PUT /api/v1/acute-exam-findings - Type mismatch trả về 409 (Mã 4008)")
    void upsertAcuteExamFindings_mismatch() throws Exception {
        UpsertAcuteExamFindingsRequest req = UpsertAcuteExamFindingsRequest.builder()
                .examId("2")
                .build();

        when(examFindingsService.upsertAcuteExamFindings(any(UpsertAcuteExamFindingsRequest.class)))
                .thenThrow(new AppException(ErrorCode.RECORD_EXAM_TYPE_MISMATCH, "Record/Exam type mismatch"));

        mockMvc.perform(put("/api/v1/acute-exam-findings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("4008"));
    }

    @Test
    @DisplayName("API 51: PUT /api/v1/chronic-exam-findings - Thành công (200 OK)")
    void upsertChronicExamFindings_success() throws Exception {
        UpsertChronicExamFindingsRequest req = UpsertChronicExamFindingsRequest.builder()
                .examId("2")
                .hienTaiCoSanPhu(true)
                .trongDotCoPhuMach(true)
                .phuViTriMoi(true)
                .diemAas7(28)
                .build();

        ChronicExamFindingsResponse response = ChronicExamFindingsResponse.builder()
                .examId("2")
                .loaiBenhAn("MAN")
                .hienTaiCoSanPhu(true)
                .trongDotCoPhuMach(true)
                .phuViTriMoi(true)
                .diemAas7(28)
                .build();

        when(examFindingsService.upsertChronicExamFindings(any(UpsertChronicExamFindingsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/chronic-exam-findings")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Cập nhật triệu chứng phiên khám mạn thành công"))
                .andExpect(jsonPath("$.data.exam_id").value("2"))
                .andExpect(jsonPath("$.data.loai_benh_an").value("MAN"))
                .andExpect(jsonPath("$.data.diem_aas7").value(28));
    }
}
