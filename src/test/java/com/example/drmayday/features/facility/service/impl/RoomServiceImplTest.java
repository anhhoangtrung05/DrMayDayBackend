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
 * Khóa các nhánh khó tạo bằng dữ liệu thật: thứ tự kiểm tra của API 29, 32, 33 và cách đổi lỗi database.
 * Chỉ giả lập repository, là ranh giới với database.
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

    // API 29

    @Test
    @DisplayName("AC-14: tạo phòng lưu tên đã trim và trả đúng các trường của phòng mới")
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
    @DisplayName("AC-16: kiểm tra trùng tên dùng tên đã trim")
    void createChecksDuplicateAfterTrim() {
        when(clinicRoomRepository.existsByBranchIdAndName(BRANCH_ID, ROOM_NAME)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS,
                () -> service.createRoom(createRequest("7", "  " + ROOM_NAME + "  ")));

        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @ParameterizedTest
    @ValueSource(strings = {"abc", "1.5", "99999999999999999999", ""})
    @DisplayName("AC-15: branch_id không phải số Long trả 2002 và chưa chạm tới database")
    void createRejectsNonNumericBranchId(String branchId) {
        assertFailsWith(ErrorCode.INVALID_PARAMETER_TYPE, () -> service.createRoom(createRequest(branchId, ROOM_NAME)));

        verifyNoInteractions(branchRepository, clinicRoomRepository);
    }

    @Test
    @DisplayName("AC-15: chi nhánh không tồn tại trả 2101 và không ghi")
    void createRejectsMissingBranch() {
        when(branchRepository.findById(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.createRoom(createRequest("404", ROOM_NAME)));

        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("Thứ tự API 29: chi nhánh ngừng hoạt động thắng lỗi trùng tên")
    void createChecksInactiveBranchBeforeDuplicateName() {
        branch.setActive(false);
        when(clinicRoomRepository.existsByBranchIdAndName(anyLong(), anyString())).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_INACTIVE, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("AC-5: tạo phòng gặp lỗi trùng khóa từ database thì trả 2104, không phải 3006")
    void createTranslatesUniqueViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23505"));

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("AC-5: tạo phòng gặp lỗi khóa ngoại từ database thì trả 2101")
    void createTranslatesForeignKeyViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23503"));

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.createRoom(createRequest("7", ROOM_NAME)));
    }

    @Test
    @DisplayName("Lỗi database khác khi tạo phòng không bị đổi thành mã 21xx")
    void createRethrowsOtherDatabaseErrors() {
        DataIntegrityViolationException checkViolation = sqlError("23514");
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(checkViolation);

        assertThatThrownBy(() -> service.createRoom(createRequest("7", ROOM_NAME))).isSameAs(checkViolation);
    }

    // API 30

    @Test
    @DisplayName("AC-17: không có branch_id thì lấy mọi phòng và không kiểm tra chi nhánh")
    void listWithoutFilterSkipsBranchCheck() {
        when(clinicRoomRepository.findAll()).thenReturn(List.of(room));

        List<RoomResponse> rooms = service.getListRooms(null);

        assertThat(rooms).extracting(roomResponse -> roomResponse.getRoomId()).containsExactly(ID);
        verifyNoInteractions(branchRepository);
    }

    @Test
    @DisplayName("AC-17: chi nhánh tồn tại nhưng không có phòng thì trả mảng rỗng")
    void listReturnsEmptyForBranchWithoutRooms() {
        when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
        when(clinicRoomRepository.findAllByBranchId(BRANCH_ID)).thenReturn(List.of());

        assertThat(service.getListRooms(BRANCH_ID)).isEmpty();
    }

    @Test
    @DisplayName("AC-17: bộ lọc với chi nhánh không tồn tại trả 2101")
    void listRejectsMissingBranch() {
        when(branchRepository.existsById(404L)).thenReturn(false);

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.getListRooms(404L));

        verify(clinicRoomRepository, never()).findAllByBranchId(anyLong());
    }

    // API 31

    @ParameterizedTest
    @ValueSource(strings = {"abc", "99999999999999999999", ""})
    @DisplayName("AC-18: id không phải số Long trả 2103 và chưa chạm tới database")
    void detailRejectsNonNumericId(String id) {
        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.getRoomDetails(id));

        verifyNoInteractions(clinicRoomRepository);
    }

    // API 32

    @Test
    @DisplayName("API 32 đọc phòng bằng khóa FOR UPDATE, không dùng findById")
    void editReadsRoomThroughRowLock() {
        service.editRoom(ID, editRequest(null, "Phòng mới"));

        verify(clinicRoomRepository).findByIdForUpdate(ROOM_ID);
        verify(clinicRoomRepository, never()).findById(anyLong());
    }

    @Test
    @DisplayName("Thứ tự API 32: id sai thắng tên rỗng")
    void editChecksRoomBeforeName() {
        when(clinicRoomRepository.findByIdForUpdate(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.editRoom("404", editRequest(null, "")));
    }

    @Test
    @DisplayName("Thứ tự API 32: tên rỗng thắng branch_id sai kiểu")
    void editChecksNameBeforeBranchId() {
        assertFailsWith(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.editRoom(ID, editRequest("abc", "   ")));
    }

    @Test
    @DisplayName("Thứ tự API 32: chi nhánh đích ngừng hoạt động thắng lỗi phòng có phiên khám")
    void editChecksTargetBranchBeforeExamSessions() {
        targetBranch.setActive(false);
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_INACTIVE, () -> service.editRoom(ID, editRequest("8", null)));

        assertRoomUntouched();
    }

    @Test
    @DisplayName("AC-19: body không có trường nào thì trả dữ liệu hiện tại và không ghi")
    void editWithEmptyBodyWritesNothing() {
        RoomResponse response = service.editRoom(ID, editRequest(null, null));

        assertThat(response.getName()).isEqualTo(ROOM_NAME);
        assertThat(response.getBranchId()).isEqualTo("7");
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-19: giá trị không đổi sau khi trim thì bỏ qua kiểm tra trùng tên và không ghi")
    void editWithSameValuesWritesNothing() {
        service.editRoom(ID, editRequest("7", "  " + ROOM_NAME + "  "));

        verify(clinicRoomRepository, never())
                .existsByBranchIdAndNameAndRoomIdNot(anyLong(), anyString(), anyLong());
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-20: gửi branch_id trùng chi nhánh hiện tại không tính là chuyển nên không xét phiên khám")
    void editWithCurrentBranchIsNotAMove() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        RoomResponse response = service.editRoom(ID, editRequest("7", "Phòng mới"));

        assertThat(response.getName()).isEqualTo("Phòng mới");
        verify(clinicRoomRepository, never()).hasMedicalVisits(anyLong());
    }

    @Test
    @DisplayName("AC-20: phòng có phiên khám không chuyển được, tên gửi kèm cũng không đổi")
    void editRefusesToMoveRoomWithExamSessions() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.editRoom(ID, editRequest("8", "Phòng mới")));

        assertRoomUntouched();
        verify(clinicRoomRepository, never()).saveAndFlush(any());
    }

    @Test
    @DisplayName("AC-20: chuyển phòng kiểm tra trùng tên ở chi nhánh đích, trừ chính phòng này")
    void editChecksDuplicateInTargetBranch() {
        when(clinicRoomRepository.existsByBranchIdAndNameAndRoomIdNot(
                TARGET_BRANCH_ID, ROOM_NAME, ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.editRoom(ID, editRequest("8", null)));

        assertRoomUntouched();
    }

    @Test
    @DisplayName("AC-20: chuyển phòng lưu chi nhánh mới và giữ tên cũ khi không gửi tên")
    void editMovesRoomToTargetBranch() {
        service.editRoom(ID, editRequest("8", null));

        ArgumentCaptor<ClinicRoom> saved = ArgumentCaptor.forClass(ClinicRoom.class);
        verify(clinicRoomRepository).saveAndFlush(saved.capture());
        assertThat(saved.getValue().getBranch()).isSameAs(targetBranch);
        assertThat(saved.getValue().getName()).isEqualTo(ROOM_NAME);
    }

    @Test
    @DisplayName("AC-5: sửa phòng gặp lỗi trùng khóa từ database thì trả 2104")
    void editTranslatesUniqueViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23505"));

        assertFailsWith(ErrorCode.ROOM_NAME_ALREADY_EXISTS, () -> service.editRoom(ID, editRequest(null, "Phòng mới")));
    }

    @Test
    @DisplayName("AC-5: chuyển phòng gặp lỗi khóa ngoại từ database thì trả 2101")
    void editTranslatesForeignKeyViolation() {
        when(clinicRoomRepository.saveAndFlush(any())).thenThrow(sqlError("23503"));

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.editRoom(ID, editRequest("8", null)));
    }

    // API 33

    @Test
    @DisplayName("AC-21: xóa rồi flush ngay, để lỗi database xảy ra trong khối try")
    void deleteFlushesInsideTheTry() {
        service.deleteRoom(ID);

        InOrder order = inOrder(clinicRoomRepository);
        order.verify(clinicRoomRepository).delete(room);
        order.verify(clinicRoomRepository).flush();
    }

    @Test
    @DisplayName("AC-22: phòng có phiên khám trả 2105 và không gọi xóa")
    void deleteRefusesRoomWithExamSessions() {
        when(clinicRoomRepository.hasMedicalVisits(ROOM_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.deleteRoom(ID));

        verify(clinicRoomRepository, never()).delete(any(ClinicRoom.class));
    }

    @Test
    @DisplayName("AC-5: phiên khám chen vào lúc xóa, database báo khóa ngoại thì trả 2105")
    void deleteTranslatesForeignKeyViolation() {
        doThrow(sqlError("23503")).when(clinicRoomRepository).flush();

        assertFailsWith(ErrorCode.ROOM_HAS_EXAM_SESSIONS, () -> service.deleteRoom(ID));
    }

    @Test
    @DisplayName("AC-5: Admin khác đã xóa phòng trước thì trả 2103, không phải 9999")
    void deleteTranslatesLostRace() {
        doThrow(new ObjectOptimisticLockingFailureException(ClinicRoom.class, ROOM_ID))
                .when(clinicRoomRepository).flush();

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom(ID));
    }

    @Test
    @DisplayName("Lỗi database khác khi xóa phòng không bị đổi thành mã 21xx")
    void deleteRethrowsOtherDatabaseErrors() {
        DataIntegrityViolationException checkViolation = sqlError("23514");
        doThrow(checkViolation).when(clinicRoomRepository).flush();

        assertThatThrownBy(() -> service.deleteRoom(ID)).isSameAs(checkViolation);
    }

    @Test
    @DisplayName("AC-18: xóa phòng với id không tồn tại trả 2103")
    void deleteRejectsMissingRoom() {
        when(clinicRoomRepository.findById(404L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.ROOM_NOT_FOUND, () -> service.deleteRoom("404"));
    }
}
