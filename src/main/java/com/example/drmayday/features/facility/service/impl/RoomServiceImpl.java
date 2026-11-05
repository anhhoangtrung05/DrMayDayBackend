package com.example.drmayday.features.facility.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.ChiNhanh;
import com.example.drmayday.entity.PhongKham;
import com.example.drmayday.features.facility.dto.CreateRoomRequest;
import com.example.drmayday.features.facility.dto.EditRoomRequest;
import com.example.drmayday.features.facility.dto.RoomResponse;
import com.example.drmayday.features.facility.service.RoomService;
import com.example.drmayday.repository.ChiNhanhRepository;
import com.example.drmayday.repository.PhongKhamRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class RoomServiceImpl implements RoomService {

    private static final Logger log = LoggerFactory.getLogger(RoomServiceImpl.class);

    private final PhongKhamRepository phongKhamRepository;
    private final ChiNhanhRepository chiNhanhRepository;

    public RoomServiceImpl(PhongKhamRepository phongKhamRepository, ChiNhanhRepository chiNhanhRepository) {
        this.phongKhamRepository = phongKhamRepository;
        this.chiNhanhRepository = chiNhanhRepository;
    }

    @Override
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req) {
        log.info("Admin tạo phòng khám mới: branchId={}", req.getBranchId());
        Long branchId = parseBodyBranchId(req.getBranchId());
        ChiNhanh chiNhanh = findActiveBranch(branchId);
        String name = req.getName().trim();

        if (phongKhamRepository.existsByChiNhanh_MaChiNhanhAndTenPhongKham(branchId, name)) {
            throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        PhongKham phongKham = PhongKham.builder().chiNhanh(chiNhanh).tenPhongKham(name).build();
        return RoomResponse.from(saveRoom(phongKham));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getListRooms(Long branchId) {
        if (branchId == null) {
            return toResponses(phongKhamRepository.findAllWithChiNhanh());
        }
        if (!chiNhanhRepository.existsById(branchId)) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        return toResponses(phongKhamRepository.findAllByBranchWithChiNhanh(branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(String id) {
        return RoomResponse.from(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse editRoom(String id, EditRoomRequest req) {
        log.info("Admin sửa phòng khám: id={}", id);
        PhongKham phongKham = phongKhamRepository.findByIdForUpdate(parseRoomId(id))
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (req.getName() != null && req.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }
        String name = req.getName() != null ? req.getName().trim() : phongKham.getTenPhongKham();

        ChiNhanh chiNhanh = phongKham.getChiNhanh();
        if (req.getBranchId() != null) {
            Long targetBranchId = parseBodyBranchId(req.getBranchId());
            // Gửi trùng chi nhánh hiện tại thì không tính là chuyển phòng
            if (!targetBranchId.equals(chiNhanh.getMaChiNhanh())) {
                chiNhanh = findActiveBranch(targetBranchId);
                if (phongKhamRepository.hasPhienKham(phongKham.getMaPhongKham())) {
                    throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
                }
            }
        }

        boolean branchChanged = chiNhanh != phongKham.getChiNhanh();
        if (!branchChanged && name.equals(phongKham.getTenPhongKham())) {
            return RoomResponse.from(phongKham);
        }

        if (phongKhamRepository.existsByChiNhanh_MaChiNhanhAndTenPhongKhamAndMaPhongKhamNot(
                chiNhanh.getMaChiNhanh(), name, phongKham.getMaPhongKham())) {
            throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        phongKham.setChiNhanh(chiNhanh);
        phongKham.setTenPhongKham(name);
        return RoomResponse.from(saveRoom(phongKham));
    }

    @Override
    @Transactional
    public void deleteRoom(String id) {
        log.info("Admin xóa phòng khám: id={}", id);
        PhongKham phongKham = findRoom(id);

        if (phongKhamRepository.hasPhienKham(phongKham.getMaPhongKham())) {
            throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
        }

        try {
            phongKhamRepository.delete(phongKham);
            phongKhamRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (FacilitySqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
            }
            throw ex;
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Admin khác đã xóa phòng trước, câu DELETE không còn dòng nào để xóa
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    // Phải saveAndFlush để lỗi database xảy ra trong khối try, không phải lúc commit
    private PhongKham saveRoom(PhongKham phongKham) {
        try {
            return phongKhamRepository.saveAndFlush(phongKham);
        } catch (DataIntegrityViolationException ex) {
            if (FacilitySqlErrors.isUniqueViolation(ex)) {
                throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
            }
            if (FacilitySqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
            }
            throw ex;
        }
    }

    private ChiNhanh findActiveBranch(Long branchId) {
        ChiNhanh chiNhanh = chiNhanhRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (!Boolean.TRUE.equals(chiNhanh.getTrangThai())) {
            throw new AppException(ErrorCode.BRANCH_INACTIVE);
        }
        return chiNhanh;
    }

    private PhongKham findRoom(String id) {
        return phongKhamRepository.findById(parseRoomId(id))
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));
    }

    private Long parseRoomId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    private Long parseBodyBranchId(String branchId) {
        try {
            return Long.parseLong(branchId);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE);
        }
    }

    private List<RoomResponse> toResponses(List<PhongKham> rooms) {
        return rooms.stream().map(RoomResponse::from).toList();
    }
}
