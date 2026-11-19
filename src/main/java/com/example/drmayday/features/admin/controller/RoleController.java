package com.example.drmayday.features.admin.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.admin.dto.CreateRoleRequest;
import com.example.drmayday.features.admin.dto.RoleResponse;
import com.example.drmayday.features.admin.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {
    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoleResponse>> createRole(
            @Valid @RequestBody CreateRoleRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(roleService.createRole(request)));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<RoleResponse>>> getRoles() {
        return ResponseEntity.ok(BaseResponse.ok(roleService.getRoles()));
    }
}
