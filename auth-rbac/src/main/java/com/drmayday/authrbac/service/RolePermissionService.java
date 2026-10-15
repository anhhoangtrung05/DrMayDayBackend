package com.drmayday.authrbac.service;

import com.drmayday.authrbac.dto.request.SetRolePermissionsRequest;
import com.drmayday.authrbac.dto.response.PermissionResponse;
import com.drmayday.authrbac.entity.Permission;
import com.drmayday.authrbac.entity.Role;
import com.drmayday.authrbac.entity.RolePermission;
import com.drmayday.authrbac.repository.PermissionRepository;
import com.drmayday.authrbac.repository.RolePermissionRepository;
import com.drmayday.authrbac.repository.RoleRepository;
import com.drmayday.common.exception.BadRequestException;
import com.drmayday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class RolePermissionService {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Transactional
    public void setRolePermissions(String roleIdStr, SetRolePermissionsRequest req) {
        Integer roleId = parseId(roleIdStr, "vai trò");

        Role role = roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        if (req == null || req.getPermissionIds() == null) {
            throw new BadRequestException("Danh sách permission_ids không được để trống");
        }

        List<Integer> permIds = new ArrayList<>();
        for (String pIdStr : req.getPermissionIds()) {
            if (pIdStr == null || pIdStr.trim().isEmpty()) {
                continue;
            }
            try {
                permIds.add(Integer.parseInt(pIdStr.trim()));
            } catch (NumberFormatException e) {
                throw new BadRequestException("ID quyền hạn không hợp lệ: " + pIdStr);
            }
        }

        List<Integer> distinctPermIds = permIds.stream().distinct().toList();
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
            List<RolePermission> rolePermissions = distinctPermIds.stream()
                    .map(pId -> RolePermission.builder()
                            .roleId(roleId)
                            .permissionId(pId)
                            .build())
                    .toList();
            rolePermissionRepository.saveAll(rolePermissions);
        }

        log.info("Gán danh sách {} quyền hạn cho vai trò id={} ({}) thành công",
                distinctPermIds.size(), roleId, role.getName());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(String roleIdStr) {
        Integer roleId = parseId(roleIdStr, "vai trò");

        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        List<Permission> permissions = rolePermissionRepository.findPermissionsByRoleId(roleId);
        return permissions.stream()
                .map(PermissionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteRolePermission(String roleIdStr, String permissionIdStr) {
        Integer roleId = parseId(roleIdStr, "vai trò");
        Integer permissionId = parseId(permissionIdStr, "quyền hạn");

        roleRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        permissionRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + permissionIdStr));

        int deletedCount = rolePermissionRepository.deleteByRoleIdAndPermissionId(roleId, permissionId);
        if (deletedCount == 0) {
            throw new ResourceNotFoundException("Vai trò không có quyền hạn này hoặc liên kết không tồn tại");
        }

        log.info("Thu hồi quyền hạn id={} khỏi vai trò id={} thành công", permissionId, roleId);
    }

    private Integer parseId(String idStr, String resourceName) {
        try {
            return Integer.parseInt(idStr);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID " + resourceName + " không hợp lệ: " + idStr);
        }
    }
}
