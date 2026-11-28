package com.example.drmayday.features.rbac.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.rbac.dto.CreateRoleRequest;
import com.example.drmayday.features.rbac.dto.RoleResponse;
import com.example.drmayday.features.rbac.service.RoleService;
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
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RoleControllerTest {

    private MockMvc mockMvc;

    @Mock
    private RoleService roleService;

    @InjectMocks
    private RoleController roleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(roleController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 15: POST /api/v1/roles - Tạo vai trò thành công (201 Created, mã 1001)")
    void setRole_success() throws Exception {
        RoleResponse response = RoleResponse.builder()
                .roleId("5")
                .roleName("duoc_si")
                .displayName("Dược sĩ")
                .description("Quản lý thuốc")
                .build();
        when(roleService.createRole(any(CreateRoleRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"duoc_si\",\"display_name\":\"Dược sĩ\",\"description\":\"Quản lý thuốc\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.role_id").value("5"))
                .andExpect(jsonPath("$.data.role_name").value("duoc_si"))
                .andExpect(jsonPath("$.data.display_name").value("Dược sĩ"));
    }

    @Test
    @DisplayName("API 15: POST /api/v1/roles - Thiếu role_name trả về 400 (2001)")
    void setRole_missingRoleName() throws Exception {
        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Thiếu tên\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("API 15: POST /api/v1/roles - Trùng role_name trả về 409 (3107)")
    void setRole_duplicate() throws Exception {
        when(roleService.createRole(any(CreateRoleRequest.class)))
                .thenThrow(new AppException(ErrorCode.ROLE_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/roles")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"admin\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3107"));
    }

    @Test
    @DisplayName("API 16: GET /api/v1/roles - Lấy danh sách vai trò thành công (200 OK)")
    void getRoles_success() throws Exception {
        when(roleService.getRoles()).thenReturn(List.of(
                RoleResponse.builder().roleId("1").roleName("admin").displayName("Quản trị viên").build(),
                RoleResponse.builder().roleId("2").roleName("bac_si").displayName("Bác sĩ").build()
        ));

        mockMvc.perform(get("/api/v1/roles"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].role_name").value("admin"))
                .andExpect(jsonPath("$.data[1].role_name").value("bac_si"));
    }
}
