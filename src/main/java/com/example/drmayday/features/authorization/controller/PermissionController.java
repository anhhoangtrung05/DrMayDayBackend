package com.example.drmayday.features.authorization.controller;

import com.example.drmayday.features.authorization.dto.EditPermissionRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.features.authorization.service.PermissionService;
import com.example.drmayday.common.dto.BaseResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

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
