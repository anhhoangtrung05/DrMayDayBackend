package com.example.drmayday.features.rbac.service;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.BadRequestException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.features.rbac.dto.CreatePermissionRequest;
import com.example.drmayday.features.rbac.dto.EditPermissionRequest;
import com.example.drmayday.features.rbac.dto.PermissionResponse;
import com.example.drmayday.repository.PermissionRepository;
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
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private static final int PERMISSION_NAME_MAX_LENGTH = 100;
    private static final int GROUP_NAME_MAX_LENGTH = 100;

    private final PermissionRepository permissionRepository;

    @Transactional
    public PermissionResponse createPermission(CreatePermissionRequest req) {
        if (req == null || req.getPermissionName() == null || req.getPermissionName().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String name = req.getPermissionName().trim();
        if (name.length() > PERMISSION_NAME_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Tên quyền hạn không được dài quá 100 ký tự");
        }

        String groupName = trimToNull(req.getGroupName());
        if (groupName != null && groupName.length() > GROUP_NAME_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Nhóm quyền không được dài quá 100 ký tự");
        }

        String description = trimToNull(req.getDescription());
        if (permissionRepository.existsByName(name)) {
            throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Permission permission = Permission.builder()
                .name(name)
                .permissionGroup(groupName)
                .description(description)
                .createdAt(now)
                .updatedAt(now)
                .build();

        try {
            Permission saved = permissionRepository.saveAndFlush(permission);
            log.info("Successfully created permission id={} ({})", saved.getPermissionId(), saved.getName());
            return PermissionResponse.fromEntity(saved);
        } catch (DataIntegrityViolationException ex) {
            if (hasConstraint(ex, "uq_quyen_han_ten")) {
                throw new AppException(ErrorCode.PERMISSION_ALREADY_EXISTS);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getListPermissions() {
        return permissionRepository.findAll(Sort.by(Sort.Direction.ASC, "permissionId")).stream()
                .map(PermissionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public PermissionResponse editPermission(String idStr, EditPermissionRequest req) {
        Long id = parseId(idStr);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + idStr));

        if (req != null) {
            if (req.getPermissionName() != null && !req.getPermissionName().trim().isEmpty()) {
                String newName = req.getPermissionName().trim();
                permissionRepository.findByName(newName)
                        .filter(p -> !p.getPermissionId().equals(id))
                        .ifPresent(p -> {
                            throw new BadRequestException("Tên quyền hạn đã tồn tại: " + newName);
                        });
                permission.setName(newName);
            }
            if (req.getDescription() != null) {
                permission.setDescription(req.getDescription());
            }
            if (req.getGroupName() != null) {
                permission.setPermissionGroup(req.getGroupName());
            }
            permission.setUpdatedAt(OffsetDateTime.now());
        }

        Permission updated = permissionRepository.save(permission);
        log.info("Successfully updated permission id={}", id);
        return PermissionResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePermission(String idStr) {
        Long id = parseId(idStr);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + idStr));

        permissionRepository.delete(permission);
        log.info("Successfully deleted permission id={} ({})", id, permission.getName());
    }

    private Long parseId(String idStr) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new BadRequestException("ID quyền hạn không được để trống");
        }
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID quyền hạn không hợp lệ: " + idStr);
        }
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
