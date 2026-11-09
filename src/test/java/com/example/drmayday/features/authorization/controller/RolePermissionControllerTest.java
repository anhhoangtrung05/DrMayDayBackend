package com.example.drmayday.features.authorization.controller;

import com.example.drmayday.features.authorization.dto.SetRolePermissionsRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.features.authorization.service.RolePermissionService;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class RolePermissionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private RolePermissionService rolePermissionService;

    @InjectMocks
    private RolePermissionController rolePermissionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(rolePermissionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 21: POST /api/v1/roles/{role_id}/permissions - Gán quyền hạn thành công (200 OK)")
    void setRolePermissions_success() throws Exception {
        SetRolePermissionsRequest request = SetRolePermissionsRequest.builder()
                .permissionIds(List.of("1", "2", "3"))
                .build();

        doNothing().when(rolePermissionService).setRolePermissions(eq("1"), any(SetRolePermissionsRequest.class));

        mockMvc.perform(post("/api/v1/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(rolePermissionService, times(1)).setRolePermissions(eq("1"), any(SetRolePermissionsRequest.class));
    }

    @Test
    @DisplayName("API 22: GET /api/v1/roles/{role_id}/permissions - Lấy danh sách thành công (200 OK)")
    void getRolePermissions_success() throws Exception {
        List<PermissionResponse> permissions = List.of(
                PermissionResponse.builder()
                        .permissionId("1")
                        .permissionName("manage_users")
                        .description("Quản lý người dùng")
                        .build(),
                PermissionResponse.builder()
                        .permissionId("2")
                        .permissionName("manage_medical_records")
                        .description("Quản lý bệnh án")
                        .build()
        );

        when(rolePermissionService.getRolePermissions("1")).thenReturn(permissions);

        mockMvc.perform(get("/api/v1/roles/1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data[0].permission_id").value("1"))
                .andExpect(jsonPath("$.data[0].permission_name").value("manage_users"))
                .andExpect(jsonPath("$.data[1].permission_id").value("2"))
                .andExpect(jsonPath("$.data[1].permission_name").value("manage_medical_records"));

        verify(rolePermissionService, times(1)).getRolePermissions("1");
    }

    @Test
    @DisplayName("API 22: GET /api/v1/roles/{role_id}/permissions - Vai trò không tồn tại (404)")
    void getRolePermissions_roleNotFound() throws Exception {
        when(rolePermissionService.getRolePermissions("999"))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy vai trò với id: 999"));

        mockMvc.perform(get("/api/v1/roles/999/permissions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("2006"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy vai trò với id: 999"));
    }

    @Test
    @DisplayName("API 23: DELETE /api/v1/roles/{role_id}/permissions/{permission_id} - Thu hồi quyền thành công (200 OK)")
    void deleteRolePermission_success() throws Exception {
        doNothing().when(rolePermissionService).deleteRolePermission("1", "2");

        mockMvc.perform(delete("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(rolePermissionService, times(1)).deleteRolePermission("1", "2");
    }
}
