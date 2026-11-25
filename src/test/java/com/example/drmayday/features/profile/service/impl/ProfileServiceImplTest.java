package com.example.drmayday.features.profile.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.Employee;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateEmployeeProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePatientProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePersonProfileRequest;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final long ACCOUNT_ID = 42L;
    private static final long PERSON_ID = 9L;
    private static final long OTHER_PERSON_ID = 10L;
    private static final long BRANCH_ID = 3L;
    private static final long NEW_BRANCH_ID = 4L;

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private BranchRepository branchRepository;

    private ProfileServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ProfileServiceImpl(
                userAccountRepository,
                personRepository,
                patientRepository,
                employeeRepository,
                branchRepository);
    }

    private void givenAccount(Person person) {
        when(userAccountRepository.findById(ACCOUNT_ID))
                .thenReturn(Optional.of(UserAccount.builder().accountId(ACCOUNT_ID).person(person).build()));
    }

    private static Person person() {
        return Person.builder()
                .personId(PERSON_ID)
                .fullName("Existing Person")
                .birthDate(LocalDate.of(1990, 2, 28))
                .gender("Nam")
                .phoneNumber("0912345678")
                .provinceCity("Hà Nội")
                .communeWard("Phường A")
                .streetAddress("Số 1")
                .createdAt(OffsetDateTime.now().minusDays(1))
                .updatedAt(OffsetDateTime.now().minusDays(1))
                .build();
    }

    private static Patient patient(Person person) {
        return Patient.builder()
                .patientId(person.getPersonId())
                .person(person)
                .hisCode("HIS-1")
                .occupation("Nong dan")
                .ethnicity("Kinh")
                .healthInsuranceNumber("BHYT-1")
                .healthInsuranceValidUntil(LocalDate.of(2030, 1, 1))
                .emergencyContactNameAndAddress("Contact, Address")
                .emergencyContactPhone("0987654321")
                .build();
    }

    private static Employee employee(Person person) {
        return Employee.builder()
                .employeeId(person.getPersonId())
                .person(person)
                .branchId(BRANCH_ID)
                .build();
    }

    private static void assertAppException(ErrorCode expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("getPersonProfile")
    class GetPersonProfileTests {

        @Test
        @DisplayName("[Happy Path] Account is linked to a person — returns person fields")
        void getPersonProfile_linkedPerson_returnsProfile() {
            Person person = person();
            givenAccount(person);

            PersonProfileResponse response = service.getPersonProfile(ACCOUNT_ID);

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.fullName()).isEqualTo("Existing Person");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(1990, 2, 28));
            assertThat(response.gender()).isEqualTo("Nam");
            assertThat(response.phoneNumber()).isEqualTo("0912345678");
            assertThat(response.provinceCity()).isEqualTo("Hà Nội");
            assertThat(response.wardCommune()).isEqualTo("Phường A");
            assertThat(response.addressLine()).isEqualTo("Số 1");
            assertThat(response.createdAt()).isNotNull();
            assertThat(response.updatedAt()).isNotNull();
        }

        @Test
        @DisplayName("[Unhappy Path] Account does not exist — returns user account not found")
        void getPersonProfile_missingAccount_returnsNotFound() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getPersonProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Unhappy Path] Account has no linked person — returns user account not found")
        void getPersonProfile_personMissing_returnsNotFound() {
            givenAccount(null);

            assertAppException(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getPersonProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Exception Path] Account repository fails — propagates original exception")
        void getPersonProfile_repositoryThrows_propagatesException() {
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(userAccountRepository.findById(ACCOUNT_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getPersonProfile(ACCOUNT_ID)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("updatePersonProfile")
    class UpdatePersonProfileTests {

        @Test
        @DisplayName("[Happy Path] Valid fields supplied — updates only supplied person fields")
        void updatePersonProfile_validRequest_updatesPerson() {
            Person person = person();
            givenAccount(person);
            when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));
            UpdatePersonProfileRequest request = new UpdatePersonProfileRequest();
            request.setFullName("  Updated Name ");
            request.setDateOfBirth(LocalDate.of(2000, 4, 5));
            request.setGender("Nu");
            request.setPhoneNumber("0999999999");
            request.setProvinceCity("  Đà Nẵng ");
            request.setWardCommune("Phường B");
            request.setAddressLine("  Đường C ");

            PersonProfileResponse response = service.updatePersonProfile(ACCOUNT_ID, request);

            assertThat(response.fullName()).isEqualTo("Updated Name");
            assertThat(response.dateOfBirth()).isEqualTo(LocalDate.of(2000, 4, 5));
            assertThat(response.gender()).isEqualTo("Nu");
            assertThat(response.phoneNumber()).isEqualTo("0999999999");
            assertThat(response.provinceCity()).isEqualTo("Đà Nẵng");
            assertThat(response.wardCommune()).isEqualTo("Phường B");
            assertThat(response.addressLine()).isEqualTo("Đường C");
            assertThat(person.getUpdatedAt()).isAfter(person.getCreatedAt());
            verify(personRepository).save(person);
        }

        @Test
        @DisplayName("[Alternate Path] One field supplied — preserves all omitted fields")
        void updatePersonProfile_partialRequest_preservesOmittedFields() {
            Person person = person();
            givenAccount(person);
            when(personRepository.save(any(Person.class))).thenAnswer(call -> call.getArgument(0));
            UpdatePersonProfileRequest request = new UpdatePersonProfileRequest();
            request.setProvinceCity("New City");

            PersonProfileResponse response = service.updatePersonProfile(ACCOUNT_ID, request);

            assertThat(response.provinceCity()).isEqualTo("New City");
            assertThat(response.fullName()).isEqualTo("Existing Person");
            assertThat(response.phoneNumber()).isEqualTo("0912345678");
            assertThat(response.gender()).isEqualTo("Nam");
        }

        @Test
        @DisplayName("[Unhappy Path] No update fields supplied — returns missing required parameter")
        void updatePersonProfile_emptyRequest_returnsMissingParameter() {
            assertAppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.updatePersonProfile(ACCOUNT_ID, new UpdatePersonProfileRequest()));

            verify(userAccountRepository, never()).findById(any());
            verify(personRepository, never()).save(any(Person.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Account has no linked person — returns user account not found")
        void updatePersonProfile_missingPerson_returnsNotFound() {
            givenAccount(null);
            UpdatePersonProfileRequest request = new UpdatePersonProfileRequest();
            request.setFullName("New Name");

            assertAppException(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> service.updatePersonProfile(ACCOUNT_ID, request));

            verify(personRepository, never()).save(any(Person.class));
        }

        @Test
        @DisplayName("[Exception Path] Person repository fails to save — propagates original exception")
        void updatePersonProfile_repositoryThrows_propagatesException() {
            givenAccount(person());
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(personRepository.save(any(Person.class))).thenThrow(failure);
            UpdatePersonProfileRequest request = new UpdatePersonProfileRequest();
            request.setFullName("New Name");

            assertThatThrownBy(() -> service.updatePersonProfile(ACCOUNT_ID, request)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getPatientProfile")
    class GetPatientProfileTests {

        @Test
        @DisplayName("[Happy Path] Linked patient exists — returns patient fields")
        void getPatientProfile_patientExists_returnsProfile() {
            Person person = person();
            givenAccount(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient(person)));

            PatientProfileResponse response = service.getPatientProfile(ACCOUNT_ID);

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.hisCode()).isEqualTo("HIS-1");
            assertThat(response.occupation()).isEqualTo("Nong dan");
            assertThat(response.ethnicity()).isEqualTo("Kinh");
            assertThat(response.healthInsuranceNumber()).isEqualTo("BHYT-1");
            assertThat(response.healthInsuranceValidUntil()).isEqualTo(LocalDate.of(2030, 1, 1));
            assertThat(response.emergencyContactNameAddress()).isEqualTo("Contact, Address");
            assertThat(response.emergencyContactPhone()).isEqualTo("0987654321");
        }

        @Test
        @DisplayName("[Unhappy Path] Linked patient does not exist — returns patient profile not found")
        void getPatientProfile_missingPatient_returnsNotFound() {
            givenAccount(person());
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.PATIENT_PROFILE_NOT_FOUND, () -> service.getPatientProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Exception Path] Patient repository fails — propagates original exception")
        void getPatientProfile_repositoryThrows_propagatesException() {
            givenAccount(person());
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(patientRepository.findById(PERSON_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getPatientProfile(ACCOUNT_ID)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("updatePatientProfile")
    class UpdatePatientProfileTests {

        @Test
        @DisplayName("[Happy Path] Matching person ID and patient exists — updates supplied patient fields")
        void updatePatientProfile_matchingPerson_updatesPatient() {
            Person person = person();
            Patient patient = patient(person);
            givenAccount(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));
            when(patientRepository.save(patient)).thenReturn(patient);
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setHisCode(" HIS-NEW ");
            request.setOccupation("Khac");
            request.setEthnicity("  Tày ");
            request.setHealthInsuranceNumber(" BHYT-NEW ");
            request.setHealthInsuranceValidUntil(LocalDate.of(2032, 5, 6));
            request.setEmergencyContactNameAddress(" New Contact ");
            request.setEmergencyContactPhone("0900000000");

            PatientProfileResponse response = service.updatePatientProfile(ACCOUNT_ID, request);

            assertThat(response.hisCode()).isEqualTo("HIS-NEW");
            assertThat(response.occupation()).isEqualTo("Khac");
            assertThat(response.ethnicity()).isEqualTo("Tày");
            assertThat(response.healthInsuranceNumber()).isEqualTo("BHYT-NEW");
            assertThat(response.healthInsuranceValidUntil()).isEqualTo(LocalDate.of(2032, 5, 6));
            assertThat(response.emergencyContactNameAddress()).isEqualTo("New Contact");
            assertThat(response.emergencyContactPhone()).isEqualTo("0900000000");
            verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("[Alternate Path] Partial update — preserves omitted fields")
        void updatePatientProfile_partialRequest_preservesOmittedFields() {
            Person person = person();
            Patient patient = patient(person);
            givenAccount(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));
            when(patientRepository.save(patient)).thenReturn(patient);
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setEthnicity("Tày");

            PatientProfileResponse response = service.updatePatientProfile(ACCOUNT_ID, request);

            assertThat(response.ethnicity()).isEqualTo("Tày");
            assertThat(response.hisCode()).isEqualTo("HIS-1");
            assertThat(response.occupation()).isEqualTo("Nong dan");
            assertThat(response.healthInsuranceNumber()).isEqualTo("BHYT-1");
        }

        @Test
        @DisplayName("[Unhappy Path] Person ID missing — returns missing required parameter")
        void updatePatientProfile_missingPersonId_returnsMissingParameter() {
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setEthnicity("Kinh");

            assertAppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.updatePatientProfile(ACCOUNT_ID, request));

            verify(userAccountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] No patient fields supplied — returns missing required parameter")
        void updatePatientProfile_noUpdateFields_returnsMissingParameter() {
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(PERSON_ID);

            assertAppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.updatePatientProfile(ACCOUNT_ID, request));

            verify(userAccountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Request person ID differs from authenticated person — returns forbidden")
        void updatePatientProfile_personMismatch_returnsForbidden() {
            givenAccount(person());
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(OTHER_PERSON_ID);
            request.setEthnicity("Kinh");

            assertAppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.updatePatientProfile(ACCOUNT_ID, request));

            verify(patientRepository, never()).findById(any());
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Matching person has no patient record — returns patient profile not found")
        void updatePatientProfile_missingPatient_returnsNotFound() {
            givenAccount(person());
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setEthnicity("Kinh");

            assertAppException(ErrorCode.PATIENT_PROFILE_NOT_FOUND,
                    () -> service.updatePatientProfile(ACCOUNT_ID, request));

            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Exception Path] Patient save fails — propagates original exception")
        void updatePatientProfile_repositoryThrows_propagatesException() {
            Person person = person();
            Patient patient = patient(person);
            givenAccount(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(patientRepository.save(patient)).thenThrow(failure);
            UpdatePatientProfileRequest request = new UpdatePatientProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setEthnicity("Kinh");

            assertThatThrownBy(() -> service.updatePatientProfile(ACCOUNT_ID, request)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("getEmployeeProfile")
    class GetEmployeeProfileTests {

        @Test
        @DisplayName("[Happy Path] Linked employee exists — returns employee fields")
        void getEmployeeProfile_employeeExists_returnsProfile() {
            Person person = person();
            givenAccount(person);
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.of(employee(person)));

            EmployeeProfileResponse response = service.getEmployeeProfile(ACCOUNT_ID);

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.branchId()).isEqualTo(BRANCH_ID);
        }

        @Test
        @DisplayName("[Unhappy Path] Linked employee does not exist — returns employee profile not found")
        void getEmployeeProfile_missingEmployee_returnsNotFound() {
            givenAccount(person());
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            assertAppException(ErrorCode.EMPLOYEE_PROFILE_NOT_FOUND,
                    () -> service.getEmployeeProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Exception Path] Employee repository fails — propagates original exception")
        void getEmployeeProfile_repositoryThrows_propagatesException() {
            givenAccount(person());
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(employeeRepository.findById(PERSON_ID)).thenThrow(failure);

            assertThatThrownBy(() -> service.getEmployeeProfile(ACCOUNT_ID)).isSameAs(failure);
        }
    }

    @Nested
    @DisplayName("updateEmployeeProfile")
    class UpdateEmployeeProfileTests {

        @Test
        @DisplayName("[Happy Path] Matching person and existing branch — updates employee branch")
        void updateEmployeeProfile_validRequest_updatesBranch() {
            Person person = person();
            Employee employee = employee(person);
            givenAccount(person);
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.of(employee));
            when(branchRepository.existsById(NEW_BRANCH_ID)).thenReturn(true);
            when(employeeRepository.save(employee)).thenReturn(employee);
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setBranchId(NEW_BRANCH_ID);

            EmployeeProfileResponse response = service.updateEmployeeProfile(ACCOUNT_ID, request);

            assertThat(response.personId()).isEqualTo(PERSON_ID);
            assertThat(response.branchId()).isEqualTo(NEW_BRANCH_ID);
            verify(employeeRepository).save(employee);
        }

        @Test
        @DisplayName("[Unhappy Path] person_id or branch_id missing — returns missing required parameter")
        void updateEmployeeProfile_requiredFieldMissing_returnsMissingParameter() {
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(PERSON_ID);

            assertAppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.updateEmployeeProfile(ACCOUNT_ID, request));

            verify(userAccountRepository, never()).findById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Request person ID differs from authenticated person — returns forbidden")
        void updateEmployeeProfile_personMismatch_returnsForbidden() {
            givenAccount(person());
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(OTHER_PERSON_ID);
            request.setBranchId(NEW_BRANCH_ID);

            assertAppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.updateEmployeeProfile(ACCOUNT_ID, request));

            verify(employeeRepository, never()).findById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Matching person has no employee record — returns employee profile not found")
        void updateEmployeeProfile_missingEmployee_returnsNotFound() {
            givenAccount(person());
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.empty());
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setBranchId(NEW_BRANCH_ID);

            assertAppException(ErrorCode.EMPLOYEE_PROFILE_NOT_FOUND,
                    () -> service.updateEmployeeProfile(ACCOUNT_ID, request));

            verify(branchRepository, never()).existsById(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Branch does not exist — returns invalid parameter value")
        void updateEmployeeProfile_missingBranch_returnsBadRequest() {
            Person person = person();
            givenAccount(person);
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.of(employee(person)));
            when(branchRepository.existsById(NEW_BRANCH_ID)).thenReturn(false);
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setBranchId(NEW_BRANCH_ID);

            assertAppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.updateEmployeeProfile(ACCOUNT_ID, request));

            verify(employeeRepository, never()).save(any(Employee.class));
        }

        @Test
        @DisplayName("[Exception Path] Employee save fails — propagates original exception")
        void updateEmployeeProfile_repositoryThrows_propagatesException() {
            Person person = person();
            Employee employee = employee(person);
            givenAccount(person);
            when(employeeRepository.findById(PERSON_ID)).thenReturn(Optional.of(employee));
            when(branchRepository.existsById(NEW_BRANCH_ID)).thenReturn(true);
            IllegalStateException failure = new IllegalStateException("database unavailable");
            when(employeeRepository.save(employee)).thenThrow(failure);
            UpdateEmployeeProfileRequest request = new UpdateEmployeeProfileRequest();
            request.setPersonId(PERSON_ID);
            request.setBranchId(NEW_BRANCH_ID);

            assertThatThrownBy(() -> service.updateEmployeeProfile(ACCOUNT_ID, request)).isSameAs(failure);
        }
    }
}
