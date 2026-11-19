package com.example.drmayday.features.uas7.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.SecurityUtils;
import com.example.drmayday.entity.DailyUasLog;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.Uas7TrackingCycle;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.uas7.dto.*;
import com.example.drmayday.features.uas7.service.Uas7Service;
import com.example.drmayday.repository.DailyUasLogRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.Uas7TrackingCycleRepository;
import com.example.drmayday.repository.UserAccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class Uas7ServiceImpl implements Uas7Service {
    private static final Set<String> TERMINAL_RECORD_STATUSES = Set.of("completed", "cancelled");
    private static final Set<String> VALID_CYCLE_STATUSES = Set.of("DANG_THEO_DOI", "HOAN_THANH", "HUY");

    private final Uas7TrackingCycleRepository cycleRepository;
    private final DailyUasLogRepository logRepository;
    private final MedicalRecordRepository medicalRecordRepository;
    private final PatientRepository patientRepository;
    private final UserAccountRepository userAccountRepository;

    @Override
    @Transactional(readOnly = true)
    public Uas7EligibilityResponse checkEligibility() {
        Long patientId = currentPatientId();
        MedicalRecord record = activeChronicRecord(patientId).orElseThrow(() -> new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD));
        LocalDate today = LocalDate.now();
        Uas7TrackingCycle cycle = cycleRepository.findActiveForDate(patientId, today).orElse(null);
        DailyUasLog todayLog = logRepository.findByPatientIdAndScoreDate(patientId, today).orElse(null);
        Integer day = cycle == null ? null : (int) (today.toEpochDay() - cycle.getStartDate().toEpochDay() + 1);
        return Uas7EligibilityResponse.builder()
                .eligible(cycle != null)
                .activeRecordId(String.valueOf(record.getMedicalRecordId()))
                .activeCycleId(cycle == null ? null : String.valueOf(cycle.getTrackingCycleId()))
                .currentDayInCycle(day)
                .scoredToday(todayLog != null)
                .todayScoreId(todayLog == null ? null : String.valueOf(todayLog.getDailyUasLogId()))
                .build();
    }

    @Override
    @Transactional
    public Uas7ScoreResponse createScore(CreateUas7ScoreRequest request) {
        Long patientId = currentPatientId();
        Long recordId = parseId(request.getRecordId(), "record_id");
        MedicalRecord record = activeChronicRecord(patientId).filter(r -> r.getMedicalRecordId().equals(recordId))
                .orElseThrow(() -> new AppException(ErrorCode.UAS7_NO_ACTIVE_RECORD));
        LocalDate date = request.getDate() == null ? LocalDate.now() : request.getDate();
        if (logRepository.findByPatientIdAndScoreDate(patientId, date).isPresent()) {
            throw new AppException(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY);
        }
        if (request.getHssScore() < 0 || request.getHssScore() > 3 || request.getIssScore() < 0 || request.getIssScore() > 3) {
            throw new AppException(ErrorCode.UAS7_SCORE_OUT_OF_RANGE);
        }
        Uas7TrackingCycle cycle = cycleRepository.findActiveForDate(patientId, date).orElseGet(() -> {
            LocalDate end = date.plusDays(6);
            if (cycleRepository.existsOverlappingActive(patientId, date, end)) {
                throw new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE);
            }
            OffsetDateTime now = OffsetDateTime.now();
            return cycleRepository.save(Uas7TrackingCycle.builder().patientId(patientId).startDate(date).endDate(end)
                    .cycleStatus("DANG_THEO_DOI").createdAt(now).updatedAt(now).build());
        });
        short cycleDay = (short) (date.toEpochDay() - cycle.getStartDate().toEpochDay() + 1);
        DailyUasLog saved = logRepository.save(DailyUasLog.builder().trackingCycleId(cycle.getTrackingCycleId())
                .chronicMedicalRecordId(record.getMedicalRecordId()).patientId(patientId).scoreDate(date)
                .cycleDay(cycleDay).whealScoreHss(request.getHssScore().shortValue()).itchScoreIss(request.getIssScore().shortValue())
                .medicationAdherenceStatus(request.getMedicationAdherenceStatus()).dayStatus("HOP_LE").updatedAt(OffsetDateTime.now()).build());
        return toScore(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Uas7ScoreResponse> getPatientScores(String patientIdValue, LocalDate from, LocalDate to, String cycleIdValue) {
        Long patientId = parseId(patientIdValue, "patient_id");
        requirePatient(patientId);
        List<DailyUasLog> logs;
        if (cycleIdValue != null && !cycleIdValue.isBlank()) {
            logs = logRepository.findByPatientIdAndTrackingCycleIdOrderByScoreDateDesc(patientId, parseId(cycleIdValue, "cycle_id"));
        } else if (from != null && to != null) {
            logs = logRepository.findByPatientIdAndScoreDateBetweenOrderByScoreDateDesc(patientId, from, to);
        } else {
            logs = logRepository.findByPatientIdOrderByScoreDateDesc(patientId);
        }
        return logs.stream().map(this::toScore).toList();
    }

    @Override
    @Transactional
    public Uas7CycleResponse createCycle(String patientIdValue, CreateUas7CycleRequest request) {
        Long patientId = parseId(patientIdValue, "patient_id");
        requirePatient(patientId);
        LocalDate start = request.getStartDate();
        LocalDate end = start.plusDays(6);
        if (cycleRepository.existsOverlappingActive(patientId, start, end)) {
            throw new AppException(ErrorCode.UAS7_OVERLAPPING_CYCLE);
        }
        OffsetDateTime now = OffsetDateTime.now();
        return toCycle(cycleRepository.save(Uas7TrackingCycle.builder().patientId(patientId).startDate(start).endDate(end)
                .cycleStatus("DANG_THEO_DOI").createdAt(now).updatedAt(now).build()));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Uas7CycleResponse> getPatientCycles(String patientIdValue) {
        Long patientId = parseId(patientIdValue, "patient_id");
        requirePatient(patientId);
        return cycleRepository.findByPatientIdOrderByStartDateDesc(patientId).stream().map(this::toCycle).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Uas7CycleDetailResponse getCycleDetail(String cycleIdValue) {
        Uas7TrackingCycle cycle = findCycle(cycleIdValue);
        List<DailyUasLog> logs = logRepository.findByTrackingCycleIdOrderByScoreDateAsc(cycle.getTrackingCycleId());
        return Uas7CycleDetailResponse.builder().cycleId(String.valueOf(cycle.getTrackingCycleId())).patientId(String.valueOf(cycle.getPatientId()))
                .startDate(cycle.getStartDate()).endDate(cycle.getEndDate()).cycleStatus(cycle.getCycleStatus())
                .validDays(validDays(cycle.getTrackingCycleId())).createdAt(cycle.getCreatedAt()).logs(logs.stream().map(this::toScore).toList()).build();
    }

    @Override
    @Transactional
    public Uas7CycleResponse updateCycleStatus(String cycleIdValue, UpdateUas7CycleStatusRequest request) {
        Uas7TrackingCycle cycle = findCycle(cycleIdValue);
        String next = request.getCycleStatus();
        if (!VALID_CYCLE_STATUSES.contains(next) || !"DANG_THEO_DOI".equals(cycle.getCycleStatus()) || "DANG_THEO_DOI".equals(next)) {
            throw new AppException(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION);
        }
        cycle.setCycleStatus(next);
        cycle.setUpdatedAt(OffsetDateTime.now());
        return toCycle(cycleRepository.save(cycle));
    }

    private java.util.Optional<MedicalRecord> activeChronicRecord(Long patientId) {
        return medicalRecordRepository.findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(patientId, "MAN", TERMINAL_RECORD_STATUSES);
    }

    private Long currentPatientId() {
        UserAccount account = userAccountRepository.findById(SecurityUtils.getCurrentUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND));
        return account.getPerson().getPersonId();
    }

    private void requirePatient(Long patientId) {
        if (!patientRepository.existsById(patientId)) throw new AppException(ErrorCode.USER_ACCOUNT_NOT_FOUND);
    }

    private Uas7TrackingCycle findCycle(String value) {
        return cycleRepository.findById(parseId(value, "cycle_id")).orElseThrow(() -> new AppException(ErrorCode.UAS7_CYCLE_NOT_FOUND));
    }

    private int validDays(Long cycleId) { return (int) logRepository.countByTrackingCycleIdAndDayStatus(cycleId, "HOP_LE"); }
    private Long parseId(String value, String name) {
        try { return Long.valueOf(value); } catch (Exception e) { throw new AppException(ErrorCode.INVALID_PARAMETER_VALUE, name + " must be numeric"); }
    }

    private Uas7ScoreResponse toScore(DailyUasLog l) {
        Short total = l.getDailyUasScore() != null ? l.getDailyUasScore() : (short) (l.getWhealScoreHss() + l.getItchScoreIss());
        return Uas7ScoreResponse.builder().uas7Id(String.valueOf(l.getDailyUasLogId())).recordId(String.valueOf(l.getChronicMedicalRecordId()))
                .cycleId(l.getTrackingCycleId() == null ? null : String.valueOf(l.getTrackingCycleId())).hssScore(l.getWhealScoreHss()).issScore(l.getItchScoreIss())
                .totalScore(total).scoreDate(l.getScoreDate()).cycleDay(l.getCycleDay()).dayStatus(l.getDayStatus())
                .medicationAdherenceStatus(l.getMedicationAdherenceStatus()).createdAt(l.getUpdatedAt()).build();
    }

    private Uas7CycleResponse toCycle(Uas7TrackingCycle c) {
        return Uas7CycleResponse.builder().cycleId(String.valueOf(c.getTrackingCycleId())).patientId(String.valueOf(c.getPatientId()))
                .startDate(c.getStartDate()).endDate(c.getEndDate()).cycleStatus(c.getCycleStatus()).validDays(validDays(c.getTrackingCycleId()))
                .createdAt(c.getCreatedAt()).updatedAt(c.getUpdatedAt()).build();
    }
}
