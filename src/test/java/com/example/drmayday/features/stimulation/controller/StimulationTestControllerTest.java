package com.example.drmayday.features.stimulation.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultInput;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionDetailResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionResponse;
import com.example.drmayday.features.stimulation.dto.UpsertStimulationTestSessionRequest;
import com.example.drmayday.features.stimulation.service.StimulationTestService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StimulationTestControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StimulationTestService stimulationTestService;

    @InjectMocks
    private StimulationTestController stimulationTestController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(stimulationTestController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 109-110: PUT & GET stimulation-test-session")
    void session_endpoints() throws Exception {
        when(stimulationTestService.upsertSession(eq(2L), eq(10L), any(UpsertStimulationTestSessionRequest.class)))
                .thenReturn(StimulationTestSessionResponse.builder()
                        .maPhienNghiemPhap(55L).maPhienKham(10L)
                        .ngungKhangHistamine(true).thoiGianNgungHistamine(7).ngungCorticoid(false).build());

        mockMvc.perform(put("/api/v1/medical-records/2/exams/10/stimulation-test-session")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ngung_khang_histamine\":true,\"thoi_gian_ngung_histamine\":7,\"ngung_corticoid\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.ma_phien_nghiem_phap").value(55));

        when(stimulationTestService.getSessionDetail(2L, 10L))
                .thenReturn(StimulationTestSessionDetailResponse.builder()
                        .maPhienNghiemPhap(55L).maPhienKham(10L).results(List.of()).build());

        mockMvc.perform(get("/api/v1/medical-records/2/exams/10/stimulation-test-session"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.results").isArray());

        when(stimulationTestService.getSessionDetail(2L, 99L))
                .thenThrow(new AppException(ErrorCode.RECORD_TYPE_MISMATCH));
        mockMvc.perform(get("/api/v1/medical-records/2/exams/99/stimulation-test-session"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("4008"));
    }

    @Test
    @DisplayName("API 111-113: POST bulk results, PUT & DELETE /api/v1/stimulation-test-results/{id}")
    void results_endpoints() throws Exception {
        when(stimulationTestService.createResultsBulk(eq(2L), eq(10L), anyList()))
                .thenReturn(List.of(StimulationTestResultResponse.builder()
                        .id(101L).maPhienNghiemPhap(55L).maNghiemPhap(1L)
                        .laCanNguyenKhac(false).ketQua("Duong tinh").build()));

        mockMvc.perform(post("/api/v1/medical-records/2/exams/10/stimulation-test-session/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"ma_nghiem_phap\":1,\"la_can_nguyen_khac\":false,\"ket_qua\":\"Duong tinh\"}]"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.length()").value(1));

        when(stimulationTestService.createResultsBulk(eq(2L), eq(10L), anyList()))
                .thenThrow(new AppException(ErrorCode.SYMPTOM_SCORE_OUT_OF_RANGE));
        mockMvc.perform(post("/api/v1/medical-records/2/exams/10/stimulation-test-session/results")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[{\"ma_nghiem_phap\":1,\"la_can_nguyen_khac\":false,\"ket_qua\":\"Duong tinh\",\"diem_ngua_nrs\":12}]"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7007"));

        when(stimulationTestService.updateResult(eq(101L), any(StimulationTestResultInput.class)))
                .thenReturn(StimulationTestResultResponse.builder().id(101L).ketQua("Am tinh").build());
        mockMvc.perform(put("/api/v1/stimulation-test-results/101")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ma_nghiem_phap\":1,\"la_can_nguyen_khac\":false,\"ket_qua\":\"Am tinh\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.ket_qua").value("Am tinh"));

        doNothing().when(stimulationTestService).deleteResult(101L);
        mockMvc.perform(delete("/api/v1/stimulation-test-results/101"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));

        mockMvc.perform(delete("/api/v1/stimulation-test-results/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2002"));

        doThrow(new AppException(ErrorCode.STIMULATION_TEST_RESULT_NOT_FOUND))
                .when(stimulationTestService).deleteResult(999L);
        mockMvc.perform(delete("/api/v1/stimulation-test-results/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("8304"));
    }
}
