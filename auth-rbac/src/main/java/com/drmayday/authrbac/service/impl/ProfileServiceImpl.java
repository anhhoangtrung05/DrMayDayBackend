package com.drmayday.authrbac.service.impl;

import com.drmayday.authrbac.dto.PatientProfileResponse;
import com.drmayday.authrbac.dto.UpdateProfileRequest;
import com.drmayday.authrbac.entity.BenhNhan;
import com.drmayday.authrbac.entity.Nguoi;
import com.drmayday.authrbac.entity.TaiKhoan;
import com.drmayday.authrbac.repository.BenhNhanRepository;
import com.drmayday.authrbac.repository.NguoiRepository;
import com.drmayday.authrbac.repository.TaiKhoanRepository;
import com.drmayday.authrbac.service.ProfileService;
import com.drmayday.common.exception.AppException;
import com.drmayday.common.exception.ErrorCode;
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

    private final TaiKhoanRepository taiKhoanRepository;
    private final NguoiRepository nguoiRepository;
    private final BenhNhanRepository benhNhanRepository;

    public ProfileServiceImpl(TaiKhoanRepository taiKhoanRepository,
                              NguoiRepository nguoiRepository,
                              BenhNhanRepository benhNhanRepository) {
        this.taiKhoanRepository = taiKhoanRepository;
        this.nguoiRepository = nguoiRepository;
        this.benhNhanRepository = benhNhanRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PatientProfileResponse getProfile(Long userId) {
        log.info("Xử lý lấy thông tin hồ sơ bệnh nhân cho user_id={}", userId);
        TaiKhoan taiKhoan = null;
        Nguoi nguoi = null;

        if (userId != null) {
            taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
            if (taiKhoan != null) {
                nguoi = taiKhoan.getNguoi();
            } else {
                nguoi = nguoiRepository.findById(userId).orElse(null);
            }
        }

        if (taiKhoan == null && nguoi == null) {
            try {
                String phone = com.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    taiKhoan = taiKhoanRepository.findByTenDangNhap(phone).orElse(null);
                    if (taiKhoan != null) {
                        nguoi = taiKhoan.getNguoi();
                    } else {
                        nguoi = nguoiRepository.findBySoDienThoai(phone).orElse(null);
                    }
                }
            } catch (Exception ignored) {
            }
        }

        if (nguoi == null && taiKhoan == null) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        Long personId = nguoi != null ? nguoi.getMaNguoi() : (taiKhoan != null && taiKhoan.getNguoi() != null ? taiKhoan.getNguoi().getMaNguoi() : null);
        BenhNhan benhNhan = personId != null ? benhNhanRepository.findById(personId).orElse(null) : null;

        return mapToProfileResponse(taiKhoan, nguoi, benhNhan);
    }

    @Override
    @Transactional
    public PatientProfileResponse updateProfile(Long userId, UpdateProfileRequest request) {
        log.info("Xử lý cập nhật hồ sơ bệnh nhân cho user_id={}", userId);
        TaiKhoan taiKhoan = null;
        Nguoi nguoi = null;

        if (userId != null) {
            taiKhoan = taiKhoanRepository.findById(userId).orElse(null);
            if (taiKhoan != null) {
                nguoi = taiKhoan.getNguoi();
            } else {
                nguoi = nguoiRepository.findById(userId).orElse(null);
            }
        }

        if (nguoi == null) {
            try {
                String phone = com.drmayday.common.security.SecurityUtils.getCurrentUserPhone();
                if (phone != null && !phone.isBlank()) {
                    taiKhoan = taiKhoanRepository.findByTenDangNhap(phone).orElse(null);
                    if (taiKhoan != null) {
                        nguoi = taiKhoan.getNguoi();
                    } else {
                        nguoi = nguoiRepository.findBySoDienThoai(phone).orElse(null);
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
            nguoi.setHoTen(request.getFullName().trim());
        }

        if (request.getGender() != null) {
            String gender = request.getGender().trim();
            nguoi.setGioiTinh(normalizeGender(gender));
        }

        if (request.getAddress() != null) {
            nguoi.setDiaChiSoNhaThonPho(request.getAddress().trim());
        }

        if (request.getYearOfBirth() != null) {
            int currentYear = Year.now().getValue();
            if (request.getYearOfBirth() < 1900 || request.getYearOfBirth() > currentYear) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Năm sinh không hợp lệ");
            }
            if (nguoi.getNgaySinh() != null) {
                nguoi.setNgaySinh(nguoi.getNgaySinh().withYear(request.getYearOfBirth()));
            } else {
                nguoi.setNgaySinh(LocalDate.of(request.getYearOfBirth(), 1, 1));
            }
        }

        OffsetDateTime nowNguoi = OffsetDateTime.now();
        if (nguoi.getThoiDiemTao() != null && nowNguoi.isBefore(nguoi.getThoiDiemTao())) {
            nowNguoi = nguoi.getThoiDiemTao();
        }
        nguoi.setThoiDiemCapNhat(nowNguoi);
        nguoi = nguoiRepository.save(nguoi);

        // Cập nhật thông tin trên bảng benhnhan
        BenhNhan benhNhan = benhNhanRepository.findById(nguoi.getMaNguoi()).orElse(null);
        if (benhNhan == null) {
            benhNhan = BenhNhan.builder()
                    .maBenhNhan(nguoi.getMaNguoi())
                    .nguoi(nguoi)
                    .build();
        }

        if (request.getEthnic() != null) {
            benhNhan.setDanToc(request.getEthnic().trim());
        }

        if (request.getOccupation() != null) {
            benhNhan.setNgheNghiep(normalizeOccupation(request.getOccupation()));
        }

        if (request.getBhytCode() != null) {
            benhNhan.setBhytSoThe(request.getBhytCode().trim());
        }

        benhNhan = benhNhanRepository.save(benhNhan);

        return mapToProfileResponse(taiKhoan, nguoi, benhNhan);
    }

    private PatientProfileResponse mapToProfileResponse(TaiKhoan taiKhoan, Nguoi nguoi, BenhNhan benhNhan) {
        Long userId = taiKhoan != null ? taiKhoan.getMaTaiKhoan() : (nguoi != null ? nguoi.getMaNguoi() : null);
        String fullName = nguoi != null ? nguoi.getHoTen() : null;
        String phone = nguoi != null ? nguoi.getSoDienThoai() : (taiKhoan != null ? taiKhoan.getTenDangNhap() : null);
        String gender = nguoi != null ? nguoi.getGioiTinh() : null;
        Integer yearOfBirth = (nguoi != null && nguoi.getNgaySinh() != null) ? nguoi.getNgaySinh().getYear() : null;
        String address = nguoi != null ? nguoi.getDiaChiSoNhaThonPho() : null;
        String ethnic = benhNhan != null ? benhNhan.getDanToc() : null;
        String occupation = benhNhan != null ? benhNhan.getNgheNghiep() : null;
        String bhytCode = benhNhan != null ? benhNhan.getBhytSoThe() : null;

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
