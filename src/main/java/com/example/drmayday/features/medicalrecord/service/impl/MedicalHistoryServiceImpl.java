package com.example.drmayday.features.medicalrecord.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.AllergyOnsetContext;
import com.example.drmayday.entity.InfectionStatus;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.PreviousDiseaseEpisode;
import com.example.drmayday.features.medicalrecord.dto.AllergyContextResponse;
import com.example.drmayday.features.medicalrecord.dto.InfectionStatusResponse;
import com.example.drmayday.features.medicalrecord.dto.PreviousEpisodesResponse;
import com.example.drmayday.features.medicalrecord.dto.UpsertAllergyContextRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertInfectionStatusRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertPreviousEpisodesRequest;
import com.example.drmayday.features.medicalrecord.dto.UpsertSectionRequest;
import com.example.drmayday.features.medicalrecord.service.MedicalHistoryService;
import com.example.drmayday.repository.AllergyOnsetContextRepository;
import com.example.drmayday.repository.InfectionStatusRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PreviousDiseaseEpisodeRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

@Service
public class MedicalHistoryServiceImpl implements MedicalHistoryService {

    private static final Logger log = LoggerFactory.getLogger(MedicalHistoryServiceImpl.class);

    private static final String TYPE_ACUTE = "CAP";
    private static final String TYPE_CHRONIC = "MAN";
    private static final Set<String> CLOSED_STATUSES = Set.of("completed", "cancelled");

    // 'Khong' của đặc tả bị bỏ, vì nó trùng nghĩa với *_tung_bi khác true
    private static final Set<String> HISTORY_TYPES = Set.of("Phan ve", "Di ung");
    private static final Set<String> COURSE_PATTERNS = Set.of(
            "Lien tuc, hau nhu khong co giai doan het trieu chung",
            "Tung dot, giua cac dot het hoan toan",
            "Khong ro");
    private static final Set<String> MANIFESTATIONS = Set.of("San phu", "Phu mach", "San phu + phu mach", "Khac");

    private static final BigDecimal DECIMAL_LIMIT = new BigDecimal("1000000000");
    private static final int MAX_ONSET_AGE = 150;
    private static final int MAX_COUNT = 10_000;

    private final MedicalRecordRepository medicalRecordRepository;
    private final AllergyOnsetContextRepository allergyContextRepository;
    private final PreviousDiseaseEpisodeRepository previousEpisodeRepository;
    private final InfectionStatusRepository infectionStatusRepository;

    public MedicalHistoryServiceImpl(MedicalRecordRepository medicalRecordRepository,
                                     AllergyOnsetContextRepository allergyContextRepository,
                                     PreviousDiseaseEpisodeRepository previousEpisodeRepository,
                                     InfectionStatusRepository infectionStatusRepository) {
        this.medicalRecordRepository = medicalRecordRepository;
        this.allergyContextRepository = allergyContextRepository;
        this.previousEpisodeRepository = previousEpisodeRepository;
        this.infectionStatusRepository = infectionStatusRepository;
    }

