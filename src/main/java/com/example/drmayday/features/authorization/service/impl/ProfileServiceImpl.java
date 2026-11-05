package com.example.drmayday.features.authorization.service.impl;

import com.example.drmayday.features.authorization.dto.PatientProfileResponse;
import com.example.drmayday.features.authorization.dto.UpdateProfileRequest;
import com.example.drmayday.entity.Patient;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.authorization.service.ProfileService;
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

    private final UserAccountRepository taiKhoanRepository;
    private final PersonRepository nguoiRepository;
    private final PatientRepository benhNhanRepository;

    public ProfileServiceImpl(UserAccountRepository taiKhoanRepository,
                              PersonRepository nguoiRepository,
                              PatientRepository benhNhanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        log.info("Xử lý lấy thông tin hồ sơ bệnh nhân cho user_id={}", userId);
        UserAccount taiKhoan = null;
        Person nguoi = null;

        if (userId != null) {
            taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
            if (taiKhoan != null) {
                nguoi = taiKhoan.getPerson();
            } else {
                nguoi = nguoiRepository.findById(userId).orElse(null);
            }
        }

        if (taiKhoan == null && nguoi == null) {
            try {
                String phone = com.example.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    taiKhoan = taiKhoanRepository.findByUsername(phone).orElse(null);
                    if (taiKhoan != null) {
                        nguoi = taiKhoan.getPerson();
                    } else {
                        nguoi = nguoiRepository.findByPhoneNumber(phone).orElse(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (nguoi == null && taiKhoan == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        Long personId = nguoi != null ? nguoi.getPersonId() : (taiKhoan != null && taiKhoan.getPerson() != null ? taiKhoan.getPerson().getPersonId() : null);
        Patient benhNhan = personId != null ? benhNhanRepository.findById(personId).orElse(null) : null;

        return mapToProfileResponse(taiKhoan, nguoi, benhNhan);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Xử lý cập nhật hồ sơ bệnh nhân cho user_id={}", userId);
        UserAccount taiKhoan = null;
        Person nguoi = null;

        if (userId != null) {
            taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
            if (taiKhoan != null) {
                nguoi = taiKhoan.getPerson();
            } else {
                nguoi = nguoiRepository.findById(userId).orElse(null);
            }
        }

        if (nguoi == null) {
            try {
                String phone = com.example.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    taiKhoan = taiKhoanRepository.findByUsername(phone).orElse(null);
                    if (taiKhoan != null) {
                        nguoi = taiKhoan.getPerson();
                    } else {
                        nguoi = nguoiRepository.findByPhoneNumber(phone).orElse(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (nguoi == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        // Cập nhật thông tin trên bảng nguoi
        if (request.getFullName() != null && !request.getFullName().trim().isEmpty()) {
            nguoi.setFullName(request.getFullName().trim());
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            nguoi.setGender(normalizeGender(gender));
        }

        if (request.getAddress() != null) {
            nguoi.setStreetAddress(request.getAddress().trim());
        }

        if (request.getYearOfBirth() != null) {
            int currentYear = Year.now().getValue();
            if (request.getYearOfBirth() < 1900 || request.getYearOfBirth() > currentYear) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ");
            }
            if (nguoi.getBirthDate() != null) {
                nguoi.setBirthDate(nguoi.getBirthDate().withYear(request.getYearOfBirth()));
            } else {
                nguoi.setBirthDate(LocalDate.of(request.getYearOfBirth(), 1, 1));
            }
        }

        OffsetDateTime nowNguoi = OffsetDateTime.now();
        if (nguoi.getCreatedAt() != null && nowNguoi.isBefore(nguoi.getCreatedAt())) {
            nowNguoi = nguoi.getCreatedAt();
        }
        nguoi.setUpdatedAt(nowNguoi);
        nguoi = nguoiRepository.save(nguoi);

        // Cập nhật thông tin trên bảng benhnhan
        Patient benhNhan = benhNhanRepository.findById(nguoi.getPersonId()).orElse(null);
        if (benhNhan == null) {
            benhNhan = Patient.builder()
                    .patientId(nguoi.getPersonId())
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

        benhNhan = benhNhanRepository.save(benhNhan);

        return mapToProfileResponse(taiKhoan, nguoi, benhNhan);
    }

    private PatientProfileResponse mapToProfileResponse(UserAccount taiKhoan, Person nguoi, Patient benhNhan) {
        Long userId = taiKhoan != null ? taiKhoan.getAccountId() : (nguoi != null ? nguoi.getPersonId() : null);
        String fullName = nguoi != null ? nguoi.getFullName() : null;
        String phone = nguoi != null ? nguoi.getPhoneNumber() : (taiKhoan != null ? taiKhoan.getUsername() : null);
        String gender = nguoi != null ? nguoi.getGender() : null;
        Integer yearOfBirth = (nguoi != null && nguoi.getBirthDate() != null) ? nguoi.getBirthDate().getYear() : null;
        String address = nguoi != null ? nguoi.getStreetAddress() : null;
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
