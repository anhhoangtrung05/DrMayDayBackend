package com.example.drmayday.features.authorization.service;

import com.example.drmayday.features.authorization.dto.EditPermissionRequest;
import com.example.drmayday.features.authorization.dto.PermissionResponse;
import com.example.drmayday.entity.QuyenHan;
import com.example.drmayday.repository.QuyenHanRepository;
import com.example.drmayday.common.exception.BadRequestException;
import com.example.drmayday.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PermissionService {

    private static final Logger log = LoggerFactory.getLogger(PermissionService.class);
    private final QuyenHanRepository quyenHanRepository;

    @Transactional
    public PermissionResponse editPermission(String id, EditPermissionRequest req) {
        Long permissionId = parseId(id);

        QuyenHan quyenHan = quyenHanRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + id));

        if (req != null) {
            if (req.getPermissionName() != null && !req.getPermissionName().trim().isEmpty()) {
                String newName = req.getPermissionName().trim();
                if (quyenHanRepository.existsByTenQuyenHanAndMaQuyenHanNot(newName, permissionId)) {
                    throw new BadRequestException("Tên quyền hạn đã tồn tại: " + newName);
                }
                quyenHan.setTenQuyenHan(newName);
            }

            if (req.getDescription() != null) {
                quyenHan.setMoTa(req.getDescription().trim());
            }

            if (req.getGroupName() != null) {
                quyenHan.setNhomQuyen(req.getGroupName().trim());
            }
        }

        QuyenHan updated = quyenHanRepository.save(quyenHan);
        log.info("Cập nhật quyền hạn thành công: id={}, name={}", updated.getMaQuyenHan(), updated.getTenQuyenHan());
        return PermissionResponse.fromEntity(updated);
    }

    @Transactional
    public void deletePermission(String id) {
        Long permissionId = parseId(id);

        QuyenHan quyenHan = quyenHanRepository.findById(permissionId)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy quyền hạn với id: " + id));

        quyenHanRepository.delete(quyenHan);
        log.info("Xóa quyền hạn thành công: id={}", permissionId);
    }

    private Long parseId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException e) {
            throw new BadRequestException("ID quyền hạn không hợp lệ: " + id);
        }
    }
}