    @Override
    @Transactional
    public AllergyContextResponse upsertAllergyContext(UpsertAllergyContextRequest req) {
        Long recordId = lockWritableRecord(req, null);

        TriggerGroup food = TriggerGroup.of(req.getFoodSuspect(), req.getFoodHours(), req.getFoodReactedBefore(),
                req.getFoodHistoryType(), req.getFoodHistoryNote());
        TriggerGroup medication = TriggerGroup.of(req.getMedicationSuspect(), req.getMedicationHours(),
                req.getMedicationReactedBefore(), req.getMedicationHistoryType(), req.getMedicationHistoryNote());
        TriggerGroup insect = TriggerGroup.of(req.getInsectSuspect(), req.getInsectHours(), req.getInsectReactedBefore(),
                req.getInsectHistoryType(), req.getInsectHistoryNote());
        TriggerGroup other = TriggerGroup.of(req.getOtherSuspect(), req.getOtherHours(), req.getOtherReactedBefore(),
                req.getOtherHistoryType(), req.getOtherHistoryNote());

        AllergyOnsetContext existing = allergyContextRepository.findByMedicalRecordId(recordId).orElse(null);
        // Ghi đè trên chính entity đang được quản lý; tạo entity mới với id cũ sẽ chèn thêm dòng và đụng UNIQUE ma_ho_so
        AllergyOnsetContext row = existing != null ? existing
                : AllergyOnsetContext.builder().medicalRecordId(recordId).build();
        row.setSuspectedFoodTrigger(food.suspect());
        row.setFoodTriggerDuration(food.hours());
        row.setPreviousFoodReaction(food.reactedBefore());
        row.setFoodHistoryType(food.historyType());
        row.setFoodHistoryDescription(food.note());
        row.setSuspectedMedicationTrigger(medication.suspect());
        row.setMedicationTriggerDuration(medication.hours());
        row.setPreviousMedicationReaction(medication.reactedBefore());
        row.setMedicationHistoryType(medication.historyType());
        row.setMedicationHistoryDescription(medication.note());
        row.setSuspectedInsectTrigger(insect.suspect());
        row.setInsectTriggerDuration(insect.hours());
        row.setPreviousInsectReaction(insect.reactedBefore());
        row.setInsectHistoryType(insect.historyType());
        row.setInsectHistoryDescription(insect.note());
        row.setSuspectedOtherTrigger(other.suspect());
        row.setOtherDuration(other.hours());
        row.setPreviousOtherReaction(other.reactedBefore());
        row.setOtherHistoryType(other.historyType());
        row.setOtherHistoryDescription(other.note());

        AllergyOnsetContext saved = save(allergyContextRepository, row);
        log.info("Ghi hoàn cảnh khởi phát dị ứng: recordId={}, taoMoi={}, taiKhoan={}",
                recordId, existing == null, SecurityUtils.getCurrentUserId());
        return AllergyContextResponse.from(saved);
    }

    @Override
    @Transactional
    public PreviousEpisodesResponse upsertPreviousEpisodes(UpsertPreviousEpisodesRequest req) {
        Long recordId = lockWritableRecord(req, TYPE_CHRONIC);

        Short onsetAge = req.getOnsetAge();
        if (onsetAge != null && (onsetAge < 0 || onsetAge > MAX_ONSET_AGE)) {
            throw invalidValue();
        }
        Integer episodeCount = count(req.getPreviousEpisodeCount());
        Integer hospitalizationCount = count(req.getHospitalizationCount());
        // Cờ để trống được coi như false: chỉ cờ true mới cho phép số đếm lớn hơn 0
        if (!Boolean.TRUE.equals(req.getHadSimilarEpisodes()) && episodeCount != null && episodeCount > 0) {
            throw invalidValue();
        }
        if (!Boolean.TRUE.equals(req.getWasHospitalized()) && hospitalizationCount != null && hospitalizationCount > 0) {
            throw invalidValue();
        }
        String coursePattern = oneOf(req.getCoursePattern(), COURSE_PATTERNS);
        String manifestation = oneOf(req.getPreviousManifestation(), MANIFESTATIONS);
        BigDecimal months = decimal(req.getMonthsSinceLastEpisode());
        String course = text(req.getPreviousEpisodeCourse());

        PreviousDiseaseEpisode existing = previousEpisodeRepository.findByMedicalRecordId(recordId).orElse(null);
        PreviousDiseaseEpisode row = existing != null ? existing
                : PreviousDiseaseEpisode.builder().medicalRecordId(recordId).build();
        row.setPreviousSimilarEpisodes(req.getHadSimilarEpisodes());
        row.setOnsetAge(onsetAge);
        row.setPreviousEpisodeCount(episodeCount);
        row.setDiseaseCoursePattern(coursePattern);
        row.setPreviousEpisodeManifestation(manifestation);
        row.setPreviouslyHospitalized(req.getWasHospitalized());
        row.setHospitalizationCount(hospitalizationCount);
        row.setTimeSinceLastEpisode(months);
        row.setPreviousEpisodeCourse(course);

        PreviousDiseaseEpisode saved = save(previousEpisodeRepository, row);
        log.info("Ghi bệnh sử các đợt trước: recordId={}, taoMoi={}, taiKhoan={}",
                recordId, existing == null, SecurityUtils.getCurrentUserId());
        return PreviousEpisodesResponse.from(saved);
    }

