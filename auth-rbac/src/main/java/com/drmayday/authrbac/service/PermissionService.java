package com.drmayday.authrbac.service;

import com.drmayday.authrbac.dto.request.EditPermissionRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.entity.Permission;
import com.drmayday.authrbac.repository.PermissionRepository;
import com.drmayday.common.exception.BadRequestException;
import com.drmayday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PermissionService {

    private final PermissionRepository permissionRepository;

    @Transactional
    public PermissionResponse editPermission(String id, EditPermissionRequest req) {
        Integer permissionId = parseId(id);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + id));

        if (req != null) {
            if (req.getPermissionName() != null && !req.getPermissionName().trim().isEmpty()) {
                String newName = req.getPermissionName().trim();
                if (permissionRepository.existsByNameAndPermissionIdNot(newName, permissionId)) {
                    throw new BadRequestException("Tên quyền hạn đã tồn tại: " + newName);
                }
                permission.setName(newName);
            }

            if (req.getDescription() != null) {
                permission.setDescription(req.getDescription().trim());
            }
        }

        Permission updated = permissionRepository.save(permission);
        log.info("Cập nhật quyền hạn thành công: id={}, name={}", updated.getPermissionId(), updated.getName());
        return PermissionResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePermission(String id) {
        Integer permissionId = parseId(id);

        Permission permission = permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + id));

        permissionRepository.delete(permission);
        log.info("Xóa quyền hạn thành công: id={}", permissionId);
    }

    private Integer parseId(String id) {
        try {
            return Integer.parseInt(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID quyền hạn không hợp lệ: " + id);
        }
    }
}
