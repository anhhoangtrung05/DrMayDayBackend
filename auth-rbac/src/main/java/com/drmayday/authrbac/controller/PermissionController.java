package com.drmayday.authrbac.controller;

import com.drmayday.authrbac.dto.request.EditPermissionRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.service.PermissionService;
import com.drmayday.common.dto.BaseResponse;
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
