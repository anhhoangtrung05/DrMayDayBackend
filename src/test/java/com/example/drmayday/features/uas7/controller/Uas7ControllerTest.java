package com.example.drmayday.features.uas7.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.uas7.dto.*;
import com.example.drmayday.features.uas7.service.Uas7Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class Uas7ControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();
    @Mock Uas7Service uas7Service;
    @InjectMocks Uas7Controller controller;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void api58_eligibility_successAndNoActiveRecord() throws Exception {
        when(uas7Service.checkEligibility()).thenReturn(Uas7EligibilityResponse.builder()
                .eligible(true).activeRecordId("170").activeCycleId("1").currentDayInCycle(1)
                .scoredToday(false).build());
        mockMvc.perform(get("/api/v1/uas7-scores/eligibility"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.is_eligible").value(true));
        when(uas7Service.checkEligibility()).thenThrow(new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD));
        mockMvc.perform(get("/api/v1/uas7-scores/eligibility"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("5004"));
    }

    @Test
    void api59_createScore_successDuplicateAndInvalidScore() throws Exception {
        when(uas7Service.createScore(any())).thenReturn(Uas7ScoreResponse.builder()
                .uas7Id("1").recordId("170").cycleId("1").hssScore((short) 2).issScore((short) 3)
                .totalScore((short) 5).scoreDate(LocalDate.of(2026, 11, 20)).build());
        String body = "{\"record_id\":\"170\",\"hss_score\":2,\"iss_score\":3}";
        mockMvc.perform(post("/api/v1/uas7-scores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.total_score").value(5));
        when(uas7Service.createScore(any())).thenThrow(new AppException(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY));
        mockMvc.perform(post("/api/v1/uas7-scores").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("5002"));
        mockMvc.perform(post("/api/v1/uas7-scores").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"record_id\":\"170\",\"hss_score\":5,\"iss_score\":3}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void api60_getScores_successUnknownPatientAndFilter() throws Exception {
        when(uas7Service.getPatientScores(eq("2"), any(), any(), any())).thenReturn(List.of(Uas7ScoreResponse.builder()
                .uas7Id("1").recordId("170").totalScore((short) 5).build()));
        mockMvc.perform(get("/api/v1/uas7-scores/patient/2").param("from_date", "2026-01-01")
                        .param("to_date", "2026-12-31").param("cycle_id", "1"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data[0].uas7_id").value("1"));
        when(uas7Service.getPatientScores(eq("999"), any(), any(), any()))
                .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));
        mockMvc.perform(get("/api/v1/uas7-scores/patient/999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("3007"));
    }

    @Test
    void api61_createCycle_successAndOverlap() throws Exception {
        when(uas7Service.createCycle(eq("2"), any())).thenReturn(Uas7CycleResponse.builder()
                .cycleId("2").patientId("2").startDate(LocalDate.of(2026, 10, 20)).endDate(LocalDate.of(2026, 10, 26)).build());
        String body = "{\"ngay_bat_dau\":\"2026-10-20\"}";
        mockMvc.perform(post("/api/v1/patients/2/uas7-cycles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("1001"));
        when(uas7Service.createCycle(eq("2"), any())).thenThrow(new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE));
        mockMvc.perform(post("/api/v1/patients/2/uas7-cycles").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("5006"));
    }

    @Test
    void api62_getCycles_success() throws Exception {
        when(uas7Service.getPatientCycles("2")).thenReturn(List.of(Uas7CycleResponse.builder().cycleId("1").patientId("2").validDays(7).build()));
        mockMvc.perform(get("/api/v1/patients/2/uas7-cycles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].cycle_id").value("1"))
                .andExpect(jsonPath("$.data[0].so_ngay_hop_le").value(7));
    }

    @Test
    void api63_detailSuccessAndNotFound() throws Exception {
        when(uas7Service.getCycleDetail("1")).thenReturn(Uas7CycleDetailResponse.builder().cycleId("1").logs(List.of()).build());
        mockMvc.perform(get("/api/v1/uas7-cycles/1")).andExpect(status().isOk())
                .andExpect(jsonPath("$.data.cycle_id").value("1"));
        when(uas7Service.getCycleDetail("999")).thenThrow(new AppException(ErrorCode.UAS7_CYCLE_NOT_FOUND));
        mockMvc.perform(get("/api/v1/uas7-cycles/999")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("5005"));
    }

    @Test
    void api64_statusSuccessAndInvalidTransition() throws Exception {
        when(uas7Service.updateCycleStatus(eq("2"), any())).thenReturn(Uas7CycleResponse.builder().cycleId("2").cycleStatus("HOAN_THANH").build());
        mockMvc.perform(put("/api/v1/uas7-cycles/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trang_thai_chu_ky\":\"HOAN_THANH\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"));
        when(uas7Service.updateCycleStatus(eq("2"), any())).thenThrow(new AppException(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION));
        mockMvc.perform(put("/api/v1/uas7-cycles/2").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"trang_thai_chu_ky\":\"DANG_THEO_DOI\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("5007"));
    }
}
