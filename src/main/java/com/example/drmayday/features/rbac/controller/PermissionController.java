package com.example.drmayday.features.rbac.controller;

import com.example.drmayday.features.rbac.dto.CreatePermissionRequest;
import com.example.drmayday.features.rbac.dto.EditPermissionRequest;
import com.example.drmayday.features.rbac.dto.PermissionResponse;
import com.example.drmayday.features.rbac.service.PermissionService;
import com.example.drmayday.common.dto.BaseResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PermissionResponse>> setPermission(
            @Valid @RequestBody CreatePermissionRequest req) {
        PermissionResponse response = permissionService.createPermission(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<PermissionResponse>>> getListPermissions() {
        return ResponseEntity.ok(BaseResponse.ok(permissionService.getListPermissions()));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<PermissionResponse>> editPermission(
            @PathVariable String id,
            @RequestBody EditPermissionRequest req) {
        PermissionResponse response = permissionService.editPermission(id, req);
        return ResponseEntity.ok(BaseResponse.ok(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<Void>> deletePermission(@PathVariable String id) {
        permissionService.deletePermission(id);
        return ResponseEntity.ok(BaseResponse.ok());
    }
}
