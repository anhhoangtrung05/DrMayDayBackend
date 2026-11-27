package com.example.drmayday.features.testtype.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.testtype.dto.CreateTestTypeRequest;
import com.example.drmayday.features.testtype.dto.TestTypeResponse;
import com.example.drmayday.features.testtype.dto.UpdateTestTypeRequest;
import com.example.drmayday.features.testtype.service.TestTypeService;
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
class TestTypeControllerTest {

    private MockMvc mockMvc;

    @Mock
    private TestTypeService testTypeService;

    @InjectMocks
    private TestTypeController testTypeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(testTypeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 104: POST /api/v1/test-types returns 201 (1001) and maps 8302 to 409")
    void createTestType_endpoints() throws Exception {
        when(testTypeService.createTestType(any(CreateTestTypeRequest.class)))
                .thenReturn(TestTypeResponse.builder()
                        .maNghiemPhap(10L).tenNghiemPhap("Test áp lực")
                        .phuongPhapThucHien("Treo tạ").thoiGianChuan(15).laCanNguyenKhac(false).build());

        mockMvc.perform(post("/api/v1/test-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_nghiem_phap\":\"Test áp lực\",\"phuong_phap_thuc_hien\":\"Treo tạ\",\"thoi_gian_chuan\":15}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.ma_nghiem_phap").value(10))
                .andExpect(jsonPath("$.data.la_can_nguyen_khac").value(false));

        when(testTypeService.createTestType(any(CreateTestTypeRequest.class)))
                .thenThrow(new AppException(ErrorCode.TEST_TYPE_ALREADY_EXISTS));
        mockMvc.perform(post("/api/v1/test-types")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_nghiem_phap\":\"Test áp lực\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("8302"));
    }

    @Test
    @DisplayName("API 105-106: GET /api/v1/test-types and /{id} (including invalid type 2002 & 404 8301)")
    void getTestTypes_endpoints() throws Exception {
        when(testTypeService.getTestTypes()).thenReturn(List.of(
                TestTypeResponse.builder().maNghiemPhap(1L).tenNghiemPhap("Da vẽ nổi").laCanNguyenKhac(false).build()
        ));

        mockMvc.perform(get("/api/v1/test-types"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.length()").value(1));

        when(testTypeService.getTestType(1L)).thenReturn(
                TestTypeResponse.builder().maNghiemPhap(1L).tenNghiemPhap("Da vẽ nổi").laCanNguyenKhac(false).build());
        mockMvc.perform(get("/api/v1/test-types/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.ma_nghiem_phap").value(1));

        mockMvc.perform(get("/api/v1/test-types/abc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2002"));

        when(testTypeService.getTestType(999L)).thenThrow(new AppException(ErrorCode.TEST_TYPE_NOT_FOUND));
        mockMvc.perform(get("/api/v1/test-types/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("8301"));
    }

    @Test
    @DisplayName("API 107-108: PUT & DELETE /api/v1/test-types/{id}")
    void updateDelete_endpoints() throws Exception {
        when(testTypeService.updateTestType(eq(1L), any(UpdateTestTypeRequest.class)))
                .thenReturn(TestTypeResponse.builder().maNghiemPhap(1L).tenNghiemPhap("Da vẽ nổi mới").laCanNguyenKhac(false).build());

        mockMvc.perform(put("/api/v1/test-types/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_nghiem_phap\":\"Da vẽ nổi mới\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.ten_nghiem_phap").value("Da vẽ nổi mới"));

        doNothing().when(testTypeService).deleteTestType(10L);
        mockMvc.perform(delete("/api/v1/test-types/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));

        doThrow(new AppException(ErrorCode.TEST_TYPE_IN_USE)).when(testTypeService).deleteTestType(1L);
        mockMvc.perform(delete("/api/v1/test-types/1"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("8303"));
    }
}
