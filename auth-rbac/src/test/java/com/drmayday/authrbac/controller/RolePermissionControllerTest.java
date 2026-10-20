package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.config.DevAuthFilter;
import com.drmayday.authrbac.config.SecurityConfig;
import com.drmayday.authrbac.dto.request.SetRolePermissionsRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.service.RolePermissionService;
import com.drmayday.common.exception.GlobalExceptionHandler;
import com.drmayday.common.exception.ResourceNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(RolePermissionController.class)
@Import({SecurityConfig.class, DevAuthFilter.class, GlobalExceptionHandler.class})
class RolePermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private RolePermissionService rolePermissionService;

    @Test
    @DisplayName("POST /api/v1/roles/{role_id}/permissions - Thành công với ROLE_ADMIN (Mã 1000 | OK)")
    @WithMockUser(roles = "ADMIN")
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
    @DisplayName("POST /api/v1/roles/{role_id}/permissions - Bị từ chối 403 khi không có ROLE_ADMIN")
    @WithMockUser(roles = "NURSE")
    void setRolePermissions_forbidden() throws Exception {
        SetRolePermissionsRequest request = SetRolePermissionsRequest.builder()
                .permissionIds(List.of("1", "2"))
                .build();

        mockMvc.perform(post("/api/v1/roles/1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"));
    }

    @Test
    @DisplayName("GET /api/v1/roles/{role_id}/permissions - Thành công (Mã 1000 | OK)")
    @WithMockUser(roles = "USER")
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
                        .description("Quản lý hồ sơ bệnh án")
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
    @DisplayName("GET /api/v1/roles/{role_id}/permissions - 404 khi vai trò không tồn tại")
    @WithMockUser
    void getRolePermissions_notFound() throws Exception {
        when(rolePermissionService.getRolePermissions("999"))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy vai trò với id: 999"));

        mockMvc.perform(get("/api/v1/roles/999/permissions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("1002"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy vai trò với id: 999"));
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{role_id}/permissions/{permission_id} - Thành công với ROLE_ADMIN (Mã 1000 | OK)")
    @WithMockUser(roles = "ADMIN")
    void deleteRolePermission_success() throws Exception {
        doNothing().when(rolePermissionService).deleteRolePermission("1", "2");

        mockMvc.perform(delete("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(rolePermissionService, times(1)).deleteRolePermission("1", "2");
    }

    @Test
    @DisplayName("DELETE /api/v1/roles/{role_id}/permissions/{permission_id} - Bị từ chối 403 khi không có ROLE_ADMIN")
    @WithMockUser(roles = "USER")
    void deleteRolePermission_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/roles/1/permissions/2"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"));
    }
}
