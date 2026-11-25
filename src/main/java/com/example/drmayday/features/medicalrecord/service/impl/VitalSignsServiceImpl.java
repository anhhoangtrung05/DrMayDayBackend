package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.entity.VitalSign;
import com.example.drmayday.features.medicalrecord.dto.AddVitalSignsRequest;
import com.example.drmayday.features.medicalrecord.dto.VitalSignsResponse;
import com.example.drmayday.features.medicalrecord.service.VitalSignsService;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.UserAccountRepository;
import com.example.drmayday.repository.VitalSignRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class VitalSignsServiceImpl implements VitalSignsService {

    private final MedicalVisitRepository medicalVisitRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final VitalSignRepository vitalSignRepository;
    private final UserAccountRepository userAccountRepository;

    public VitalSignsServiceImpl(MedicalVisitRepository medicalVisitRepository,
                                 MedicalRecordRepository medicalRecordRepository,
                                 VitalSignRepository vitalSignRepository,
                                 UserAccountRepository userAccountRepository) {
        this.medicalVisitRepository = medicalVisitRepository;
        this.medicalRecordRepository = medicalRecordRepository;
        this.vitalSignRepository = vitalSignRepository;
        this.userAccountRepository = userAccountRepository;
    }

    @Override
    @Transactional
    public VitalSignsResponse addVitalSigns(AddVitalSignsRequest req) {
        if (req == null || req.getExamId() == null || req.getExamId().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: exam_id");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(req.getExamId().trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "exam_id phải là giá trị số");
        }

        if (!medicalVisitRepository.existsById(examIdLong)) {
            throw new AppException(ErrorCode.EXAM_NOT_FOUND, "Phiên khám không tìm thấy: " + examIdLong);
        }

        if (req.getMachLanPhut() != null && req.getMachLanPhut() <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Mạch/phút phải là một giá trị dương");
        }
        if (req.getHuyetApTamThu() != null && req.getHuyetApTamThu() <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Huyết áp tâm thu phải là một giá trị dương");
        }
        if (req.getHuyetApTamTruong() != null && req.getHuyetApTamTruong() <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Huyết áp tâm trương phải là một giá trị dương");
        }
        if (req.getHuyetApTamThu() != null && req.getHuyetApTamTruong() != null
                && req.getHuyetApTamTruong() >= req.getHuyetApTamThu()) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Huyết áp tâm trương phải nhỏ hơn Huyết áp tâm thu");
        }
        if (req.getNhipThoLanPhut() != null && req.getNhipThoLanPhut() <= 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Nhịp thở/phút phải là một giá trị dương");
        }
        if (req.getSpo2PhanTram() != null) {
            if (req.getSpo2PhanTram().compareTo(BigDecimal.ZERO) < 0 || req.getSpo2PhanTram().compareTo(BigDecimal.valueOf(100)) > 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "spo2_phan_tram phải nằm trong khoảng từ 0 đến 100");
            }
        }
        if (Boolean.TRUE.equals(req.getCoBatThuongCoQuan())
                && (req.getBatThuongCoQuanGhiRo() == null || req.getBatThuongCoQuanGhiRo().isBlank())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "bat_thuong_co_quan_ghi_ro là bắt buộc khi co_bat_thuong_co_quan=true");
        }

        VitalSign entity = VitalSign.builder()
                .medicalVisitId(examIdLong)
                .timestamp(req.getThoiDiemDo() != null ? req.getThoiDiemDo() : OffsetDateTime.now())
                .pulse(req.getMachLanPhut() != null ? BigDecimal.valueOf(req.getMachLanPhut()) : null)
                .systolicBloodPressure(req.getHuyetApTamThu() != null ? BigDecimal.valueOf(req.getHuyetApTamThu()) : null)
                .diastolicBloodPressure(req.getHuyetApTamTruong() != null ? BigDecimal.valueOf(req.getHuyetApTamTruong()) : null)
                .respiratoryRate(req.getNhipThoLanPhut() != null ? BigDecimal.valueOf(req.getNhipThoLanPhut()) : null)
                .oxygenSaturation(req.getSpo2PhanTram())
                .otherOrganAbnormality(req.getCoBatThuongCoQuan())
                .otherOrganAbnormalityDescription(req.getBatThuongCoQuanGhiRo())
                .build();

        VitalSign saved = vitalSignRepository.save(entity);

        return mapToResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<VitalSignsResponse> getListVitalSigns(String examId) {
        if (examId == null || examId.isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: exam_id");
        }

        Long examIdLong;
        try {
            examIdLong = Long.parseLong(examId.trim());
        } catch (NumberFormatException e) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "exam_id phải là một giá trị số");
        }

        MedicalVisit visit = medicalVisitRepository.findById(examIdLong)
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND, "Phiên khám không tìm thấy: " + examIdLong));

        // Verify ownership when the caller is a patient.
        if (SecurityUtils.hasRole("patient") && !SecurityUtils.hasRole("doctor") && !SecurityUtils.hasRole("admin")) {
            MedicalRecord record = medicalRecordRepository.findById(visit.getMedicalRecordId())
                    .orElseThrow(() -> new AppException(ErrorCode.RESOURCE_NOT_FOUND, "Hồ sơ bệnh án không tìm thấy"));

            Long currentUserId = SecurityUtils.getCurrentUserId();
            UserAccount account = userAccountRepository.findById(currentUserId).orElse(null);
            Long callerPatientId = (account != null && account.getPerson() != null)
                    ? account.getPerson().getPersonId()
                    : currentUserId;

            if (!record.getPatientId().equals(callerPatientId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, "Không thể truy cập dữ liệu của người dùng khác");
            }
        }

        List<VitalSign> list = vitalSignRepository.findByMedicalVisitIdOrderByTimestampAsc(examIdLong);
        return list.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    private VitalSignsResponse mapToResponse(VitalSign entity) {
        return VitalSignsResponse.builder()
                .vitalSignId(entity.getVitalSignId() != null ? String.valueOf(entity.getVitalSignId()) : null)
                .examId(entity.getMedicalVisitId() != null ? String.valueOf(entity.getMedicalVisitId()) : null)
                .thoiDiemDo(entity.getTimestamp())
                .machLanPhut(entity.getPulse() != null ? entity.getPulse().intValue() : null)
                .huyetApTamThu(entity.getSystolicBloodPressure() != null ? entity.getSystolicBloodPressure().intValue() : null)
                .huyetApTamTruong(entity.getDiastolicBloodPressure() != null ? entity.getDiastolicBloodPressure().intValue() : null)
                .nhipThoLanPhut(entity.getRespiratoryRate() != null ? entity.getRespiratoryRate().intValue() : null)
                .spo2PhanTram(entity.getOxygenSaturation())
                .coBatThuongCoQuan(entity.getOtherOrganAbnormality())
                .batThuongCoQuanGhiRo(entity.getOtherOrganAbnormalityDescription())
                .build();
    }
}