    @Override
    @Transactional
    public InfectionStatusResponse upsertInfectionStatus(UpsertInfectionStatusRequest req) {
        Long recordId = lockWritableRecord(req, TYPE_ACUTE);

        BigDecimal respiratoryDays = decimal(req.getRespiratoryDays());
        BigDecimal respiratoryInterval = decimal(req.getRespiratoryIntervalDays());
        requireSymptomForTiming(respiratoryDays, respiratoryInterval, req.getRespiratoryCough(),
                req.getRespiratorySoreThroat(), req.getRespiratorySneezing(), req.getRespiratoryRunnyNose());
        BigDecimal urinaryDays = decimal(req.getUrinaryDays());
        BigDecimal urinaryInterval = decimal(req.getUrinaryIntervalDays());
        requireSymptomForTiming(urinaryDays, urinaryInterval, req.getUrinaryPainful(), req.getUrinaryBurning());
        BigDecimal skinDays = decimal(req.getSkinDays());
        BigDecimal skinInterval = decimal(req.getSkinIntervalDays());
        requireSymptomForTiming(skinDays, skinInterval, req.getSkinAcne(), req.getSkinBoils());
        // Nhóm "khác" không có cờ: khac_ghi_ro đóng vai trò triệu chứng
        String otherDescription = text(req.getOtherDescription());
        BigDecimal otherDays = decimal(req.getOtherDays());
        BigDecimal otherInterval = decimal(req.getOtherIntervalDays());
        if (otherDescription == null && (otherDays != null || otherInterval != null)) {
            throw invalidValue();
        }

        InfectionStatus existing = infectionStatusRepository.findByMedicalRecordId(recordId).orElse(null);
        InfectionStatus row = existing != null ? existing
                : InfectionStatus.builder().medicalRecordId(recordId).build();
        row.setRespiratoryCough(req.getRespiratoryCough());
        row.setRespiratorySoreThroat(req.getRespiratorySoreThroat());
        row.setRespiratorySneezing(req.getRespiratorySneezing());
        row.setRespiratoryRunnyNose(req.getRespiratoryRunnyNose());
        row.setRespiratoryDuration(respiratoryDays);
        row.setRespiratoryInterval(respiratoryInterval);
        row.setUrinaryPainfulUrination(req.getUrinaryPainful());
        row.setUrinaryBurningUrination(req.getUrinaryBurning());
        row.setUrinaryDuration(urinaryDays);
        row.setUrinaryInterval(urinaryInterval);
        row.setSkinAcne(req.getSkinAcne());
        row.setSkinBoils(req.getSkinBoils());
        row.setSkinDuration(skinDays);
        row.setSkinInterval(skinInterval);
        row.setOtherDescription(otherDescription);
        row.setOtherDuration(otherDays);
        row.setOtherInterval(otherInterval);

        InfectionStatus saved = save(infectionStatusRepository, row);
        log.info("Ghi tình trạng nhiễm trùng: recordId={}, taoMoi={}, taiKhoan={}",
                recordId, existing == null, SecurityUtils.getCurrentUserId());
        return InfectionStatusResponse.from(saved);
    }

