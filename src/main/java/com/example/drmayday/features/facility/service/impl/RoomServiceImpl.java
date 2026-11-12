package com.example.drmayday.features.facility.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.features.facility.dto.CreateRoomRequest;
import com.example.drmayday.features.facility.dto.EditRoomRequest;
import com.example.drmayday.features.facility.dto.RoomResponse;
import com.example.drmayday.features.facility.service.RoomService;
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

        ClinicRoom clinicRoom = ClinicRoom.builder().branch(branch).name(name).build();
        return RoomResponse.from(saveRoom(clinicRoom));
    }

    @Override
    @Transactional(readOnly = true)
    public List<RoomResponse> getListRooms(Long branchId) {
        if (branchId == null) {
            return toResponses(clinicRoomRepository.findAll());
        }
        if (!branchRepository.existsById(branchId)) {
            throw new AppException(ErrorCode.BRANCH_NOT_FOUND);
        }
        return toResponses(clinicRoomRepository.findAllByBranchId(branchId));
    }

    @Override
    @Transactional(readOnly = true)
    public RoomResponse getRoomDetails(String id) {
        return RoomResponse.from(findRoom(id));
    }

    @Override
    @Transactional
    public RoomResponse editRoom(String id, EditRoomRequest req) {
        log.info("Admin updating a clinic room: id={}", id);
        ClinicRoom clinicRoom = clinicRoomRepository.findByIdForUpdate(parseRoomId(id))
                .orElseThrow(() -> new AppException(ErrorCode.ROOM_NOT_FOUND));

        if (req.getName() != null && req.getName().isBlank()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }
        String name = req.getName() != null ? req.getName().trim() : clinicRoom.getName();

        Branch branch = clinicRoom.getBranch();
        if (req.getBranchId() != null) {
            Long targetBranchId = parseBodyBranchId(req.getBranchId());
            // Specifying the current branch does not count as moving the room.
            if (!targetBranchId.equals(branch.getBranchId())) {
                branch = findActiveBranch(targetBranchId);
                if (clinicRoomRepository.hasExamSessions(clinicRoom.getClinicRoomId())) {
                    throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
                }
            }
        }

        boolean branchChanged = branch != clinicRoom.getBranch();
        if (!branchChanged && name.equals(clinicRoom.getName())) {
            return RoomResponse.from(clinicRoom);
        }

        if (clinicRoomRepository.existsByBranchIdAndNameAndRoomIdNot(
                branch.getBranchId(), name, clinicRoom.getClinicRoomId())) {
            throw new AppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS);
        }

        clinicRoom.setBranch(branch);
        clinicRoom.setName(name);
        return RoomResponse.from(saveRoom(clinicRoom));
    }

    @Override
    @Transactional
    public void deleteRoom(String id) {
        log.info("Admin deleting a clinic room: id={}", id);
        ClinicRoom clinicRoom = findRoom(id);

        if (clinicRoomRepository.hasExamSessions(clinicRoom.getClinicRoomId())) {
            throw new AppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS);
        }

        try {
            clinicRoomRepository.delete(clinicRoom);
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

    // saveAndFlush ensures database errors occur inside the try block instead of during commit.
    private ClinicRoom saveRoom(ClinicRoom clinicRoom) {
        try {
            return clinicRoomRepository.saveAndFlush(clinicRoom);
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
        return branchRepository.findById(branchId)
                .orElseThrow(() -> new AppException(ErrorCode.BRANCH_NOT_FOUND));
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
        List<RoomResponse> responses = new java.util.ArrayList<>(rooms.size());
        for (ClinicRoom room : rooms) {
            responses.add(RoomResponse.from(room));
        }
        return responses;
    }
}
