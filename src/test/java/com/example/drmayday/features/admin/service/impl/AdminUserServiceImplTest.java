package com.example.drmayday.features.admin.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Branch;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Role;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.admin.dto.AssignUserRequest;
import com.example.drmayday.features.admin.dto.CreatePatientRequest;
import com.example.drmayday.features.admin.dto.CreatePersonRequest;
import com.example.drmayday.features.admin.dto.CreateStaffRequest;
import com.example.drmayday.features.admin.dto.CreateUserRequest;
import com.example.drmayday.features.admin.dto.CreatedUserResponse;
import com.example.drmayday.features.admin.dto.EditUserRequest;
import com.example.drmayday.features.admin.dto.StaffAssignResponse;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.RoleRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceImplTest {

    private static final long PERSON_ID = 101L;
    private static final long ROLE_ID = 2L;
    private static final long BRANCH_ID = 7L;
    private static final String HASHED_PASSWORD = "$2a$test-hash";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private BranchRepository branchRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    private AdminUserServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new AdminUserServiceImpl(
                userAccountRepository,
                personRepository,
                patientRepository,
                employeeRepository,
                roleRepository,
                branchRepository,
                passwordEncoder);
    }

    private static Person person() {
        return Person.builder()
                .personId(PERSON_ID)
                .fullName("Existing Person")
                .phoneNumber("0912345678")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();
    }

    private static Role role() {
        return Role.builder().roleId(ROLE_ID).name("bac_si").displayName("Bác sĩ").build();
    }

    private static UserAccount userAccount() {
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        return UserAccount.builder()
                .accountId(202L)
                .person(person())
                .role(role())
                .username("doctor.101")
                .passwordHash(HASHED_PASSWORD)
                .active(true)
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private static void assertError(ErrorCode expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("getListUsers")
    class GetListUsersTests {

        @Test
        @DisplayName("[Happy Path] Requested page — returns mapped users and passes paging")
        void getListUsers_validPage_returnsUsersAndPassesPaging() {
            UserAccount account = userAccount();
            when(userAccountRepository.findAll(any(Pageable.class)))
                    .thenReturn(new PageImpl<>(List.of(account)));

            Page<com.example.drmayday.features.admin.dto.UserResponse> response =
                    service.getListUsers(2, 5);

            assertThat(response.getContent()).singleElement().satisfies(user -> {
                assertThat(user.getUserId()).isEqualTo("202");
                assertThat(user.getFullName()).isEqualTo("Existing Person");
                assertThat(user.getPhone()).isEqualTo("doctor.101");
                assertThat(user.getRoleId()).isEqualTo(String.valueOf(ROLE_ID));
                assertThat(user.getIsActive()).isEqualTo("1");
            });

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(userAccountRepository).findAll(pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isEqualTo(1);
            assertThat(pageable.getValue().getPageSize()).isEqualTo(5);
            assertThat(pageable.getValue().getSort().getOrderFor("accountId").getDirection())
                    .isEqualTo(org.springframework.data.domain.Sort.Direction.DESC);
        }

        @Test
        @DisplayName("[Alternate Path] Invalid page and limit — applies safe defaults")
        void getListUsers_invalidPageAndLimit_appliesSafeDefaults() {
            when(userAccountRepository.findAll(any(Pageable.class))).thenReturn(Page.empty());

            service.getListUsers(0, 0);

            ArgumentCaptor<Pageable> pageable = ArgumentCaptor.forClass(Pageable.class);
            verify(userAccountRepository).findAll(pageable.capture());
            assertThat(pageable.getValue().getPageNumber()).isZero();
            assertThat(pageable.getValue().getPageSize()).isEqualTo(20);
        }
    }

    @Nested
    @DisplayName("getUserDetails")
    class GetUserDetailsTests {

        @Test
        @DisplayName("[Happy Path] Existing account — returns mapped details")
        void getUserDetails_existingAccount_returnsMappedDetails() {
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(userAccount()));

            var response = service.getUserDetails("202");

            assertThat(response.getUserId()).isEqualTo("202");
            assertThat(response.getFullName()).isEqualTo("Existing Person");
            assertThat(response.getPhone()).isEqualTo("doctor.101");
            assertThat(response.getRoleId()).isEqualTo(String.valueOf(ROLE_ID));
            assertThat(response.getIsActive()).isEqualTo("1");
        }

        @Test
        @DisplayName("[Unhappy Path] Account does not exist — returns not found")
        void getUserDetails_missingAccount_returnsNotFound() {
            when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getUserDetails("999"));
        }

        @Test
        @DisplayName("[Unhappy Path] ID is not numeric — returns not found")
        void getUserDetails_invalidId_returnsNotFound() {
            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getUserDetails("not-a-number"));

            verify(userAccountRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("createPerson")
    class CreatePersonTests {

        @Test
        @DisplayName("[Happy Path] Valid profile data — creates only a person record")
        void createPerson_validRequest_createsPersonOnly() {
            when(personRepository.saveAndFlush(any(Person.class))).thenAnswer(invocation -> {
                Person saved = invocation.getArgument(0);
                saved.setPersonId(PERSON_ID);
                return saved;
            });
            CreatePersonRequest request = new CreatePersonRequest(
                    " New Person ",
                    "0999999999",
                    LocalDate.of(1990, 1, 2),
                    "Nu",
                    " Hà Nội ",
                    " Phường A ",
                    " Số 1 ");

            PersonProfileResponse response = service.createPerson(request);

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.fullName()).isEqualTo("New Person");
            assertThat(response.phoneNumber()).isEqualTo("0999999999");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 1, 2));
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
            verify(patientRepository, never()).save(any(Patient.class));
            verify(employeeRepository, never()).save(any(Employee.class));
            verify(userAccountRepository, never()).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Required fields missing — rejects request")
        void createPerson_missingRequiredFields_throwsBadRequest() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.createPerson(null));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.createPerson(new CreatePersonRequest(" ", "0999999999", null, null, null, null, null)));

            verify(personRepository, never()).saveAndFlush(any(Person.class));
        }
    }

    @Nested
    @DisplayName("createUser")
    class CreateUserTests {

        @Test
        @DisplayName("[Happy Path] Existing person and role — creates account without creating profiles")
        void createUser_validRequest_createsOnlyAccount() {
            Person person = person();
            Role role = role();
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(false);
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role));
            when(userAccountRepository.existsByUsernameIgnoreCase("doctor.101")).thenReturn(false);
            when(passwordEncoder.encode("Secret123!")).thenReturn(HASHED_PASSWORD);
            when(userAccountRepository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
                UserAccount saved = invocation.getArgument(0);
                saved.setAccountId(303L);
                return saved;
            });

            CreatedUserResponse response = service.createUser(
                    new CreateUserRequest(PERSON_ID, ROLE_ID, " doctor.101 ", "Secret123!", null));

            assertThat(response.userId()).isEqualTo(303L);
            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.roleId()).isEqualTo(ROLE_ID);
            assertThat(response.username()).isEqualTo("doctor.101");
            assertThat(response.isActive()).isTrue();
            assertThat(response.createdAt()).isNotNull();
            verify(passwordEncoder).encode("Secret123!");
            verify(userAccountRepository).saveAndFlush(any(UserAccount.class));
            verify(patientRepository, never()).save(any(Patient.class));
            verify(employeeRepository, never()).save(any(Employee.class));
            verify(personRepository, never()).save(any(Person.class));
        }

        @Test
        @DisplayName("[Alternate Path] is_active false — creates an inactive account")
        void createUser_inactiveRequest_preservesInactiveStatus() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(false);
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role()));
            when(userAccountRepository.existsByUsernameIgnoreCase("user.101")).thenReturn(false);
            when(passwordEncoder.encode("Secret123!")).thenReturn(HASHED_PASSWORD);
            when(userAccountRepository.saveAndFlush(any(UserAccount.class))).thenAnswer(invocation -> {
                UserAccount saved = invocation.getArgument(0);
                saved.setAccountId(303L);
                return saved;
            });

            CreatedUserResponse response = service.createUser(
                    new CreateUserRequest(PERSON_ID, ROLE_ID, "user.101", "Secret123!", false));

            assertThat(response.isActive()).isFalse();
        }

        @Test
        @DisplayName("[Unhappy Path] Person does not exist — returns person not found")
        void createUser_missingPerson_returnsNotFound() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PERSON_NOT_FOUND,
                    () -> service.createUser(new CreateUserRequest(PERSON_ID, ROLE_ID, "user", "password", null)));
        }

        @Test
        @DisplayName("[Unhappy Path] Person already has an account — returns conflict")
        void createUser_accountAlreadyExists_returnsConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(true);

            assertError(ErrorCode.PERSON_ALREADY_HAS_USER_ACCOUNT,
                    () -> service.createUser(new CreateUserRequest(PERSON_ID, ROLE_ID, "user", "password", null)));

            verify(userAccountRepository, never()).saveAndFlush(any(UserAccount.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Role does not exist — returns invalid parameter")
        void createUser_invalidRole_returnsBadRequest() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(false);
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.createUser(new CreateUserRequest(PERSON_ID, ROLE_ID, "user", "password", null)));
        }

        @Test
        @DisplayName("[Unhappy Path] Username already exists — returns invalid parameter")
        void createUser_duplicateUsername_returnsBadRequest() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(false);
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role()));
            when(userAccountRepository.existsByUsernameIgnoreCase("user.101")).thenReturn(true);

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.createUser(new CreateUserRequest(PERSON_ID, ROLE_ID, "user.101", "password", null)));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent account creation — maps unique person constraint to conflict")
        void createUser_uniquePersonConstraint_mapsToConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(userAccountRepository.existsByPerson_PersonId(PERSON_ID)).thenReturn(false);
            when(roleRepository.findById(ROLE_ID)).thenReturn(Optional.of(role()));
            when(userAccountRepository.existsByUsernameIgnoreCase("user.101")).thenReturn(false);
            when(passwordEncoder.encode("password")).thenReturn(HASHED_PASSWORD);
            when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate uq_tai_khoan_nguoi"));

            assertError(ErrorCode.PERSON_ALREADY_HAS_USER_ACCOUNT,
                    () -> service.createUser(new CreateUserRequest(PERSON_ID, ROLE_ID, "user.101", "password", null)));
        }
    }

    @Nested
    @DisplayName("createPatient")
    class CreatePatientTests {

        @Test
        @DisplayName("[Happy Path] Existing person — creates a patient profile only")
        void createPatient_validRequest_createsPatient() {
            Person person = person();
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person));
            when(patientRepository.existsById(PERSON_ID)).thenReturn(false);
            when(patientRepository.existsByHisCode("HIS-101")).thenReturn(false);
            when(patientRepository.saveAndFlush(any(Patient.class))).thenAnswer(invocation -> {
                Patient saved = invocation.getArgument(0);
                saved.setPatientId(PERSON_ID);
                return saved;
            });

            PatientProfileResponse response = service.createPatient(new CreatePatientRequest(
                    PERSON_ID, " HIS-101 ", "Nong dan", "Kinh", "BHYT-1",
                    LocalDate.of(2030, 1, 1), "Contact, Address", "0987654321"));

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.hisCode()).isEqualTo("HIS-101");
            assertThat(response.occupation()).isEqualTo("Nong dan");
            assertThat(response.healthInsuranceNumber()).isEqualTo("BHYT-1");
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(employeeRepository, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Person does not exist — returns person not found")
        void createPatient_missingPerson_returnsNotFound() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PERSON_NOT_FOUND,
                    () -> service.createPatient(new CreatePatientRequest(PERSON_ID, null, null, null, null,
                            null, null, null)));
        }

        @Test
        @DisplayName("[Unhappy Path] Patient profile already exists — returns conflict")
        void createPatient_duplicatePerson_returnsConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(patientRepository.existsById(PERSON_ID)).thenReturn(true);

            assertError(ErrorCode.PERSON_ALREADY_REGISTERED_AS_PATIENT,
                    () -> service.createPatient(new CreatePatientRequest(PERSON_ID, null, null, null, null,
                            null, null, null)));
        }

        @Test
        @DisplayName("[Unhappy Path] HIS code already exists — returns conflict")
        void createPatient_duplicateHisCode_returnsConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(patientRepository.existsById(PERSON_ID)).thenReturn(false);
            when(patientRepository.existsByHisCode("HIS-101")).thenReturn(true);

            assertError(ErrorCode.PATIENT_HIS_CODE_ALREADY_EXISTS,
                    () -> service.createPatient(new CreatePatientRequest(PERSON_ID, "HIS-101", null, null, null,
                            null, null, null)));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent patient creation — maps primary key violation to conflict")
        void createPatient_primaryKeyConstraint_mapsToConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(patientRepository.existsById(PERSON_ID)).thenReturn(false);
            when(patientRepository.saveAndFlush(any(Patient.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate pk_benh_nhan"));

            assertError(ErrorCode.PERSON_ALREADY_REGISTERED_AS_PATIENT,
                    () -> service.createPatient(new CreatePatientRequest(PERSON_ID, null, null, null, null,
                            null, null, null)));
        }
    }

    @Nested
    @DisplayName("createStaff")
    class CreateStaffTests {

        @Test
        @DisplayName("[Happy Path] Existing person and branch — creates a staff profile only")
        void createStaff_validRequest_createsStaff() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
            when(employeeRepository.existsById(PERSON_ID)).thenReturn(false);
            when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> {
                Employee saved = invocation.getArgument(0);
                saved.setEmployeeId(PERSON_ID);
                return saved;
            });

            EmployeeProfileResponse response = service.createStaff(new CreateStaffRequest(PERSON_ID, BRANCH_ID));

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.branchId()).isEqualTo(BRANCH_ID);
            verify(userAccountRepository, never()).save(any(UserAccount.class));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Person does not exist — returns person not found")
        void createStaff_missingPerson_returnsNotFound() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.PERSON_NOT_FOUND,
                    () -> service.createStaff(new CreateStaffRequest(PERSON_ID, BRANCH_ID)));
        }

        @Test
        @DisplayName("[Unhappy Path] Branch does not exist — returns invalid parameter")
        void createStaff_missingBranch_returnsBadRequest() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(false);

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.createStaff(new CreateStaffRequest(PERSON_ID, BRANCH_ID)));
        }

        @Test
        @DisplayName("[Unhappy Path] Employee profile already exists — returns conflict")
        void createStaff_duplicatePerson_returnsConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
            when(employeeRepository.existsById(PERSON_ID)).thenReturn(true);

            assertError(ErrorCode.PERSON_ALREADY_REGISTERED_AS_EMPLOYEE,
                    () -> service.createStaff(new CreateStaffRequest(PERSON_ID, BRANCH_ID)));
        }

        @Test
        @DisplayName("[Exception Path] Concurrent staff creation — maps primary key violation to conflict")
        void createStaff_primaryKeyConstraint_mapsToConflict() {
            when(personRepository.findById(PERSON_ID)).thenReturn(Optional.of(person()));
            when(branchRepository.existsById(BRANCH_ID)).thenReturn(true);
            when(employeeRepository.existsById(PERSON_ID)).thenReturn(false);
            when(employeeRepository.saveAndFlush(any(Employee.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate pk_nhan_vien"));

            assertError(ErrorCode.PERSON_ALREADY_REGISTERED_AS_EMPLOYEE,
                    () -> service.createStaff(new CreateStaffRequest(PERSON_ID, BRANCH_ID)));
        }
    }

    @Nested
    @DisplayName("editUser")
    class EditUserTests {

        @Test
        @DisplayName("[Happy Path] Name and active status supplied — updates account and person")
        void editUser_validRequest_updatesUserAndPerson() {
            UserAccount account = userAccount();
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(account));
            when(userAccountRepository.save(any(UserAccount.class))).thenAnswer(invocation -> invocation.getArgument(0));
            EditUserRequest request = new EditUserRequest(" Updated Name ", null, "0");

            var response = service.editUser("202", request);

            assertThat(response.getUserId()).isEqualTo("202");
            assertThat(response.getFullName()).isEqualTo("Updated Name");
            assertThat(response.getIsActive()).isEqualTo("0");
            verify(personRepository).save(account.getPerson());
            verify(userAccountRepository).save(account);
        }

        @Test
        @DisplayName("[Unhappy Path] Account does not exist — returns not found")
        void editUser_missingAccount_returnsNotFound() {
            when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> service.editUser("999", new EditUserRequest()));

            verify(userAccountRepository, never()).save(any(UserAccount.class));
        }

        @Test
        @DisplayName("[Unhappy Path] ID is not numeric — returns not found")
        void editUser_invalidId_returnsNotFound() {
            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> service.editUser("not-a-number", new EditUserRequest()));

            verify(userAccountRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("deleteUser")
    class DeleteUserTests {

        @Test
        @DisplayName("[Happy Path] Existing account — deletes account and flushes")
        void deleteUser_existingAccount_deletesAccount() {
            UserAccount account = userAccount();
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(account));

            service.deleteUser("202");

            verify(userAccountRepository).delete(account);
            verify(userAccountRepository).flush();
        }

        @Test
        @DisplayName("[Unhappy Path] Account does not exist — returns not found")
        void deleteUser_missingAccount_returnsNotFound() {
            when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.deleteUser("999"));

            verify(userAccountRepository, never()).delete(any(UserAccount.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Non-numeric ID — returns not found")
        void deleteUser_invalidId_returnsNotFound() {
            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.deleteUser("abc"));

            verify(userAccountRepository, never()).findById(any());
        }
    }

    @Nested
    @DisplayName("assignUser")
    class AssignUserTests {

        @Test
        @DisplayName("[Happy Path] Valid staff user and active branch — updates branchId")
        void assignUser_validRequest_updatesBranch() {
            UserAccount account = userAccount();
            Branch branch = Branch.builder().branchId(BRANCH_ID).name("Chi nhánh Cầu Giấy").active(true).build();
            Employee employee = Employee.builder().employeeId(PERSON_ID).branchId(1L).build();

            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(account));
            when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(branch));
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.of(employee));
            when(employeeRepository.saveAndFlush(any(Employee.class))).thenAnswer(invocation -> invocation.getArgument(0));

            StaffAssignResponse response = service.assignUser(new AssignUserRequest(202L, BRANCH_ID));

            assertThat(response.getStaffId()).isEqualTo(String.valueOf(PERSON_ID));
            assertThat(response.getUserId()).isEqualTo("202");
            assertThat(response.getBranchId()).isEqualTo(String.valueOf(BRANCH_ID));
            assertThat(response.getBranchName()).isEqualTo("Chi nhánh Cầu Giấy");
            assertThat(employee.getBranchId()).isEqualTo(BRANCH_ID);
            verify(employeeRepository).saveAndFlush(employee);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing required fields — returns 2001")
        void assignUser_missingFields_returnsBadRequest() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.assignUser(null));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.assignUser(new AssignUserRequest(null, BRANCH_ID)));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.assignUser(new AssignUserRequest(202L, null)));
        }

        @Test
        @DisplayName("[Unhappy Path] User account not found — returns 3007")
        void assignUser_missingAccount_returnsNotFound() {
            when(userAccountRepository.findById(999L)).thenReturn(Optional.empty());

            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.assignUser(new AssignUserRequest(999L, BRANCH_ID)));
        }

        @Test
        @DisplayName("[Unhappy Path] Branch not found — returns 2003")
        void assignUser_missingBranch_returnsBadRequest() {
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(userAccount()));
            when(branchRepository.findById(999L)).thenReturn(Optional.empty());

            assertError(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.assignUser(new AssignUserRequest(202L, 999L)));
        }

        @Test
        @DisplayName("[Unhappy Path] Inactive branch — returns 2106")
        void assignUser_inactiveBranch_returnsBadRequest() {
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(userAccount()));
            when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(
                    Branch.builder().branchId(BRANCH_ID).name("Inactive").active(false).build()));

            assertError(ErrorCode.BRANCH_INACTIVE, () -> service.assignUser(new AssignUserRequest(202L, BRANCH_ID)));
        }

        @Test
        @DisplayName("[Unhappy Path] User is not medical staff — returns 2003")
        void assignUser_notEmployee_returnsBadRequest() {
            when(userAccountRepository.findById(202L)).thenReturn(Optional.of(userAccount()));
            when(branchRepository.findById(BRANCH_ID)).thenReturn(Optional.of(
                    Branch.builder().branchId(BRANCH_ID).name("Active").active(true).build()));
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertError(ErrorCode.INVALID_PARAMETER_VALUE, () -> service.assignUser(new AssignUserRequest(202L, BRANCH_ID)));
        }
    }
}