    /**
     * Thứ tự kiểm tra của spec 0003: trường lạ (2003), parse record_id (2002), khóa hồ sơ (4001),
     * loại hồ sơ (4008), hồ sơ đóng (4003). Các luật giá trị chạy sau hàm này.
     */
    private Long lockWritableRecord(UpsertSectionRequest req, String requiredType) {
        if (req.hasUnknownFields()) {
            log.warn("Body bệnh sử có trường lạ: {}", req.unknownFields());
            throw invalidValue();
        }
        Long recordId = parseBodyRecordId(req.getRecordId());
        // FOR UPDATE phải là lần đọc hồ sơ đầu tiên trong giao dịch. Nó xếp hàng mọi lần ghi trên cùng hồ sơ,
        // nên hai upsert đầu tiên không cùng chèn, và upsert chờ lúc hồ sơ bị hủy sẽ thấy trạng thái mới
        MedicalRecord record = medicalRecordRepository.findByIdForUpdate(recordId)
                .orElseThrow(() -> new AppException(ErrorCode.MEDICAL_RECORD_NOT_FOUND));
        if (requiredType != null && !requiredType.equals(record.getMedicalRecordType())) {
            throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH);
        }
        if (CLOSED_STATUSES.contains(record.getStatus())) {
            throw new AppException(ErrorCode.MEDICAL_RECORD_ALREADY_COMPLETED);
        }
        return recordId;
    }

    // record_id trong body sai kiểu là lỗi của client (2002), khác với id trên đường dẫn (4001)
    private static Long parseBodyRecordId(String value) {
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_TYPE);
        }
    }

    private <T> T save(JpaRepository<T, Long> repository, T row) {
        try {
            return repository.saveAndFlush(row);
        } catch (DataIntegrityViolationException ex) {
            if (MedicalRecordSqlErrors.isInvalidValue(ex)) {
                throw invalidValue();
            }
            // Khóa ngoại tới subtype hỏng, tức hồ sơ thiếu dòng benhanmaydayman hoặc benhanmaydaycap
            if (MedicalRecordSqlErrors.isForeignKeyViolation(ex)) {
                throw new AppException(ErrorCode.RECORD_TYPE_MISMATCH);
            }
            throw ex;
        }
    }

    private static void requireSymptomForTiming(BigDecimal days, BigDecimal interval, Boolean... symptoms) {
        if (days == null && interval == null) {
            return;
        }
        for (Boolean symptom : symptoms) {
            if (Boolean.TRUE.equals(symptom)) {
                return;
            }
        }
        throw invalidValue();
    }

    // Bỏ khoảng trắng hai đầu; null giữ null; chuỗi rỗng sau khi bỏ khoảng trắng là 2003
    static String text(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            throw invalidValue();
        }
        return trimmed;
    }

    private static String oneOf(String value, Set<String> allowed) {
        String trimmed = text(value);
        if (trimmed != null && !allowed.contains(trimmed)) {
            throw invalidValue();
        }
        return trimmed;
    }

    static BigDecimal decimal(BigDecimal value) {
        if (value == null) {
            return null;
        }
        if (value.signum() < 0 || value.compareTo(DECIMAL_LIMIT) >= 0
                || Math.max(value.stripTrailingZeros().scale(), 0) > 2) {
            throw invalidValue();
        }
        return value;
    }

    private static Integer count(Integer value) {
        if (value != null && (value < 0 || value > MAX_COUNT)) {
            throw invalidValue();
        }
        return value;
    }

    private static AppException invalidValue() {
        return new AppException(ErrorCode.INVALID_PARAMETER_VALUE);
    }

    /**
     * Một nhóm yếu tố nghi ngờ của API 43, đã kiểm tra: ghi chú chỉ khi từng bị, loại tiền sử chỉ khi từng bị,
     * thời gian chỉ khi có yếu tố nghi ngờ.
     */
    private record TriggerGroup(String suspect, BigDecimal hours, Boolean reactedBefore, String historyType, String note) {

        static TriggerGroup of(String suspect, BigDecimal hours, Boolean reactedBefore, String historyType, String note) {
            String cleanSuspect = text(suspect);
            String cleanType = text(historyType);
            String cleanNote = text(note);
            BigDecimal cleanHours = decimal(hours);
            if (Boolean.TRUE.equals(reactedBefore)) {
                if (cleanNote == null || (cleanType != null && !HISTORY_TYPES.contains(cleanType))) {
                    throw invalidValue();
                }
            } else if (cleanNote != null || cleanType != null) {
                throw invalidValue();
            }
            if (cleanHours != null && cleanSuspect == null) {
                throw invalidValue();
            }
            return new TriggerGroup(cleanSuspect, cleanHours, reactedBefore, cleanType, cleanNote);
        }
    }
}
