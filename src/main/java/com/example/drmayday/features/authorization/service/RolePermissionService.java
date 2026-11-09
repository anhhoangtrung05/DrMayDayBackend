package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.SetRolePermissionsRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.entity.Permission;
import com.example.drmayday.entity.Role;
import com.example.drmayday.entity.RolePermission;
import com.example.drmayday.repository.PermissionRepository;
import com.example.drmayday.repository.RolePermissionRepository;
import com.example.drmayday.repository.RoleRepository;
import com.example.drmayday.common.exception.BadRequestException;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionService.class);
    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional
    public void setRolePermissions(String roleIdStr, SetRolePermissionsRequest req) {
        Long roleId = parseId(roleIdStr, "vai trò");

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        if (req == null || req.getPermissionIds() == null) {
            throw new BadRequestException("Danh sách permission_ids không được để trống");
        }

        List<Long> permIds = new ArrayList<>();
        for (String pIdStr : req.getPermissionIds()) {
            if (pIdStr == null || pIdStr.trim().isEmpty()) {
                continue;
            }
            try {
                permIds.add(Long.parseLong(pIdStr.trim()));
            } catch (NumberFormatException e) {
                throw new BadRequestException("ID quyền hạn không hợp lệ: " + pIdStr);
            }
        }

        List<Long> distinctPermIds = permIds.stream().distinct().toList();
        if (!distinctPermIds.isEmpty()) {
            List<Permission> foundPerms = permissionRepository.findAllById(distinctPermIds);
            if (foundPerms.size() != distinctPermIds.size()) {
                throw new BadRequestException("Một hoặc nhiều quyền hạn không tồn tại trong hệ thống");
            }
        }

        // Xóa liên kết cũ của role này
        rolePermissionRepository.deleteByRoleId(roleId);

        // Lưu danh sách liên kết mới
        if (!distinctPermIds.isEmpty()) {
            OffsetDateTime now = OffsetDateTime.now();
            List<RolePermission> rolePermissions = distinctPermIds.stream()
                    .map(pId -> RolePermission.builder()
                            .roleId(roleId)
                            .permissionId(pId)
                            .assignedAt(now)
                            .build())
                    .toList();
            rolePermissionRepository.saveAll(rolePermissions);
        }

        log.info("Gán danh sách {} quyền hạn cho vai trò id={} ({}) thành công",
                distinctPermIds.size(), roleId, role.getName());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(String roleIdStr) {
        Long roleId = parseId(roleIdStr, "vai trò");

        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(roleId);
        return permissions.stream()
                .map(PermissionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteRolePermission(String roleIdStr, String permissionIdStr) {
        Long roleId = parseId(roleIdStr, "vai trò");
        Long permissionId = parseId(permissionIdStr, "quyền hạn");

        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + permissionIdStr));

        if (!rolePermissionRepository.existsByRoleIdAndPermissionId(roleId, permissionId)) {
            throw new ResourceNotFoundException(String.format("Vai trò id=%d chưa được gán quyền hạn id=%d", roleId, permissionId));
        }

        rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        log.info("Thu hồi quyền hạn id={} khỏi vai trò id={} thành công", permissionId, roleId);
    }

    private Long parseId(String idStr, String entityName) {
        if (idStr == null || idStr.trim().isEmpty()) {
            throw new BadRequestException("ID " + entityName + " không được để trống");
        }
        try {
            return Long.parseLong(idStr.trim());
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID " + entityName + " không hợp lệ: " + idStr);
        }
    }
}
