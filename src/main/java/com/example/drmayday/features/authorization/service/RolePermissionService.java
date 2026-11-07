package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.SetRolePermissionsRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.entity.QuyenHan;
import com.example.drmayday.entity.VaiTro;
import com.example.drmayday.entity.VaiTroQuyenHan;
import com.example.drmayday.repository.QuyenHanRepository;
import com.example.drmayday.repository.VaiTroQuyenHanRepository;
import com.example.drmayday.repository.VaiTroRepository;
import com.example.drmayday.common.exception.BadRequestException;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RolePermissionService {

    private static final Logger log = LoggerFactory.getLogger(RolePermissionService.class);
    private final VaiTroRepository vaiTroRepository;
    private final QuyenHanRepository quyenHanRepository;
    private final VaiTroQuyenHanRepository vaiTroQuyenHanRepository;

    @Transactional
    public void setRolePermissions(String roleIdStr, SetRolePermissionsRequest req) {
        Long roleId = parseId(roleIdStr, "vai trò");

        VaiTro role = vaiTroRepository.findById(roleId)
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
            List<QuyenHan> foundPerms = quyenHanRepository.findAllById(distinctPermIds);
            if (foundPerms.size() != distinctPermIds.size()) {
                throw new BadRequestException("Một hoặc nhiều quyền hạn không tồn tại trong hệ thống");
            }
        }

        // Xóa liên kết cũ của role này
        vaiTroQuyenHanRepository.deleteByMaVaiTro(roleId);

        // Lưu danh sách liên kết mới
        if (!distinctPermIds.isEmpty()) {
            LocalDateTime now = LocalDateTime.now();
            List<VaiTroQuyenHan> rolePermissions = distinctPermIds.stream()
                    .map(pId -> VaiTroQuyenHan.builder()
                            .maVaiTro(roleId)
                            .maQuyenHan(pId)
                            .thoiDiemGan(now)
                            .build())
                    .toList();
            vaiTroQuyenHanRepository.saveAll(rolePermissions);
        }

        log.info("Gán danh sách {} quyền hạn cho vai trò id={} ({}) thành công",
                distinctPermIds.size(), roleId, role.getTenVaiTro());
    }

    @Transactional(readOnly = true)
    public List<PermissionResponse> getRolePermissions(String roleIdStr) {
        Long roleId = parseId(roleIdStr, "vai trò");

        vaiTroRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        List<QuyenHan> permissions = vaiTroQuyenHanRepository.findPermissionsByMaVaiTro(roleId);
        return permissions.stream()
                .map(PermissionResponse::fromEntity)
                .toList();
    }

    @Transactional
    public void deleteRolePermission(String roleIdStr, String permissionIdStr) {
        Long roleId = parseId(roleIdStr, "vai trò");
        Long permissionId = parseId(permissionIdStr, "quyền hạn");

        vaiTroRepository.findById(roleId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy vai trò với id: " + roleIdStr));

        quyenHanRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + permissionIdStr));

        vaiTroQuyenHanRepository.deleteByMaVaiTroAndMaQuyenHan(roleId, permissionId);
        log.info("Xóa quyền hạn id={} khỏi vai trò id={} thành công", permissionId, roleId);
    }

    private Long parseId(String id, String entityName) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID " + entityName + " không hợp lệ: " + id);
        }
    }
}
