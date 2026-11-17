package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.dto.PageResponse;
import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.AcuteUrticariaRecord;
import com.example.drmayday.entity.ChronicUrticariaRecord;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.features.medicalrecord.dto.AcuteDiagnosisDto;
import com.example.drmayday.features.medicalrecord.dto.AcuteMedicalRecordDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.AllergyContextDto;
import com.example.drmayday.features.medicalrecord.dto.ChronicDiagnosisDto;
import com.example.drmayday.features.medicalrecord.dto.ChronicMedicalRecordDetailResponse;
import com.example.drmayday.features.medicalrecord.dto.CreateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.dto.ExamHistoryResponse;
import com.example.drmayday.features.medicalrecord.dto.InfectionStatusDto;
import com.example.drmayday.features.medicalrecord.dto.JsonValues;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordListItem;
import com.example.drmayday.features.medicalrecord.dto.MedicalRecordResponse;
import com.example.drmayday.features.medicalrecord.dto.PreviousEpisodesDto;
import com.example.drmayday.features.medicalrecord.dto.SelfHistoryDto;
import com.example.drmayday.features.medicalrecord.dto.UpdateMedicalRecordRequest;
import com.example.drmayday.features.medicalrecord.service.MedicalRecordService;
import com.example.drmayday.repository.AcuteUrticariaRecordRepository;
import com.example.drmayday.repository.AllergyOnsetContextRepository;
import com.example.drmayday.repository.ChronicUrticariaRecordRepository;
import com.example.drmayday.repository.InfectionStatusRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.PersonalMedicalHistoryRepository;
import com.example.drmayday.repository.PreviousDiseaseEpisodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final Logger log = LoggerFactory.getLogger(MedicalRecordServiceImpl.class);

    private static final String TYPE_ACUTE = "CAP";
    private static final String TYPE_CHRONIC = "MAN";
    private static final Set<String> TYPES = Set.of(TYPE_ACUTE, TYPE_CHRONIC);

    private static final String STATUS_DRAFT = "draft";
    private static final String STATUS_CANCELLED = "cancelled";
    private static final String STATUS_COMPLETED = "completed";
    // Thứ tự đi tới của trạng thái. cancelled vào được từ mọi trạng thái mở
    private static final Map<String, Integer> FORWARD_ORDER =
            Map.of(STATUS_DRAFT, 0, "pending", 1, "in_progress", 2, STATUS_COMPLETED, 3);
    private static final Set<String> OPEN_STATUSES = Set.of(STATUS_DRAFT, "pending", "in_progress");
    private static final Set<String> CLOSED_STATUSES = Set.of(STATUS_COMPLETED, STATUS_CANCELLED);
    private static final Set<String> ALL_STATUSES =
            Set.of(STATUS_DRAFT, "pending", "in_progress", STATUS_COMPLETED, STATUS_CANCELLED);

    private static final String FAMILY_HISTORY_YES = "Co";
    private static final Set<String> FAMILY_HISTORY_VALUES = Set.of(FAMILY_HISTORY_YES, "Khong", "Khong ro");
    private static final Set<String> INSURANCE_OUTCOMES =
            Set.of("Khoi benh", "Do", "Khong do", "Nang hon", "Chet", "Bien chung");
    // NUMERIC(12,2): tối đa 10 chữ số phần nguyên
    private static final BigDecimal ONSET_WEEKS_LIMIT = new BigDecimal("10000000000");

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 20;
    private static final int MAX_SIZE = 100;

    private final MedicalRecordRepository medicalRecordRepository;
    private final AcuteUrticariaRecordRepository acuteRecordRepository;
    private final ChronicUrticariaRecordRepository chronicRecordRepository;
    private final PersonalMedicalHistoryRepository selfHistoryRepository;
    private final AllergyOnsetContextRepository allergyContextRepository;
    private final PreviousDiseaseEpisodeRepository previousEpisodeRepository;
    private final InfectionStatusRepository infectionStatusRepository;
    private final MedicalVisitRepository medicalVisitRepository;
    private final PatientRepository patientRepository;
    private final RecordOwnerCheck recordOwnerCheck;

    public MedicalRecordServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                    AcuteUrticariaRecordRepository acuteRecordRepository,
                                    ChronicUrticariaRecordRepository chronicRecordRepository,
                                    PersonalMedicalHistoryRepository selfHistoryRepository,
                                    AllergyOnsetContextRepository allergyContextRepository,
                                    PreviousDiseaseEpisodeRepository previousEpisodeRepository,
                                    InfectionStatusRepository infectionStatusRepository,
                                    MedicalVisitRepository medicalVisitRepository,
                                    PatientRepository patientRepository,
                                    RecordOwnerCheck recordOwnerCheck) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.acuteRecordRepository = acuteRecordRepository;
        this.chronicRecordRepository = chronicRecordRepository;
        this.selfHistoryRepository = selfHistoryRepository;
        this.allergyContextRepository = allergyContextRepository;
        this.previousEpisodeRepository = previousEpisodeRepository;
        this.infectionStatusRepository = infectionStatusRepository;
        this.medicalVisitRepository = medicalVisitRepository;
        this.patientRepository = patientRepository;
        this.recordOwnerCheck = recordOwnerCheck;
    }

    @Override
    @Transactional
    public MedicalRecordResponse createMedicalRecord(String patientIdValue, CreateMedicalRecordRequest req) {
        Long patientId = parsePatientId(patientIdValue);

        String type = req.getMedicalRecordType();
        if (!TYPES.contains(type)) {
            throw invalidValue();
        }
        String status = req.getStatus() != null ? req.getStatus() : STATUS_DRAFT;
        if (!OPEN_STATUSES.contains(status)) {
            throw invalidValue();
        }
        validateOnsetWeeks(req.getOnsetDurationWeeks());
        validateInsuranceOutcome(req.getInsuranceTreatmentOutcome());
        validateTreatmentDates(req.getOutpatientTreatmentStartDate(), req.getOutpatientTreatmentEndDate());
        OffsetDateTime firstVisitAt = parseTimestamp(req.getFirstVisitAt());
        String familyHistory = req.getFamilyHistory();
        String familyHistoryDescription = resolveFamilyHistoryDescription(familyHistory, req.getFamilyHistoryDescription());

        // Khóa dòng bệnh nhân trước khi kiểm tra 4006, để request thứ hai chờ và thấy hồ sơ vừa tạo
        patientRepository.findByIdForUpdate(patientId).orElseThrow(this::patientNotFound);
        if (medicalRecordRepository.existsByPatientIdAndMedicalRecordTypeAndStatusIn(patientId, type, OPEN_STATUSES)) {
            throw new AppException(ErrorCode.PATIENT_ALREADY_HAS_ACTIVE_RECORD);
        }

        MedicalRecord record = MedicalRecord.builder()
                .patientId(patientId)
                .medicalRecordType(type)
                .status(status)
                .referringFacilityDiagnosis(req.getReferringFacilityDiagnosis())
                .initialClinicDiagnosis(req.getInitialClinicDiagnosis())
                .onsetDurationWeeks(req.getOnsetDurationWeeks())
                .firstVisitAt(firstVisitAt)
                .outpatientTreatmentStartDate(req.getOutpatientTreatmentStartDate())
                .outpatientTreatmentEndDate(req.getOutpatientTreatmentEndDate())
                .insuranceTreatmentOutcome(req.getInsuranceTreatmentOutcome())
                .familyHistory(familyHistory)
                .familyHistoryDescription(familyHistoryDescription)
                .treatedBeforeVisit(req.getTreatedBeforeVisit())
                .build();

        MedicalRecord saved;
        try {
            saved = medicalRecordRepository.saveAndFlush(record);
            // Khóa ngoại kép giữa hồ sơ và subtype chỉ được kiểm tra lúc commit, nên phải chèn subtype ngay trong giao dịch này
            if (TYPE_ACUTE.equals(type)) {
                acuteRecordRepository.saveAndFlush(
                        AcuteUrticariaRecord.builder().medicalRecordId(saved.getMedicalRecordId()).build());
            } else {
                chronicRecordRepository.saveAndFlush(
                        ChronicUrticariaRecord.builder().medicalRecordId(saved.getMedicalRecordId()).build());
            }
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteError(ex);
        }

        log.info("Tạo hồ sơ bệnh án: recordId={}, loai={}, benhNhan={}, taiKhoan={}",
                saved.getMedicalRecordId(), type, patientId, SecurityUtils.getCurrentUserId());
        return new MedicalRecordResponse(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<MedicalRecordListItem> listMedicalRecords(String patientIdValue, String medicalRecordType,
                                                                  String status, Integer page, Integer size) {
        int pageNumber = page != null ? page : DEFAULT_PAGE;
        int pageSize = size != null ? size : DEFAULT_SIZE;
        if (pageNumber < 1 || pageSize < 1 || pageSize > MAX_SIZE) {
            throw invalidValue();
        }

        Long patientId = null;
        if (patientIdValue != null && !patientIdValue.isBlank()) {
            try {
                patientId = Long.parseLong(patientIdValue.trim());
            } catch (NumberFormatException ex) {
                throw invalidValue();
            }
        }
        String type = blankToNull(medicalRecordType);
        if (type != null && !TYPES.contains(type)) {
            throw invalidValue();
        }
        String statusFilter = blankToNull(status);
        if (statusFilter != null && !ALL_STATUSES.contains(statusFilter)) {
            throw invalidValue();
        }

        return PageResponse.from(
                medicalRecordRepository.search(patientId, type, statusFilter, PageRequest.of(pageNumber - 1, pageSize)),
                MedicalRecordListItem::from);
    }

    @Override
    @Transactional(readOnly = true)
    public MedicalRecordResponse getMedicalRecord(String id) {
        MedicalRecord record = medicalRecordRepository.findById(parseRecordId(id))
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        // Kiểm tra 4001 trước 3106, theo đúng thứ tự của đặc tả
        if (SecurityUtils.hasRole("BENH_NHAN")) {
            recordOwnerCheck.assertCurrentPatientOwns(record.getPatientId());
        }

        Long recordId = record.getMedicalRecordId();
        SelfHistoryDto selfHistory = SelfHistoryDto.from(selfHistoryRepository.findByMedicalRecordId(recordId).orElse(null));
        AllergyContextDto allergyContext =
                AllergyContextDto.from(allergyContextRepository.findByMedicalRecordId(recordId).orElse(null));

        if (TYPE_ACUTE.equals(record.getMedicalRecordType())) {
            return new AcuteMedicalRecordDetailResponse(record,
                    AcuteDiagnosisDto.from(acuteRecordRepository.findById(recordId).orElse(null)),
                    selfHistory, allergyContext,
                    InfectionStatusDto.from(infectionStatusRepository.findByMedicalRecordId(recordId).orElse(null)));
        }
        return new ChronicMedicalRecordDetailResponse(record,
                ChronicDiagnosisDto.from(chronicRecordRepository.findById(recordId).orElse(null)),
                selfHistory, allergyContext,
                PreviousEpisodesDto.from(previousEpisodeRepository.findByMedicalRecordId(recordId).orElse(null)));
    }

    @Override
    @Transactional
    public MedicalRecordResponse updateMedicalRecord(String id, UpdateMedicalRecordRequest req) {
        // FOR UPDATE phải là lần đọc hồ sơ đầu tiên trong giao dịch, để thấy bản mới nhất sau khi chờ khóa
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(parseRecordId(id))
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (CLOSED_STATUSES.contains(record.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
        }
        if (req.getStatus() != null && !req.getStatus().equals(record.getStatus())) {
            validateTransition(record.getStatus(), req.getStatus());
        }

        // Gộp giá trị gửi lên với giá trị hiện tại, rồi kiểm tra trên giá trị cuối
        BigDecimal onsetWeeks = valueOrCurrent(req.getOnsetDurationWeeks(), record.getOnsetDurationWeeks());
        String insuranceOutcome = valueOrCurrent(req.getInsuranceTreatmentOutcome(), record.getInsuranceTreatmentOutcome());
        LocalDate startDate = valueOrCurrent(req.getOutpatientTreatmentStartDate(), record.getOutpatientTreatmentStartDate());
        LocalDate endDate = valueOrCurrent(req.getOutpatientTreatmentEndDate(), record.getOutpatientTreatmentEndDate());
        String familyHistory = valueOrCurrent(req.getFamilyHistory(), record.getFamilyHistory());
        String familyHistoryDescription = resolveFamilyHistoryDescription(familyHistory,
                valueOrCurrent(req.getFamilyHistoryDescription(), record.getFamilyHistoryDescription()));
        validateOnsetWeeks(onsetWeeks);
        validateInsuranceOutcome(insuranceOutcome);
        validateTreatmentDates(startDate, endDate);
        OffsetDateTime firstVisitAt = req.getFirstVisitAt() != null
                ? parseTimestamp(req.getFirstVisitAt())
                : record.getFirstVisitAt();

        record.setStatus(valueOrCurrent(req.getStatus(), record.getStatus()));
        record.setReferringFacilityDiagnosis(
                valueOrCurrent(req.getReferringFacilityDiagnosis(), record.getReferringFacilityDiagnosis()));
        record.setInitialClinicDiagnosis(valueOrCurrent(req.getInitialClinicDiagnosis(), record.getInitialClinicDiagnosis()));
        record.setOnsetDurationWeeks(onsetWeeks);
        record.setFirstVisitAt(firstVisitAt);
        record.setOutpatientTreatmentStartDate(startDate);
        record.setOutpatientTreatmentEndDate(endDate);
        record.setInsuranceTreatmentOutcome(insuranceOutcome);
        record.setFamilyHistory(familyHistory);
        record.setFamilyHistoryDescription(familyHistoryDescription);
        record.setTreatedBeforeVisit(valueOrCurrent(req.getTreatedBeforeVisit(), record.getTreatedBeforeVisit()));

        MedicalRecord saved;
        try {
            saved = medicalRecordRepository.saveAndFlush(record);
        } catch (DataIntegrityViolationException ex) {
            throw translateWriteError(ex);
        }

        log.info("Sửa hồ sơ bệnh án: recordId={}, trangThai={}, taiKhoan={}",
                saved.getMedicalRecordId(), saved.getStatus(), SecurityUtils.getCurrentUserId());
        return new MedicalRecordResponse(saved);
    }

    @Override
    @Transactional
    public void deleteMedicalRecord(String id) {
        // Request đến sau một lần xóa đã commit thì FOR UPDATE không thấy dòng nào và nhận 4001
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(parseRecordId(id))
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        Long recordId = record.getMedicalRecordId();

        try {
            if (TYPE_ACUTE.equals(record.getMedicalRecordType())) {
                acuteRecordRepository.findById(recordId).ifPresent(acuteRecordRepository::delete);
            } else {
                chronicRecordRepository.findById(recordId).ifPresent(chronicRecordRepository::delete);
            }
            medicalRecordRepository.delete(record);
            // Phải flush qua repository để lỗi khóa ngoại được đổi thành DataIntegrityViolationException ngay tại đây
            medicalRecordRepository.flush();
        } catch (DataIntegrityViolationException ex) {
            if (MedicalRecordSqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.MEDICAL_RECORD_HAS_CHILD_DATA);
            }
            throw ex;
        }

        log.info("Xóa hồ sơ bệnh án: recordId={}, taiKhoan={}", recordId, SecurityUtils.getCurrentUserId());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ExamHistoryResponse> getPatientRecordHistory(String patientIdValue) {
        Long patientId = parsePatientId(patientIdValue);
        if (!patientRepository.existsById(patientId)) {
            throw patientNotFound();
        }
        // Kiểm tra 3007 trước 3106, theo đúng thứ tự của đặc tả
        if (SecurityUtils.hasRole("BENH_NHAN")) {
            recordOwnerCheck.assertCurrentPatientOwns(patientId);
        }
        return medicalVisitRepository.findHistoryByPatientId(patientId).stream()
                .map(ExamHistoryResponse::from)
                .toList();
    }

    private void validateTransition(String current, String target) {
        if (!ALL_STATUSES.contains(target)) {
            throw invalidValue();
        }
        if (STATUS_CANCELLED.equals(target)) {
            return;
        }
        if (FORWARD_ORDER.get(target) < FORWARD_ORDER.get(current)) {
            throw invalidValue();
        }
    }

    private void validateOnsetWeeks(BigDecimal weeks) {
        if (weeks == null) {
            return;
        }
        if (weeks.signum() < 0 || weeks.compareTo(ONSET_WEEKS_LIMIT) >= 0 || weeks.stripTrailingZeros().scale() > 2) {
            throw invalidValue();
        }
    }

    private void validateInsuranceOutcome(String outcome) {
        if (outcome != null && !INSURANCE_OUTCOMES.contains(outcome)) {
            throw invalidValue();
        }
    }

    private void validateTreatmentDates(LocalDate start, LocalDate end) {
        if (start != null && end != null && end.isBefore(start)) {
            throw invalidValue();
        }
    }

    // Ghi chú tiền sử gia đình chỉ tồn tại khi tien_su_gia_dinh = Co, còn lại luôn là NULL
    private String resolveFamilyHistoryDescription(String familyHistory, String description) {
        if (familyHistory == null) {
            return null;
        }
        if (!FAMILY_HISTORY_VALUES.contains(familyHistory)) {
            throw invalidValue();
        }
        if (!FAMILY_HISTORY_YES.equals(familyHistory)) {
            return null;
        }
        if (description == null || description.isBlank()) {
            throw invalidValue();
        }
        return description;
    }

    private OffsetDateTime parseTimestamp(String value) {
        if (value == null) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value, JsonValues.TIMESTAMP_FORMAT);
        } catch (DateTimeParseException ex) {
            throw invalidValue();
        }
    }

    private AppException translateWriteError(DataIntegrityViolationException ex) {
        if (MedicalRecordSqlErrors.isInvalidValue(ex)) {
            log.warn("Dữ liệu hồ sơ bệnh án vi phạm ràng buộc database: {}", ex.getMostSpecificCause().getMessage());
            return invalidValue();
        }
        throw ex;
    }

    private Long parseRecordId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND);
        }
    }

    private Long parsePatientId(String patientId) {
        try {
            return Long.parseLong(patientId);
        } catch (NumberFormatException ex) {
            throw patientNotFound();
        }
    }

    private AppException patientNotFound() {
        return new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND, "Patient not found");
    }

    private static AppException invalidValue() {
        return new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static <T> T valueOrCurrent(T sent, T current) {
        return sent != null ? sent : current;
    }
}
