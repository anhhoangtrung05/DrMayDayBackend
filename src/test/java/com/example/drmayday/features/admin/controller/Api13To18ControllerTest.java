package com.example.drmayday.features.admin.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.features.admin.dto.AssignUserRequest;
import com.example.drmayday.features.admin.dto.RoleResponse;
import com.example.drmayday.features.admin.dto.StaffAssignResponse;
import com.example.drmayday.features.admin.dto.PermissionResponse;
import com.example.drmayday.features.admin.service.AdminUserService;
import com.example.drmayday.features.admin.service.PermissionService;
import com.example.drmayday.features.admin.service.RoleService;
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

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class Api13To18ControllerTest {
    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Mock AdminUserService adminUserService;
    @Mock PermissionService permissionService;
    @Mock RoleService roleService;
    @InjectMocks AdminUserController adminUserController;
    @InjectMocks PermissionController permissionController;
    @InjectMocks RoleController roleController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminUserController, permissionController, roleController)
                .setControllerAdvice(new GlobalExceptionHandler()).build();
    }

    @Test
    void api13_deleteUser_successAndNotFound() throws Exception {
        mockMvc.perform(delete("/api/v1/admin/users/9"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"));
        verify(adminUserService).deleteUser("9");

        doThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND)).when(adminUserService).deleteUser("999");
        mockMvc.perform(delete("/api/v1/admin/users/999"))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("3007"));
    }

    @Test
    void api14_assignUser_successAndMissingField() throws Exception {
        when(adminUserService.assignUser(any(AssignUserRequest.class))).thenReturn(StaffAssignResponse.builder()
                .staffId("1").userId("1").branchId("1").departmentId("1").roomId("1").build());
        mockMvc.perform(post("/api/v1/admin/users/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"1\",\"branch_id\":\"1\",\"department_id\":\"1\",\"room_id\":\"1\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data.department_id").value("1"));
        mockMvc.perform(post("/api/v1/admin/users/assign").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"user_id\":\"1\",\"branch_id\":\"1\",\"department_id\":\"1\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    void api15_createRole_successDuplicateAndInvalidParent() throws Exception {
        when(roleService.createRole(any())).thenReturn(RoleResponse.builder().roleId("7").roleName("manager").build());
        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"manager\",\"description\":\"Manager\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("1001"));
        when(roleService.createRole(any())).thenThrow(new AppException(ErrorCode.ROLE_NAME_ALREADY_EXISTS));
        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role_name\":\"manager\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("3201"));
        mockMvc.perform(post("/api/v1/roles").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"missing name\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    void api16_getRoles_success() throws Exception {
        when(roleService.getRoles()).thenReturn(List.of(RoleResponse.builder().roleId("1").roleName("admin").build()));
        mockMvc.perform(get("/api/v1/roles")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[0].role_name").value("admin"));
    }

    @Test
    void api17_createPermission_successDuplicateAndMissingName() throws Exception {
        when(permissionService.createPermission(any())).thenReturn(PermissionResponse.builder()
                .permissionId("7").permissionName("USER_VIEW").build());
        mockMvc.perform(post("/api/v1/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission_name\":\"USER_VIEW\",\"description\":\"View users\"}"))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.code").value("1001"));
        when(permissionService.createPermission(any())).thenThrow(new AppException(ErrorCode.PERMISSION_NAME_ALREADY_EXISTS));
        mockMvc.perform(post("/api/v1/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"permission_name\":\"USER_VIEW\"}"))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("3202"));
        mockMvc.perform(post("/api/v1/permissions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"missing name\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("2001"));
    }

    @Test
    void api18_getPermissions_success() throws Exception {
        when(permissionService.getPermissions()).thenReturn(List.of(PermissionResponse.builder()
                .permissionId("1").permissionName("USER_VIEW").build()));
        mockMvc.perform(get("/api/v1/permissions")).andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("1000"))
                .andExpect(jsonPath("$.data[0].permission_name").value("USER_VIEW"));
    }
}
