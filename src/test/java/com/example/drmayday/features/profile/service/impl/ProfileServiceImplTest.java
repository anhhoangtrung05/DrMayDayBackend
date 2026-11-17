package com.example.drmayday.features.profile.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceImplTest {

    private static final Long ACCOUNT_ID = 42L;
    private static final Long PERSON_ID = 9L;
    private static final String PHONE = "0912345678";

    @Mock
    private UserAccountRepository userAccountRepository;

    @Mock
    private PersonRepository personRepository;

    @Mock
    private PatientRepository patientRepository;

    private ProfileServiceImpl profileService;

    @BeforeEach
    void setUp() {
        profileService = new ProfileServiceImpl(
                userAccountRepository,
                personRepository,
                patientRepository
        );
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Nested
    @DisplayName("getProfile")
    class GetProfileTests {

        @Test
        @DisplayName("[Happy Path] Account and patient exist — returns complete profile")
        void getProfile_accountAndPatientExist_returnsCompleteProfile() {
            Person person = person();
            UserAccount account = account(person);
            Patient patient = patient(person);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));

            PatientProfileResponse response = profileService.getProfile(ACCOUNT_ID);

            assertEquals(ACCOUNT_ID, response.getUserId());
            assertEquals("Existing Name", response.getFullName());
            assertEquals(PHONE, response.getPhone());
            assertEquals("Nam", response.getGender());
            assertEquals(1990, response.getYearOfBirth());
            assertEquals("Old address", response.getAddress());
            assertEquals("Kinh", response.getEthnic());
            assertEquals("Nong dan", response.getOccupation());
            assertEquals("BHYT-OLD", response.getBhytCode());
            verify(patientRepository).findById(PERSON_ID);
            verify(personRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Account and person do not exist — throws user account not found")
        void getProfile_accountAndPersonDoNotExist_throwsUserAccountNotFound() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> profileService.getProfile(ACCOUNT_ID)
            );

            verify(patientRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Null user ID and no security context — throws user account not found")
        void getProfile_nullUserIdAndNoSecurityContext_throwsUserAccountNotFound() {
            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> profileService.getProfile((Long) null)
            );

            verify(userAccountRepository, never()).findById(any(Long.class));
            verify(personRepository, never()).findById(any(Long.class));
            verify(patientRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Alternate Path] Account missing but person exists — returns person profile")
        void getProfile_accountMissingButPersonExists_returnsPersonProfile() {
            Person person = personWithId(ACCOUNT_ID);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(person));
            when(patientRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            PatientProfileResponse response = profileService.getProfile(ACCOUNT_ID);

            assertEquals(ACCOUNT_ID, response.getUserId());
            assertEquals("Existing Name", response.getFullName());
            assertEquals(PHONE, response.getPhone());
            assertNull(response.getEthnic());
            assertNull(response.getOccupation());
            assertNull(response.getBhytCode());
        }

        @Test
        @DisplayName("[Alternate Path] Account has no patient — returns profile with empty patient fields")
        void getProfile_accountHasNoPatient_returnsProfileWithEmptyPatientFields() {
            Person person = person();
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account(person)));
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            PatientProfileResponse response = profileService.getProfile(ACCOUNT_ID);

            assertEquals(ACCOUNT_ID, response.getUserId());
            assertEquals("Existing Name", response.getFullName());
            assertNull(response.getEthnic());
            assertNull(response.getOccupation());
            assertNull(response.getBhytCode());
        }

        @Test
        @DisplayName("[Alternate Path] Null user ID with account matching security phone — returns account profile")
        void getProfile_nullUserIdWithAccountMatchingSecurityPhone_returnsAccountProfile() {
            Person person = person();
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE)).thenReturn(Optional.of(account(person)));
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient(person)));

            PatientProfileResponse response = profileService.getProfile((Long) null);

            assertEquals(ACCOUNT_ID, response.getUserId());
            assertEquals(PHONE, response.getPhone());
            verify(personRepository, never()).findByPhoneNumber(PHONE);
        }

        @Test
        @DisplayName("[Alternate Path] Security phone has no account but has person — returns person profile")
        void getProfile_securityPhoneHasNoAccountButHasPerson_returnsPersonProfile() {
            Person person = personWithId(PERSON_ID);
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE)).thenReturn(Optional.empty());
            when(personRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(person));
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            PatientProfileResponse response = profileService.getProfile((Long) null);

            assertEquals(PERSON_ID, response.getUserId());
            assertEquals(PHONE, response.getPhone());
        }

        @Test
        @DisplayName("[Alternate Path] Missing ID falls back to security phone — finds profile")
        void getProfile_missingIdFallsBackToSecurityPhone_findsProfile() {
            Person person = personWithId(PERSON_ID);
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(userAccountRepository.findByUsername(PHONE)).thenReturn(Optional.empty());
            when(personRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(person));
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());

            PatientProfileResponse response = profileService.getProfile(ACCOUNT_ID);

            assertEquals(PERSON_ID, response.getUserId());
            assertEquals(PHONE, response.getPhone());
        }

        @Test
        @DisplayName("[Exception Path] Account lookup throws — propagates exception")
        void getProfile_accountLookupThrows_propagatesException() {
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(IllegalStateException.class, () -> profileService.getProfile(ACCOUNT_ID));
            verify(personRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Exception Path] Direct person lookup throws — propagates exception")
        void getProfile_directPersonLookupThrows_propagatesException() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(IllegalStateException.class, () -> profileService.getProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Exception Path] Patient lookup throws — propagates exception")
        void getProfile_patientLookupThrows_propagatesException() {
            Person person = person();
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account(person)));
            when(patientRepository.findById(PERSON_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(IllegalStateException.class, () -> profileService.getProfile(ACCOUNT_ID));
        }

        @Test
        @DisplayName("[Exception Path] Security fallback lookup throws — returns user account not found")
        void getProfile_securityFallbackLookupThrows_returnsUserAccountNotFound() {
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> profileService.getProfile((Long) null)
            );
        }
    }

    @Nested
    @DisplayName("updateProfile")
    class UpdateProfileTests {

        @Test
        @DisplayName("[Happy Path] Existing account and patient — updates and returns normalized profile")
        void updateProfile_existingAccountAndPatient_updatesAndReturnsNormalizedProfile() {
            Person person = person();
            UserAccount account = account(person);
            Patient patient = patient(person);
            stubExistingProfile(account, person, patient);
            UpdateProfileRequest request = request(
                    "  New Name  ",
                    " Nữ ",
                    2001,
                    "  New Address  ",
                    "  Tày  ",
                    " Nông dân ",
                    " BHYT-NEW "
            );

            PatientProfileResponse response = profileService.updateProfile(ACCOUNT_ID, request);

            assertEquals("New Name", person.getFullName());
            assertEquals("Nu", person.getGender());
            assertEquals(LocalDate.of(2001, 2, 28), person.getBirthDate());
            assertEquals("New Address", person.getStreetAddress());
            assertEquals("Tày", patient.getEthnicity());
            assertEquals("Nong dan", patient.getOccupation());
            assertEquals("BHYT-NEW", patient.getHealthInsuranceNumber());
            assertEquals(ACCOUNT_ID, response.getUserId());
            assertEquals("New Name", response.getFullName());
            assertEquals("Nu", response.getGender());
            assertEquals(2001, response.getYearOfBirth());
            assertEquals("Tày", response.getEthnic());
            assertEquals("Nong dan", response.getOccupation());
            assertEquals("BHYT-NEW", response.getBhytCode());
            assertFalse(person.getUpdatedAt().isBefore(person.getCreatedAt()));
            InOrder order = inOrder(personRepository, patientRepository);
            order.verify(personRepository).save(person);
            order.verify(patientRepository).findById(PERSON_ID);
            order.verify(patientRepository).save(patient);
        }

        @Test
        @DisplayName("[Unhappy Path] No person found — throws user account not found without saving")
        void updateProfile_noPersonFound_throwsUserAccountNotFoundWithoutSaving() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> profileService.updateProfile(ACCOUNT_ID, request(null, null, null, null, null, null, null))
            );

            verify(personRepository, never()).save(any(Person.class));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Year below 1900 — throws invalid parameter without saving")
        void updateProfile_yearBelow1900_throwsInvalidParameterWithoutSaving() {
            stubAccountById(account(person()));

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, 1899, null, null, null, null))
            );

            verify(personRepository, never()).save(any(Person.class));
            verify(patientRepository, never()).findById(any(Long.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Future year — throws invalid parameter without saving")
        void updateProfile_futureYear_throwsInvalidParameterWithoutSaving() {
            stubAccountById(account(person()));

            assertAppException(
                    ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, Year.now().getValue() + 1, null, null, null, null))
            );

            verify(personRepository, never()).save(any(Person.class));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Alternate Path] Blank and null fields — preserves or clears values as defined")
        void updateProfile_blankAndNullFields_preservesOrClearsValuesAsDefined() {
            Person person = person();
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            PatientProfileResponse response = profileService.updateProfile(
                    ACCOUNT_ID, request("   ", "   ", null, "   ", null, null, null));

            assertEquals("Existing Name", person.getFullName());
            assertNull(person.getGender());
            assertEquals("", person.getStreetAddress());
            assertEquals("Kinh", patient.getEthnicity());
            assertEquals("Nong dan", patient.getOccupation());
            assertEquals("BHYT-OLD", patient.getHealthInsuranceNumber());
            assertNull(response.getGender());
            assertEquals("", response.getAddress());
        }

        @Test
        @DisplayName("[Alternate Path] Unknown gender and occupation — defaults to Nam and Khac")
        void updateProfile_unknownGenderAndOccupation_defaultsToNamAndKhak() {
            Person person = person();
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            profileService.updateProfile(
                    ACCOUNT_ID, request(null, "unspecified", null, null, null, "artist", null));

            assertEquals("Nam", person.getGender());
            assertEquals("Khac", patient.getOccupation());
        }

        @Test
        @DisplayName("[Alternate Path] Blank gender and occupation — normalizes to null")
        void updateProfile_blankGenderAndOccupation_normalizesToNull() {
            Person person = person();
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            profileService.updateProfile(
                    ACCOUNT_ID, request(null, "  ", null, null, null, "  ", null));

            assertNull(person.getGender());
            assertNull(patient.getOccupation());
        }

        @Test
        @DisplayName("[Alternate Path] Patient absent — creates patient linked to person")
        void updateProfile_patientAbsent_createsPatientLinkedToPerson() {
            Person person = person();
            stubAccountById(account(person));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());
            when(patientRepository.save(any(Patient.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0, Patient.class));

            PatientProfileResponse response = profileService.updateProfile(
                    ACCOUNT_ID, request(null, null, null, null, "Kinh", "Học sinh", "BHYT-NEW"));

            org.mockito.ArgumentCaptor<Patient> savedPatient =
                    org.mockito.ArgumentCaptor.forClass(Patient.class);
            verify(patientRepository).save(savedPatient.capture());
            assertEquals(person, savedPatient.getValue().getPerson());
            assertEquals("Kinh", response.getEthnic());
            assertEquals("Hoc sinh - Sinh vien", response.getOccupation());
            assertEquals("BHYT-NEW", response.getBhytCode());
        }

        @Test
        @DisplayName("[Alternate Path] Account absent but person exists — updates person profile")
        void updateProfile_accountAbsentButPersonExists_updatesPersonProfile() {
            Person person = personWithId(ACCOUNT_ID);
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(person));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(patientRepository.save(any(Patient.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0, Patient.class));

            PatientProfileResponse response = profileService.updateProfile(
                    ACCOUNT_ID, request(" Updated ", null, null, null, null, null, null));

            assertEquals("Updated", person.getFullName());
            assertEquals(ACCOUNT_ID, response.getUserId());
        }

        @Test
        @DisplayName("[Alternate Path] User ID missing but security phone matches account — updates account profile")
        void updateProfile_nullUserIdWithAccountMatchingSecurityPhone_updatesAccountProfile() {
            Person person = person();
            UserAccount account = account(person);
            Patient patient = patient(person);
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE)).thenReturn(Optional.of(account));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));
            when(patientRepository.save(patient)).thenReturn(patient);

            PatientProfileResponse response = profileService.updateProfile(
                    (Long) null, request("Updated", null, null, null, null, null, null));

            assertEquals("Updated", response.getFullName());
            assertEquals(ACCOUNT_ID, response.getUserId());
            verify(personRepository, never()).findByPhoneNumber(PHONE);
        }

        @Test
        @DisplayName("[Alternate Path] Year 1900 — accepts lower boundary")
        void updateProfile_year1900_acceptsLowerBoundary() {
            Person person = person();
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            PatientProfileResponse response = profileService.updateProfile(
                    ACCOUNT_ID, request(null, null, 1900, null, null, null, null));

            assertEquals(1900, response.getYearOfBirth());
            assertEquals(LocalDate.of(1900, 2, 28), person.getBirthDate());
        }

        @Test
        @DisplayName("[Alternate Path] Current year — accepts upper boundary")
        void updateProfile_currentYear_acceptsUpperBoundary() {
            Person person = person();
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);
            int currentYear = Year.now().getValue();

            PatientProfileResponse response = profileService.updateProfile(
                    ACCOUNT_ID, request(null, null, currentYear, null, null, null, null));

            assertEquals(currentYear, response.getYearOfBirth());
        }

        @Test
        @DisplayName("[Alternate Path] Null birth date — initializes date to January first")
        void updateProfile_nullBirthDate_initializesDateToJanuaryFirst() {
            Person person = person();
            person.setBirthDate(null);
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            profileService.updateProfile(
                    ACCOUNT_ID, request(null, null, 2000, null, null, null, null));

            assertEquals(LocalDate.of(2000, 1, 1), person.getBirthDate());
        }

        @Test
        @DisplayName("[Alternate Path] Future creation timestamp — does not move updated time earlier")
        void updateProfile_createdAtAfterNow_clampsUpdatedAtToCreatedAt() {
            Person person = person();
            OffsetDateTime futureCreatedAt = OffsetDateTime.now().plusDays(1);
            person.setCreatedAt(futureCreatedAt);
            Patient patient = patient(person);
            stubExistingProfile(account(person), person, patient);

            profileService.updateProfile(
                    ACCOUNT_ID, request(null, null, null, null, null, null, null));

            assertEquals(futureCreatedAt, person.getUpdatedAt());
        }

        @Test
        @DisplayName("[Alternate Path] Unknown security phone account — falls back to person by phone")
        void updateProfile_securityPhoneHasNoAccountButHasPerson_updatesPerson() {
            Person person = personWithId(PERSON_ID);
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE)).thenReturn(Optional.empty());
            when(personRepository.findByPhoneNumber(PHONE)).thenReturn(Optional.of(person));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.empty());
            when(patientRepository.save(any(Patient.class)))
                    .thenAnswer(invocation -> invocation.getArgument(0, Patient.class));

            PatientProfileResponse response = profileService.updateProfile(
                    (Long) null, request("Updated", null, null, null, null, null, null));

            assertEquals("Updated", response.getFullName());
            assertEquals(PERSON_ID, response.getUserId());
        }

        @Test
        @DisplayName("[Exception Path] Account lookup throws — propagates exception")
        void updateProfile_accountLookupThrows_propagatesException() {
            when(userAccountRepository.findById(ACCOUNT_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, null, null, null, null, null))
            );

            verify(personRepository, never()).save(any(Person.class));
        }

        @Test
        @DisplayName("[Exception Path] Person lookup throws — propagates exception")
        void updateProfile_personLookupThrows_propagatesException() {
            when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.empty());
            when(personRepository.findById(ACCOUNT_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, null, null, null, null, null))
            );
        }

        @Test
        @DisplayName("[Exception Path] Person save throws — skips patient operations")
        void updateProfile_personSaveThrows_skipsPatientOperations() {
            Person person = person();
            stubAccountById(account(person));
            when(personRepository.save(person))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request("Updated", null, null, null, null, null, null))
            );

            verify(patientRepository, never()).findById(any(Long.class));
            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Exception Path] Patient lookup throws — propagates exception")
        void updateProfile_patientLookupThrows_propagatesException() {
            Person person = person();
            stubAccountById(account(person));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(PERSON_ID))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, null, null, null, null, null))
            );

            verify(patientRepository, never()).save(any(Patient.class));
        }

        @Test
        @DisplayName("[Exception Path] Patient save throws — propagates exception")
        void updateProfile_patientSaveThrows_propagatesException() {
            Person person = person();
            Patient patient = patient(person);
            stubAccountById(account(person));
            when(personRepository.save(person)).thenReturn(person);
            when(patientRepository.findById(PERSON_ID)).thenReturn(Optional.of(patient));
            when(patientRepository.save(patient))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertThrows(
                    IllegalStateException.class,
                    () -> profileService.updateProfile(
                            ACCOUNT_ID, request(null, null, null, null, null, null, null))
            );
        }

        @Test
        @DisplayName("[Exception Path] Security fallback lookup throws — returns user account not found")
        void updateProfile_securityFallbackLookupThrows_returnsUserAccountNotFound() {
            authenticateWithPhone(PHONE);
            when(userAccountRepository.findByUsername(PHONE))
                    .thenThrow(new IllegalStateException("database unavailable"));

            assertAppException(
                    ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> {
                    profileService.updateProfile(
                        (Long) null, request(null, null, null, null, null, null, null));
                    }
            );

            verify(personRepository, never()).save(any(Person.class));
        }
    }

    private void stubAccountById(UserAccount account) {
        when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(account));
    }

    private void stubExistingProfile(UserAccount account, Person person, Patient patient) {
        stubAccountById(account);
        when(personRepository.save(person)).thenReturn(person);
        when(patientRepository.findById(person.getPersonId())).thenReturn(Optional.of(patient));
        when(patientRepository.save(patient)).thenReturn(patient);
    }

    private void authenticateWithPhone(String phone) {
        UserPrincipal principal = UserPrincipal.create(ACCOUNT_ID, phone, "benh_nhan");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private Person person() {
        return personWithId(PERSON_ID);
    }

    private Person personWithId(Long personId) {
        OffsetDateTime createdAt = OffsetDateTime.now().minusDays(1);
        return Person.builder()
                .personId(personId)
                .fullName("Existing Name")
                .phoneNumber(PHONE)
                .gender("Nam")
                .birthDate(LocalDate.of(1990, 2, 28))
                .streetAddress("Old address")
                .createdAt(createdAt)
                .updatedAt(createdAt)
                .build();
    }

    private UserAccount account(Person person) {
        return UserAccount.builder()
                .accountId(ACCOUNT_ID)
                .username(PHONE)
                .person(person)
                .build();
    }

    private Patient patient(Person person) {
        return Patient.builder()
                .patientId(person.getPersonId())
                .person(person)
                .ethnicity("Kinh")
                .occupation("Nong dan")
                .healthInsuranceNumber("BHYT-OLD")
                .build();
    }

    private UpdateProfileRequest request(
            String fullName,
            String gender,
            Integer yearOfBirth,
            String address,
            String ethnic,
            String occupation,
            String bhytCode
    ) {
        return new UpdateProfileRequest(
                fullName, gender, yearOfBirth, address, ethnic, occupation, bhytCode);
    }

    private AppException assertAppException(ErrorCode expectedCode, Executable action) {
        AppException exception = assertThrows(AppException.class, action);
        assertEquals(expectedCode, exception.getErrorCode());
        assertNotNull(exception.getMessage());
        return exception;
    }
}
