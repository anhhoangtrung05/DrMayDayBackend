package com.drmayday.authrbac.service.impl;

import com.drmayday.authrbac.dto.PatientProfileResponse;
import com.drmayday.authrbac.dto.UpdateProfileRequest;
import com.drmayday.authrbac.entity.Patient;
import com.drmayday.authrbac.entity.Role;
import com.drmayday.authrbac.entity.User;
import com.drmayday.authrbac.repository.PatientRepository;
import com.drmayday.authrbac.repository.RoleRepository;
import com.drmayday.authrbac.repository.UserRepository;
import com.drmayday.authrbac.service.ProfileService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.Year;

@Service
public class ProfileServiceImpl implements ProfileService {

    private static final Logger log = LoggerFactory.getLogger(ProfileServiceImpl.class);

    private final UserRepository userRepository;
    private final PatientRepository patientRepository;
    private final RoleRepository roleRepository;

    public ProfileServiceImpl(UserRepository userRepository,
                              PatientRepository patientRepository,
                              RoleRepository roleRepository) {
        this.userRepository = userRepository;
        this.patientRepository = patientRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Integer userId) {
        log.info("Xử lý lấy thông tin hồ sơ bệnh nhân cho user_id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        Patient patient = patientRepository.findByUserId(userId).orElse(null);

        return mapToProfileResponse(user, patient);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(Integer userId, UpdateProfileRequest request) {
        log.info("Xử lý cập nhật hồ sơ bệnh nhân cho user_id={}", userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));

        // Cập nhật thông tin trên bảng users
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            user.setFullName(request.getFullName().trim());
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            user.setGender(normalizeGender(gender));
        }

        userRepository.save(user);

        // Cập nhật thông tin trên bảng patients
        Patient patient = patientRepository.findByUserId(userId).orElse(null);
        if (patient == null) {
            Role patientRole = roleRepository.findByName("patient")
                    .orElseGet(() -> Role.builder().name("patient").build());
            Integer roleId = patientRole.getRoleId() != null ? patientRole.getRoleId() : 2;

            patient = Patient.builder()
                    .user(user)
                    .userId(user.getUserId())
                    .roleId(roleId)
                    .build();
        }

        if (request.getYearOfBirth() != null) {
            int currentYear = Year.now().getValue();
            if (request.getYearOfBirth() < 1900 || request.getYearOfBirth() > currentYear) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ");
            }
            if (patient.getDateOfBirth() != null) {
                patient.setDateOfBirth(patient.getDateOfBirth().withYear(request.getYearOfBirth()));
            } else {
                patient.setDateOfBirth(LocalDate.of(request.getYearOfBirth(), 1, 1));
            }
        }

        if (request.getAddress() != null) {
            patient.setAddress(request.getAddress().trim());
        }

        if (request.getEthnic() != null) {
            patient.setEthnic(request.getEthnic().trim());
        }

        if (request.getOccupation() != null) {
            patient.setOccupation(normalizeOccupation(request.getOccupation()));
        }

        if (request.getBhytCode() != null) {
            patient.setBhytCode(request.getBhytCode().trim());
        }

        patient = patientRepository.save(patient);

        return mapToProfileResponse(user, patient);
    }

    private PatientProfileResponse mapToProfileResponse(User user, Patient patient) {
        Integer yearOfBirth = null;
        String address = null;
        String ethnic = null;
        String occupation = null;
        String bhytCode = null;

        if (patient != null) {
            yearOfBirth = patient.getYearOfBirth();
            address = patient.getAddress();
            ethnic = patient.getEthnic();
            occupation = patient.getOccupation();
            bhytCode = patient.getBhytCode();
        }

        return PatientProfileResponse.builder()
                .userId(user.getUserId())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .gender(user.getGender())
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
        for (String valid : java.util.List.of("Cong nhan", "Nong dan", "Hoc sinh - Sinh vien", "Can bo cong chuc", "Khac")) {
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
