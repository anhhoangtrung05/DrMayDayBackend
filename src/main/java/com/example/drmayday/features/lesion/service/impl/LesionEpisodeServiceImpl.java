package com.example.drmayday.features.lesion.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.SkinLesionEpisode;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.lesion.dto.CreateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.dto.LesionEpisodeResponse;
import com.example.drmayday.features.lesion.dto.UpdateLesionEpisodeRequest;
import com.example.drmayday.features.lesion.service.LesionEpisodeService;
import com.example.drmayday.repository.LesionLocationRepository;
import com.example.drmayday.repository.LesionPhotoRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.SkinLesionEpisodeRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
public class LesionEpisodeServiceImpl implements LesionEpisodeService {

    private static final String CONTEXT_OTHER = "Khac";
    private static final Set<String> VALID_DURATION_UNITS = Set.of("hour", "day");
    private static final int MAX_VARCHAR_255 = 255;
    private static final int MAX_VARCHAR_100 = 100;

    private final SkinLesionEpisodeRepository skinLesionEpisodeRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final LesionLocationRepository lesionLocationRepository;
    private final LesionPhotoRepository lesionPhotoRepository;
    private final UserAccountRepository userAccountRepository;

    public LesionEpisodeServiceImpl(SkinLesionEpisodeRepository skinLesionEpisodeRepository,
                                    MedicalRecordRepository medicalRecordRepository,
                                    PatientRepository patientRepository,
                                    LesionLocationRepository lesionLocationRepository,
                                    LesionPhotoRepository lesionPhotoRepository,
                                    UserAccountRepository userAccountRepository) {
        this.skinLesionEpisodeRepository = skinLesionEpisodeRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.patientRepository = patientRepository;
        this.lesionLocationRepository = lesionLocationRepository;
        this.lesionPhotoRepository = lesionPhotoRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public LesionEpisodeResponse createLesionEpisode(CreateLesionEpisodeRequest req) {
        if (req == null || req.getTenDotTonThuongDa() == null || req.getTenDotTonThuongDa().isBlank()) {
            throw new AppException(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY,
                    "Tên đợt tổn thương không được để trống");
        }
        String episodeName = validateMaxLength(req.getTenDotTonThuongDa().trim(), MAX_VARCHAR_255, "ten_dot_ton_thuong_da");

        Long callerPatientId = resolveCallerPatientId();

        Long hoSoManId = null;
        if (req.getHoSoManId() != null && !req.getHoSoManId().isBlank()) {
            Long parsedRecordId = parsePositiveId(req.getHoSoManId(), "ho_so_man_id");

            MedicalRecord record = medicalRecordRepository.findById(parsedRecordId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                            "Không tìm thấy hồ sơ bệnh án: " + parsedRecordId));

            // Check ownership before record type to avoid leaking other patients' record types
            if (!record.getPatientId().equals(callerPatientId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                        "Không thể liên kết với hồ sơ của người dùng khác");
            }
            if (!"MAN".equalsIgnoreCase(record.getMedicalRecordType())) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                        "Hồ sơ bệnh án phải là loại MẠN");
            }
            hoSoManId = parsedRecordId;
        }

        if (!patientRepository.existsById(callerPatientId)) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    "Không tìm thấy hồ sơ bệnh nhân: " + callerPatientId);
        }

        OnsetContextPair resolvedContext = resolveOnsetContext(
                req.getHoanCanhXuatHien(),
                req.getHoanCanhXuatHienKhac()
        );
        String durationUnit = normalizeAndValidateDurationUnit(req.getDonViThoiGian());
        String medicationName = normalizeOptionalString(req.getTenThuocDungGanThoiDiemXuatHien(), MAX_VARCHAR_255,
                "ten_thuoc_dung_gan_thoi_diem_xuat_hien");
        String medicationDuration = normalizeOptionalString(req.getThoiGianDungThuocTruocKhiXuatHien(), MAX_VARCHAR_100,
                "thoi_gian_dung_thuoc_truoc_khi_xuat_hien");

        OffsetDateTime now = OffsetDateTime.now();
        SkinLesionEpisode episode = SkinLesionEpisode.builder()
                .patientId(callerPatientId)
                .chronicMedicalRecordId(hoSoManId)
                .episodeName(episodeName)
                .onsetAt(req.getThoiDiemXuatHien())
                .onsetContext(resolvedContext.context())
                .otherOnsetContext(resolvedContext.otherContext())
                .medicationUsedNearOnset(medicationName)
                .medicationUseDurationBeforeOnset(medicationDuration)
                .durationUnit(durationUnit)
                .episodeDetails(req.getChiTietDotTonThuong())
                .createdAt(now)
                .updatedAt(now)
                .build();

        SkinLesionEpisode saved = skinLesionEpisodeRepository.saveAndFlush(episode);
        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<LesionEpisodeResponse> getMyLesionEpisodes(String hoSoManIdStr) {
        boolean isPrivileged = isDoctorOrAdmin();
        Long callerPatientId = resolveCallerPatientId();

        if (hoSoManIdStr != null && !hoSoManIdStr.isBlank()) {
            Long recordId = parsePositiveId(hoSoManIdStr, "ho_so_man_id");

            MedicalRecord record = medicalRecordRepository.findById(recordId)
                    .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                            "Không tìm thấy hồ sơ bệnh án: " + recordId));

            if (!isPrivileged && !record.getPatientId().equals(callerPatientId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                        "Không thể truy cập dữ liệu của người dùng khác");
            }

            List<SkinLesionEpisode> list = isPrivileged
                    ? skinLesionEpisodeRepository.findByChronicMedicalRecordIdOrderByOnsetAtDesc(recordId)
                    : skinLesionEpisodeRepository.findByPatientIdAndChronicMedicalRecordIdOrderByOnsetAtDesc(callerPatientId, recordId);
            return list.stream().map(this::mapToResponse).toList();
        }

        List<SkinLesionEpisode> list = isPrivileged
                ? skinLesionEpisodeRepository.findAllOrderByOnsetAtDesc()
                : skinLesionEpisodeRepository.findByPatientIdOrderByOnsetAtDesc(callerPatientId);
        return list.stream().map(this::mapToResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public LesionEpisodeResponse getLesionEpisode(String idStr) {
        Long episodeId = parsePositiveId(idStr, "episode_id");
        SkinLesionEpisode episode = skinLesionEpisodeRepository.findById(episodeId)
                .orElseThrow(() -> new AppException(ErrorCode.LESION_EPISODE_NOT_FOUND,
                        "Không tìm thấy đợt tổn thương da: " + episodeId));

        if (!isDoctorOrAdmin()) {
            Long callerPatientId = resolveCallerPatientId();
            if (!episode.getPatientId().equals(callerPatientId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                        "Không thể truy cập dữ liệu của người dùng khác");
            }
        }

        return mapToResponse(episode);
    }

    @Override
    @Transactional
    public LesionEpisodeResponse editLesionEpisode(String idStr, UpdateLesionEpisodeRequest req) {
        Long episodeId = parsePositiveId(idStr, "episode_id");
        SkinLesionEpisode episode = skinLesionEpisodeRepository.findById(episodeId)
                .orElseThrow(() -> new AppException(ErrorCode.LESION_EPISODE_NOT_FOUND,
                        "Không tìm thấy đợt tổn thương da: " + episodeId));

        Long callerPatientId = resolveCallerPatientId();
        if (!episode.getPatientId().equals(callerPatientId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    "Không thể chỉnh sửa đợt tổn thương của người dùng khác");
        }

        if (req == null) {
            return mapToResponse(episode);
        }

        if (req.getTenDotTonThuongDa() != null) {
            if (req.getTenDotTonThuongDa().isBlank()) {
                throw new AppException(ErrorCode.LESION_NAME_CANNOT_BE_EMPTY,
                        "Tên đợt tổn thương không được để trống");
            }
            episode.setEpisodeName(validateMaxLength(req.getTenDotTonThuongDa().trim(), MAX_VARCHAR_255, "ten_dot_ton_thuong_da"));
        }

        if (req.getThoiDiemXuatHien() != null) {
            episode.setOnsetAt(req.getThoiDiemXuatHien());
        }

        if (req.getHoanCanhXuatHien() != null || req.getHoanCanhXuatHienKhac() != null) {
            String targetContext = req.getHoanCanhXuatHien() != null
                    ? req.getHoanCanhXuatHien()
                    : episode.getOnsetContext();
            String targetOther = req.getHoanCanhXuatHienKhac() != null
                    ? req.getHoanCanhXuatHienKhac()
                    : (isOtherContext(targetContext) ? episode.getOtherOnsetContext() : null);

            OnsetContextPair resolved = resolveOnsetContext(targetContext, targetOther);
            episode.setOnsetContext(resolved.context());
            episode.setOtherOnsetContext(resolved.otherContext());
        }

        if (req.getTenThuocDungGanThoiDiemXuatHien() != null) {
            episode.setMedicationUsedNearOnset(normalizeOptionalString(
                    req.getTenThuocDungGanThoiDiemXuatHien(), MAX_VARCHAR_255,
                    "ten_thuoc_dung_gan_thoi_diem_xuat_hien"));
        }

        if (req.getThoiGianDungThuocTruocKhiXuatHien() != null) {
            episode.setMedicationUseDurationBeforeOnset(normalizeOptionalString(
                    req.getThoiGianDungThuocTruocKhiXuatHien(), MAX_VARCHAR_100,
                    "thoi_gian_dung_thuoc_truoc_khi_xuat_hien"));
        }

        if (req.getDonViThoiGian() != null) {
            episode.setDurationUnit(normalizeAndValidateDurationUnit(req.getDonViThoiGian()));
        }

        if (req.getChiTietDotTonThuong() != null) {
            episode.setEpisodeDetails(req.getChiTietDotTonThuong());
        }

        episode.setUpdatedAt(OffsetDateTime.now());
        SkinLesionEpisode saved = skinLesionEpisodeRepository.saveAndFlush(episode);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void deleteLesionEpisode(String idStr) {
        Long episodeId = parsePositiveId(idStr, "episode_id");
        SkinLesionEpisode episode = skinLesionEpisodeRepository.findById(episodeId)
                .orElseThrow(() -> new AppException(ErrorCode.LESION_EPISODE_NOT_FOUND,
                        "Không tìm thấy đợt tổn thương da: " + episodeId));

        Long callerPatientId = resolveCallerPatientId();
        if (!episode.getPatientId().equals(callerPatientId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    "Không thể xóa đợt tổn thương của người dùng khác");
        }

        boolean hasLocations = lesionLocationRepository.existsBySkinLesionEpisodeId(episodeId);
        boolean hasPhotos = lesionPhotoRepository.existsBySkinLesionEpisodeId(episodeId);
        if (hasLocations || hasPhotos) {
            throw new AppException(ErrorCode.EPISODE_HAS_CHILD_DATA,
                    "Đợt tổn thương vẫn còn vị trí hoặc hình ảnh đính kèm");
        }

        try {
            skinLesionEpisodeRepository.delete(episode);
            skinLesionEpisodeRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.EPISODE_HAS_CHILD_DATA,
                    "Đợt tổn thương vẫn còn vị trí hoặc hình ảnh đính kèm");
        }
    }

    private boolean isDoctorOrAdmin() {
        return SecurityUtils.hasRole("DOCTOR") || SecurityUtils.hasRole("BAC_SI")
                || SecurityUtils.hasRole("ADMIN") || SecurityUtils.hasRole("QUAN_TRI");
    }

    private Long resolveCallerPatientId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        UserAccount account = userAccountRepository.findById(currentUserId).orElse(null);
        return (account != null && account.getPerson() != null)
                ? account.getPerson().getPersonId()
                : currentUserId;
    }

    private Long parsePositiveId(String rawId, String paramName) {
        if (rawId == null || rawId.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    "Thiếu tham số bắt buộc: " + paramName);
        }
        try {
            long id = Long.parseLong(rawId.trim());
            if (id <= 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, paramName + " phải lớn hơn 0");
            }
            return id;
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, paramName + " không hợp lệ: " + rawId);
        }
    }

    private boolean isOtherContext(String context) {
        if (context == null) {
            return false;
        }
        String trimmed = context.trim();
        return "Khac".equalsIgnoreCase(trimmed) || "Khác".equalsIgnoreCase(trimmed);
    }

    private OnsetContextPair resolveOnsetContext(String rawContext, String rawOtherContext) {
        String context = rawContext != null && !rawContext.isBlank() ? rawContext.trim() : null;
        String otherContext = rawOtherContext != null && !rawOtherContext.isBlank() ? rawOtherContext.trim() : null;

        if (isOtherContext(context)) {
            if (otherContext == null) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                        "Vui lòng ghi rõ hoan_canh_xuat_hien_khac khi chọn Khác");
            }
            return new OnsetContextPair(CONTEXT_OTHER, otherContext);
        }

        if (otherContext != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "hoan_canh_xuat_hien_khac chỉ được nhập khi hoan_canh_xuat_hien là Khác");
        }
        return new OnsetContextPair(
                context != null ? validateMaxLength(context, MAX_VARCHAR_255, "hoan_canh_xuat_hien") : null,
                null
        );
    }

    private String normalizeAndValidateDurationUnit(String rawUnit) {
        if (rawUnit == null || rawUnit.isBlank()) {
            return null;
        }
        String normalized = rawUnit.trim().toLowerCase(Locale.ROOT);
        if (!VALID_DURATION_UNITS.contains(normalized)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "don_vi_thoi_gian không hợp lệ (chỉ chấp nhận hour | day)");
        }
        return normalized;
    }

    private String normalizeOptionalString(String value, int maxLength, String fieldName) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return validateMaxLength(value.trim(), maxLength, fieldName);
    }

    private String validateMaxLength(String value, int maxLength, String fieldName) {
        if (value.length() > maxLength) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    fieldName + " không được vượt quá " + maxLength + " ký tự");
        }
        return value;
    }

    private LesionEpisodeResponse mapToResponse(SkinLesionEpisode entity) {
        return LesionEpisodeResponse.builder()
                .episodeId(String.valueOf(entity.getSkinLesionEpisodeId()))
                .patientId(String.valueOf(entity.getPatientId()))
                .hoSoManId(entity.getChronicMedicalRecordId() != null ? String.valueOf(entity.getChronicMedicalRecordId()) : null)
                .tenDotTonThuongDa(entity.getEpisodeName())
                .thoiDiemXuatHien(entity.getOnsetAt())
                .hoanCanhXuatHien(entity.getOnsetContext())
                .hoanCanhXuatHienKhac(entity.getOtherOnsetContext())
                .tenThuocDungGanThoiDiemXuatHien(entity.getMedicationUsedNearOnset())
                .thoiGianDungThuocTruocKhiXuatHien(entity.getMedicationUseDurationBeforeOnset())
                .donViThoiGian(entity.getDurationUnit())
                .chiTietDotTonThuong(entity.getEpisodeDetails())
                .thoiDiemTao(entity.getCreatedAt())
                .build();
    }

    private record OnsetContextPair(String context, String otherContext) {}
}
