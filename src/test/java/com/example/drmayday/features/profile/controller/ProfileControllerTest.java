package com.example.drmayday.features.profile.controller;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.exception.GlobalExceptionHandler;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateEmployeeProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePatientProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePersonProfileRequest;
import com.example.drmayday.features.profile.service.ProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.time.OffsetDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@ContextConfiguration(classes = ProfileControllerTest.TestWebConfiguration.class)
class ProfileControllerTest {

    @Configuration(proxyBeanMethods = false)
    @EnableWebSecurity
    @Import({ProfileController.class, GlobalExceptionHandler.class})
    static class TestWebConfiguration {

        @Bean
        SecurityFilterChain profileTestSecurityFilterChain(HttpSecurity http) throws Exception {
            return http
                    .csrf(csrf -> csrf.disable())
                    .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
                    .build();
        }
    }

    private static final String BASE_URL = "/api/v1/profile";
    private static final Long ACCOUNT_ID = 42L;
    private static final Long PERSON_ID = 9L;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProfileService profileService;

    @Nested
    @DisplayName("GET /api/v1/profile/person")
    class GetPersonProfileTests {

        @Test
        @DisplayName("[Happy Path] Authenticated request — returns person profile JSON")
        void getPersonProfile_authenticated_returnsPersonProfile() throws Exception {
            when(profileService.getPersonProfile(ACCOUNT_ID)).thenReturn(personResponse());

            mockMvc.perform(get(BASE_URL + "/person").with(user(testUser())))
                    .andExpect(status().isOk())
                    .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID))
                    .andExpect(jsonPath("$.data.full_name").value("Test Person"))
                    .andExpect(jsonPath("$.data.date_of_birth").value("1990-02-28"))
                    .andExpect(jsonPath("$.data.gender").value("Nam"))
                    .andExpect(jsonPath("$.data.phone_number").value("0912345678"))
                    .andExpect(jsonPath("$.data.province_city").value("Hà Nội"))
                    .andExpect(jsonPath("$.data.ward_commune").value("Phường A"))
                    .andExpect(jsonPath("$.data.address_line").value("Số 1"));

            verify(profileService).getPersonProfile(ACCOUNT_ID);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing authenticated principal — returns unauthorized response")
        void getPersonProfile_missingAuthentication_returnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/person"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("3003"));

