package com.example.drmayday.features.rbac.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import com.example.drmayday.features.rbac.dto.CreatePermissionRequest;
import com.example.drmayday.features.rbac.dto.EditPermissionRequest;
import com.example.drmayday.features.rbac.dto.PermissionResponse;
import com.example.drmayday.features.rbac.service.PermissionService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PermissionControllerTest {

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private PermissionService permissionService;

    @InjectMocks
    private PermissionController permissionController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(permissionController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("API 17: POST /api/v1/permissions - Tạo quyền hạn thành công (201 Created, mã 1001)")
    void setPermission_success() throws Exception {
        PermissionResponse response = PermissionResponse.builder()
                .permissionId("3")
                .permissionName("export_reports")
                .description("Xuất báo cáo")
                .groupName("REPORTS")
                .build();
        when(permissionService.createPermission(any(CreatePermissionRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission_name\":\"export_reports\",\"description\":\"Xuất báo cáo\",\"group_name\":\"REPORTS\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.code").value("1001"))
                .andExpect(jsonPath("$.data.permission_id").value("3"))
                .andExpect(jsonPath("$.data.permission_name").value("export_reports"))
                .andExpect(jsonPath("$.data.group_name").value("REPORTS"));
    }

    @Test
    @DisplayName("API 17: POST /api/v1/permissions - Thiếu permission_name trả về 400 (2001)")
    void setPermission_missingName() throws Exception {
        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"Thiếu tên\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    @DisplayName("API 17: POST /api/v1/permissions - Trùng tên quyền hạn trả về 409 (3108)")
    void setPermission_duplicate() throws Exception {
        when(permissionService.createPermission(any(CreatePermissionRequest.class)))
                .thenThrow(new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS));

        mockMvc.perform(post("/api/v1/permissions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission_name\":\"manage_clinical\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("3108"));
    }

    @Test
    @DisplayName("API 18: GET /api/v1/permissions - Lấy danh sách quyền hạn thành công (200 OK)")
    void getListPermissions_success() throws Exception {
        when(permissionService.getListPermissions()).thenReturn(List.of(
                PermissionResponse.builder().permissionId("1").permissionName("manage_users_v1").groupName("USER_MANAGEMENT").build(),
                PermissionResponse.builder().permissionId("2").permissionName("manage_clinical").groupName("CLINICAL").build()
        ));

        mockMvc.perform(get("/api/v1/permissions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].permission_name").value("manage_users_v1"))
                .andExpect(jsonPath("$.data[1].permission_name").value("manage_clinical"));
    }

    @Test
    @DisplayName("API 19: PUT /api/v1/permissions/{id} - Chỉnh sửa quyền hạn thành công (200 OK)")
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

        when(permissionService.editPermission(eq("1"), any(EditPermissionRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/v1/permissions/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Thành công"))
                .andExpect(jsonPath("$.data.permission_id").value("1"))
                .andExpect(jsonPath("$.data.permission_name").value("manage_users_v2"))
                .andExpect(jsonPath("$.data.description").value("Quản lý người dùng phiên bản 2"));

        verify(permissionService, times(1)).editPermission(eq("1"), any(EditPermissionRequest.class));
    }

    @Test
    @DisplayName("API 19: PUT /api/v1/permissions/{id} - 404 khi quyền hạn không tồn tại")
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
                .andExpect(jsonPath("$.code").value("2006"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy quyền hạn với id: 999"));
    }

    @Test
    @DisplayName("API 20: DELETE /api/v1/permissions/{id} - Xóa quyền hạn thành công (200 OK)")
    void deletePermission_success() throws Exception {
        doNothing().when(permissionService).deletePermission("1");

        mockMvc.perform(delete("/api/v1/permissions/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.message").value("Thành công"));

        verify(permissionService, times(1)).deletePermission("1");
    }

    @Test
    @DisplayName("API 20: DELETE /api/v1/permissions/{id} - 404 khi quyền hạn không tồn tại")
    void deletePermission_notFound() throws Exception {
        doThrow(new ResourceNotFoundException("Không tìm thấy quyền hạn với id: 999"))
                .when(permissionService).deletePermission("999");

        mockMvc.perform(delete("/api/v1/permissions/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("2006"))
                .andExpect(jsonPath("$.message").value("Không tìm thấy quyền hạn với id: 999"));
    }
}
