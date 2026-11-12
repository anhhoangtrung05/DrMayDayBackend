package com.example.drmayday.features.facility.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.features.facility.dto.CreateRoomRequest;
import com.example.drmayday.features.facility.dto.EditRoomRequest;
import com.example.drmayday.features.facility.dto.RoomResponse;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers hard-to-reproduce branches using test data: validation order for TOPOLOGY_SET_ROOM, 32, and 33, plus database error translation.
 * Only the repository boundary is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class RoomServiceImplTest {

    private static final long ROOM_ID = 3L;
    private static final String ID = "3";
    private static final long BRANCH_ID = 7L;
    private static final long TARGET_BRANCH_ID = 8L;
    private static final String ROOM_NAME = "Phòng 101";

    @Mock
    private ClinicRoomRepository clinicRoomRepository;

    @Mock
    private BranchRepository branchRepository;

    private RoomServiceImpl service;
    private Branch branch;
    private Branch targetBranch;
    private ClinicRoom room;

    @BeforeEach
    void setUp() {
        service = new RoomServiceImpl(clinicRoomRepository, branchRepository);
        branch = new Branch(BRANCH_ID, "Cơ sở Hoàn Kiếm", "Số 1 Phố Huế", "0241234567", true);
        targetBranch = new Branch(TARGET_BRANCH_ID, "Cơ sở Cầu Giấy", "Số 2 Duy Tân", "0241234568", true);
        room = new ClinicRoom(ROOM_ID, branch.getBranchId(), ROOM_NAME, branch);

        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));
        when(clinicRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
        when(clinicRoomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
        when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> call.getArgument(0));
    }

    private static CreateRoomRequest createRequest(String branchId, String name) {
        CreateRoomRequest request = new CreateRoomRequest();
        request.setBranchId(branchId);
        request.setName(name);
        return request;
    }

    private static EditRoomRequest editRequest(String branchId, String name) {
        EditRoomRequest request = new EditRoomRequest();
        request.setBranchId(branchId);
        request.setName(name);
        return request;
    }

    private static DataIntegrityViolationException sqlError(String sqlState) {
        return new DataIntegrityViolationException("lỗi ghi", new SQLException("lỗi gốc", sqlState));
    }

    private static void assertFailsWith(ErrorCode code, ThrowingCallable call) {
        assertThatThrownBy(call)
                .isInstanceOfSatisfying(AppException.class, ex -> assertThat(ex.getErrorCode()).isEqualTo(code));
    }

    private void assertRoomUntouched() {
        assertThat(room.getBranch()).isSameAs(branch);
        assertThat(room.getName()).isEqualTo(ROOM_NAME);
    }

    // TOPOLOGY_SET_ROOM

    @Test
    @DisplayName("AC-14: room creation saves the trimmed name and returns the new room's fields")
    void createTrimsNameAndMapsResponse() {
        when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> {
            ClinicRoom saved = call.getArgument(0);
            saved.setClinicRoomId(55L);
            return saved;
        });

        RoomResponse response = service.createRoom(createRequest("7", "  Phòng 202  "));

        assertThat(response.getRoomId()).isEqualTo("55");
        assertThat(response.getBranchId()).isEqualTo("7");
        assertThat(response.getBranchName()).isEqualTo("Cơ sở Hoàn Kiếm");
        assertThat(response.getName()).isEqualTo("Phòng 202");
    }

    @Test
    @DisplayName("AC-16: duplicate-name checks use the trimmed name")
    void createChecksDuplicateAfterTrim() {
        when(clinicRoomRepository.existsByBranchIdAndName(BRANCH_ID, ROOM_NAME)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                () -> service.createRoom(createRequest("7", "  " + ROOM_NAME + "  ")));

        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "99999999999999999999", ""})
    @DisplayName("AC-15: a branch_id that is not a Long returns 2002 without accessing the database")
    void createRejectsNonNumericBranchId(String branchId) {
        assertFailsWith(ErrorCode.INVALID_PARAMETER_TYPE, () -> service.createRoom(createRequest(branchId, ROOM_NAME)));

        verifyNoInteractions(branchRepository, clinicRoomRepository);
    }

    @Test
    @DisplayName("AC-15: a nonexistent branch returns 2101 and performs no writes")
    void createRejectsMissingBranch() {
        when(branchRepository.findById(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.createRoom(createRequest("404", ROOM_NAME)));

        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("TOPOLOGY_SET_ROOM validation order: an inactive branch takes precedence over a duplicate-name error")
    void createChecksInactiveBranchBeforeDuplicateName() {
        branch.setActive(false);
        when(clinicRoomRepository.existsByBranchIdAndName(anyLong(), anyString())).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_INACTIVE, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("AC-5: a unique constraint violation during room creation returns 2104, not 3006")
    void createTranslatesUniqueViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23505"));

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("AC-5: a foreign key violation during room creation returns 2101")
    void createTranslatesForeignKeyViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23503"));

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("Other database errors during room creation are not translated to a 21xx code")
    void createRethrowsOtherDatabaseErrors() {
        DataIntegrityViolationException checkViolation = sqlError("23514");
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(checkViolation);

        assertThatThrownBy(() -> service.createRoom(createRequest("7", ROOM_NAME))).isSameAs(checkViolation);
    }

    // TOPOLOGY_GET_LIST_ROOMS

    @Test
    @DisplayName("AC-17: when branch_id is omitted, all rooms are returned and no branch lookup is performed")
    void listWithoutFilterSkipsBranchCheck() {
        when(clinicRoomRepository.findAll()).thenReturn(List.of(room));

        List<RoomResponse> rooms = service.getListRooms(null);

        assertThat(rooms).extracting(roomResponse -> roomResponse.getRoomId()).containsExactly(ID);
        verifyNoInteractions(branchRepository);
    }

    @Test
    @DisplayName("AC-17: an existing branch with no rooms returns an empty array")
    void listReturnsEmptyForBranchWithoutRooms() {
        when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
        when(clinicRoomRepository.findAllByBranchId(BRANCH_ID)).thenReturn(List.of());

        assertThat(service.getListRooms(BRANCH_ID)).isEmpty();
    }

    @Test
    @DisplayName("AC-17: filtering by a nonexistent branch returns 2101")
    void listRejectsMissingBranch() {
        when(branchRepository.existsById(404L)).thenReturn(false);

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.getListRooms(404L));

        verify(clinicRoomRepository, never()).findAllByBranchId(anyLong());
    }

    // TOPOLOGY_GET_ROOM_DETAIL

    @ParameterizedTest
    @ValueSource(strings = {"abc", "99999999999999999999", ""})
    @DisplayName("AC-18: an ID that is not a Long returns 2103 without accessing the database")
    void detailRejectsNonNumericId(String id) {
        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.getRoomDetails(id));

        verifyNoInteractions(clinicRoomRepository);
    }

    // TOPOLOGY_EDIT_ROOM

    @Test
    @DisplayName("TOPOLOGY_EDIT_ROOM retrieves the room using FOR UPDATE instead of findById")
    void editReadsRoomThroughRowLock() {
        service.editRoom(ID, editRequest(null, "Phòng mới"));

        verify(clinicRoomRepository).findByIdForUpdate(ROOM_ID);
        verify(clinicRoomRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("TOPOLOGY_EDIT_ROOM validation order: an invalid ID takes precedence over an empty name")
    void editChecksRoomBeforeName() {
        when(clinicRoomRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.editRoom("404", editRequest(null, "")));
    }

    @Test
    @DisplayName("TOPOLOGY_EDIT_ROOM validation order: an empty name takes precedence over an invalid branch_id")
    void editChecksNameBeforeBranchId() {
        assertFailsWith(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.editRoom(ID, editRequest("abc", "   ")));
    }

    @Test
    @DisplayName("TOPOLOGY_EDIT_ROOM validation order: an inactive target branch takes precedence over an exam-session error")
    void editChecksTargetBranchBeforeExamSessions() {
        targetBranch.setActive(false);
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_INACTIVE, () -> service.editRoom(ID, editRequest("8", null)));

        assertRoomUntouched();
    }

    @Test
    @DisplayName("AC-19: a body with no fields returns the current data and performs no writes")
    void editWithEmptyBodyWritesNothing() {
        RoomResponse response = service.editRoom(ID, editRequest(null, null));

        assertThat(response.getName()).isEqualTo(ROOM_NAME);
        assertThat(response.getBranchId()).isEqualTo("7");
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-19: values unchanged after trimming skip duplicate-name checks and writes")
    void editWithSameValuesWritesNothing() {
        service.editRoom(ID, editRequest("7", "  " + ROOM_NAME + "  "));

        verify(clinicRoomRepository, never())
                .existsByBranchIdAndNameAndRoomIdNot(anyLong(), anyString(), anyLong());
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-20: sending the current branch_id is not a move, so exam sessions are not checked")
    void editWithCurrentBranchIsNotAMove() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        RoomResponse response = service.editRoom(ID, editRequest("7", "Phòng mới"));

        assertThat(response.getName()).isEqualTo("Phòng mới");
        verify(clinicRoomRepository, never()).hasMedicalVisits(anyLong());
    }

    @Test
    @DisplayName("AC-20: a room with exam sessions cannot be moved, and its supplied name is not changed")
    void editRefusesToMoveRoomWithExamSessions() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.editRoom(ID, editRequest("8", "Phòng mới")));

        assertRoomUntouched();
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-20: moving a room checks for duplicate names in the target branch, excluding the room itself")
    void editChecksDuplicateInTargetBranch() {
        when(clinicRoomRepository.existsByBranchIdAndNameAndRoomIdNot(
                TARGET_BRANCH_ID, ROOM_NAME, ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.editRoom(ID, editRequest("8", null)));

        assertRoomUntouched();
    }

    @Test
    @DisplayName("AC-20: moving a room saves the new branch and preserves the old name when no name is supplied")
    void editMovesRoomToTargetBranch() {
        service.editRoom(ID, editRequest("8", null));

        ArgumentCaptor<ClinicRoom> saved = ArgumentCaptor.forClass(ClinicRoom.class);
        verify(clinicRoomRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getBranch()).isSameAs(targetBranch);
        assertThat(saved.getValue().getName()).isEqualTo(ROOM_NAME);
    }

    @Test
    @DisplayName("AC-5: a unique constraint violation while updating a room returns 2104")
    void editTranslatesUniqueViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23505"));

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.editRoom(ID, editRequest(null, "Phòng mới")));
    }

    @Test
    @DisplayName("AC-5: a foreign key violation while moving a room returns 2101")
    void editTranslatesForeignKeyViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23503"));

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.editRoom(ID, editRequest("8", null)));
    }

    // TOPOLOGY_DELETE_ROOM

    @Test
    @DisplayName("AC-21: deletion is flushed immediately so database errors occur inside the try block")
    void deleteFlushesInsideTheTry() {
        service.deleteRoom(ID);

        InOrder order = inOrder(clinicRoomRepository);
        order.verify(clinicRoomRepository).delete(room);
        order.verify(clinicRoomRepository).flush();
    }

    @Test
    @DisplayName("AC-22: a room with exam sessions returns 2105 and is not deleted")
    void deleteRefusesRoomWithExamSessions() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.deleteRoom(ID));

        verify(clinicRoomRepository, never()).delete(any(ClinicRoom.class));
    }

    @Test
    @DisplayName("AC-5: an exam session inserted during deletion causes the database foreign key violation to return 2105")
    void deleteTranslatesForeignKeyViolation() {
        doThrow(sqlError("23503")).when(clinicRoomRepository).flush();

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.deleteRoom(ID));
    }

    @Test
    @DisplayName("AC-5: if another admin deleted the room first, the request returns 2103, not 9999")
    void deleteTranslatesLostRace() {
        doThrow(new ObjectOptimisticLockingFailureException(ClinicRoom.class, ROOM_ID))
                .when(clinicRoomRepository).flush();

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom(ID));
    }

    @Test
    @DisplayName("Other database errors during room deletion are not translated to a 21xx code")
    void deleteRethrowsOtherDatabaseErrors() {
        DataIntegrityViolationException checkViolation = sqlError("23514");
        doThrow(checkViolation).when(clinicRoomRepository).flush();

        assertThatThrownBy(() -> service.deleteRoom(ID)).isSameAs(checkViolation);
    }

    @Test
    @DisplayName("AC-18: deleting a room with a nonexistent ID returns 2103")
    void deleteRejectsMissingRoom() {
        when(clinicRoomRepository.findById(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom("404"));
    }
}
