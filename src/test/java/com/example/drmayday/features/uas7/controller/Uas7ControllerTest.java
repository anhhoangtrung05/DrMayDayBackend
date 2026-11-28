package com.example.drmayday.features.uas7.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.uas7.dto.CreateUas7CycleRequest;
import com.example.drmayday.features.uas7.dto.CreateUas7ScoreRequest;
import com.example.drmayday.features.uas7.dto.Uas7CycleDetailResponse;
import com.example.drmayday.features.uas7.dto.Uas7CycleResponse;
import com.example.drmayday.features.uas7.dto.Uas7EligibilityResponse;
import com.example.drmayday.features.uas7.dto.Uas7ScoreResponse;
import com.example.drmayday.features.uas7.dto.UpdateUas7CycleStatusRequest;
import com.example.drmayday.features.uas7.service.Uas7Service;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class Uas7ControllerTest {

    private MockMvc mockMvc;

    @Mock
    private Uas7Service uas7Service;

    @InjectMocks
    private Uas7Controller uas7Controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(uas7Controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Nested
    @DisplayName("API 58: GET /api/v1/uas7-scores/eligibility")
    class EligibilityTests {

        @Test
        @DisplayName("[Happy Path] Returns eligibility including explicit null fields")
        void checkEligibility_success() throws Exception {
            when(uas7Service.checkEligibility()).thenReturn(Uas7EligibilityResponse.builder()
                    .eligible(true)
                    .activeRecordId("20")
                    .activeCycleId("30")
                    .currentDayInCycle(3)
                    .scoredToday(false)
                    .todayScoreId(null)
                    .build());

            mockMvc.perform(get("/api/v1/uas7-scores/eligibility"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.is_eligible").value(true))
                    .andExpect(jsonPath("$.data.active_record_id").value("20"))
                    .andExpect(jsonPath("$.data.active_cycle_id").value("30"))
                    .andExpect(jsonPath("$.data.current_day_in_cycle").value(3))
                    .andExpect(jsonPath("$.data.scored_today").value(false))
                    .andExpect(jsonPath("$.data.today_score_id").isEmpty());
        }

        @Test
        @DisplayName("[Unhappy Path] No active MAN record — returns 400 (5004)")
        void checkEligibility_noRecord() throws Exception {
            when(uas7Service.checkEligibility()).thenThrow(new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD));

            mockMvc.perform(get("/api/v1/uas7-scores/eligibility"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("5004"));
        }
    }

    @Nested
    @DisplayName("API 59: POST /api/v1/uas7-scores")
    class CreateScoreTests {

        @Test
        @DisplayName("[Happy Path] Creates score and returns 201 (1001)")
        void createScore_success() throws Exception {
            when(uas7Service.createScore(any(CreateUas7ScoreRequest.class))).thenReturn(Uas7ScoreResponse.builder()
                    .uas7Id("101")
                    .recordId("20")
                    .cycleId("30")
                    .hssScore(2)
                    .issScore(3)
                    .totalScore(5)
                    .ngayChamDiem(LocalDate.of(2026, 11, 20))
                    .ngayThuMayTrongChuKy(1)
                    .trangThaiNgay("HOP_LE")
                    .trangThaiDungThuoc("DUNG_DEU")
                    .createdAt(OffsetDateTime.parse("2026-11-20T09:00:00Z"))
                    .build());

            mockMvc.perform(post("/api/v1/uas7-scores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\":20,\"hss_score\":2,\"iss_score\":3,\"date\":\"2026-11-20\",\"trang_thai_dung_thuoc\":\"DUNG_DEU\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.uas7_id").value("101"))
                    .andExpect(jsonPath("$.data.total_score").value(5))
                    .andExpect(jsonPath("$.data.ngay_thu_may_trong_chu_ky").value(1));
        }

        @Test
        @DisplayName("[Unhappy Path] Score out of range — returns 400 (5003)")
        void createScore_outOfRange() throws Exception {
            when(uas7Service.createScore(any(CreateUas7ScoreRequest.class)))
                    .thenThrow(new AppException(ErrorCode.UAS7_SCORE_OUT_OF_RANGE));

            mockMvc.perform(post("/api/v1/uas7-scores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\":20,\"hss_score\":2,\"iss_score\":5}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("5003"));
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate score today — returns 409 (5002)")
        void createScore_duplicate() throws Exception {
            when(uas7Service.createScore(any(CreateUas7ScoreRequest.class)))
                    .thenThrow(new AppException(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY));

            mockMvc.perform(post("/api/v1/uas7-scores")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"record_id\":20,\"hss_score\":2,\"iss_score\":3}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("5002"));
        }
    }

    @Nested
    @DisplayName("API 60: GET /api/v1/uas7-scores/patient/{patient_id}")
    class GetPatientScoresTests {

        @Test
        @DisplayName("[Happy Path] Returns filtered scores")
        void getScores_success() throws Exception {
            LocalDate from = LocalDate.of(2026, 11, 1);
            LocalDate to = LocalDate.of(2026, 11, 30);
            when(uas7Service.getPatientScores("2", from, to, "30")).thenReturn(List.of(
                    Uas7ScoreResponse.builder().uas7Id("101").recordId("20").cycleId("30").totalScore(5).build()
            ));

            mockMvc.perform(get("/api/v1/uas7-scores/patient/2")
                            .param("from_date", "2026-11-01")
                            .param("to_date", "2026-11-30")
                            .param("cycle_id", "30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(1))
                    .andExpect(jsonPath("$.data[0].uas7_id").value("101"));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid date format in query — returns 400 (2002)")
        void getScores_invalidDate() throws Exception {
            mockMvc.perform(get("/api/v1/uas7-scores/patient/2")
                            .param("from_date", "20-11-2026"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2002"));
        }
    }

    @Nested
    @DisplayName("API 61-64: UAS7 Cycles")
    class CycleTests {

        @Test
        @DisplayName("API 61: POST /api/v1/patients/{patient_id}/uas7-cycles - Returns 201 (1001)")
        void createCycle_success() throws Exception {
            when(uas7Service.createCycle(eq("2"), any(CreateUas7CycleRequest.class)))
                    .thenReturn(Uas7CycleResponse.builder()
                            .cycleId("30")
                            .patientId("2")
                            .ngayBatDau(LocalDate.of(2026, 10, 20))
                            .ngayKetThuc(LocalDate.of(2026, 10, 26))
                            .trangThaiChuKy("DANG_THEO_DOI")
                            .soNgayHopLe(0)
                            .build());

            mockMvc.perform(post("/api/v1/patients/2/uas7-cycles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ngay_bat_dau\":\"2026-10-20\"}"))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.code").value("1001"))
                    .andExpect(jsonPath("$.data.cycle_id").value("30"))
                    .andExpect(jsonPath("$.data.ngay_ket_thuc").value("2026-10-26"));
        }

        @Test
        @DisplayName("API 61: POST /api/v1/patients/{patient_id}/uas7-cycles - Overlapping cycle returns 409 (5006)")
        void createCycle_overlap() throws Exception {
            when(uas7Service.createCycle(eq("2"), any(CreateUas7CycleRequest.class)))
                    .thenThrow(new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE));

            mockMvc.perform(post("/api/v1/patients/2/uas7-cycles")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ngay_bat_dau\":\"2026-10-20\"}"))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.code").value("5006"));
        }

        @Test
        @DisplayName("API 62: GET /api/v1/patients/{patient_id}/uas7-cycles - Returns 200 (1000)")
        void getPatientCycles_success() throws Exception {
            when(uas7Service.getPatientCycles("2")).thenReturn(List.of(
                    Uas7CycleResponse.builder().cycleId("30").patientId("2").soNgayHopLe(7).trangThaiChuKy("HOAN_THANH").build(),
                    Uas7CycleResponse.builder().cycleId("31").patientId("2").soNgayHopLe(3).trangThaiChuKy("DANG_THEO_DOI").build()
            ));

            mockMvc.perform(get("/api/v1/patients/2/uas7-cycles"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.length()").value(2))
                    .andExpect(jsonPath("$.data[0].so_ngay_hop_le").value(7))
                    .andExpect(jsonPath("$.data[1].so_ngay_hop_le").value(3));
        }

        @Test
        @DisplayName("API 63: GET /api/v1/uas7-cycles/{id} - Returns 200 (1000)")
        void getCycleDetail_success() throws Exception {
            when(uas7Service.getCycleDetail("30")).thenReturn(Uas7CycleDetailResponse.builder()
                    .cycleId("30")
                    .patientId("2")
                    .soNgayHopLe(4)
                    .logs(List.of(Uas7ScoreResponse.builder().uas7Id("1").totalScore(4).build()))
                    .build());

            mockMvc.perform(get("/api/v1/uas7-cycles/30"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.so_ngay_hop_le").value(4))
                    .andExpect(jsonPath("$.data.logs.length()").value(1));
        }

        @Test
        @DisplayName("API 63: GET /api/v1/uas7-cycles/{id} - Not found returns 404 (5005)")
        void getCycleDetail_notFound() throws Exception {
            when(uas7Service.getCycleDetail("999")).thenThrow(new AppException(ErrorCode.UAS7_CYCLE_NOT_FOUND));

            mockMvc.perform(get("/api/v1/uas7-cycles/999"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("5005"));
        }

        @Test
        @DisplayName("API 64: PUT /api/v1/uas7-cycles/{id} - Updates status returns 200 (1000)")
        void updateCycleStatus_success() throws Exception {
            when(uas7Service.updateCycleStatus(eq("30"), any(UpdateUas7CycleStatusRequest.class)))
                    .thenReturn(Uas7CycleResponse.builder()
                            .cycleId("30")
                            .patientId("2")
                            .trangThaiChuKy("HOAN_THANH")
                            .soNgayHopLe(7)
                            .thoiDiemCapNhat(OffsetDateTime.parse("2026-10-26T12:00:00Z"))
                            .build());

            mockMvc.perform(put("/api/v1/uas7-cycles/30")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trang_thai_chu_ky\":\"HOAN_THANH\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.trang_thai_chu_ky").value("HOAN_THANH"));
        }

        @Test
        @DisplayName("API 64: PUT /api/v1/uas7-cycles/{id} - Invalid transition returns 400 (5007)")
        void updateCycleStatus_invalidTransition() throws Exception {
            when(uas7Service.updateCycleStatus(eq("30"), any(UpdateUas7CycleStatusRequest.class)))
                    .thenThrow(new AppException(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION));

            mockMvc.perform(put("/api/v1/uas7-cycles/30")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"trang_thai_chu_ky\":\"DANG_THEO_DOI\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("5007"));
        }
    }
}
