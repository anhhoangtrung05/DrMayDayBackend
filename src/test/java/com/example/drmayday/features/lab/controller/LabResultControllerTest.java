package com.example.drmayday.features.lab.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.lab.dto.LabResultResponse;
import com.example.drmayday.features.lab.dto.UpsertLabResultsRequest;
import com.example.drmayday.features.lab.service.LabResultService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LabResultControllerTest {

    private static final String BASE = "/api/v1/lab-results";

    private MockMvc mockMvc;

    @Mock
    private LabResultService labResultService;

    @InjectMocks
    private LabResultController controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private static LabResultResponse acuteResponse(boolean created) {
        return LabResultResponse.builder()
                .created(created).labResultId("500").recordId("1").phienKhamId("10").loai("CAP")
                .wbc(new BigDecimal("8.1")).neu(new BigDecimal("4")).lym(new BigDecimal("2"))
                .nlr(new BigDecimal("2.000000"))
                .build();
    }

    // ── LAB_UPSERT_LAB_RESULTS ──

    @Nested
    @DisplayName("PUT /api/v1/lab-results")
    class UpsertLabResults {

        @Test
        @DisplayName("[Happy Path] Nhập kết quả lần đầu trả về 201 và mã 1001")
        void returnsCreated() throws Exception {
            when(labResultService.upsertLabResults(any(UpsertLabResultsRequest.class))).thenReturn(acuteResponse(true));

            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON).content("""
                            {"record_id": 1, "phien_kham_id": 10, "wbc": 8.1, "neu": 4, "lym": 2,
                             "thoi_diem_thuc_hien": "2026-11-24T08:30:00+07:00"}
                            """))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.lab_result_id").value("500"))
                    .andExpect(jsonPath("$.data.loai").value("CAP"))
                    .andExpect(jsonPath("$.data.nlr").value(2.0))
                    .andExpect(jsonPath("$.data.created").doesNotExist());

            ArgumentCaptor<UpsertLabResultsRequest> captor = ArgumentCaptor.forClass(UpsertLabResultsRequest.class);
            verify(labResultService).upsertLabResults(captor.capture());
            assertEquals(new BigDecimal("8.1"), captor.getValue().getWbc());
            assertEquals(1L, captor.getValue().getRecordId());
        }

        @Test
        @DisplayName("[Happy Path] Nhập lại kết quả đã có trả về 200 và mã 1000")
        void returnsOk() throws Exception {
            when(labResultService.upsertLabResults(any())).thenReturn(acuteResponse(false));

            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"crp\": 12.5}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));
        }

        @Test
        @DisplayName("[Unhappy Path] Field không khớp loại phiên trả về 400 và mã 8103")
        void fieldTypeMismatch() throws Exception {
            when(labResultService.upsertLabResults(any()))
                    .thenThrow(new AppException(ErrorCode.LAB_RESULT_FIELD_TYPE_MISMATCH));

            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"lym\": 2}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("8103"));
        }

        @Test
        @DisplayName("[Unhappy Path] asst_duong_kinh_mm khi test huyết thanh không dương tính trả về 400 và mã 8105")
        void asstWithoutPositiveSerumTest() throws Exception {
            when(labResultService.upsertLabResults(any()))
                    .thenThrow(new AppException(ErrorCode.ASST_REQUIRES_POSITIVE_SERUM_TEST));

            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"asst_duong_kinh_mm\": 5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("8105"));
        }

        @Test
        @DisplayName("[Unhappy Path] Chỉ số không phải số trả về 400 và mã 2003")
        void nonNumericValue() throws Exception {
            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 1, \"phien_kham_id\": 10, \"wbc\": \"cao\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"))
                    .andExpect(jsonPath("$.message").value("Giá trị của trường wbc không đúng kiểu dữ liệu"));
            verify(labResultService, never()).upsertLabResults(any());
        }

        @Test
        @DisplayName("[Exception Path] Hồ sơ bệnh án không tồn tại trả về 404 và mã 4001")
        void recordNotFound() throws Exception {
            when(labResultService.upsertLabResults(any())).thenThrow(new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

            mockMvc.perform(put(BASE).contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\": 999, \"phien_kham_id\": 10}"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("4001"));
        }
    }

    // ── LAB_GET_LAB_RESULTS ──

    @Nested
    @DisplayName("GET /api/v1/lab-results")
    class GetLabResults {

        @Test
        @DisplayName("[Happy Path] Phiên đã có kết quả trả về 200 và mã 1000")
        void returnsResult() throws Exception {
            when(labResultService.getLabResults(10L)).thenReturn(acuteResponse(false));

            mockMvc.perform(get(BASE).param("phien_kham_id", "10"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.phien_kham_id").value("10"))
                    .andExpect(jsonPath("$.data.neu").value(4));
        }

        @Test
        @DisplayName("[Exception Path] Phiên chưa có kết quả trả về 404 và mã 8104")
        void resultNotFound() throws Exception {
            when(labResultService.getLabResults(10L)).thenThrow(new AppException(ErrorCode.LAB_RESULT_NOT_FOUND));

            mockMvc.perform(get(BASE).param("phien_kham_id", "10"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("8104"));
        }

        @Test
        @DisplayName("[Unhappy Path] Thiếu phien_kham_id trả về 400 và mã 2001")
        void missingVisitId() throws Exception {
            when(labResultService.getLabResults(null))
                    .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: phien_kham_id"));

            mockMvc.perform(get(BASE))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));
        }
    }
}
