package com.example.drmayday.features.profile.service.impl;

import com.example.drmayday.features.profile.dto.PatientProfileResponse;
import com.example.drmayday.features.profile.dto.UpdateProfileRequest;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.profile.service.ProfileService;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.Year;
import java.util.List;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserAccountRepository userAccountRepository;
    private final PersonRepository personRepository;
    private final PatientRepository patientRepository;

    public ProfileServiceImpl(UserAccountRepository userAccountRepository,
                              PersonRepository personRepository,
                              PatientRepository patientRepository) {
        this.userAccountRepository = userAccountRepository;
        this.personRepository = personRepository;
        this.patientRepository = patientRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        log.info("Retrieving patient profile for user_id={}", userId);
        UserAccount userAccount = null;
        Person person = null;

        if (userId != null) {
            userAccount = userAccountRepository.findById(userId).orElse(null);
            if (userAccount != null) {
                person = userAccount.getPerson();
            } else {
                person = personRepository.findById(userId).orElse(null);
            }
        }

        if (userAccount == null && person == null) {
            try {
                String phone = com.example.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    userAccount = userAccountRepository.findByUsername(phone).orElse(null);
                    if (userAccount != null) {
                        person = userAccount.getPerson();
                    } else {
                        person = personRepository.findByPhoneNumber(phone).orElse(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (person == null && userAccount == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        Long personId = person != null ? person.getPersonId() : (userAccount != null && userAccount.getPerson() != null ? userAccount.getPerson().getPersonId() : null);
        Patient patient = personId != null ? patientRepository.findById(personId).orElse(null) : null;

        return mapToProfileResponse(userAccount, person, patient);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Updating patient profile for user_id={}", userId);
        UserAccount userAccount = null;
        Person person = null;

        if (userId != null) {
            userAccount = userAccountRepository.findById(userId).orElse(null);
            if (userAccount != null) {
                person = userAccount.getPerson();
            } else {
                person = personRepository.findById(userId).orElse(null);
            }
        }

        if (person == null) {
            try {
                String phone = com.example.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    userAccount = userAccountRepository.findByUsername(phone).orElse(null);
                    if (userAccount != null) {
                        person = userAccount.getPerson();
                    } else {
                        person = personRepository.findByPhoneNumber(phone).orElse(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (person == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        // Update personal information.
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            person.setFullName(request.getFullName().trim());
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            person.setGender(normalizeGender(gender));
        }

        if (request.getAddress() != null) {
            person.setStreetAddress(request.getAddress().trim());
        }

        if (request.getYearOfBirth() != null) {
            int currentYear = Year.now().getValue();
            if (request.getYearOfBirth() < 1900 || request.getYearOfBirth() > currentYear) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ");
            }
            if (person.getBirthDate() != null) {
                person.setBirthDate(person.getBirthDate().withYear(request.getYearOfBirth()));
            } else {
                person.setBirthDate(LocalDate.of(request.getYearOfBirth(), 1, 1));
            }
        }

        OffsetDateTime nowPerson = OffsetDateTime.now();
        if (person.getCreatedAt() != null && nowPerson.isBefore(person.getCreatedAt())) {
            nowPerson = person.getCreatedAt();
        }
        person.setUpdatedAt(nowPerson);
        person = personRepository.save(person);

        // Update patient-specific information.
        Patient benhNhan = patientRepository.findById(person.getPersonId()).orElse(null);
        if (benhNhan == null) {
            benhNhan = Patient.builder()
                    .person(person)
                    .build();
        }

        if (request.getEthnic() != null) {
            benhNhan.setEthnicity(request.getEthnic().trim());
        }

        if (request.getOccupation() != null) {
            benhNhan.setOccupation(normalizeOccupation(request.getOccupation()));
        }

        if (request.getBhytCode() != null) {
            benhNhan.setHealthInsuranceNumber(request.getBhytCode().trim());
        }

        benhNhan = patientRepository.save(benhNhan);

        return mapToProfileResponse(userAccount, person, benhNhan);
    }

    private PatientProfileResponse mapToProfileResponse(UserAccount userAccount, Person person, Patient benhNhan) {
        Long userId = userAccount != null ? userAccount.getAccountId() : (person != null ? person.getPersonId() : null);
        String fullName = person != null ? person.getFullName() : null;
        String phone = person != null ? person.getPhoneNumber() : (userAccount != null ? userAccount.getUsername() : null);
        String gender = person != null ? person.getGender() : null;
        Integer yearOfBirth = (person != null && person.getBirthDate() != null) ? person.getBirthDate().getYear() : null;
        String address = person != null ? person.getStreetAddress() : null;
        String ethnic = benhNhan != null ? benhNhan.getEthnicity() : null;
        String occupation = benhNhan != null ? benhNhan.getOccupation() : null;
        String bhytCode = benhNhan != null ? benhNhan.getHealthInsuranceNumber() : null;

        return PatientProfileResponse.builder()
                .userId(userId)
                .fullName(fullName)
                .phone(phone)
                .gender(gender)
                .yearOfBirth(yearOfBirth)
                .address(address)
                .ethnic(ethnic)
                .occupation(occupation)
                .bhytCode(bhytCode)
                .build();
    }

    private String normalizeGender(String gender) {
        if (gender == null || gender.trim().isEmpty()) {
            return null;
        }
        String trimmed = gender.trim();
        if ("male".equalsIgnoreCase(trimmed) || "Nam".equalsIgnoreCase(trimmed)) {
            return "Nam";
        }
        if ("female".equalsIgnoreCase(trimmed) || "Nu".equalsIgnoreCase(trimmed) || "Nữ".equalsIgnoreCase(trimmed)) {
            return "Nu";
        }
        return "Nam";
    }

    private String normalizeOccupation(String occupation) {
        if (occupation == null || occupation.trim().isEmpty()) {
            return null;
        }
        String trimmed = occupation.trim();
        for (String valid : List.of("Cong nhan", "Nong dan", "Hoc sinh - Sinh vien", "Can bo cong chuc", "Khac")) {
            if (valid.equalsIgnoreCase(trimmed)) {
                return valid;
            }
        }
        if ("Công nhân".equalsIgnoreCase(trimmed)) return "Cong nhan";
        if ("Nông dân".equalsIgnoreCase(trimmed)) return "Nong dan";
        if ("Học sinh - Sinh viên".equalsIgnoreCase(trimmed) || "Học sinh".equalsIgnoreCase(trimmed) || "Sinh viên".equalsIgnoreCase(trimmed)) return "Hoc sinh - Sinh vien";
        if ("Cán bộ công chức".equalsIgnoreCase(trimmed) || "Công chức".equalsIgnoreCase(trimmed)) return "Can bo cong chuc";
        return "Khac";
    }
}
