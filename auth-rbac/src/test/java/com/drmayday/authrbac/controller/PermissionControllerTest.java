package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.config.DevAuthFilter;
import com.drmayday.authrbac.config.SecurityConfig;
import com.drmayday.authrbac.dto.request.EditPermissionRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.service.PermissionService;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(PermissionController.class)
@Import({SecurityConfig.class, DevAuthFilter.class, GlobalExceptionHandler.class})
class PermissionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private PermissionService permissionService;

    @Test
    @DisplayName("PUT /api/v1/permissions/{id} - Thành công với ROLE_ADMIN (Mã 1000 | OK)")
    @WithMockUser(roles = "ADMIN")
    void editPermission_success() throws Exception {
        EditPermissionRequest request = EditPermissionRequest.builder()
                .permissionName("manage_users_v2")
                .description("Quản lý người dùng phiên bản 2")
                .build();

        PermissionResponse response = PermissionResponse.builder()
                .permissionId("1")
                .permissionName("manage_users_v2")
                .description("Quản lý người dùng phiên bản 2")
                .build();

        when(permissionService.editPermission(eq("1"), any(EditPermissionRequest.class)))
                .thenReturn(response);

        mockMvc.perform(put("/api/v1/permissions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"))
                .andExpect(jsonPath("$.data.permission_id").value("1"))
                .andExpect(jsonPath("$.data.permission_name").value("manage_users_v2"))
                .andExpect(jsonPath("$.data.description").value("Quản lý người dùng phiên bản 2"));

        verify(permissionService, times(1)).editPermission(eq("1"), any(EditPermissionRequest.class));
    }

    @Test
    @DisplayName("PUT /api/v1/permissions/{id} - Bị từ chối 403 khi không có ROLE_ADMIN")
    @WithMockUser(roles = "USER")
    void editPermission_forbidden() throws Exception {
        EditPermissionRequest request = EditPermissionRequest.builder()
                .permissionName("manage_users_v2")
                .build();

        mockMvc.perform(put("/api/v1/permissions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"));
    }

    @Test
    @DisplayName("PUT /api/v1/permissions/{id} - 404 khi quyền hạn không tồn tại")
    @WithMockUser(roles = "ADMIN")
    void editPermission_notFound() throws Exception {
        EditPermissionRequest request = EditPermissionRequest.builder()
                .permissionName("unknown_perm")
                .build();

        when(permissionService.editPermission(eq("999"), any(EditPermissionRequest.class)))
                .thenThrow(new ResourceNotFoundException("Không tìm thấy quyền hạn với id: 999"));

        mockMvc.perform(put("/api/v1/permissions/999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("1002"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy quyền hạn với id: 999"));
    }

    @Test
    @DisplayName("DELETE /api/v1/permissions/{id} - Thành công với ROLE_ADMIN (Mã 1000 | OK)")
    @WithMockUser(roles = "ADMIN")
    void deletePermission_success() throws Exception {
        doNothing().when(permissionService).deletePermission("1");

        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("OK"));

        verify(permissionService, times(1)).deletePermission("1");
    }

    @Test
    @DisplayName("DELETE /api/v1/permissions/{id} - Bị từ chối 403 khi không có ROLE_ADMIN")
    @WithMockUser(roles = "DOCTOR")
    void deletePermission_forbidden() throws Exception {
        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("1004"));
    }
}
