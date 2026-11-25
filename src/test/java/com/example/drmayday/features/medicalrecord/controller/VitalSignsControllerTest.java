package com.example.drmayday.features.medicalrecord.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.medicalrecord.dto.AddVitalSignsRequest;
import com.example.drmayday.features.medicalrecord.dto.VitalSignsResponse;
import com.example.drmayday.features.medicalrecord.service.VitalSignsService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class VitalSignsControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private VitalSignsService vitalSignsService;

    @InjectMocks
    private VitalSignsController vitalSignsController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(vitalSignsController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 52: POST /api/v1/vital-signs - Thành công (201 Created)")
    void addVitalSigns_success() throws Exception {
        AddVitalSignsRequest req = AddVitalSignsRequest.builder()
                .examId("1")
                .machLanPhut(80)
                .huyetApTamThu(120)
                .huyetApTamTruong(80)
                .nhipThoLanPhut(18)
                .spo2PhanTram(BigDecimal.valueOf(98))
                .build();

        VitalSignsResponse response = VitalSignsResponse.builder()
                .vitalSignId("10")
                .examId("1")
                .machLanPhut(80)
                .huyetApTamThu(120)
                .huyetApTamTruong(80)
                .nhipThoLanPhut(18)
                .spo2PhanTram(BigDecimal.valueOf(98))
                .build();

        when(vitalSignsService.addVitalSigns(any(AddVitalSignsRequest.class)))
                .thenReturn(response);

        mockMvc.perform(post("/api/v1/vital-signs")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.vital_sign_id").value("10"))
                .andExpect(jsonPath("$.data.mach_lan_phut").value(80));
    }

    @Test
    @DisplayName("API 53: GET /api/v1/vital-signs - Lấy danh sách thành công (200 OK)")
    void getListVitalSigns_success() throws Exception {
        VitalSignsResponse item = VitalSignsResponse.builder()
                .vitalSignId("10")
                .examId("1")
                .machLanPhut(80)
                .build();

        when(vitalSignsService.getListVitalSigns("1"))
                .thenReturn(List.of(item));

        mockMvc.perform(get("/api/v1/vital-signs").param("exam_id", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data[0].vital_sign_id").value("10"));
    }

    @Test
    @DisplayName("API 53: GET /api/v1/vital-signs - Thiếu exam_id trả về 400 (Mã 2001)")
    void getListVitalSigns_missingParam() throws Exception {
        when(vitalSignsService.getListVitalSigns(null))
                .thenThrow(new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Missing required parameter: exam_id"));

        mockMvc.perform(get("/api/v1/vital-signs"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }
}