            verify(profileService, never()).getPersonProfile(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Person not found — returns not found response")
        void getPersonProfile_missingPerson_returnsNotFound() throws Exception {
            when(profileService.getPersonProfile(ACCOUNT_ID))
                    .thenThrow(new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/person").with(user(testUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("3007"))
                    .andExpect(jsonPath("$.data").doesNotExist());
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/profile/person")
    class UpdatePersonProfileTests {

        @Test
        @DisplayName("[Happy Path] Valid update — returns updated person response")
        void updatePersonProfile_validRequest_returnsUpdatedPerson() throws Exception {
            when(profileService.updatePersonProfile(eq(ACCOUNT_ID), any(UpdatePersonProfileRequest.class)))
                    .thenReturn(personResponse());

            mockMvc.perform(put(BASE_URL + "/person")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "full_name":"Updated Person",
                                      "date_of_birth":"1995-01-02",
                                      "gender":"Nu",
                                      "phone_number":"0999999999",
                                      "province_city":"Đà Nẵng",
                                      "ward_commune":"Phường B",
                                      "address_line":"Đường C"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID));

            ArgumentCaptor<UpdatePersonProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdatePersonProfileRequest.class);
            verify(profileService).updatePersonProfile(eq(ACCOUNT_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getFullName()).isEqualTo("Updated Person");
            assertThat(requestCaptor.getValue().getGender()).isEqualTo("Nu");
            assertThat(requestCaptor.getValue().getDateOfBirth()).isEqualTo(LocalDate.of(1995, 1, 2));
            assertThat(requestCaptor.getValue().getPhoneNumber()).isEqualTo("0999999999");
        }

        @Test
        @DisplayName("[Alternate Path] Partial update — accepts omitted profile fields")
        void updatePersonProfile_partialRequest_acceptsOmittedFields() throws Exception {
            when(profileService.updatePersonProfile(eq(ACCOUNT_ID), any(UpdatePersonProfileRequest.class)))
                    .thenReturn(personResponse());

            mockMvc.perform(put(BASE_URL + "/person")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"province_city\":\"New City\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));

            ArgumentCaptor<UpdatePersonProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdatePersonProfileRequest.class);
            verify(profileService).updatePersonProfile(eq(ACCOUNT_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getProvinceCity()).isEqualTo("New City");
            assertThat(requestCaptor.getValue().getFullName()).isNull();
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid phone format — returns validation error without service call")
        void updatePersonProfile_invalidPhone_returnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/person")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"phone_number\":\"123\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2004"));

            verify(profileService, never()).updatePersonProfile(
                    any(), any(UpdatePersonProfileRequest.class));
        }

        @Test
        @DisplayName("[Exception Path] Service fails — returns standardized server error")
        void updatePersonProfile_serviceThrows_returnsInternalServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(profileService).updatePersonProfile(eq(ACCOUNT_ID), any(UpdatePersonProfileRequest.class));

            mockMvc.perform(put(BASE_URL + "/person")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"full_name\":\"Updated Person\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profile/patient")
    class GetPatientProfileTests {

        @Test
        @DisplayName("[Happy Path] Authenticated request — returns patient profile JSON")
        void getPatientProfile_authenticated_returnsPatientProfile() throws Exception {
            when(profileService.getPatientProfile(ACCOUNT_ID)).thenReturn(patientResponse());

            mockMvc.perform(get(BASE_URL + "/patient").with(user(testUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID))
                    .andExpect(jsonPath("$.data.his_code").value("HIS-1"))
                    .andExpect(jsonPath("$.data.occupation").value("Nong dan"))
                    .andExpect(jsonPath("$.data.ethnicity").value("Kinh"))
                    .andExpect(jsonPath("$.data.health_insurance_number").value("BHYT-1"))
                    .andExpect(jsonPath("$.data.health_insurance_valid_until").value("2030-01-01"))
                    .andExpect(jsonPath("$.data.emergency_contact_name_address").value("Contact, Address"))
                    .andExpect(jsonPath("$.data.emergency_contact_phone").value("0987654321"));

            verify(profileService).getPatientProfile(ACCOUNT_ID);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing authentication — returns unauthorized response")
        void getPatientProfile_missingAuthentication_returnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/patient"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("3003"));

            verify(profileService, never()).getPatientProfile(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Patient profile absent — returns not found response")
        void getPatientProfile_missingPatient_returnsNotFound() throws Exception {
            when(profileService.getPatientProfile(ACCOUNT_ID))
                    .thenThrow(new AppException(ErrorCode.PATIENT_PROFILE_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/patient").with(user(testUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("3009"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/profile/patient")
    class UpdatePatientProfileTests {

        @Test
        @DisplayName("[Happy Path] Valid update — returns updated patient response")
        void updatePatientProfile_validRequest_returnsUpdatedPatient() throws Exception {
            when(profileService.updatePatientProfile(eq(ACCOUNT_ID), any(UpdatePatientProfileRequest.class)))
                    .thenReturn(patientResponse());

            mockMvc.perform(put(BASE_URL + "/patient")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {
                                      "person_id":9,
                                      "his_code":"HIS-NEW",
                                      "occupation":"Khac",
                                      "ethnicity":"Tày",
                                      "health_insurance_number":"BHYT-NEW",
                                      "health_insurance_valid_until":"2032-05-06",
                                      "emergency_contact_name_address":"New Contact, Address",
                                      "emergency_contact_phone":"0900000000"
                                    }
                                    """))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID));

            ArgumentCaptor<UpdatePatientProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdatePatientProfileRequest.class);
            verify(profileService).updatePatientProfile(eq(ACCOUNT_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getPersonId()).isEqualTo(PERSON_ID);
            assertThat(requestCaptor.getValue().getOccupation()).isEqualTo("Khac");
            assertThat(requestCaptor.getValue().getEmergencyContactPhone()).isEqualTo("0900000000");
        }

        @Test
        @DisplayName("[Alternate Path] Partial update — accepts omitted patient fields")
        void updatePatientProfile_partialRequest_acceptsOmittedFields() throws Exception {
            when(profileService.updatePatientProfile(eq(ACCOUNT_ID), any(UpdatePatientProfileRequest.class)))
                    .thenReturn(patientResponse());

            mockMvc.perform(put(BASE_URL + "/patient")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"ethnicity\":\"Tày\"}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"));

            ArgumentCaptor<UpdatePatientProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdatePatientProfileRequest.class);
            verify(profileService).updatePatientProfile(eq(ACCOUNT_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getEthnicity()).isEqualTo("Tày");
            assertThat(requestCaptor.getValue().getHisCode()).isNull();
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid occupation — returns validation error without service call")
        void updatePatientProfile_invalidOccupation_returnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/patient")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"occupation\":\"Unknown\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"));

            verify(profileService, never()).updatePatientProfile(
                    any(), any(UpdatePatientProfileRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing person_id — returns validation error")
        void updatePatientProfile_missingPersonId_returnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/patient")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"ethnicity\":\"Tày\"}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));
        }

        @Test
        @DisplayName("[Exception Path] Service fails — returns standardized server error")
        void updatePatientProfile_serviceThrows_returnsInternalServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(profileService).updatePatientProfile(eq(ACCOUNT_ID), any(UpdatePatientProfileRequest.class));

            mockMvc.perform(put(BASE_URL + "/patient")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"ethnicity\":\"Tày\"}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/profile/employee")
    class GetEmployeeProfileTests {

        @Test
        @DisplayName("[Happy Path] Authenticated request — returns employee profile JSON")
        void getEmployeeProfile_authenticated_returnsEmployeeProfile() throws Exception {
            when(profileService.getEmployeeProfile(ACCOUNT_ID)).thenReturn(employeeResponse());

            mockMvc.perform(get(BASE_URL + "/employee").with(user(testUser())))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID))
                    .andExpect(jsonPath("$.data.branch_id").value(3));

            verify(profileService).getEmployeeProfile(ACCOUNT_ID);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing authentication — returns unauthorized response")
        void getEmployeeProfile_missingAuthentication_returnsUnauthorized() throws Exception {
            mockMvc.perform(get(BASE_URL + "/employee"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("3003"));

            verify(profileService, never()).getEmployeeProfile(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Employee profile absent — returns not found response")
        void getEmployeeProfile_missingEmployee_returnsNotFound() throws Exception {
            when(profileService.getEmployeeProfile(ACCOUNT_ID))
                    .thenThrow(new AppException(ErrorCode.EMPLOYEE_PROFILE_NOT_FOUND));

            mockMvc.perform(get(BASE_URL + "/employee").with(user(testUser())))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("3010"));
        }
    }

    @Nested
    @DisplayName("PUT /api/v1/profile/employee")
    class UpdateEmployeeProfileTests {

        @Test
        @DisplayName("[Happy Path] Valid update — returns updated employee response")
        void updateEmployeeProfile_validRequest_returnsUpdatedEmployee() throws Exception {
            when(profileService.updateEmployeeProfile(eq(ACCOUNT_ID), any(UpdateEmployeeProfileRequest.class)))
                    .thenReturn(employeeResponse());

            mockMvc.perform(put(BASE_URL + "/employee")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"branch_id\":3}"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.code").value("1000"))
                    .andExpect(jsonPath("$.data.person_id").value(PERSON_ID))
                    .andExpect(jsonPath("$.data.branch_id").value(3));

            ArgumentCaptor<UpdateEmployeeProfileRequest> requestCaptor =
                    ArgumentCaptor.forClass(UpdateEmployeeProfileRequest.class);
            verify(profileService).updateEmployeeProfile(eq(ACCOUNT_ID), requestCaptor.capture());
            assertThat(requestCaptor.getValue().getPersonId()).isEqualTo(PERSON_ID);
            assertThat(requestCaptor.getValue().getBranchId()).isEqualTo(3L);
        }

        @Test
        @DisplayName("[Unhappy Path] Missing required identifiers — returns validation error without service call")
        void updateEmployeeProfile_missingIds_returnsBadRequest() throws Exception {
            mockMvc.perform(put(BASE_URL + "/employee")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2001"));

            verify(profileService, never()).updateEmployeeProfile(
                    any(), any(UpdateEmployeeProfileRequest.class));
        }

        @Test
        @DisplayName("[Unhappy Path] Service rejects unknown branch — returns bad request response")
        void updateEmployeeProfile_unknownBranch_returnsBadRequest() throws Exception {
            when(profileService.updateEmployeeProfile(eq(ACCOUNT_ID), any(UpdateEmployeeProfileRequest.class)))
                    .thenThrow(new AppException(ErrorCode.INVALID_PARAMETER_VALUE));

            mockMvc.perform(put(BASE_URL + "/employee")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"branch_id\":999}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("2003"));
        }

        @Test
        @DisplayName("[Exception Path] Service fails — returns standardized server error")
        void updateEmployeeProfile_serviceThrows_returnsInternalServerError() throws Exception {
            doThrow(new IllegalStateException("service unavailable"))
                    .when(profileService).updateEmployeeProfile(eq(ACCOUNT_ID), any(UpdateEmployeeProfileRequest.class));

            mockMvc.perform(put(BASE_URL + "/employee")
                            .with(user(testUser()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("{\"person_id\":9,\"branch_id\":3}"))
                    .andExpect(status().isInternalServerError())
                    .andExpect(jsonPath("$.code").value("9999"));
        }
    }

    private static UserPrincipal testUser() {
        return UserPrincipal.create(ACCOUNT_ID, "0912345678", "patient");
    }

    private static PersonProfileResponse personResponse() {
        OffsetDateTime timestamp = OffsetDateTime.parse("2025-01-01T10:00:00Z");
        return new PersonProfileResponse(
                PERSON_ID,
                "Test Person",
                LocalDate.of(1990, 2, 28),
                "Nam",
                "0912345678",
                "Hà Nội",
                "Phường A",
                "Số 1",
                timestamp,
                timestamp);
    }

    private static PatientProfileResponse patientResponse() {
        return new PatientProfileResponse(
                PERSON_ID,
                "HIS-1",
                "Nong dan",
                "Kinh",
                "BHYT-1",
                LocalDate.of(2030, 1, 1),
                "Contact, Address",
                "0987654321");
    }

    private static EmployeeProfileResponse employeeResponse() {
        return new EmployeeProfileResponse(PERSON_ID, 3L);
    }
}
