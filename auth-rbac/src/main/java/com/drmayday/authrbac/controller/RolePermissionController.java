package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.dto.request.SetRolePermissionsRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.service.RolePermissionService;
import com.drmayday.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles/{role_id}/permissions")
@RequiredArgsConstructor
public class RolePermissionController {

    private final RolePermissionService rolePermissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> setRolePermissions(
            @PathVariable("role_id") String roleId,
            @RequestBody SetRolePermissionsRequest req) {
        rolePermissionService.setRolePermissions(roleId, req);
        return ResponseEntity.ok(BaseResponse.ok());
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<PermissionResponse>>> getRolePermissions(
            @PathVariable("role_id") String roleId) {
        List<PermissionResponse> list = rolePermissionService.getRolePermissions(roleId);
        return ResponseEntity.ok(BaseResponse.ok(list));
    }

    @DeleteMapping("/{permission_id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deleteRolePermission(
            @PathVariable("role_id") String roleId,
            @PathVariable("permission_id") String permissionId) {
        rolePermissionService.deleteRolePermission(roleId, permissionId);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
