package com.example.drmayday.features.uas7.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.DailyUasLog;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.Uas7TrackingCycle;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.uas7.dto.CreateUas7CycleRequest;
import com.example.drmayday.features.uas7.dto.CreateUas7ScoreRequest;
import com.example.drmayday.features.uas7.dto.Uas7CycleDetailResponse;
import com.example.drmayday.features.uas7.dto.Uas7CycleResponse;
import com.example.drmayday.features.uas7.dto.Uas7EligibilityResponse;
import com.example.drmayday.features.uas7.dto.Uas7ScoreResponse;
import com.example.drmayday.features.uas7.dto.UpdateUas7CycleStatusRequest;
import com.example.drmayday.features.uas7.service.Uas7Service;
import com.example.drmayday.repository.ChronicUrticariaRecordRepository;
import com.example.drmayday.repository.DailyUasLogRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.Uas7TrackingCycleRepository;
import com.example.drmayday.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class Uas7ServiceImpl implements Uas7Service {

    private static final String RECORD_TYPE_CHRONIC = "MAN";
    private static final Set<String> TERMINAL_RECORD_STATUSES = Set.of("completed", "cancelled");
    private static final String CYCLE_STATUS_ACTIVE = "DANG_THEO_DOI";
    private static final String CYCLE_STATUS_COMPLETED = "HOAN_THANH";
    private static final String CYCLE_STATUS_CANCELLED = "HUY";
    private static final Set<String> VALID_CYCLE_STATUSES =
            Set.of(CYCLE_STATUS_ACTIVE, CYCLE_STATUS_COMPLETED, CYCLE_STATUS_CANCELLED);
    private static final String DAY_STATUS_VALID = "HOP_LE";
    private static final int MEDICATION_STATUS_MAX_LENGTH = 50;

    private final MedicalRecordRepository medicalRecordRepository;
    private final ChronicUrticariaRecordRepository chronicUrticariaRecordRepository;
    private final Uas7TrackingCycleRepository cycleRepository;
    private final DailyUasLogRepository dailyUasLogRepository;
    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public Uas7EligibilityResponse checkEligibility() {
        Long patientId = resolveCurrentPatientId();
        MedicalRecord activeRecord = medicalRecordRepository
                .findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
                        patientId, RECORD_TYPE_CHRONIC, TERMINAL_RECORD_STATUSES)
                .filter(record -> chronicUrticariaRecordRepository.existsById(record.getMedicalRecordId()))
                .orElseThrow(() -> new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD));

        LocalDate today = LocalDate.now();
        Uas7TrackingCycle activeCycle = cycleRepository
                .findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        patientId, CYCLE_STATUS_ACTIVE, today, today)
                .orElse(null);

        Integer currentDayInCycle = activeCycle != null
                ? (int) ChronoUnit.DAYS.between(activeCycle.getStartDate(), today) + 1
                : null;

        DailyUasLog todayLog = dailyUasLogRepository.findByPatientIdAndScoreDate(patientId, today).orElse(null);

        return Uas7EligibilityResponse.builder()
                .eligible(true)
                .activeRecordId(String.valueOf(activeRecord.getMedicalRecordId()))
                .activeCycleId(activeCycle != null ? String.valueOf(activeCycle.getTrackingCycleId()) : null)
                .currentDayInCycle(currentDayInCycle)
                .scoredToday(todayLog != null)
                .todayScoreId(todayLog != null ? String.valueOf(todayLog.getDailyUasLogId()) : null)
                .build();
    }

    @Override
    @Transactional
    public Uas7ScoreResponse createScore(CreateUas7ScoreRequest request) {
        if (request == null || request.getRecordId() == null
                || request.getHssScore() == null || request.getIssScore() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        int hss = request.getHssScore();
        int iss = request.getIssScore();
        if (hss < 0 || hss > 3 || iss < 0 || iss > 3) {
            throw new AppException(ErrorCode.UAS7_SCORE_OUT_OF_RANGE);
        }

        String medicationStatus = trimToNull(request.getTrangThaiDungThuoc());
        if (medicationStatus != null && medicationStatus.length() > MEDICATION_STATUS_MAX_LENGTH) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "trang_thai_dung_thuoc không được dài quá 50 ký tự");
        }

        Long patientId = resolveCurrentPatientId();
        MedicalRecord record = medicalRecordRepository.findById(request.getRecordId()).orElse(null);
        if (record == null
                || !patientId.equals(record.getPatientId())
                || !RECORD_TYPE_CHRONIC.equals(record.getMedicalRecordType())
                || TERMINAL_RECORD_STATUSES.contains(record.getStatus())
                || !chronicUrticariaRecordRepository.existsById(record.getMedicalRecordId())) {
            throw new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD);
        }

        LocalDate scoreDate = request.getDate() != null ? request.getDate() : LocalDate.now();
        if (dailyUasLogRepository.existsByPatientIdAndScoreDate(patientId, scoreDate)) {
            throw new AppException(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY);
        }

        Uas7TrackingCycle cycle = cycleRepository
                .findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                        patientId, CYCLE_STATUS_ACTIVE, scoreDate, scoreDate)
                .orElseGet(() -> createAutomaticCycle(patientId, scoreDate));

        short cycleDay = (short) (ChronoUnit.DAYS.between(cycle.getStartDate(), scoreDate) + 1);
        OffsetDateTime now = OffsetDateTime.now();

        DailyUasLog logEntry = DailyUasLog.builder()
                .trackingCycleId(cycle.getTrackingCycleId())
                .chronicMedicalRecordId(record.getMedicalRecordId())
                .patientId(patientId)
                .scoreDate(scoreDate)
                .cycleDay(cycleDay)
                .whealScoreHss((short) hss)
                .itchScoreIss((short) iss)
                .medicationAdherenceStatus(medicationStatus)
                .dayStatus(DAY_STATUS_VALID)
                .updatedAt(now)
                .build();

        try {
            DailyUasLog saved = dailyUasLogRepository.saveAndFlush(logEntry);
            return toScoreResponse(saved);
        } catch (DataIntegrityViolationException ex) {
            if (hasConstraint(ex, "uq_nhatkyuas_benhnhan_ngay")) {
                throw new AppException(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY);
            }
            throw ex;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Uas7ScoreResponse> getPatientScores(String patientIdStr,
                                                    LocalDate fromDate,
                                                    LocalDate toDate,
                                                    String cycleIdStr) {
        Long patientId = parseId(patientIdStr, ErrorCode.USER_ACCOUNT_NOT_FOUND);
        requirePatientExists(patientId);
        checkPatientOwnership(patientId, true);

        if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, "from_date phải nhỏ hơn hoặc bằng to_date");
        }

        Long cycleId = null;
        if (cycleIdStr != null && !cycleIdStr.isBlank()) {
            cycleId = parseId(cycleIdStr, ErrorCode.INVALID_PARAMETER_VALUE);
        }

        return dailyUasLogRepository.findByPatientIdFiltered(patientId, fromDate, toDate, cycleId).stream()
                .map(this::toScoreResponse)
                .toList();
    }

    @Override
    @Transactional
    public Uas7CycleResponse createCycle(String patientIdStr, CreateUas7CycleRequest request) {
        Long patientId = parseId(patientIdStr, ErrorCode.USER_ACCOUNT_NOT_FOUND);
        requirePatientExists(patientId);
        checkPatientOwnership(patientId, false);

        if (request == null || request.getNgayBatDau() == null) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        LocalDate start = request.getNgayBatDau();
        LocalDate end = start.plusDays(6);
        if (cycleRepository.existsOverlappingActive(patientId, start, end)) {
            throw new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE);
        }

        OffsetDateTime now = OffsetDateTime.now();
        Uas7TrackingCycle saved = cycleRepository.saveAndFlush(Uas7TrackingCycle.builder()
                .patientId(patientId)
                .startDate(start)
                .endDate(end)
                .cycleStatus(CYCLE_STATUS_ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());

        return toCycleResponse(saved, 0, false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Uas7CycleResponse> getPatientCycles(String patientIdStr) {
        Long patientId = parseId(patientIdStr, ErrorCode.USER_ACCOUNT_NOT_FOUND);
        requirePatientExists(patientId);
        checkPatientOwnership(patientId, true);

        List<Uas7TrackingCycle> cycles = cycleRepository
                .findByPatientIdOrderByStartDateDescTrackingCycleIdDesc(patientId);
        if (cycles.isEmpty()) {
            return List.of();
        }

        List<Long> cycleIds = cycles.stream().map(Uas7TrackingCycle::getTrackingCycleId).toList();
        Map<Long, Integer> validDaysByCycle = new HashMap<>();
        for (Object[] row : dailyUasLogRepository.countByTrackingCycleIdsAndDayStatus(cycleIds, DAY_STATUS_VALID)) {
            Long id = ((Number) row[0]).longValue();
            Integer count = ((Number) row[1]).intValue();
            validDaysByCycle.put(id, count);
        }

        return cycles.stream()
                .map(c -> toCycleResponse(c, validDaysByCycle.getOrDefault(c.getTrackingCycleId(), 0), false))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Uas7CycleDetailResponse getCycleDetail(String cycleIdStr) {
        Long cycleId = parseId(cycleIdStr, ErrorCode.UAS7_CYCLE_NOT_FOUND);
        Uas7TrackingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new AppException(ErrorCode.UAS7_CYCLE_NOT_FOUND));
        checkPatientOwnership(cycle.getPatientId(), true);

        List<DailyUasLog> logs = dailyUasLogRepository.findByTrackingCycleIdOrderByScoreDateAsc(cycleId);
        int validDays = (int) logs.stream()
                .filter(logEntry -> DAY_STATUS_VALID.equals(logEntry.getDayStatus()))
                .count();

        return Uas7CycleDetailResponse.builder()
                .cycleId(String.valueOf(cycle.getTrackingCycleId()))
                .patientId(String.valueOf(cycle.getPatientId()))
                .ngayBatDau(cycle.getStartDate())
                .ngayKetThuc(cycle.getEndDate())
                .trangThaiChuKy(cycle.getCycleStatus())
                .soNgayHopLe(validDays)
                .thoiDiemTao(cycle.getCreatedAt())
                .logs(logs.stream().map(this::toScoreResponse).toList())
                .build();
    }

    @Override
    @Transactional
    public Uas7CycleResponse updateCycleStatus(String cycleIdStr, UpdateUas7CycleStatusRequest request) {
        Long cycleId = parseId(cycleIdStr, ErrorCode.UAS7_CYCLE_NOT_FOUND);
        Uas7TrackingCycle cycle = cycleRepository.findById(cycleId)
                .orElseThrow(() -> new AppException(ErrorCode.UAS7_CYCLE_NOT_FOUND));
        checkPatientOwnership(cycle.getPatientId(), false);

        if (request == null || request.getTrangThaiChuKy() == null || request.getTrangThaiChuKy().isBlank()) {
            throw new AppException(ErrorCode.MISSING_REQUIRED_PARAMETER);
        }

        String nextStatus = request.getTrangThaiChuKy().trim();
        if (!VALID_CYCLE_STATUSES.contains(nextStatus)) {
            throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE,
                    "trang_thai_chu_ky không hợp lệ, chỉ chấp nhận: DANG_THEO_DOI, HOAN_THANH, HUY");
        }

        if (!CYCLE_STATUS_ACTIVE.equals(cycle.getCycleStatus()) || CYCLE_STATUS_ACTIVE.equals(nextStatus)) {
            throw new AppException(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION);
        }

        cycle.setCycleStatus(nextStatus);
        cycle.setUpdatedAt(OffsetDateTime.now());
        Uas7TrackingCycle saved = cycleRepository.saveAndFlush(cycle);
        int validDays = (int) dailyUasLogRepository.countByTrackingCycleIdAndDayStatus(cycleId, DAY_STATUS_VALID);

        return toCycleResponse(saved, validDays, true);
    }

    private Uas7TrackingCycle createAutomaticCycle(Long patientId, LocalDate startDate) {
        LocalDate endDate = startDate.plusDays(6);
        if (cycleRepository.existsOverlappingActive(patientId, startDate, endDate)) {
            throw new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE);
        }
        OffsetDateTime now = OffsetDateTime.now();
        return cycleRepository.saveAndFlush(Uas7TrackingCycle.builder()
                .patientId(patientId)
                .startDate(startDate)
                .endDate(endDate)
                .cycleStatus(CYCLE_STATUS_ACTIVE)
                .createdAt(now)
                .updatedAt(now)
                .build());
    }

    private Long resolveCurrentPatientId() {
        Long currentUserId = SecurityUtils.getCurrentUserId();
        return userAccountRepository.findById(currentUserId)
                .map(UserAccount::getPerson)
                .map(Person::getPersonId)
                .orElse(currentUserId);
    }

    private void checkPatientOwnership(Long targetPatientId, boolean allowClinicalStaff) {
        if (SecurityUtils.hasRole("ADMIN")) {
            return;
        }
        if (allowClinicalStaff && (SecurityUtils.hasRole("BAC_SI")
                || SecurityUtils.hasRole("DOCTOR")
                || SecurityUtils.hasRole("DIEU_DUONG")
                || SecurityUtils.hasRole("NURSE"))) {
            return;
        }
        Long currentPatientId = resolveCurrentPatientId();
        if (!targetPatientId.equals(currentPatientId)) {
            throw new AppException(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA);
        }
    }

    private void requirePatientExists(Long patientId) {
        if (!patientRepository.existsById(patientId)) {
            throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
        }
    }

    private Long parseId(String value, ErrorCode errorCode) {
        if (value == null || value.isBlank()) {
            throw new AppException(errorCode);
        }
        try {
            return Long.parseLong(value.trim());
        } catch (NumberFormatException ex) {
            throw new AppException(errorCode);
        }
    }

    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private boolean hasConstraint(Throwable throwable, String constraintName) {
        for (Throwable cause = throwable; cause != null; cause = cause.getCause()) {
            String message = cause.getMessage();
            if (message != null && message.toLowerCase(Locale.ROOT).contains(constraintName)) {
                return true;
            }
        }
        return false;
    }

    private Uas7ScoreResponse toScoreResponse(DailyUasLog logEntry) {
        int hss = logEntry.getWhealScoreHss() != null ? logEntry.getWhealScoreHss().intValue() : 0;
        int iss = logEntry.getItchScoreIss() != null ? logEntry.getItchScoreIss().intValue() : 0;
        int total = logEntry.getDailyUasScore() != null ? logEntry.getDailyUasScore().intValue() : (hss + iss);

        return Uas7ScoreResponse.builder()
                .uas7Id(String.valueOf(logEntry.getDailyUasLogId()))
                .recordId(logEntry.getChronicMedicalRecordId() != null
                        ? String.valueOf(logEntry.getChronicMedicalRecordId())
                        : null)
                .cycleId(logEntry.getTrackingCycleId() != null
                        ? String.valueOf(logEntry.getTrackingCycleId())
                        : null)
                .hssScore(hss)
                .issScore(iss)
                .totalScore(total)
                .ngayChamDiem(logEntry.getScoreDate())
                .ngayThuMayTrongChuKy(logEntry.getCycleDay() != null ? logEntry.getCycleDay().intValue() : null)
                .trangThaiNgay(logEntry.getDayStatus())
                .trangThaiDungThuoc(logEntry.getMedicationAdherenceStatus())
                .createdAt(logEntry.getUpdatedAt())
                .build();
    }

    private Uas7CycleResponse toCycleResponse(Uas7TrackingCycle cycle, int validDays, boolean includeUpdatedAt) {
        return Uas7CycleResponse.builder()
                .cycleId(String.valueOf(cycle.getTrackingCycleId()))
                .patientId(String.valueOf(cycle.getPatientId()))
                .ngayBatDau(cycle.getStartDate())
                .ngayKetThuc(cycle.getEndDate())
                .trangThaiChuKy(cycle.getCycleStatus())
                .soNgayHopLe(validDays)
                .thoiDiemTao(includeUpdatedAt ? null : cycle.getCreatedAt())
                .thoiDiemCapNhat(includeUpdatedAt ? cycle.getUpdatedAt() : null)
                .build();
    }
}
