package com.example.drmayday.features.profile.service;

import com.example.drmayday.features.profile.dto.EmployeeProfileResponse;
import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.PersonProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateEmployeeProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePatientProfileRequest;
import com.example.drmayday.features.profile.dto.UpdatePersonProfileRequest;

public interface ProfileService {
    PersonProfileResponse getPersonProfile(Long accountId);

    PersonProfileResponse updatePersonProfile(Long accountId, UpdatePersonProfileRequest request);

    PatientProfileResponse getPatientProfile(Long accountId);

    PatientProfileResponse updatePatientProfile(Long accountId, UpdatePatientProfileRequest request);

    EmployeeProfileResponse getEmployeeProfile(Long accountId);

    EmployeeProfileResponse updateEmployeeProfile(Long accountId, UpdateEmployeeProfileRequest request);
}
