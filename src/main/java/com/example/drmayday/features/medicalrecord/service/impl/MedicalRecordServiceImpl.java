package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.*;
import com.example.drmayday.features.medicalrecord.dto.*;
import com.example.drmayday.features.medicalrecord.service.MedicalRecordService;
import com.example.drmayday.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordServiceImpl.class);
    private static final Set<String> VALID_TYPES = Set.of("CAP", "MAN");
    private static final Set<String> VALID_STATUSES = Set.of("draft", "pending", "in_progress", "completed", "cancelled");
    private static final Set<String> TERMINAL_STATUSES = Set.of("completed", "cancelled");
    private static final Set<String> VALID_FAMILY_HISTORY = Set.of("Co", "Khong", "Khong ro");
    private static final Set<String> VALID_TREATMENT_OUTCOMES = Set.of("Khoi benh", "Do", "Khong do", "Nang hon", "Chet", "Bien chung");
    private static final Set<String> VALID_TRI_STATE = Set.of("CO", "KHONG", "KHONG_RO");
    private final MedicalRecordRepository medicalRecordRepo;
    private final AcuteUrticariaRecordRepository acuteRepo;
    private final ChronicUrticariaRecordRepository chronicRepo;
    private final PersonalMedicalHistoryRepository selfHistoryRepo;
    private final MedicalVisitRepository visitRepo;
    private final PatientRepository patientRepo;
    private final AllergyOnsetContextRepository allergyContextRepo;
    private final PreviousDiseaseEpisodeRepository previousEpisodeRepo;
    private final InfectionStatusRepository infectionStatusRepo;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepo,
                                     AcuteUrticariaRecordRepository acuteRepo,
                                     ChronicUrticariaRecordRepository chronicRepo,
                                     PersonalMedicalHistoryRepository selfHistoryRepo,
                                     MedicalVisitRepository visitRepo,
                                     PatientRepository patientRepo,
                                     AllergyOnsetContextRepository allergyContextRepo,
                                     PreviousDiseaseEpisodeRepository previousEpisodeRepo,
                                     InfectionStatusRepository infectionStatusRepo) {
        this.medicalRecordRepo = medicalRecordRepo;
        this.acuteRepo = acuteRepo;
        this.chronicRepo = chronicRepo;
        this.selfHistoryRepo = selfHistoryRepo;
        this.visitRepo = visitRepo;
        this.patientRepo = patientRepo;
        this.allergyContextRepo = allergyContextRepo;
        this.previousEpisodeRepo = previousEpisodeRepo;
        this.infectionStatusRepo = infectionStatusRepo;
    }

    // ── API 30: create_medical_record ──

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(CreateMedicalRecordRequest req) {
        Long patientId = req.getPatientId();
        if (!patientRepo.existsById(patientId)) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        String type = req.getLoaiBenhAn();
        if (!VALID_TYPES.contains(type)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "loai_benh_an must be CAP or MAN");
        }

        if (medicalRecordRepo.existsByPatientIdAndStatusNotIn(patientId, TERMINAL_STATUSES)) {
            throw new AppException(ErrorCode.PATIENT_ALREADY_HAS_ACTIVE_RECORD);
        }

        validateFamilyHistory(req.getTienSuGiaDinh(), req.getTienSuGiaDinhGhiRo());
        if (req.getKetQuaDieuTriBaoHiem() != null && !VALID_TREATMENT_OUTCOMES.contains(req.getKetQuaDieuTriBaoHiem())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "ket_qua_dieu_tri_bao_hiem invalid");
        }

        MedicalRecord record = MedicalRecord.builder()
                .patientId(patientId)
                .medicalRecordType(type)
                .status(req.getTrangThai() != null ? req.getTrangThai() : "draft")
                .referringFacilityDiagnosis(req.getChanDoanTuyenTruoc())
                .initialClinicDiagnosis(req.getChanDoanBanDauPhongKham())
                .onsetDurationWeeks(req.getThoiGianKhoiPhatTuan())
                .firstVisitAt(parseOffsetDateTime(req.getThoiDiemKhamLanDau()))
                .outpatientTreatmentStartDate(parseLocalDate(req.getNgayBatDauDieuTriNgoaiTru()))
                .outpatientTreatmentEndDate(parseLocalDate(req.getNgayKetThucDieuTriNgoaiTru()))
                .insuranceTreatmentOutcome(req.getKetQuaDieuTriBaoHiem())
                .familyHistory(req.getTienSuGiaDinh())
                .familyHistoryDescription(req.getTienSuGiaDinhGhiRo())
                .treatedBeforeVisit(req.getDaDieuTriTruocKham())
                .build();

        record = medicalRecordRepo.saveAndFlush(record);

        if ("CAP".equals(type)) {
            AcuteUrticariaRecord acute = AcuteUrticariaRecord.builder()
                    .medicalRecordId(record.getMedicalRecordId())
                    .updatedAt(OffsetDateTime.now())
                    .build();
            acuteRepo.saveAndFlush(acute);
        } else {
            ChronicUrticariaRecord chronic = ChronicUrticariaRecord.builder()
                    .medicalRecordId(record.getMedicalRecordId())
                    .build();
            chronicRepo.saveAndFlush(chronic);
        }

        log.info("Created medical record {} type={} for patient={}", record.getMedicalRecordId(), type, patientId);
        return MedicalRecordResponse.from(record);
    }

    // ── API 31: list_medical_records ──

    @Override
    @Transactional(readOnly = true)
    public Page<MedicalRecordResponse> listMedicalRecords(String patientIdStr, String loaiBenhAn, String trangThai, int page, int size) {
        Long patientId = patientIdStr != null ? parseLong(patientIdStr, ErrorCode.INVALID_PARAMETER_VALUE) : null;

        if (loaiBenhAn != null && !VALID_TYPES.contains(loaiBenhAn)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "loai_benh_an must be CAP or MAN");
        }
        if (trangThai != null && !VALID_STATUSES.contains(trangThai)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "trang_thai invalid");
        }

        return medicalRecordRepo.findFiltered(patientId, loaiBenhAn, trangThai, PageRequest.of(page - 1, size))
                .map(r -> {
                    String hoTen = null;
                    if (r.getPatient() != null && r.getPatient().getPerson() != null) {
                        hoTen = r.getPatient().getPerson().getFullName();
                    }
                    return MedicalRecordResponse.fromWithName(r, hoTen);
                });
    }

    // ── API 32: get_medical_record ──

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordDetailResponse getMedicalRecord(String idStr) {
        Long id = parseLong(idStr, ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        MedicalRecord record = medicalRecordRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        checkPatientAccess(record.getPatientId());

        MedicalRecordResponse recordResp = MedicalRecordResponse.from(record);

        Object diagnosis = null;
        Object previousEpisodes = null;
        Object infectionStatus = null;

        // Các phần bệnh sử do API 43-45 ghi: previous_episodes chỉ có ở hồ sơ MAN, infection_status chỉ ở CAP
        if ("CAP".equals(record.getMedicalRecordType())) {
            diagnosis = acuteRepo.findById(id).map(AcuteDiagnosisResponse::from).orElse(null);
            infectionStatus = infectionStatusRepo.findByMedicalRecordId(id).map(InfectionStatusResponse::from).orElse(null);
        } else {
            diagnosis = chronicRepo.findById(id).map(ChronicDiagnosisResponse::from).orElse(null);
            previousEpisodes = previousEpisodeRepo.findByMedicalRecordId(id).map(PreviousEpisodesResponse::from).orElse(null);
        }
        AllergyContextResponse allergyContext = allergyContextRepo.findByMedicalRecordId(id)
                .map(AllergyContextResponse::from).orElse(null);

        SelfHistoryResponse selfHistory = selfHistoryRepo.findByMedicalRecordId(id)
                .map(SelfHistoryResponse::from).orElse(null);

        return MedicalRecordDetailResponse.builder()
                .record(recordResp)
                .diagnosis(diagnosis)
                .selfHistory(selfHistory)
                .allergyContext(allergyContext)
                .previousEpisodes(previousEpisodes)
                .infectionStatus(infectionStatus)
                .build();
    }

    // ── API 33: update_medical_record ──

    @Override
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(String idStr, UpdateMedicalRecordRequest req) {
        Long id = parseLong(idStr, ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        MedicalRecord record = medicalRecordRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        if (req.getTrangThai() != null) {
            if (!VALID_STATUSES.contains(req.getTrangThai())) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "trang_thai invalid");
            }
            record.setStatus(req.getTrangThai());
        }
        if (req.getChanDoanTuyenTruoc() != null) record.setReferringFacilityDiagnosis(req.getChanDoanTuyenTruoc());
        if (req.getChanDoanBanDauPhongKham() != null) record.setInitialClinicDiagnosis(req.getChanDoanBanDauPhongKham());
        if (req.getThoiGianKhoiPhatTuan() != null) record.setOnsetDurationWeeks(req.getThoiGianKhoiPhatTuan());
        if (req.getThoiDiemKhamLanDau() != null) record.setFirstVisitAt(parseOffsetDateTime(req.getThoiDiemKhamLanDau()));
        if (req.getNgayBatDauDieuTriNgoaiTru() != null) record.setOutpatientTreatmentStartDate(parseLocalDate(req.getNgayBatDauDieuTriNgoaiTru()));
        if (req.getNgayKetThucDieuTriNgoaiTru() != null) record.setOutpatientTreatmentEndDate(parseLocalDate(req.getNgayKetThucDieuTriNgoaiTru()));
        if (req.getKetQuaDieuTriBaoHiem() != null) {
            if (!VALID_TREATMENT_OUTCOMES.contains(req.getKetQuaDieuTriBaoHiem())) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "ket_qua_dieu_tri_bao_hiem invalid");
            }
            record.setInsuranceTreatmentOutcome(req.getKetQuaDieuTriBaoHiem());
        }
        if (req.getTienSuGiaDinh() != null || req.getTienSuGiaDinhGhiRo() != null) {
            String familyHistory = req.getTienSuGiaDinh() != null ? req.getTienSuGiaDinh() : record.getFamilyHistory();
            String familyHistoryDesc = req.getTienSuGiaDinhGhiRo() != null ? req.getTienSuGiaDinhGhiRo() : record.getFamilyHistoryDescription();
            validateFamilyHistory(familyHistory, familyHistoryDesc);
            record.setFamilyHistory(familyHistory);
            record.setFamilyHistoryDescription(familyHistoryDesc);
        }
        if (req.getDaDieuTriTruocKham() != null) record.setTreatedBeforeVisit(req.getDaDieuTriTruocKham());

        record = medicalRecordRepo.saveAndFlush(record);
        log.info("Updated medical record {}", id);
        return MedicalRecordResponse.from(record);
    }

    // ── API 34: delete_medical_record ──

    @Override
    @Transactional
    public void deleteMedicalRecord(String idStr) {
        Long id = parseLong(idStr, ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        MedicalRecord record = medicalRecordRepo.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));

        try {
            selfHistoryRepo.findByMedicalRecordId(id).ifPresent(selfHistoryRepo::delete);

            if ("CAP".equals(record.getMedicalRecordType())) {
                acuteRepo.findById(id).ifPresent(acuteRepo::delete);
            } else {
                chronicRepo.findById(id).ifPresent(chronicRepo::delete);
            }

            medicalRecordRepo.delete(record);
            medicalRecordRepo.flush();
        } catch (DataIntegrityViolationException ex) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_HAS_CHILD_DATA);
        }

        log.info("Deleted medical record {}", id);
    }

    // ── API 35: get_patient_record_history ──

    @Override
    @Transactional(readOnly = true)
    public List<ExamHistoryResponse> getPatientRecordHistory(String patientIdStr) {
        Long patientId = parseLong(patientIdStr, ErrorCode.USER_ACCOUNT_NOT_FOUND);
        if (!patientRepo.existsById(patientId)) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }

        checkPatientAccess(patientId);

        return visitRepo.findByPatientIdOrderByVisitDateDesc(patientId).stream()
                .map(ExamHistoryResponse::from)
                .toList();
    }

    // ── API 38: upsert_self_history ──

    @Override
    @Transactional
    public SelfHistoryResponse upsertSelfHistory(UpsertSelfHistoryRequest req) {
        if (!medicalRecordRepo.existsById(req.getRecordId())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }

        validateTriState(req.getTienSuCoDia(), "tien_su_co_dia");
        validateTriState(req.getTienSuDiUng(), "tien_su_di_ung");
        validateBooleanDetail(req.getTienSuTuyenGiap(), req.getTienSuTuyenGiapGhiRo(), "tien_su_tuyen_giap");
        validateBooleanDetail(req.getTienSuTuMien(), req.getTienSuTuMienGhiRo(), "tien_su_tu_mien");
        validateBooleanDetail(req.getTienSuPhanVe(), req.getTienSuPhanVeGhiRo(), "tien_su_phan_ve");
        validateBooleanDetail(req.getTienSuViemNhiemMan(), req.getTienSuViemNhiemManGhiRo(), "tien_su_viem_nhiem_man");
        validateBooleanDetail(req.getTienSuBenhKhac(), req.getTienSuBenhKhacGhiRo(), "tien_su_benh_khac");

        boolean isNew = false;
        PersonalMedicalHistory history = selfHistoryRepo.findByMedicalRecordId(req.getRecordId())
                .orElse(null);
        if (history == null) {
            isNew = true;
            history = PersonalMedicalHistory.builder().medicalRecordId(req.getRecordId()).build();
        }

        history.setAtopicHistory(req.getTienSuCoDia());
        history.setAtopicDermatitis(req.getCoDiaViemDaCoDia());
        history.setAsthma(req.getCoDiaHen());
        history.setAllergicRhinitis(req.getCoDiaViemMuiDiUng());
        history.setOtherAtopicConditions(req.getCoDiaKhac());
        history.setOtherAtopicConditionsDescription(req.getCoDiaKhacGhiRo());
        history.setThyroidHistory(req.getTienSuTuyenGiap());
        history.setThyroidHistoryDescription(req.getTienSuTuyenGiapGhiRo());
        history.setAutoimmuneHistory(req.getTienSuTuMien());
        history.setAutoimmuneHistoryDescription(req.getTienSuTuMienGhiRo());
        history.setAllergyHistory(req.getTienSuDiUng());
        history.setMedicationAllergy(req.getDiUngThuoc());
        history.setMedicationAllergyDescription(req.getDiUngThuocGhiRo());
        history.setFoodAllergy(req.getDiUngThucAn());
        history.setFoodAllergyDescription(req.getDiUngThucAnGhiRo());
        history.setOtherAllergy(req.getDiUngDiNguyenKhac());
        history.setOtherAllergyDescription(req.getDiUngDiNguyenKhacGhiRo());
        history.setAnaphylaxisHistory(req.getTienSuPhanVe());
        history.setAnaphylaxisHistoryDescription(req.getTienSuPhanVeGhiRo());
        history.setChronicInfectionHistory(req.getTienSuViemNhiemMan());
        history.setChronicInfectionHistoryDescription(req.getTienSuViemNhiemManGhiRo());
        history.setOtherMedicalHistory(req.getTienSuBenhKhac());
        history.setOtherMedicalHistoryDescription(req.getTienSuBenhKhacGhiRo());

        history = selfHistoryRepo.saveAndFlush(history);
        log.info("{} self history for record {}", isNew ? "Created" : "Updated", req.getRecordId());
        SelfHistoryResponse response = SelfHistoryResponse.from(history);
        response.setCreated(isNew);
        return response;
    }

    // ── Helper methods ──

    private void checkPatientAccess(Long patientId) {
        if (SecurityUtils.hasRole("BENH_NHAN")) {
            Long currentUserId = SecurityUtils.getCurrentUserId();
            if (!patientId.equals(currentUserId)) {
                throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
            }
        }
    }

    private void validateFamilyHistory(String familyHistory, String familyHistoryDesc) {
        if (familyHistory != null && !VALID_FAMILY_HISTORY.contains(familyHistory)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "tien_su_gia_dinh invalid");
        }
        if ("Co".equals(familyHistory) && (familyHistoryDesc == null || familyHistoryDesc.isBlank())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "tien_su_gia_dinh_ghi_ro required when tien_su_gia_dinh=Co");
        }
        if (!"Co".equals(familyHistory) && familyHistoryDesc != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "tien_su_gia_dinh_ghi_ro must be null when tien_su_gia_dinh != Co");
        }
    }

    private void validateTriState(String value, String fieldName) {
        if (value != null && !VALID_TRI_STATE.contains(value)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    fieldName + " must be one of CO, KHONG, KHONG_RO");
        }
    }

    private void validateBooleanDetail(Boolean flag, String detail, String fieldName) {
        if (Boolean.TRUE.equals(flag) && (detail == null || detail.isBlank())) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, fieldName + "_ghi_ro required when " + fieldName + "=true");
        }
        if (!Boolean.TRUE.equals(flag) && detail != null) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, fieldName + "_ghi_ro must be null when " + fieldName + " is not true");
        }
    }

    private Long parseLong(String value, ErrorCode errorOnFail) {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException ex) {
            throw new AppException(errorOnFail);
        }
    }

    private OffsetDateTime parseOffsetDateTime(String value) {
        if (value == null) return null;
        try {
            return OffsetDateTime.parse(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Invalid datetime format");
        }
    }

    private LocalDate parseLocalDate(String value) {
        if (value == null) return null;
        try {
            return LocalDate.parse(value);
        } catch (Exception ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "Invalid date format");
        }
    }

}
