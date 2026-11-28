package com.example.drmayday.features.rbac.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Role;
import com.example.drmayday.features.rbac.dto.CreateRoleRequest;
import com.example.drmayday.features.rbac.dto.RoleResponse;
import com.example.drmayday.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class RoleService {

    private static final Logger log = LoggerFactory.getLogger(RoleService.class);
    private static final int ROLE_NAME_MAX_LENGTH = 50;
    private static final int DISPLAY_NAME_MAX_LENGTH = 100;

    private final RoleRepository roleRepository;

    @Transactional
    public RoleResponse createRole(CreateRoleRequest req) {
        if (req == null || req.getRoleName() == null || req.getRoleName().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String roleName = req.getRoleName().trim();
        if (roleName.length() > ROLE_NAME_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên vai trò không được dài quá 50 ký tự");
        }

        String displayName = (req.getDisplayName() != null && !req.getDisplayName().isBlank())
                ? req.getDisplayName().trim()
                : roleName;
        if (displayName.length() > DISPLAY_NAME_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên hiển thị vai trò không được dài quá 100 ký tự");
        }

        String description = trimToNull(req.getDescription());
        if (roleRepository.existsByName(roleName)) {
            throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Role role = Role.builder()
                .name(roleName)
                .displayName(displayName)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            Role saved = roleRepository.saveAndFlush(role);
            log.info("Successfully created role id={} ({})", saved.getRoleId(), saved.getName());
            return RoleResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            if (hasConstraint(ex, "uq_vai_tro_ten")) {
                throw new AppException(ErrorCode.ROLE_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<RoleResponse> getRoles() {
        return roleRepository.findAll(Sort.by(Sort.Direction.ASC, "roleId")).stream()
                .map(RoleResponse::fromEntity)
                .toList();
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasConstraint(Throwable throwable, String constraintName) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(constraintName)) {
                return true;
            }
        }
        return false;
    }
}
