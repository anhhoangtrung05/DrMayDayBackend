package com.example.drmayday.features.rbac.controller;

import com.example.drmayday.common.dto.BaseResponse;
import com.example.drmayday.features.rbac.dto.CreateRoleRequest;
import com.example.drmayday.features.rbac.dto.RoleResponse;
import com.example.drmayday.features.rbac.service.RoleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/roles")
@RequiredArgsConstructor
public class RoleController {

    private final RoleService roleService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BaseResponse<RoleResponse>> setRole(@Valid @RequestBody CreateRoleRequest req) {
        RoleResponse response = roleService.createRole(req);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(BaseResponse.created(response));
    }

    @GetMapping
    public ResponseEntity<BaseResponse<List<RoleResponse>>> getRoles() {
        return ResponseEntity.ok(BaseResponse.ok(roleService.getRoles()));
    }
}
