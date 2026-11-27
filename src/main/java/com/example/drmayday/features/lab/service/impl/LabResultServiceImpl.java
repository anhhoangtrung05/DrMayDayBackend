package com.example.drmayday.features.lab.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.AcuteClinicalTestResult;
import com.example.drmayday.entity.ChronicClinicalTestResult;
import com.example.drmayday.entity.ClinicalTestResult;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.features.lab.dto.LabResultResponse;
import com.example.drmayday.features.lab.dto.UpsertLabResultsRequest;
import com.example.drmayday.features.lab.service.LabResultService;
import com.example.drmayday.repository.AcuteClinicalTestResultRepository;
import com.example.drmayday.repository.ChronicClinicalTestResultRepository;
import com.example.drmayday.repository.ClinicalTestResultRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Lab APIs (LAB_UPSERT_LAB_RESULTS, LAB_GET_LAB_RESULTS): lab results of an exam session. ketquacanlamsang is the supertype and
 * exactly one of ketquacanlamsangcap / ketquacanlamsangman must exist for it; the
 * DEFERRABLE FKs check that at commit, so parent and subtype are written in one transaction.
 */
@Service
public class LabResultServiceImpl implements LabResultService {

    static final String TYPE_ACUTE = "CAP";
    static final String TYPE_CHRONIC = "MAN";
    static final String POSITIVE = "Duong tinh";
    static final Set<String> TRI_STATE_RESULTS = Set.of(POSITIVE, "Am tinh", "Chua lam");
    static final Set<String> BINARY_RESULTS = Set.of(POSITIVE, "Am tinh");

    private final MedicalRecordRepository medicalRecordRepository;
    private final MedicalVisitRepository medicalVisitRepository;
    private final ClinicalTestResultRepository clinicalTestResultRepository;
    private final AcuteClinicalTestResultRepository acuteRepository;
    private final ChronicClinicalTestResultRepository chronicRepository;

