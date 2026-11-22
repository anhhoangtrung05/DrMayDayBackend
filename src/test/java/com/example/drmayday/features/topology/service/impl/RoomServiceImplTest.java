package com.example.drmayday.features.topology.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.ClinicRoom;
import com.example.drmayday.features.topology.dto.CreateRoomRequest;
import com.example.drmayday.features.topology.dto.EditRoomRequest;
import com.example.drmayday.features.topology.dto.RoomResponse;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

@ExtendWith(MockitoExtension.class)
class RoomServiceImplTest {

    private static final long ROOM_ID = 3L;
    private static final String ROOM_ID_STRING = "3";
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
        branch = branch(BRANCH_ID, "Cơ sở Hoàn Kiếm", true);
        targetBranch = branch(TARGET_BRANCH_ID, "Cơ sở Cầu Giấy", true);
        room = new ClinicRoom(ROOM_ID, BRANCH_ID, ROOM_NAME, branch);
    }

    private static Branch branch(long id, String name, boolean active) {
        return new Branch(id, name, "Số 1 Phố Huế", "0241234567", active);
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
        return new DataIntegrityViolationException(
                "database write failed", new SQLException("database constraint failed", sqlState));
    }

    private void givenExistingBranch() {
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
    }

    private void givenExistingRoom() {
        when(clinicRoomRepository.findById(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private void givenRoomForUpdate() {
        when(clinicRoomRepository.findByIdForUpdate(ROOM_ID)).thenReturn(Optional.of(room));
    }

    private static void assertAppException(ErrorCode expected, ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    private void assertRoomUnchanged() {
        assertThat(room.getBranch()).isSameAs(branch);
        assertThat(room.getBranchId()).isEqualTo(BRANCH_ID);
        assertThat(room.getName()).isEqualTo(ROOM_NAME);
    }

    @Nested
    @DisplayName("createRoom")
    class CreateRoomTests {

        @Test
        @DisplayName("[Happy Path] Valid request — trims name and maps saved room")
        void createRoom_validRequest_savesAndReturnsRoom() {
            givenExistingBranch();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> {
                ClinicRoom saved = call.getArgument(0);
                saved.setClinicRoomId(55L);
                return saved;
            });

            RoomResponse response = service.createRoom(createRequest("7", "  Phòng 202  "));

            ArgumentCaptor<ClinicRoom> captor = ArgumentCaptor.forClass(ClinicRoom.class);
            verify(clinicRoomRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getBranchId()).isEqualTo(BRANCH_ID);
            assertThat(captor.getValue().getBranch()).isSameAs(branch);
            assertThat(captor.getValue().getName()).isEqualTo("Phòng 202");
            assertThat(response.getRoomId()).isEqualTo("55");
            assertThat(response.getBranchName()).isEqualTo("Cơ sở Hoàn Kiếm");
            assertThat(response.getName()).isEqualTo("Phòng 202");
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "1.5", "99999999999999999999", ""})
        @DisplayName("[Unhappy Path] branch_id is not a Long — returns invalid parameter type without database access")
        void createRoom_invalidBranchId_returnsTypeError(String branchId) {
            assertAppException(ErrorCode.INVALID_PARAMETER_TYPE,
                    () -> service.createRoom(createRequest(branchId, ROOM_NAME)));

            verifyNoInteractions(branchRepository, clinicRoomRepository);
        }

        @Test
        @DisplayName("[Unhappy Path] Branch does not exist — returns branch not found")
        void createRoom_missingBranch_returnsNotFound() {
            when(branchRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.BRANCH_NOT_FOUND,
                    () -> service.createRoom(createRequest("404", ROOM_NAME)));

            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Branch is inactive — rejects creation before duplicate check")
        void createRoom_inactiveBranch_rejectsCreation() {
            branch.setActive(false);
            givenExistingBranch();

            assertAppException(ErrorCode.BRANCH_INACTIVE,
                    () -> service.createRoom(createRequest("7", ROOM_NAME)));

            verify(clinicRoomRepository, never()).existsByBranchIdAndName(anyLong(), anyString());
            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Room name already exists — rejects duplicate")
        void createRoom_duplicateName_rejectsCreation() {
            givenExistingBranch();
            when(clinicRoomRepository.existsByBranchIdAndName(BRANCH_ID, ROOM_NAME)).thenReturn(true);

            assertAppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                    () -> service.createRoom(createRequest("7", "  " + ROOM_NAME + "  ")));

            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Exception Path] Unique constraint violation — translates to duplicate-name error")
        void createRoom_uniqueViolation_returnsDuplicateError() {
            givenExistingBranch();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(sqlError("23505"));

            assertAppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                    () -> service.createRoom(createRequest("7", ROOM_NAME)));
        }

        @Test
        @DisplayName("[Exception Path] Foreign-key violation — translates to branch not found")
        void createRoom_foreignKeyViolation_returnsBranchNotFound() {
            givenExistingBranch();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(sqlError("23503"));

            assertAppException(ErrorCode.BRANCH_NOT_FOUND,
                    () -> service.createRoom(createRequest("7", ROOM_NAME)));
        }

        @Test
        @DisplayName("[Exception Path] Unrelated database failure — propagates original exception")
        void createRoom_unrelatedDatabaseFailure_propagatesException() {
            givenExistingBranch();
            DataIntegrityViolationException failure = sqlError("23514");
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(failure);

            assertThatThrownBy(() -> service.createRoom(createRequest("7", ROOM_NAME))).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getListRooms")
    class GetListRoomsTests {

        @Test
        @DisplayName("[Alternate Path] branch_id omitted — returns all rooms without checking a branch")
        void getListRooms_filterOmitted_returnsAllRooms() {
            when(clinicRoomRepository.findAllWithBranch()).thenReturn(List.of(room));

            List<RoomResponse> responses = service.getListRooms(null);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getRoomId()).isEqualTo(ROOM_ID_STRING);
            assertThat(responses.get(0).getBranchId()).isEqualTo("7");
            assertThat(responses.get(0).getName()).isEqualTo(ROOM_NAME);
            verifyNoInteractions(branchRepository);
        }

        @Test
        @DisplayName("[Happy Path] Existing branch filter — returns rooms for that branch")
        void getListRooms_existingBranch_returnsRooms() {
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
            when(clinicRoomRepository.findAllByBranchIdWithBranch(BRANCH_ID)).thenReturn(List.of(room));

            List<RoomResponse> responses = service.getListRooms(BRANCH_ID);

            assertThat(responses).hasSize(1);
            assertThat(responses.get(0).getRoomId()).isEqualTo(ROOM_ID_STRING);
            assertThat(responses.get(0).getBranchName()).isEqualTo("Cơ sở Hoàn Kiếm");
            verify(clinicRoomRepository).findAllByBranchIdWithBranch(BRANCH_ID);
        }

        @Test
        @DisplayName("[Alternate Path] Existing branch has no rooms — returns empty list")
        void getListRooms_emptyBranch_returnsEmptyList() {
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);

            assertThat(service.getListRooms(BRANCH_ID)).isEmpty();
        }

        @Test
        @DisplayName("[Unhappy Path] Branch filter references missing branch — returns branch not found")
        void getListRooms_missingBranch_returnsNotFound() {
            when(branchRepository.existsById(404L)).thenReturn(false);

            assertAppException(ErrorCode.BRANCH_NOT_FOUND, () -> service.getListRooms(404L));

            verify(clinicRoomRepository, never()).findAllByBranchIdWithBranch(anyLong());
        }

        @Test
        @DisplayName("[Exception Path] Room repository fails — propagates original exception")
        void getListRooms_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(clinicRoomRepository.findAllWithBranch()).thenThrow(failure);

            assertThatThrownBy(() -> service.getListRooms(null)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getRoomDetails")
    class GetRoomDetailsTests {

        @Test
        @DisplayName("[Happy Path] Existing ID — returns mapped room")
        void getRoomDetails_existingId_returnsRoom() {
            givenExistingRoom();

            RoomResponse response = service.getRoomDetails(ROOM_ID_STRING);

            assertThat(response.getRoomId()).isEqualTo(ROOM_ID_STRING);
            assertThat(response.getBranchId()).isEqualTo("7");
            assertThat(response.getBranchName()).isEqualTo("Cơ sở Hoàn Kiếm");
            assertThat(response.getName()).isEqualTo(ROOM_NAME);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "99999999999999999999", ""})
        @DisplayName("[Unhappy Path] ID is not a Long — returns room not found without repository lookup")
        void getRoomDetails_invalidId_returnsNotFound(String id) {
            assertAppException(ErrorCode.ROOM_NOT_FOUND, () -> service.getRoomDetails(id));

            verifyNoInteractions(clinicRoomRepository);
        }

        @Test
        @DisplayName("[Unhappy Path] ID does not exist — returns room not found")
        void getRoomDetails_missingRoom_returnsNotFound() {
            when(clinicRoomRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.ROOM_NOT_FOUND, () -> service.getRoomDetails("404"));
        }

        @Test
        @DisplayName("[Exception Path] Repository fails — propagates original exception")
        void getRoomDetails_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(clinicRoomRepository.findById(ROOM_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getRoomDetails(ROOM_ID_STRING)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("editRoom")
    class EditRoomTests {

        @Test
        @DisplayName("[Happy Path] New name supplied — saves and returns updated room")
        void editRoom_newName_savesUpdatedRoom() {
            givenRoomForUpdate();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> call.getArgument(0));

            RoomResponse response = service.editRoom(ROOM_ID_STRING, editRequest(null, "Phòng mới"));

            assertThat(response.getName()).isEqualTo("Phòng mới");
            ArgumentCaptor<ClinicRoom> captor = ArgumentCaptor.forClass(ClinicRoom.class);
            verify(clinicRoomRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Phòng mới");
            assertThat(captor.getValue().getBranchId()).isEqualTo(BRANCH_ID);
        }

        @Test
        @DisplayName("[Alternate Path] Empty update — returns current room without writes")
        void editRoom_noFields_returnsCurrentRoom() {
            givenRoomForUpdate();

            RoomResponse response = service.editRoom(ROOM_ID_STRING, editRequest(null, null));

            assertThat(response.getName()).isEqualTo(ROOM_NAME);
            assertThat(response.getBranchId()).isEqualTo("7");
            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Alternate Path] Values are unchanged after trimming — skips duplicate check and save")
        void editRoom_sameValues_skipsWrite() {
            givenRoomForUpdate();

            service.editRoom(ROOM_ID_STRING, editRequest("7", "  " + ROOM_NAME + "  "));

            verify(clinicRoomRepository, never())
                    .existsByBranchIdAndNameAndClinicRoomIdNot(anyLong(), anyString(), anyLong());
            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Happy Path] Move to active branch — updates branch and preserves omitted name")
        void editRoom_moveToActiveBranch_updatesRoom() {
            givenRoomForUpdate();
            when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> call.getArgument(0));

            RoomResponse response = service.editRoom(ROOM_ID_STRING, editRequest("8", null));

            assertThat(response.getBranchId()).isEqualTo("8");
            assertThat(response.getName()).isEqualTo(ROOM_NAME);
            assertThat(room.getBranch()).isSameAs(targetBranch);
            verify(clinicRoomRepository).hasPhienKham(ROOM_ID);
        }

        @Test
        @DisplayName("[Alternate Path] Same branch ID — does not perform move-only exam-session check")
        void editRoom_sameBranchId_doesNotCheckExamSessions() {
            givenRoomForUpdate();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenAnswer(call -> call.getArgument(0));

            service.editRoom(ROOM_ID_STRING, editRequest("7", "Phòng mới"));

            verify(clinicRoomRepository, never()).hasPhienKham(anyLong());
        }

        @Test
        @DisplayName("[Unhappy Path] Room has exam sessions — rejects branch move without modifying it")
        void editRoom_roomHasExamSessions_rejectsMove() {
            givenRoomForUpdate();
            when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));
            when(clinicRoomRepository.hasPhienKham(ROOM_ID)).thenReturn(true);

            assertAppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest("8", "Phòng mới")));

            assertRoomUnchanged();
            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Target branch is inactive — rejects move before checking exam sessions")
        void editRoom_inactiveTargetBranch_rejectsMoveFirst() {
            givenRoomForUpdate();
            targetBranch.setActive(false);
            when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));

            assertAppException(ErrorCode.BRANCH_INACTIVE,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest("8", null)));

            verify(clinicRoomRepository, never()).hasPhienKham(anyLong());
            assertRoomUnchanged();
        }

        @Test
        @DisplayName("[Unhappy Path] Target branch already has room with that name — rejects move")
        void editRoom_duplicateNameInTargetBranch_rejectsMove() {
            givenRoomForUpdate();
            when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));
            when(clinicRoomRepository.existsByBranchIdAndNameAndClinicRoomIdNot(
                    TARGET_BRANCH_ID, ROOM_NAME, ROOM_ID)).thenReturn(true);

            assertAppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest("8", null)));

            assertRoomUnchanged();
        }

        @Test
        @DisplayName("[Unhappy Path] Blank name — rejects update")
        void editRoom_blankName_rejectsUpdate() {
            givenRoomForUpdate();

            assertAppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest(null, "  ")));

            verify(clinicRoomRepository, never()).saveAndFlush(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid target branch ID — returns invalid parameter type")
        void editRoom_invalidTargetBranchId_returnsTypeError() {
            givenRoomForUpdate();

            assertAppException(ErrorCode.INVALID_PARAMETER_TYPE,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest("abc", null)));

            verifyNoInteractions(branchRepository);
            assertRoomUnchanged();
        }

        @Test
        @DisplayName("[Unhappy Path] Room does not exist — returns room not found")
        void editRoom_missingRoom_returnsNotFound() {
            when(clinicRoomRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.ROOM_NOT_FOUND,
                    () -> service.editRoom("404", editRequest(null, "")));
        }

        @Test
        @DisplayName("[Exception Path] Unique constraint violation — translates to duplicate-name error")
        void editRoom_uniqueViolation_returnsDuplicateError() {
            givenRoomForUpdate();
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(sqlError("23505"));

            assertAppException(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest(null, "Phòng mới")));
        }

        @Test
        @DisplayName("[Exception Path] Foreign-key violation during move — translates to branch not found")
        void editRoom_foreignKeyViolation_returnsBranchNotFound() {
            givenRoomForUpdate();
            when(branchRepository.findById(TARGET_BRANCH_ID)).thenReturn(Optional.of(targetBranch));
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(sqlError("23503"));

            assertAppException(ErrorCode.BRANCH_NOT_FOUND,
                    () -> service.editRoom(ROOM_ID_STRING, editRequest("8", null)));
        }

        @Test
        @DisplayName("[Exception Path] Repository fails to save — propagates unrelated exception")
        void editRoom_unrelatedDatabaseFailure_propagatesException() {
            givenRoomForUpdate();
            DataIntegrityViolationException failure = sqlError("23514");
            when(clinicRoomRepository.saveAndFlush(any(ClinicRoom.class))).thenThrow(failure);

            assertThatThrownBy(() -> service.editRoom(ROOM_ID_STRING, editRequest(null, "Phòng mới")))
                    .isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("deleteRoom")
    class DeleteRoomTests {

        @Test
        @DisplayName("[Happy Path] Room has no exam sessions — deletes and flushes")
        void deleteRoom_noExamSessions_deletesAndFlushes() {
            givenExistingRoom();

            service.deleteRoom(ROOM_ID_STRING);

            InOrder order = inOrder(clinicRoomRepository);
            order.verify(clinicRoomRepository).delete(room);
            order.verify(clinicRoomRepository).flush();
        }

        @Test
        @DisplayName("[Unhappy Path] Room has exam sessions — rejects deletion")
        void deleteRoom_roomHasExamSessions_rejectsDeletion() {
            givenExistingRoom();
            when(clinicRoomRepository.hasPhienKham(ROOM_ID)).thenReturn(true);

            assertAppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS,
                    () -> service.deleteRoom(ROOM_ID_STRING));

            verify(clinicRoomRepository, never()).delete(any(ClinicRoom.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Room does not exist — returns room not found")
        void deleteRoom_missingRoom_returnsNotFound() {
            when(clinicRoomRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom("404"));
        }

        @Test
        @DisplayName("[Exception Path] Foreign-key violation during flush — translates to exam-session error")
        void deleteRoom_foreignKeyViolation_returnsExamSessionError() {
            givenExistingRoom();
            doThrow(sqlError("23503")).when(clinicRoomRepository).flush();

            assertAppException(ErrorCode.ROOM_HAS_EXAM_SESSIONS,
                    () -> service.deleteRoom(ROOM_ID_STRING));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent deletion — translates optimistic lock failure to not found")
        void deleteRoom_concurrentDelete_returnsNotFound() {
            givenExistingRoom();
            doThrow(new ObjectOptimisticLockingFailureException(ClinicRoom.class, ROOM_ID))
                    .when(clinicRoomRepository).flush();

            assertAppException(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom(ROOM_ID_STRING));
        }

        @Test
        @DisplayName("[Exception Path] Unrelated database failure — propagates original exception")
        void deleteRoom_unrelatedDatabaseFailure_propagatesException() {
            givenExistingRoom();
            DataIntegrityViolationException failure = sqlError("23514");
            doThrow(failure).when(clinicRoomRepository).flush();

            assertThatThrownBy(() -> service.deleteRoom(ROOM_ID_STRING)).isSameAs(failure);
        }
    }
}
