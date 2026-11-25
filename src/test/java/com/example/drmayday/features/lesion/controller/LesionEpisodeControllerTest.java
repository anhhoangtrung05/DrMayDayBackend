package com.example.drmayday.features.lesion.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.lesion.dto.CreateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.dto.LesionEpisodeResponse;
import com.example.drmayday.features.lesion.dto.UpdateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.service.LesionEpisodeService;
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
import tools.jackson.databind.ObjectMapper;

import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class LesionEpisodeControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private LesionEpisodeService lesionEpisodeService;

    @InjectMocks
    private LesionEpisodeController lesionEpisodeController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(lesionEpisodeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 87: POST /api/v1/lesion-episodes thanh cong -> 201 Created (1001)")
    void createLesionEpisode_success() throws Exception {
        CreateLesionEpisodeRequest req = CreateLesionEpisodeRequest.builder()
                .tenDotTonThuongDa("Đợt bùng phát")
                .hoanCanhXuatHien("Sau an")
                .donViThoiGian("hour")
                .build();

        LesionEpisodeResponse res = LesionEpisodeResponse.builder()
                .episodeId("201")
                .patientId("2")
                .tenDotTonThuongDa("Đợt bùng phát")
                .thoiDiemTao(OffsetDateTime.now())
                .build();

        doReturn(res).when(lesionEpisodeService).createLesionEpisode(any());
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.episode_id").value("201"));
    }

    @Test
    @DisplayName("API 87: POST /api/v1/lesion-episodes JSON sai va cac ma loi 7004, 2003, 3106, 4001")
    void createLesionEpisode_errors() throws Exception {
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{bad json"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));

        doThrow(new AppException(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY))
                .when(lesionEpisodeService).createLesionEpisode(any());
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_dot_ton_thuong_da\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7004"));

        doThrow(new AppException(ErrorCode.INVALID_PARAMETER_VALUE))
                .when(lesionEpisodeService).createLesionEpisode(any());
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_dot_ton_thuong_da\":\"A\",\"ho_so_man_id\":\"10\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2003"));

        doThrow(new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA))
                .when(lesionEpisodeService).createLesionEpisode(any());
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_dot_ton_thuong_da\":\"A\",\"ho_so_man_id\":\"99\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));

        doThrow(new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND))
                .when(lesionEpisodeService).createLesionEpisode(any());
        mockMvc.perform(post("/api/v1/lesion-episodes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_dot_ton_thuong_da\":\"A\",\"ho_so_man_id\":\"999\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("4001"));
    }

    @Test
    @DisplayName("API 88 & 89: GET /api/v1/lesion-episodes va GET /{id}")
    void getEpisodes_allPaths() throws Exception {
        LesionEpisodeResponse ep = LesionEpisodeResponse.builder()
                .episodeId("201").patientId("2").tenDotTonThuongDa("Đợt 1").build();
        doReturn(List.of(ep)).when(lesionEpisodeService).getMyLesionEpisodes(null);
        mockMvc.perform(get("/api/v1/lesion-episodes"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[0].episode_id").value("201"));

        doThrow(new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA))
                .when(lesionEpisodeService).getMyLesionEpisodes("99");
        mockMvc.perform(get("/api/v1/lesion-episodes").param("ho_so_man_id", "99"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("3106"));

        doReturn(ep).when(lesionEpisodeService).getLesionEpisode("201");
        mockMvc.perform(get("/api/v1/lesion-episodes/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));

        doThrow(new AppException(ErrorCode.LESION_EPISODE_NOT_FOUND))
                .when(lesionEpisodeService).getLesionEpisode("999");
        mockMvc.perform(get("/api/v1/lesion-episodes/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("7001"));
    }

    @Test
    @DisplayName("API 90 & 91: PUT va DELETE /api/v1/lesion-episodes/{id}")
    void editAndDeleteEpisode_allPaths() throws Exception {
        UpdateLesionEpisodeRequest req = UpdateLesionEpisodeRequest.builder()
                .tenDotTonThuongDa("Tên mới").build();
        LesionEpisodeResponse res = LesionEpisodeResponse.builder()
                .episodeId("201").tenDotTonThuongDa("Tên mới").build();
        doReturn(res).when(lesionEpisodeService).editLesionEpisode(eq("201"), any());

        mockMvc.perform(put("/api/v1/lesion-episodes/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.ten_dot_ton_thuong_da").value("Tên mới"));

        doThrow(new AppException(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY))
                .when(lesionEpisodeService).editLesionEpisode(eq("201"), any());
        mockMvc.perform(put("/api/v1/lesion-episodes/201")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"ten_dot_ton_thuong_da\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("7004"));

        mockMvc.perform(delete("/api/v1/lesion-episodes/201"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"));

        doThrow(new AppException(ErrorCode.EPISODE_HAS_CHILD_DATA))
                .when(lesionEpisodeService).deleteLesionEpisode("201");
        mockMvc.perform(delete("/api/v1/lesion-episodes/201"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("7009"));
    }
}
