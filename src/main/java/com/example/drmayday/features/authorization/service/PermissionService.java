package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.EditPermissionRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.repository.PermissionRepository;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import com.example.drmayday.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private final PermissionRepository permissionRepository;

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
        log.info("Chỉnh sửa quyền hạn id={} thành công", id);
        return PermissionResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePermission(String idStr) {
        Long id = parseId(idStr);

        Permission permission = permissionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + idStr));

        permissionRepository.delete(permission);
        log.info("Xóa quyền hạn id={} ({}) thành công", id, permission.getName());
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
}
