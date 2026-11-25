package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.ChronicUrticariaRecord;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.features.medicalrecord.dto.ChronicDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertChronicDiagnosisRequest;
import com.example.drmayday.features.medicalrecord.service.ChronicDiagnosisService;
import com.example.drmayday.repository.ChronicUrticariaRecordRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class ChronicDiagnosisServiceImpl implements ChronicDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(ChronicDiagnosisServiceImpl.class);
    private static final Set<String> VALID_CHAN_DOAN_CHINH = Set.of("CSU", "CIndU", "CSU + CIndU", "Phu mach don thuan");
    private static final Set<String> VALID_PHAN_NHOM_CSU = Set.of("CSU Type I", "CSU Type IIb", "Chong lap Type I + Type IIb", "Khong xac dinh");
    private static final Set<String> VALID_BIEU_HIEN = Set.of("Chi san phu", "Chi phu mach", "San phu + phu mach");

    private final MedicalRecordRepository medicalRecordRepo;
    private final ChronicUrticariaRecordRepository chronicRepo;

    public ChronicDiagnosisServiceImpl(MedicalRecordRepository medicalRecordRepo,
                                        ChronicUrticariaRecordRepository chronicRepo) {
        this.medicalRecordRepo = medicalRecordRepo;
        this.chronicRepo = chronicRepo;
    }

    @Override
    @Transactional
    public ChronicDiagnosisResponse upsertChronicDiagnosis(UpsertChronicDiagnosisRequest req) {
        MedicalRecord record = medicalRecordRepo.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!"MAN".equals(record.getMedicalRecordType())) {
            throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH, "Hồ sơ bệnh án không phải hồ sơ bệnh án mạn tính");
        }

        if (req.getChanDoanChinh() != null && !VALID_CHAN_DOAN_CHINH.contains(req.getChanDoanChinh())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Chẩn đoán chính không hợp lệ");
        }
        if (req.getPhanNhomCsu() != null && !VALID_PHAN_NHOM_CSU.contains(req.getPhanNhomCsu())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Phân nhóm CSU không hợp lệ");
        }
        if (req.getBieuHienQuaTrinhBenh() != null && !VALID_BIEU_HIEN.contains(req.getBieuHienQuaTrinhBenh())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Biểu hiện quá trình bệnh không hợp lệ");
        }
        if (Boolean.TRUE.equals(req.getMayDayCamUngKhac()) &&
                (req.getMayDayCamUngKhacGhiRo() == null || req.getMayDayCamUngKhacGhiRo().isBlank())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Mô tả phản ứng khác khi mắc bệnh là bắt buộc khi may_day_cam_ung_khac=true");
        }
        if (!Boolean.TRUE.equals(req.getMayDayCamUngKhac()) && req.getMayDayCamUngKhacGhiRo() != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Mô tả phản ứng khác khi mắc bệnh phải để trống khi may_day_cam_ung_khac=false");
        }

        ChronicUrticariaRecord chronic = chronicRepo.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        chronic.setPrimaryDiagnosis(req.getChanDoanChinh());
        chronic.setCsuSubtype(req.getPhanNhomCsu());
        chronic.setDermographism(req.getChungDaVeNoi());
        chronic.setCholinergicUrticaria(req.getMayDayCholinergic());
        chronic.setColdUrticaria(req.getMayDayDoLanh());
        chronic.setAdrenergicUrticaria(req.getMayDayAdrenergic());
        chronic.setAquagenicUrticaria(req.getMayDayDoNuoc());
        chronic.setSolarUrticaria(req.getMayDayDoAnhSang());
        chronic.setOtherInducibleUrticaria(req.getMayDayCamUngKhac());
        chronic.setOtherInducibleUrticariaDescription(req.getMayDayCamUngKhacGhiRo());
        chronic.setDiseaseCourseManifestation(req.getBieuHienQuaTrinhBenh());

        chronic = chronicRepo.saveAndFlush(chronic);
        log.info("Upserted chronic diagnosis for record {}", req.getRecordId());
        return ChronicDiagnosisResponse.from(chronic);
    }
}
