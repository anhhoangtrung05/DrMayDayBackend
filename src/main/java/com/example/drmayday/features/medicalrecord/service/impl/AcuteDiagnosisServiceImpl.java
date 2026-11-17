package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.entity.AcuteUrticariaRecord;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.features.medicalrecord.dto.AcuteDiagnosisResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAcuteDiagnosisRequest;
import com.example.drmayday.features.medicalrecord.service.AcuteDiagnosisService;
import com.example.drmayday.repository.AcuteUrticariaRecordRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.OffsetDateTime;

@Service
public class AcuteDiagnosisServiceImpl implements AcuteDiagnosisService {

    private static final Logger log = LoggerFactory.getLogger(AcuteDiagnosisServiceImpl.class);

    private final MedicalRecordRepository medicalRecordRepo;
    private final AcuteUrticariaRecordRepository acuteRepo;

    public AcuteDiagnosisServiceImpl(MedicalRecordRepository medicalRecordRepo,
                                      AcuteUrticariaRecordRepository acuteRepo) {
        this.medicalRecordRepo = medicalRecordRepo;
        this.acuteRepo = acuteRepo;
    }

    @Override
    @Transactional
    public AcuteDiagnosisResponse upsertAcuteDiagnosis(UpsertAcuteDiagnosisRequest req) {
        MedicalRecord record = medicalRecordRepo.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (!"CAP".equals(record.getMedicalRecordType())) {
            throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH, "Record is not CAP type");
        }

        if (req.getTuoiLanDauBiBenh() != null && req.getTuoiLanDauBiBenh() < 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "tuoi_lan_dau_bi_benh must be >= 0");
        }
        if (req.getSoLanMayDayCapTruoc() != null && req.getSoLanMayDayCapTruoc() < 0) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "so_lan_may_day_cap_truoc must be >= 0");
        }

        AcuteUrticariaRecord acute = acuteRepo.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        acute.setHospitalized(req.getNhapVien());
        acute.setDiagnosedAnaphylacticShock(req.getCdSocPhanVe());
        acute.setDiagnosedAnaphylaxis(req.getCdPhanVe());
        acute.setDiagnosedSevereAcuteUrticaria(req.getCdMayDayCapNang());
        acute.setDiagnosedTypicalAcuteUrticaria(req.getCdMayDayCapThongThuong());
        acute.setSkinComorbidityHistory(req.getTienSuBenhNenDa());
        acute.setRespiratoryComorbidityHistory(req.getTienSuBenhNenHoHap());
        acute.setCirculatoryComorbidityHistory(req.getTienSuBenhNenTuanHoan());
        acute.setCardiovascularHistoryDescription(req.getTienSuBenhTimMachGhiRo());
        acute.setChronicGastrointestinalHistoryDescription(req.getTienSuBenhTieuHoaManTinhGhiRo());
        acute.setGastrointestinalComorbidityHistory(req.getTienSuBenhNenTieuHoa());
        acute.setSuspectedTriggerGroup(req.getNhomYeuToNghiNgo());
        acute.setSuspectedTriggerInCurrentEpisode(req.getCoNghiNgoDotNay());
        acute.setSuspectedTriggerName(req.getTenYeuToNghiNgo());
        acute.setHistoryType(req.getLoaiTienSu());
        acute.setPreviousReaction(req.getDaTungPhanUngTruocDay());
        acute.setExposureToOnsetDuration(parseDuration(req.getThoiGianTiepXucDenKhoiPhat()));
        acute.setAverageDuration(parseDuration(req.getThoiGianKeoDaiTb()));
        acute.setTimeSinceLastEpisode(parseDuration(req.getDotGanNhatCach()));
        acute.setAnaphylaxisHistoryDescription(req.getTienSuPhanVeGhiRo());
        acute.setAllergyHistoryDescription(req.getTienSuDiUngGhiRo());
        acute.setTreatedCurrentEpisode(req.getDaDieuTriBenhDotNay());
        acute.setAgeAtFirstOnset(req.getTuoiLanDauBiBenh());
        acute.setPreviousAcuteUrticariaCount(req.getSoLanMayDayCapTruoc());
        acute.setComorbidities(req.getBenhPhu());
        acute.setUpdatedAt(OffsetDateTime.now());

        acute = acuteRepo.saveAndFlush(acute);
        log.info("Upserted acute diagnosis for record {}", req.getRecordId());
        return AcuteDiagnosisResponse.from(acute);
    }

    private Duration parseDuration(String value) {
        if (value == null) return null;
        try {
            return Duration.parse(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Invalid interval/duration format");
        }
    }
}
