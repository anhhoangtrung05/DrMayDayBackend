package com.example.drmayday.features.facility.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.features.facility.dto.CreateBranchRequest;
import com.example.drmayday.features.facility.dto.EditBranchRequest;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Covers hard-to-reproduce branches using test data: TOPOLOGY_DELETE_BRANCH validation order and database error translation.
 * Only the repository boundary is mocked.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BranchServiceImplTest {

    private static final long BRANCH_ID = 7L;
    private static final String ID = "7";

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private ClinicRoomRepository clinicRoomRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    private BranchServiceImpl service;
    private Branch branch;

    @BeforeEach
    void setUp() {
        service = new BranchServiceImpl(branchRepository, clinicRoomRepository, employeeRepository);
        branch = new Branch(BRANCH_ID, "Cơ sở Hoàn Kiếm", "Số 1 Phố Huế", "0241234567", true);
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
        when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(call -> call.getArgument(0));
    }

    private void givenBranchCount(long count) {
        when(branchRepository.count()).thenReturn(count);
    }

    private static void assertFailsWith(ErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }

    // TOPOLOGY_DELETE_BRANCH: validation order.

    @Test
    @DisplayName("AC-13: the last branch returns 2107, not 2102, even when it has rooms and employees")
    void lastBranchWinsOverDependencies() {
        givenBranchCount(1);
        when(clinicRoomRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);
        when(employeeRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.LAST_BRANCH_CANNOT_BE_DELETED, () -> service.deleteBranch(ID));

        verify(branchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("AC-13: the last branch cannot be deleted even when it has no dependencies")
    void lastEmptyBranchCannotBeDeleted() {
        givenBranchCount(1);

        assertFailsWith(ErrorCode.LAST_BRANCH_CANNOT_BE_DELETED, () -> service.deleteBranch(ID));

        verify(branchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("AC-12: with at least two branches, a branch containing rooms returns 2102")
    void branchWithRoomsCannotBeDeleted() {
        givenBranchCount(2);
        when(clinicRoomRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_HAS_DEPENDENCIES, () -> service.deleteBranch(ID));

        verify(branchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("AC-12: with at least two branches, a branch containing employees returns 2102")
    void branchWithStaffCannotBeDeleted() {
        givenBranchCount(2);
        when(employeeRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);

        assertFailsWith(ErrorCode.BRANCH_HAS_DEPENDENCIES, () -> service.deleteBranch(ID));

        verify(branchRepository, never()).delete(any());
    }

    @Test
    @DisplayName("AC-11: an empty branch is deleted and flushed immediately so database errors surface in the service")
    void emptyBranchIsDeletedAndFlushed() {
        givenBranchCount(2);

        service.deleteBranch(ID);

        InOrder order = inOrder(branchRepository);
        order.verify(branchRepository).delete(branch);
        order.verify(branchRepository).flush();
    }

    @Test
    @DisplayName("AC-9: a nonexistent ID returns 2101 before any other checks")
    void missingBranchFailsBeforeOtherChecks() {
        when(branchRepository.findById(99L)).thenReturn(Optional.empty());

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.deleteBranch("99"));

        verify(branchRepository, never()).count();
        verifyNoInteractions(clinicRoomRepository, employeeRepository);
    }

    // TOPOLOGY_DELETE_BRANCH: translate database errors when two admins act concurrently.

    @Test
    @DisplayName("AC-5: a foreign key violation during flush is translated to 2102")
    void foreignKeyViolationBecomesBranchHasDependencies() {
        givenBranchCount(2);
        doThrow(new DataIntegrityViolationException("fk", new SQLException("fk", "23503")))
                .when(branchRepository).flush();

        assertFailsWith(ErrorCode.BRANCH_HAS_DEPENDENCIES, () -> service.deleteBranch(ID));
    }

    @Test
    @DisplayName("AC-5: other database errors are not translated and the original exception is rethrown")
    void otherIntegrityViolationIsRethrown() {
        givenBranchCount(2);
        DataIntegrityViolationException original =
                new DataIntegrityViolationException("check", new SQLException("check", "23514"));
        doThrow(original).when(branchRepository).flush();

        assertThatThrownBy(() -> service.deleteBranch(ID)).isSameAs(original);
    }

    @Test
    @DisplayName("AC-5: if another admin deleted the branch first, the subsequent request receives 2101")
    void concurrentDeleteBecomesBranchNotFound() {
        givenBranchCount(2);
        doThrow(new ObjectOptimisticLockingFailureException(Branch.class, BRANCH_ID))
                .when(branchRepository).flush();

        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.deleteBranch(ID));
    }

    // Path ID.

    @ParameterizedTest
    @ValueSource(strings = {"abc", "", "1.5", "9223372036854775808"})
    @DisplayName("AC-9: an ID that cannot be parsed as Long returns 2101 without calling the database")
    void unparsableIdIsNotFound(String id) {
        assertFailsWith(ErrorCode.BRANCH_NOT_FOUND, () -> service.getBranchDetails(id));

        verify(branchRepository, never()).findById(any());
    }

    // TOPOLOGY_SET_BRANCH and TOPOLOGY_EDIT_BRANCH: normalize data before saving.

    private Branch savedBranch() {
        ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
        verify(branchRepository).saveAndFlush(captor.capture());
        return captor.getValue();
    }

    private static CreateBranchRequest createRequest(String phone) {
        CreateBranchRequest req = new CreateBranchRequest();
        req.setName("  Cơ sở Cầu Giấy  ");
        req.setAddress("  Số 2 Duy Tân  ");
        req.setPhone(phone);
        return req;
    }

    @Test
    @DisplayName("AC-6: branch creation trims the name, address, and phone and always activates the branch")
    void createTrimsValuesAndStartsActive() {
        service.createBranch(createRequest(" +84901234567 "));

        Branch saved = savedBranch();
        assertThat(saved.getName()).isEqualTo("Cơ sở Cầu Giấy");
        assertThat(saved.getAddress()).isEqualTo("Số 2 Duy Tân");
        assertThat(saved.getPhoneNumber()).isEqualTo("+84901234567");
        assertThat(saved.getActive()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("AC-6: an empty phone on creation is stored as NULL because the column disallows empty strings")
    void createStoresBlankPhoneAsNull(String phone) {
        service.createBranch(createRequest(phone));

        assertThat(savedBranch().getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("AC-10: omitting phone preserves the existing number")
    void editKeepsPhoneWhenNotSent() {
        service.editBranch(ID, new EditBranchRequest());

        assertThat(savedBranch().getPhoneNumber()).isEqualTo("0241234567");
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("AC-10: an empty phone removes the phone number")
    void editClearsPhoneWhenBlank(String phone) {
        EditBranchRequest req = new EditBranchRequest();
        req.setPhone(phone);

        service.editBranch(ID, req);

        assertThat(savedBranch().getPhoneNumber()).isNull();
    }

    @Test
    @DisplayName("AC-10: an empty address returns 2003 and does not write any data")
    void editRejectsBlankAddressWithoutSaving() {
        EditBranchRequest req = new EditBranchRequest();
        req.setAddress("   ");

        assertFailsWith(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.editBranch(ID, req));

        verify(branchRepository, never()).saveAndFlush(any());
    }
}
