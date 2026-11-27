package com.example.drmayday.features.topology.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.features.topology.dto.BranchResponse;
import com.example.drmayday.features.topology.dto.CreateBranchRequest;
import com.example.drmayday.features.topology.dto.EditBranchRequest;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.ClinicRoomRepository;
import com.example.drmayday.repository.EmployeeRepository;
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
import org.springframework.data.domain.Sort;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.sql.SQLException;
import java.util.List;
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

@ExtendWith(MockitoExtension.class)
class BranchServiceImplTest {

    private static final long BRANCH_ID = 7L;
    private static final String BRANCH_ID_STRING = "7";

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
        branch = branch(BRANCH_ID, "Cơ sở Hoàn Kiếm", true);
    }

    private static Branch branch(long id, String name, boolean active) {
        return new Branch(id, name, "Số 1 Phố Huế", "0241234567", active);
    }

    private static CreateBranchRequest createRequest(String name, String address, String phone) {
        CreateBranchRequest request = new CreateBranchRequest();
        request.setName(name);
        request.setAddress(address);
        request.setPhone(phone);
        return request;
    }

    private static EditBranchRequest editRequest(String name, String address, String phone, Boolean status) {
        EditBranchRequest request = new EditBranchRequest();
        request.setName(name);
        request.setAddress(address);
        request.setPhone(phone);
        request.setStatus(status);
        return request;
    }

    private void givenExistingBranch() {
        when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
    }

    private static void assertAppException(ErrorCode expected, Runnable action) {
        assertThatThrownBy(action::run)
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("createBranch")
    class CreateBranchTests {

        @Test
        @DisplayName("[Happy Path] Valid request — trims fields, activates branch, and returns saved response")
        void createBranch_validRequest_persistsNormalizedBranch() {
            when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(call -> {
                Branch saved = call.getArgument(0);
                saved.setBranchId(BRANCH_ID);
                return saved;
            });

            BranchResponse response = service.createBranch(
                    createRequest("  Cơ sở Cầu Giấy ", " Số 2 Duy Tân ", " +84901234567 "));

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getName()).isEqualTo("Cơ sở Cầu Giấy");
            assertThat(captor.getValue().getAddress()).isEqualTo("Số 2 Duy Tân");
            assertThat(captor.getValue().getPhoneNumber()).isEqualTo("+84901234567");
            assertThat(captor.getValue().getActive()).isTrue();
            assertThat(response.getBranchId()).isEqualTo(BRANCH_ID_STRING);
            assertThat(response.getName()).isEqualTo("Cơ sở Cầu Giấy");
        }

        @ParameterizedTest
        @ValueSource(strings = {"", "   "})
        @DisplayName("[Alternate Path] Blank phone — stores null")
        void createBranch_blankPhone_storesNull(String phone) {
            when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(call -> call.getArgument(0));

            service.createBranch(createRequest("Branch", "Address", phone));

            ArgumentCaptor<Branch> captor = ArgumentCaptor.forClass(Branch.class);
            verify(branchRepository).saveAndFlush(captor.capture());
            assertThat(captor.getValue().getPhoneNumber()).isNull();
        }

        @Test
        @DisplayName("[Exception Path] Repository fails — propagates original exception")
        void createBranch_repositoryThrows_propagatesException() {
            DataIntegrityViolationException failure = new DataIntegrityViolationException("database unavailable");
            when(branchRepository.saveAndFlush(any(Branch.class))).thenThrow(failure);

            assertThatThrownBy(() -> service.createBranch(createRequest("Branch", "Address", null)))
                    .isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getListBranches")
    class GetListBranchesTests {

        @Test
        @DisplayName("[Happy Path] Branches exist — sorts by ID and maps each response")
        void getListBranches_branchesExist_returnsSortedResponses() {
            Branch second = branch(8L, "Cơ sở Cầu Giấy", false);
            when(branchRepository.findAll(Sort.by(Sort.Direction.ASC, "branchId")))
                    .thenReturn(List.of(branch, second));

            List<BranchResponse> responses = service.getListBranches();

            assertThat(responses).hasSize(2);
            assertThat(responses).extracting(response -> response.getBranchId())
                    .containsExactly("7", "8");
            assertThat(responses.get(1).getStatus()).isFalse();
            verify(branchRepository).findAll(Sort.by(Sort.Direction.ASC, "branchId"));
        }

        @Test
        @DisplayName("[Alternate Path] No branches — returns empty list")
        void getListBranches_noBranches_returnsEmptyList() {
            assertThat(service.getListBranches()).isEmpty();
        }

        @Test
        @DisplayName("[Exception Path] Repository fails — propagates original exception")
        void getListBranches_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(branchRepository.findAll(Sort.by(Sort.Direction.ASC, "branchId"))).thenThrow(failure);

            assertThatThrownBy(() -> service.getListBranches()).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getBranchDetails")
    class GetBranchDetailsTests {

        @Test
        @DisplayName("[Happy Path] Existing ID — returns mapped branch")
        void getBranchDetails_existingId_returnsBranch() {
            givenExistingBranch();

            BranchResponse response = service.getBranchDetails(BRANCH_ID_STRING);

            assertThat(response.getBranchId()).isEqualTo(BRANCH_ID_STRING);
            assertThat(response.getName()).isEqualTo("Cơ sở Hoàn Kiếm");
            assertThat(response.getPhone()).isEqualTo("0241234567");
            verify(branchRepository).findById(BRANCH_ID);
        }

        @ParameterizedTest
        @ValueSource(strings = {"abc", "", "1.5", "9223372036854775808"})
        @DisplayName("[Unhappy Path] ID is not a Long — returns branch not found without repository lookup")
        void getBranchDetails_invalidId_returnsNotFound(String id) {
            assertAppException(ErrorCode.BRANCH_NOT_FOUND, () -> service.getBranchDetails(id));

            verifyNoInteractions(branchRepository);
        }

        @Test
        @DisplayName("[Unhappy Path] ID does not exist — returns branch not found")
        void getBranchDetails_missingBranch_returnsNotFound() {
            when(branchRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.BRANCH_NOT_FOUND, () -> service.getBranchDetails("404"));
        }

        @Test
        @DisplayName("[Exception Path] Repository fails — propagates original exception")
        void getBranchDetails_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(branchRepository.findById(BRANCH_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getBranchDetails(BRANCH_ID_STRING)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("editBranch")
    class EditBranchTests {

        @Test
        @DisplayName("[Happy Path] Valid fields — trims and updates supplied values, retaining omitted values")
        void editBranch_validRequest_updatesBranch() {
            givenExistingBranch();
            when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(call -> call.getArgument(0));

            BranchResponse response = service.editBranch(
                    BRANCH_ID_STRING, editRequest(" Cơ sở mới ", null, " 0249999999 ", false));

            assertThat(response.getName()).isEqualTo("Cơ sở mới");
            assertThat(response.getAddress()).isEqualTo("Số 1 Phố Huế");
            assertThat(response.getPhone()).isEqualTo("0249999999");
            assertThat(response.getStatus()).isFalse();
            verify(branchRepository).saveAndFlush(branch);
        }

        @Test
        @DisplayName("[Alternate Path] Optional fields omitted — retains existing values")
        void editBranch_noFields_retainsExistingBranch() {
            givenExistingBranch();
            when(branchRepository.saveAndFlush(any(Branch.class))).thenAnswer(call -> call.getArgument(0));

            BranchResponse response = service.editBranch(BRANCH_ID_STRING, new EditBranchRequest());

            assertThat(response.getName()).isEqualTo("Cơ sở Hoàn Kiếm");
            assertThat(response.getAddress()).isEqualTo("Số 1 Phố Huế");
            assertThat(response.getPhone()).isEqualTo("0241234567");
            assertThat(response.getStatus()).isTrue();
        }

        @ParameterizedTest
        @ValueSource(strings = {"name", "address"})
        @DisplayName("[Unhappy Path] Supplied name or address is blank — rejects update without saving")
        void editBranch_blankRequiredField_rejectsUpdate(String field) {
            givenExistingBranch();
            EditBranchRequest request = new EditBranchRequest();
            if ("name".equals(field)) {
                request.setName("   ");
            } else {
                request.setAddress("   ");
            }

            assertAppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.editBranch(BRANCH_ID_STRING, request));

            verify(branchRepository, never()).saveAndFlush(any(Branch.class));
        }

        @Test
        @DisplayName("[Unhappy Path] ID does not exist — returns branch not found")
        void editBranch_missingBranch_returnsNotFound() {
            when(branchRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.BRANCH_NOT_FOUND,
                    () -> service.editBranch("404", new EditBranchRequest()));

            verify(branchRepository, never()).saveAndFlush(any(Branch.class));
        }

        @Test
        @DisplayName("[Exception Path] Repository fails to save — propagates original exception")
        void editBranch_repositoryThrows_propagatesException() {
            givenExistingBranch();
            DataIntegrityViolationException failure = new DataIntegrityViolationException("database unavailable");
            when(branchRepository.saveAndFlush(any(Branch.class))).thenThrow(failure);

            assertThatThrownBy(() -> service.editBranch(BRANCH_ID_STRING, new EditBranchRequest()))
                    .isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("deleteBranch")
    class DeleteBranchTests {

        @Test
        @DisplayName("[Happy Path] Branch is not the last and has no dependencies — deletes and flushes")
        void deleteBranch_branchCanBeDeleted_deletesAndFlushes() {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(2L);

            service.deleteBranch(BRANCH_ID_STRING);

            InOrder order = inOrder(branchRepository);
            order.verify(branchRepository).delete(branch);
            order.verify(branchRepository).flush();
        }

        @Test
        @DisplayName("[Unhappy Path] Branch is the last one — rejects deletion before checking dependencies")
        void deleteBranch_lastBranch_rejectsDeletionFirst() {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(1L);

            assertAppException(ErrorCode.LAST_BRANCH_CANNOT_BE_DELETED,
                    () -> service.deleteBranch(BRANCH_ID_STRING));

            verifyNoInteractions(clinicRoomRepository, employeeRepository);
            verify(branchRepository, never()).delete(any(Branch.class));
        }

        @ParameterizedTest
        @ValueSource(strings = {"room", "employee"})
        @DisplayName("[Unhappy Path] Branch has rooms or employees — rejects deletion")
        void deleteBranch_hasDependencies_rejectsDeletion(String dependency) {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(2L);
            if ("room".equals(dependency)) {
                when(clinicRoomRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);
            } else {
                when(employeeRepository.existsByBranchId(BRANCH_ID)).thenReturn(true);
            }

            assertAppException(ErrorCode.BRANCH_HAS_DEPENDENCIES,
                    () -> service.deleteBranch(BRANCH_ID_STRING));

            verify(branchRepository, never()).delete(any(Branch.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Branch does not exist — returns not found before dependency checks")
        void deleteBranch_missingBranch_returnsNotFound() {
            when(branchRepository.findById(404L)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.BRANCH_NOT_FOUND, () -> service.deleteBranch("404"));

            verify(branchRepository, never()).count();
            verifyNoInteractions(clinicRoomRepository, employeeRepository);
        }

        @Test
        @DisplayName("[Exception Path] Concurrent foreign-key dependency — translates to dependency error")
        void deleteBranch_foreignKeyViolation_returnsDependencyError() {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(2L);
            doThrow(new DataIntegrityViolationException("fk", new SQLException("fk", "23503")))
                    .when(branchRepository).flush();

            assertAppException(ErrorCode.BRANCH_HAS_DEPENDENCIES,
                    () -> service.deleteBranch(BRANCH_ID_STRING));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent deletion — translates optimistic lock failure to not found")
        void deleteBranch_concurrentDelete_returnsNotFound() {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(2L);
            doThrow(new ObjectOptimisticLockingFailureException(Branch.class, BRANCH_ID))
                    .when(branchRepository).flush();

            assertAppException(ErrorCode.BRANCH_NOT_FOUND,
                    () -> service.deleteBranch(BRANCH_ID_STRING));
        }

        @Test
        @DisplayName("[Exception Path] Unrelated database failure — propagates original exception")
        void deleteBranch_unrelatedDatabaseFailure_propagatesException() {
            givenExistingBranch();
            when(branchRepository.count()).thenReturn(2L);
            DataIntegrityViolationException failure =
                    new DataIntegrityViolationException("check", new SQLException("check", "23514"));
            doThrow(failure).when(branchRepository).flush();

            assertThatThrownBy(() -> service.deleteBranch(BRANCH_ID_STRING)).isSameAs(failure);
        }
    }
}
