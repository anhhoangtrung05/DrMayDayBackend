package com.example.drmayday.features.topology.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.features.topology.dto.CreateRoomRequest;
import com.example.drmayday.features.topology.dto.EditRoomRequest;
import com.example.drmayday.features.topology.dto.RoomResponse;
import com.example.drmayday.features.topology.service.RoomService;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
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

    private final ClinicRoomRepository clinicRoomRepository;
    private final BranchRepository branchRepository;

    public RoomServiceImpl(ClinicRoomRepository clinicRoomRepository, BranchRepository branchRepository) {
        this.clinicRoomRepository = clinicRoomRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional
    public RoomResponse createRoom(CreateRoomRequest req) {
        log.info("Admin creating a clinic room: branchId={}", req.getBranchId());
        Long branchId = parseBodyBranchId(req.getBranchId());
        Branch branch = findActiveBranch(branchId);
        String name = req.getName().trim();

        if (clinicRoomRepository.existsByBranchIdAndName(branchId, name)) {
            throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        // The ma_chi_nhanh column is written through branchId; branch is read-only (insertable = false).
        ClinicRoom room = ClinicRoom.builder().branchId(branchId).branch(branch).name(name).build();
        return RoomResponse.from(saveRoom(room));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getListRooms(Long branchId) {
        if (branchId == null) {
            return toResponses(clinicRoomRepository.findAllWithBranch());
        }
        if (!branchRepository.existsById(branchId)) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        return toResponses(clinicRoomRepository.findAllByBranchIdWithBranch(branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(String id) {
        return RoomResponse.from(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse editRoom(String id, EditRoomRequest req) {
        log.info("Admin updating clinic room: id={}", id);
        ClinicRoom room = clinicRoomRepository.findByIdForUpdate(parseRoomId(id))
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (req.getName() != null && req.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }
        String name = req.getName() != null ? req.getName().trim() : room.getName();

        Branch targetBranch = null;
        if (req.getBranchId() != null) {
            Long targetBranchId = parseBodyBranchId(req.getBranchId());
            // Selecting the current branch does not count as moving the room.
            if (!targetBranchId.equals(room.getBranchId())) {
                targetBranch = findActiveBranch(targetBranchId);
                if (clinicRoomRepository.hasPhienKham(room.getClinicRoomId())) {
                    throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
                }
            }
        }

        boolean branchChanged = targetBranch != null;
        if (!branchChanged && name.equals(room.getName())) {
            return RoomResponse.from(room);
        }

        Long branchId = targetBranch != null ? targetBranch.getBranchId() : room.getBranchId();
        if (clinicRoomRepository.existsByBranchIdAndNameAndClinicRoomIdNot(branchId, name, room.getClinicRoomId())) {
            throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        if (branchChanged) {
            room.setBranchId(branchId);
            room.setBranch(targetBranch);
        }
        room.setName(name);
        return RoomResponse.from(saveRoom(room));
    }

    @Override
    @Transactional
    public void deleteRoom(String id) {
        log.info("Admin deleting clinic room: id={}", id);
        ClinicRoom room = findRoom(id);

        if (clinicRoomRepository.hasPhienKham(room.getClinicRoomId())) {
            throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
        }

        try {
            clinicRoomRepository.delete(room);
            clinicRoomRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (FacilitySqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
            }
            throw ex;
        } catch (ObjectOptimisticLockingFailureException ex) {
            // Another admin already deleted the room, so the DELETE statement found no row.
            throw new AppException(ErrorCode.ROOM_NOT_FOUND);
        }
    }

    // Use saveAndFlush so database errors occur inside the try block rather than at commit.
    private ClinicRoom saveRoom(ClinicRoom room) {
        try {
            return clinicRoomRepository.saveAndFlush(room);
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

    private Branch findActiveBranch(Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
        if (!Boolean.TRUE.equals(branch.getActive())) {
            throw new AppException(ErrorCode.BRANCH_INACTIVE);
        }
        return branch;
    }

    private ClinicRoom findRoom(String id) {
        return clinicRoomRepository.findById(parseRoomId(id))
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

    private List<RoomResponse> toResponses(List<ClinicRoom> rooms) {
        return rooms.stream().map(RoomResponse::from).toList();
    }
}