    public LabResultServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                MedicalVisitRepository medicalVisitRepository,
                                ClinicalTestResultRepository clinicalTestResultRepository,
                                AcuteClinicalTestResultRepository acuteRepository,
                                ChronicClinicalTestResultRepository chronicRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.medicalVisitRepository = medicalVisitRepository;
        this.clinicalTestResultRepository = clinicalTestResultRepository;
        this.acuteRepository = acuteRepository;
        this.chronicRepository = chronicRepository;
    }

    // ── LAB_UPSERT_LAB_RESULTS ──

    @Override
    @Transactional
    public LabResultResponse upsertLabResults(UpsertLabResultsRequest req) {
        if (req == null || req.getRecordId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: record_id");
        }
        if (req.getPhienKhamId() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: phien_kham_id");
        }

        medicalRecordRepository.findById(req.getRecordId())
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND,
                        "Không tìm thấy hồ sơ bệnh án " + req.getRecordId()));
        MedicalVisit visit = medicalVisitRepository.findById(req.getPhienKhamId())
                .filter(v -> req.getRecordId().equals(v.getMedicalRecordId()))
                .orElseThrow(() -> new AppException(ErrorCode.EXAM_NOT_FOUND,
                        "Không tìm thấy phiên khám " + req.getPhienKhamId()
                                + " thuộc hồ sơ bệnh án " + req.getRecordId()));

        String type = visit.getMedicalRecordType();
        validate(req, type);

        ClinicalTestResult existing = clinicalTestResultRepository
                .findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(visit.getMedicalVisitId())
                .orElse(null);
        boolean created = existing == null;
        ClinicalTestResult parent = existing;
        if (parent == null) {
            parent = ClinicalTestResult.builder()
                .medicalVisitId(visit.getMedicalVisitId())
                .clinicalTestResultType(type)
                .build();
        }

        // PUT replaces the whole result: fields that are not sent are cleared.
        parent.setWbc(req.getWbc());
        parent.setCrp(req.getCrp());
        parent.setTotalIge(req.getIgeToanPhan());
        parent.setOtherLaboratoryTest(req.getXetNghiemKhac());
        parent.setOtherClinicalTestNotes(req.getCanLamSangKhacGhiChu());
        parent.setPerformedAt(req.getThoiDiemThucHien());
        parent = clinicalTestResultRepository.saveAndFlush(parent);
        Long resultId = parent.getClinicalTestResultId();

        LabResultResponse response;
        if (TYPE_ACUTE.equals(type)) {
            AcuteClinicalTestResult cap = acuteRepository.findById(resultId)
                    .orElseGet(() -> AcuteClinicalTestResult.builder().clinicalTestResultId(resultId).build());
            cap.setLymphocytes(req.getLym());
            cap.setNeutrophils(req.getNeu());
            cap.setPlatelets(req.getPlt());
            cap.setAcuteTryptase(req.getTryptaseCap());
            cap.setBaselineTryptase(req.getTryptaseNen());
            cap = acuteRepository.saveAndFlush(cap);
            response = LabResultResponse.fromAcute(visit.getMedicalRecordId(), parent, cap);
        } else {
            ChronicClinicalTestResult man = chronicRepository.findById(resultId)
                    .orElseGet(() -> ChronicClinicalTestResult.builder().clinicalTestResultId(resultId).build());
            man.setAutologousSerumTest(req.getTestHuyetThanhTuThan());
            man.setAntiTpoIgeResult(req.getIgeKhangTpoKetQua());
            man.setBasophilActivationTestResult(req.getBatKetQua());
            man.setAntiIl24IgeResult(req.getIgeKhangIl24KetQua());
            man.setAnaHep2Result(req.getAnaHep2());
            man.setAsstDiameterMm(req.getAsstDuongKinhMm());
            man.setEosinophils(req.getEo());
            man.setBasophils(req.getBaBachCau());
            man.setAntiTpo(req.getAntiTpo());
            man.setTsh(req.getTsh());
            man.setFt4(req.getFt4());
            man.setFt3(req.getFt3());
            man.setEsrAtTwoHours(req.getMauLang2h());
            man.setEsrAtOneHour(req.getMauLang1h());
            man.setThyroidUltrasoundResult(req.getSieuAmTuyenGiap());
            man = chronicRepository.saveAndFlush(man);
            response = LabResultResponse.fromChronic(visit.getMedicalRecordId(), parent, man);
        }
        response.setCreated(created);
        return response;
    }

    // ── LAB_GET_LAB_RESULTS ──

    @Override
    @Transactional(readOnly = true)
    public LabResultResponse getLabResults(Long examId) {
        if (examId == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER, "Thiếu tham số bắt buộc: phien_kham_id");
        }

        ClinicalTestResult parent = clinicalTestResultRepository
                .findFirstByMedicalVisitIdOrderByClinicalTestResultIdAsc(examId)
                .orElseThrow(() -> new AppException(ErrorCode.LAB_RESULT_NOT_FOUND,
                        "Phiên khám " + examId + " chưa có kết quả cận lâm sàng"));

        MedicalVisit visit = medicalVisitRepository.findById(examId).orElse(null);
        Long recordId = visit == null ? null : visit.getMedicalRecordId();
        checkPatientAccessToRecord(recordId);

        Long resultId = parent.getClinicalTestResultId();
        if (TYPE_ACUTE.equals(parent.getClinicalTestResultType())) {
            return LabResultResponse.fromAcute(recordId, parent, acuteRepository.findById(resultId).orElse(null));
        }
        return LabResultResponse.fromChronic(recordId, parent, chronicRepository.findById(resultId).orElse(null));
    }

    // ── Validation ──

    private static void validate(UpsertLabResultsRequest req, String type) {
        Map<String, Object> acuteFields = acuteFields(req);
        Map<String, Object> chronicFields = chronicFields(req);

        Map<String, Object> foreignFields = TYPE_ACUTE.equals(type) ? chronicFields : acuteFields;
        List<String> sentForeign = foreignFields.entrySet().stream()
                .filter(e -> e.getValue() != null)
                .map(entry -> entry.getKey())
                .toList();
        if (!sentForeign.isEmpty()) {
            String otherType = TYPE_ACUTE.equals(type) ? TYPE_CHRONIC : TYPE_ACUTE;
            throw new AppException(ErrorCode.LAB_RESULT_FIELD_TYPE_MISMATCH,
                    "Phiên khám " + req.getPhienKhamId() + " thuộc loại " + type
                            + " nên không nhận các trường của loại " + otherType + ": " + String.join(", ", sentForeign));
        }

        Map<String, BigDecimal> numbers = new LinkedHashMap<>();
        numbers.put("wbc", req.getWbc());
        numbers.put("crp", req.getCrp());
        numbers.put("ige_toan_phan", req.getIgeToanPhan());
        (TYPE_ACUTE.equals(type) ? acuteFields : chronicFields).forEach((name, value) -> {
            if (value instanceof BigDecimal decimal) {
                numbers.put(name, decimal);
            }
        });
        numbers.forEach((name, value) -> {
            if (value != null && value.signum() < 0) {
                throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, name + " phải lớn hơn hoặc bằng 0");
            }
        });

        if (TYPE_CHRONIC.equals(type)) {
            checkEnum(req.getTestHuyetThanhTuThan(), TRI_STATE_RESULTS, "test_huyet_thanh_tu_than", "Duong tinh, Am tinh, Chua lam");
            checkEnum(req.getIgeKhangTpoKetQua(), TRI_STATE_RESULTS, "ige_khang_tpo_ket_qua", "Duong tinh, Am tinh, Chua lam");
            checkEnum(req.getBatKetQua(), TRI_STATE_RESULTS, "bat_ket_qua", "Duong tinh, Am tinh, Chua lam");
            checkEnum(req.getIgeKhangIl24KetQua(), TRI_STATE_RESULTS, "ige_khang_il24_ket_qua", "Duong tinh, Am tinh, Chua lam");
            checkEnum(req.getAnaHep2(), BINARY_RESULTS, "ana_hep2", "Duong tinh, Am tinh");

            // ck_kqcls_man_asst_logic
            if (req.getAsstDuongKinhMm() != null && !POSITIVE.equals(req.getTestHuyetThanhTuThan())) {
                throw new AppException(ErrorCode.ASST_REQUIRES_POSITIVE_SERUM_TEST);
            }
        }
    }

    private static Map<String, Object> acuteFields(UpsertLabResultsRequest req) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("lym", req.getLym());
        fields.put("neu", req.getNeu());
        fields.put("plt", req.getPlt());
        fields.put("tryptase_cap", req.getTryptaseCap());
        fields.put("tryptase_nen", req.getTryptaseNen());
        return fields;
    }

    private static Map<String, Object> chronicFields(UpsertLabResultsRequest req) {
        Map<String, Object> fields = new LinkedHashMap<>();
        fields.put("test_huyet_thanh_tu_than", req.getTestHuyetThanhTuThan());
        fields.put("ige_khang_tpo_ket_qua", req.getIgeKhangTpoKetQua());
        fields.put("bat_ket_qua", req.getBatKetQua());
        fields.put("ige_khang_il24_ket_qua", req.getIgeKhangIl24KetQua());
        fields.put("ana_hep2", req.getAnaHep2());
        fields.put("asst_duong_kinh_mm", req.getAsstDuongKinhMm());
        fields.put("eo", req.getEo());
        fields.put("ba_bach_cau", req.getBaBachCau());
        fields.put("anti_tpo", req.getAntiTpo());
        fields.put("tsh", req.getTsh());
        fields.put("ft4", req.getFt4());
        fields.put("ft3", req.getFt3());
        fields.put("mau_lang_2h", req.getMauLang2h());
        fields.put("mau_lang_1h", req.getMauLang1h());
        fields.put("sieu_am_tuyen_giap", req.getSieuAmTuyenGiap());
        return fields;
    }

    private static void checkEnum(String value, Set<String> allowed, String field, String allowedText) {
        if (value != null && !allowed.contains(value)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    field + " không hợp lệ, chỉ chấp nhận: " + allowedText);
        }
    }

    /** Patients may only read lab results of their own medical records. */
    private void checkPatientAccessToRecord(Long recordId) {
        if (!SecurityUtils.hasRole("BENH_NHAN") || recordId == null) {
            return;
        }
        Long currentUserId = SecurityUtils.getCurrentUserId();
        MedicalRecord record = medicalRecordRepository.findById(recordId).orElse(null);
        Long ownerId = record == null ? null : record.getPatientId();
        if (ownerId != null && !Objects.equals(ownerId, currentUserId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }
    }
}
