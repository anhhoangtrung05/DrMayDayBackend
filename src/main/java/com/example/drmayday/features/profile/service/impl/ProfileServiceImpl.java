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
import com.example.drmayday.features.profile.service.ProfileService;
import com.example.drmayday.repository.BranchRepository;
import com.example.drmayday.repository.EmployeeRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final UserAccountRepository userAccountRepository;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;
    private final EmployeeRepository employeeRepository;
    private final BranchRepository branchRepository;

    public ProfileServiceImpl(UserAccountRepository userAccountRepository,
                              PersonRepository personRepository,
                              PatientRepository patientRepository,
                              EmployeeRepository employeeRepository,
                              BranchRepository branchRepository) {
        this.userAccountRepository = userAccountRepository;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
        this.employeeRepository = employeeRepository;
        this.branchRepository = branchRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PersonProfileResponse getPersonProfile(Long accountId) {
        return PersonProfileResponse.from(findAuthenticatedPerson(accountId));
    }

    @Override
    @Transactional
    public PersonProfileResponse updatePersonProfile(Long accountId, UpdatePersonProfileRequest request) {
        if (request == null || !request.hasUpdates()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = findAuthenticatedPerson(accountId);
        if (request.getFullName() != null) {
            person.setFullName(request.getFullName().trim());
        }
        if (request.getDateOfBirth() != null) {
            person.setBirthDate(request.getDateOfBirth());
        }
        if (request.getGender() != null) {
            person.setGender(request.getGender());
        }
        if (request.getPhoneNumber() != null) {
            person.setPhoneNumber(request.getPhoneNumber().trim());
        }
        if (request.getProvinceCity() != null) {
            person.setProvinceCity(request.getProvinceCity().trim());
        }
        if (request.getWardCommune() != null) {
            person.setCommuneWard(request.getWardCommune().trim());
        }
        if (request.getAddressLine() != null) {
            person.setStreetAddress(request.getAddressLine().trim());
        }
        person.setUpdatedAt(OffsetDateTime.now());

        return PersonProfileResponse.from(personRepository.save(person));
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getPatientProfile(Long accountId) {
        Person person = findAuthenticatedPerson(accountId);
        return PatientProfileResponse.from(findPatient(person.getPersonId()));
    }

    @Override
    @Transactional
    public PatientProfileResponse updatePatientProfile(Long accountId, UpdatePatientProfileRequest request) {
        if (request == null || request.getPersonId() == null || !request.hasUpdates()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = findAuthenticatedPerson(accountId);
        if (!person.getPersonId().equals(request.getPersonId())) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }

        Patient patient = findPatient(person.getPersonId());
        if (request.getHisCode() != null) {
            patient.setHisCode(request.getHisCode().trim());
        }
        if (request.getOccupation() != null) {
            patient.setOccupation(request.getOccupation());
        }
        if (request.getEthnicity() != null) {
            patient.setEthnicity(request.getEthnicity().trim());
        }
        if (request.getHealthInsuranceNumber() != null) {
            patient.setHealthInsuranceNumber(request.getHealthInsuranceNumber().trim());
        }
        if (request.getHealthInsuranceValidUntil() != null) {
            patient.setHealthInsuranceValidUntil(request.getHealthInsuranceValidUntil());
        }
        if (request.getEmergencyContactNameAddress() != null) {
            patient.setEmergencyContactNameAndAddress(request.getEmergencyContactNameAddress().trim());
        }
        if (request.getEmergencyContactPhone() != null) {
            patient.setEmergencyContactPhone(request.getEmergencyContactPhone().trim());
        }

        return PatientProfileResponse.from(patientRepository.save(patient));
    }

    @Override
    @Transactional(readOnly = true)
    public EmployeeProfileResponse getEmployeeProfile(Long accountId) {
        Person person = findAuthenticatedPerson(accountId);
        return EmployeeProfileResponse.from(findEmployee(person.getPersonId()));
    }

    @Override
    @Transactional
    public EmployeeProfileResponse updateEmployeeProfile(Long accountId, UpdateEmployeeProfileRequest request) {
        if (request == null || request.getPersonId() == null || request.getBranchId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        Person person = findAuthenticatedPerson(accountId);
        if (!person.getPersonId().equals(request.getPersonId())) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }

        Employee employee = findEmployee(person.getPersonId());
        if (!branchRepository.existsById(request.getBranchId())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
        }

        employee.setBranchId(request.getBranchId());
        return EmployeeProfileResponse.from(employeeRepository.save(employee));
    }

    private Person findAuthenticatedPerson(Long accountId) {
        if (accountId == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
        UserAccount account = userAccountRepository.findById(accountId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));
        if (account.getPerson() == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
        return account.getPerson();
    }

    private Patient findPatient(Long personId) {
        return patientRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.PATIENT_PROFILE_NOT_FOUND));
    }

    private Employee findEmployee(Long personId) {
        return employeeRepository.findById(personId)
                .orElseThrow(() -> new AppException(ErrorCode.EMPLOYEE_PROFILE_NOT_FOUND));
    }
}
