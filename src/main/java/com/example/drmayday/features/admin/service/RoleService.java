package com.example.drmayday.features.admin.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.BadRequestException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.admin.dto.CreateRoleRequest;
import com.example.drmayday.features.admin.dto.RoleResponse;
import com.example.drmayday.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest request) {
        String name = request.getRoleName().trim();
        if (roleRepository.existsByName(name)) {
            throw new AppException(ErrorCode.ROLE_NAME_ALREADY_EXISTS);
        }

        Long parentId = parseOptionalId(request.getParentRoleId());
        if (parentId != null && !roleRepository.existsById(parentId)) {
            throw new ResourceNotFoundException("Không tìm thấy vai trò cha: " + parentId);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Role role = Role.builder()
                .name(name)
                .displayName(name)
                .description(request.getDescription())
                .parentRoleId(parentId)
                .createdAt(now)
                .updatedAt(now)
                .build();
        return RoleResponse.fromEntity(roleRepository.save(role));
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll().stream().map(RoleResponse::fromEntity).toList();
    }

    private Long parseOptionalId(String value) {
        if (value == null || value.trim().isEmpty()) return null;
        try {
            return Long.valueOf(value.trim());
        } catch (NumberFormatException ex) {
            throw new BadRequestException("parent_role_id không hợp lệ");
        }
    }
}
