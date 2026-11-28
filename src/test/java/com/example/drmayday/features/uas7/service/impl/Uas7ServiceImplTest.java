package com.example.drmayday.features.uas7.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
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
import com.example.drmayday.repository.ChronicUrticariaRecordRepository;
import com.example.drmayday.repository.DailyUasLogRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.PatientRepository;
import com.example.drmayday.repository.Uas7TrackingCycleRepository;
import com.example.drmayday.repository.UserAccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class Uas7ServiceImplTest {

    private static final long ACCOUNT_ID = 10L;
    private static final long PATIENT_ID = 2L;
    private static final long OTHER_PATIENT_ID = 99L;
    private static final long RECORD_ID = 20L;
    private static final long CYCLE_ID = 30L;

    @Mock
    private MedicalRecordRepository medicalRecordRepository;

    @Mock
    private ChronicUrticariaRecordRepository chronicUrticariaRecordRepository;

    @Mock
    private Uas7TrackingCycleRepository cycleRepository;

    @Mock
    private DailyUasLogRepository dailyUasLogRepository;

    @Mock
    private PatientRepository patientRepository;

    @Mock
    private UserAccountRepository userAccountRepository;

    @InjectMocks
    private Uas7ServiceImpl service;

    @BeforeEach
    void setUp() {
        authenticateAs(ACCOUNT_ID, "benh_nhan");
        lenient().when(userAccountRepository.findById(ACCOUNT_ID)).thenReturn(Optional.of(
                UserAccount.builder()
                        .accountId(ACCOUNT_ID)
                        .person(Person.builder().personId(PATIENT_ID).build())
                        .build()
        ));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private void authenticateAs(Long userId, String role) {
        UserPrincipal principal = UserPrincipal.create(userId, "0912345678", role);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities())
        );
    }

    private static MedicalRecord chronicRecord(Long patientId, String status) {
        return MedicalRecord.builder()
                .medicalRecordId(RECORD_ID)
                .patientId(patientId)
                .medicalRecordType("MAN")
                .status(status)
                .build();
    }

    private static Uas7TrackingCycle activeCycle(LocalDate startDate) {
        OffsetDateTime now = OffsetDateTime.now();
        return Uas7TrackingCycle.builder()
                .trackingCycleId(CYCLE_ID)
                .patientId(PATIENT_ID)
                .startDate(startDate)
                .endDate(startDate.plusDays(6))
                .cycleStatus("DANG_THEO_DOI")
                .createdAt(now)
                .updatedAt(now)
                .build();
    }

    private static void assertError(ErrorCode expected, org.assertj.core.api.ThrowableAssert.ThrowingCallable action) {
        assertThatThrownBy(action)
                .isInstanceOfSatisfying(AppException.class,
                        ex -> assertThat(ex.getErrorCode()).isEqualTo(expected));
    }

    @Nested
    @DisplayName("API 58: checkEligibility")
    class CheckEligibilityTests {

        @Test
        @DisplayName("[Happy Path] Active MAN record and cycle covering today, not scored today")
        void eligibleWithCycleNotScoredToday() {
            LocalDate today = LocalDate.now();
            LocalDate cycleStart = today.minusDays(2);
            when(medicalRecordRepository.findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
                    eq(PATIENT_ID), eq("MAN"), any())).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", today, today)).thenReturn(Optional.of(activeCycle(cycleStart)));
            when(dailyUasLogRepository.findByPatientIdAndScoreDate(PATIENT_ID, today)).thenReturn(Optional.empty());

            Uas7EligibilityResponse response = service.checkEligibility();

            assertThat(response.isEligible()).isTrue();
            assertThat(response.getActiveRecordId()).isEqualTo(String.valueOf(RECORD_ID));
            assertThat(response.getActiveCycleId()).isEqualTo(String.valueOf(CYCLE_ID));
            assertThat(response.getCurrentDayInCycle()).isEqualTo(3);
            assertThat(response.isScoredToday()).isFalse();
            assertThat(response.getTodayScoreId()).isNull();
        }

        @Test
        @DisplayName("[Alternate Path] Already scored today — returns scored_today=true and today_score_id")
        void eligibleAlreadyScoredToday() {
            LocalDate today = LocalDate.now();
            when(medicalRecordRepository.findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
                    eq(PATIENT_ID), eq("MAN"), any())).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", today, today)).thenReturn(Optional.of(activeCycle(today)));
            when(dailyUasLogRepository.findByPatientIdAndScoreDate(PATIENT_ID, today))
                    .thenReturn(Optional.of(DailyUasLog.builder().dailyUasLogId(555L).build()));

            Uas7EligibilityResponse response = service.checkEligibility();

            assertThat(response.isEligible()).isTrue();
            assertThat(response.isScoredToday()).isTrue();
            assertThat(response.getTodayScoreId()).isEqualTo("555");
            assertThat(response.getCurrentDayInCycle()).isEqualTo(1);
        }

        @Test
        @DisplayName("[Alternate Path] Active MAN record but no active cycle yet — cycle fields are null")
        void eligibleWithoutCycle() {
            LocalDate today = LocalDate.now();
            when(medicalRecordRepository.findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
                    eq(PATIENT_ID), eq("MAN"), any())).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "draft")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", today, today)).thenReturn(Optional.empty());
            when(dailyUasLogRepository.findByPatientIdAndScoreDate(PATIENT_ID, today)).thenReturn(Optional.empty());

            Uas7EligibilityResponse response = service.checkEligibility();

            assertThat(response.isEligible()).isTrue();
            assertThat(response.getActiveCycleId()).isNull();
            assertThat(response.getCurrentDayInCycle()).isNull();
            assertThat(response.isScoredToday()).isFalse();
        }

        @Test
        @DisplayName("[Unhappy Path] No active MAN record — throws 5004")
        void noActiveChronicRecord_throws5004() {
            when(medicalRecordRepository.findFirstByPatientIdAndMedicalRecordTypeAndStatusNotInOrderByMedicalRecordIdDesc(
                    eq(PATIENT_ID), eq("MAN"), any())).thenReturn(Optional.empty());

            assertError(ErrorCode.UAS7_NO_ACTIVE_RECORD, () -> service.checkEligibility());
        }
    }

    @Nested
    @DisplayName("API 59: createScore")
    class CreateScoreTests {

        @Test
        @DisplayName("[Happy Path] First score without cycle — auto-creates 7-day cycle and returns day 1")
        void createScore_autoCreatesCycle() {
            LocalDate scoreDate = LocalDate.of(2026, 11, 20);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(dailyUasLogRepository.existsByPatientIdAndScoreDate(PATIENT_ID, scoreDate)).thenReturn(false);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", scoreDate, scoreDate)).thenReturn(Optional.empty());
            when(cycleRepository.existsOverlappingActive(PATIENT_ID, scoreDate, scoreDate.plusDays(6))).thenReturn(false);
            when(cycleRepository.saveAndFlush(any(Uas7TrackingCycle.class))).thenAnswer(inv -> {
                Uas7TrackingCycle c = inv.getArgument(0);
                c.setTrackingCycleId(77L);
                return c;
            });
            when(dailyUasLogRepository.saveAndFlush(any(DailyUasLog.class))).thenAnswer(inv -> {
                DailyUasLog log = inv.getArgument(0);
                log.setDailyUasLogId(101L);
                log.setDailyUasScore((short) 5);
                return log;
            });

            Uas7ScoreResponse response = service.createScore(CreateUas7ScoreRequest.builder()
                    .recordId(RECORD_ID)
                    .hssScore(2)
                    .issScore(3)
                    .date(scoreDate)
                    .trangThaiDungThuoc("DUNG_DEU")
                    .build());

            assertThat(response.getUas7Id()).isEqualTo("101");
            assertThat(response.getCycleId()).isEqualTo("77");
            assertThat(response.getTotalScore()).isEqualTo(5);
            assertThat(response.getNgayThuMayTrongChuKy()).isEqualTo(1);
            assertThat(response.getTrangThaiNgay()).isEqualTo("HOP_LE");
        }

        @Test
        @DisplayName("[Happy Path] Existing active cycle — assigns cycle day 4 without creating new cycle")
        void createScore_existingCycle_day4() {
            LocalDate start = LocalDate.of(2026, 11, 10);
            LocalDate scoreDate = LocalDate.of(2026, 11, 13);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(dailyUasLogRepository.existsByPatientIdAndScoreDate(PATIENT_ID, scoreDate)).thenReturn(false);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", scoreDate, scoreDate)).thenReturn(Optional.of(activeCycle(start)));
            when(dailyUasLogRepository.saveAndFlush(any(DailyUasLog.class))).thenAnswer(inv -> {
                DailyUasLog log = inv.getArgument(0);
                log.setDailyUasLogId(102L);
                return log;
            });

            Uas7ScoreResponse response = service.createScore(CreateUas7ScoreRequest.builder()
                    .recordId(RECORD_ID)
                    .hssScore(1)
                    .issScore(2)
                    .date(scoreDate)
                    .build());

            assertThat(response.getCycleId()).isEqualTo(String.valueOf(CYCLE_ID));
            assertThat(response.getNgayThuMayTrongChuKy()).isEqualTo(4);
            assertThat(response.getTotalScore()).isEqualTo(3);
            verify(cycleRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("[Unhappy Path] Duplicate score same date — throws 5002")
        void createScore_duplicateDay_throws5002() {
            LocalDate scoreDate = LocalDate.of(2026, 11, 20);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(dailyUasLogRepository.existsByPatientIdAndScoreDate(PATIENT_ID, scoreDate)).thenReturn(true);

            assertError(ErrorCode.UAS7_RECORD_ALREADY_EXISTS_TODAY,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(2).issScore(2).date(scoreDate).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Score out of 0-3 range — throws 5003")
        void createScore_outOfRange_throws5003() {
            assertError(ErrorCode.UAS7_SCORE_OUT_OF_RANGE,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(2).issScore(5).build()));
            assertError(ErrorCode.UAS7_SCORE_OUT_OF_RANGE,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(-1).issScore(2).build()));
            assertError(ErrorCode.UAS7_SCORE_OUT_OF_RANGE,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(4).issScore(1).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing required fields — throws 2001")
        void createScore_missingRequired_throws2001() {
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER, () -> service.createScore(null));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.createScore(CreateUas7ScoreRequest.builder().hssScore(1).issScore(1).build()));
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.createScore(CreateUas7ScoreRequest.builder().recordId(RECORD_ID).issScore(1).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid/closed/CAP/foreign record — throws 5004")
        void createScore_invalidRecord_throws5004() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.empty());
            assertError(ErrorCode.UAS7_NO_ACTIVE_RECORD,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(1).issScore(1).build()));

            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "completed")));
            assertError(ErrorCode.UAS7_NO_ACTIVE_RECORD,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(1).issScore(1).build()));

            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                    MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(PATIENT_ID).medicalRecordType("CAP").status("in_progress").build()));
            assertError(ErrorCode.UAS7_NO_ACTIVE_RECORD,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(1).issScore(1).build()));

            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(OTHER_PATIENT_ID, "in_progress")));
            assertError(ErrorCode.UAS7_NO_ACTIVE_RECORD,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(1).issScore(1).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Auto-created cycle overlaps existing active cycle — throws 5006")
        void createScore_overlappingAutoCycle_throws5006() {
            LocalDate scoreDate = LocalDate.of(2026, 11, 15);
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(chronicRecord(PATIENT_ID, "in_progress")));
            when(chronicUrticariaRecordRepository.existsById(RECORD_ID)).thenReturn(true);
            when(dailyUasLogRepository.existsByPatientIdAndScoreDate(PATIENT_ID, scoreDate)).thenReturn(false);
            when(cycleRepository.findFirstByPatientIdAndCycleStatusAndStartDateLessThanEqualAndEndDateGreaterThanEqualOrderByStartDateDesc(
                    PATIENT_ID, "DANG_THEO_DOI", scoreDate, scoreDate)).thenReturn(Optional.empty());
            when(cycleRepository.existsOverlappingActive(PATIENT_ID, scoreDate, scoreDate.plusDays(6))).thenReturn(true);

            assertError(ErrorCode.UAS7_OVERLAPPING_CYCLE,
                    () -> service.createScore(CreateUas7ScoreRequest.builder()
                            .recordId(RECORD_ID).hssScore(1).issScore(1).date(scoreDate).build()));
        }
    }

    @Nested
    @DisplayName("API 60: getPatientScores")
    class GetPatientScoresTests {

        @Test
        @DisplayName("[Happy Path] Owner patient fetches history with filters")
        void getPatientScores_ownerSuccess() {
            LocalDate from = LocalDate.of(2026, 11, 1);
            LocalDate to = LocalDate.of(2026, 11, 30);
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            when(dailyUasLogRepository.findByPatientIdFiltered(PATIENT_ID, from, to, CYCLE_ID)).thenReturn(List.of(
                    DailyUasLog.builder()
                            .dailyUasLogId(1L)
                            .chronicMedicalRecordId(RECORD_ID)
                            .trackingCycleId(CYCLE_ID)
                            .patientId(PATIENT_ID)
                            .scoreDate(LocalDate.of(2026, 11, 12))
                            .cycleDay((short) 3)
                            .whealScoreHss((short) 2)
                            .itchScoreIss((short) 1)
                            .dailyUasScore((short) 3)
                            .dayStatus("HOP_LE")
                            .build()
            ));

            List<Uas7ScoreResponse> result = service.getPatientScores(String.valueOf(PATIENT_ID), from, to, String.valueOf(CYCLE_ID));

            assertThat(result).hasSize(1);
            assertThat(result.get(0).getUas7Id()).isEqualTo("1");
            assertThat(result.get(0).getTotalScore()).isEqualTo(3);
        }

        @Test
        @DisplayName("[Alternate Path] Doctor reads any patient's scores")
        void getPatientScores_doctorAllowed() {
            authenticateAs(999L, "bac_si");
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            when(dailyUasLogRepository.findByPatientIdFiltered(PATIENT_ID, null, null, null)).thenReturn(List.of());

            assertThat(service.getPatientScores(String.valueOf(PATIENT_ID), null, null, null)).isEmpty();
        }

        @Test
        @DisplayName("[Unhappy Path] Missing patient — throws 3007")
        void getPatientScores_missingPatient_throws3007() {
            when(patientRepository.existsById(9999L)).thenReturn(false);

            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getPatientScores("9999", null, null, null));
        }

        @Test
        @DisplayName("[Unhappy Path] Foreign patient — throws 3106")
        void getPatientScores_foreignPatient_throws3106() {
            when(patientRepository.existsById(OTHER_PATIENT_ID)).thenReturn(true);

            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.getPatientScores(String.valueOf(OTHER_PATIENT_ID), null, null, null));
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid date range or cycle_id — throws 2003")
        void getPatientScores_invalidParams_throws2003() {
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.getPatientScores(String.valueOf(PATIENT_ID),
                            LocalDate.of(2026, 11, 20), LocalDate.of(2026, 11, 10), null));
            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.getPatientScores(String.valueOf(PATIENT_ID), null, null, "not-a-number"));
        }
    }

    @Nested
    @DisplayName("API 61: createCycle")
    class CreateCycleTests {

        @Test
        @DisplayName("[Happy Path] Creates 7-day cycle with end_date = start_date + 6")
        void createCycle_success() {
            LocalDate start = LocalDate.of(2026, 10, 20);
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            when(cycleRepository.existsOverlappingActive(PATIENT_ID, start, start.plusDays(6))).thenReturn(false);
            when(cycleRepository.saveAndFlush(any(Uas7TrackingCycle.class))).thenAnswer(inv -> {
                Uas7TrackingCycle c = inv.getArgument(0);
                c.setTrackingCycleId(CYCLE_ID);
                return c;
            });

            Uas7CycleResponse response = service.createCycle(String.valueOf(PATIENT_ID),
                    CreateUas7CycleRequest.builder().ngayBatDau(start).build());

            assertThat(response.getCycleId()).isEqualTo(String.valueOf(CYCLE_ID));
            assertThat(response.getNgayBatDau()).isEqualTo(LocalDate.of(2026, 10, 20));
            assertThat(response.getNgayKetThuc()).isEqualTo(LocalDate.of(2026, 10, 26));
            assertThat(response.getTrangThaiChuKy()).isEqualTo("DANG_THEO_DOI");
            assertThat(response.getSoNgayHopLe()).isZero();
        }

        @Test
        @DisplayName("[Unhappy Path] Overlapping active cycle — throws 5006")
        void createCycle_overlap_throws5006() {
            LocalDate start = LocalDate.of(2026, 10, 20);
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            when(cycleRepository.existsOverlappingActive(PATIENT_ID, start, start.plusDays(6))).thenReturn(true);

            assertError(ErrorCode.UAS7_OVERLAPPING_CYCLE,
                    () -> service.createCycle(String.valueOf(PATIENT_ID),
                            CreateUas7CycleRequest.builder().ngayBatDau(start).build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing patient / foreign patient / missing ngay_bat_dau")
        void createCycle_validationErrors() {
            when(patientRepository.existsById(999L)).thenReturn(false);
            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND,
                    () -> service.createCycle("999", CreateUas7CycleRequest.builder().ngayBatDau(LocalDate.now()).build()));

            when(patientRepository.existsById(OTHER_PATIENT_ID)).thenReturn(true);
            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.createCycle(String.valueOf(OTHER_PATIENT_ID),
                            CreateUas7CycleRequest.builder().ngayBatDau(LocalDate.now()).build()));

            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.createCycle(String.valueOf(PATIENT_ID), new CreateUas7CycleRequest()));
        }
    }

    @Nested
    @DisplayName("API 62: getPatientCycles")
    class GetPatientCyclesTests {

        @Test
        @DisplayName("[Happy Path] Returns cycles with batch-counted valid days (7 and 3)")
        void getPatientCycles_returnsCounts() {
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            Uas7TrackingCycle c1 = activeCycle(LocalDate.of(2026, 11, 10));
            c1.setTrackingCycleId(101L);
            c1.setCycleStatus("HOAN_THANH");
            Uas7TrackingCycle c2 = activeCycle(LocalDate.of(2026, 11, 18));
            c2.setTrackingCycleId(102L);
            when(cycleRepository.findByPatientIdOrderByStartDateDescTrackingCycleIdDesc(PATIENT_ID))
                    .thenReturn(List.of(c2, c1));
            when(dailyUasLogRepository.countByTrackingCycleIdsAndDayStatus(List.of(102L, 101L), "HOP_LE"))
                    .thenReturn(List.of(new Object[]{101L, 7L}, new Object[]{102L, 3L}));

            List<Uas7CycleResponse> result = service.getPatientCycles(String.valueOf(PATIENT_ID));

            assertThat(result).hasSize(2);
            assertThat(result.get(0).getCycleId()).isEqualTo("102");
            assertThat(result.get(0).getSoNgayHopLe()).isEqualTo(3);
            assertThat(result.get(1).getCycleId()).isEqualTo("101");
            assertThat(result.get(1).getSoNgayHopLe()).isEqualTo(7);
        }

        @Test
        @DisplayName("[Alternate Path] No cycles returns empty list")
        void getPatientCycles_empty() {
            when(patientRepository.existsById(PATIENT_ID)).thenReturn(true);
            when(cycleRepository.findByPatientIdOrderByStartDateDescTrackingCycleIdDesc(PATIENT_ID)).thenReturn(List.of());

            assertThat(service.getPatientCycles(String.valueOf(PATIENT_ID))).isEmpty();
        }

        @Test
        @DisplayName("[Unhappy Path] Missing or foreign patient")
        void getPatientCycles_errors() {
            when(patientRepository.existsById(999L)).thenReturn(false);
            assertError(ErrorCode.USER_ACCOUNT_NOT_FOUND, () -> service.getPatientCycles("999"));

            when(patientRepository.existsById(OTHER_PATIENT_ID)).thenReturn(true);
            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.getPatientCycles(String.valueOf(OTHER_PATIENT_ID)));
        }
    }

    @Nested
    @DisplayName("API 63: getCycleDetail")
    class GetCycleDetailTests {

        @Test
        @DisplayName("[Happy Path] Returns cycle detail with logs ASC and valid count")
        void getCycleDetail_success() {
            Uas7TrackingCycle cycle = activeCycle(LocalDate.of(2026, 11, 1));
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
            when(dailyUasLogRepository.findByTrackingCycleIdOrderByScoreDateAsc(CYCLE_ID)).thenReturn(List.of(
                    DailyUasLog.builder().dailyUasLogId(1L).trackingCycleId(CYCLE_ID).chronicMedicalRecordId(RECORD_ID)
                            .patientId(PATIENT_ID).scoreDate(LocalDate.of(2026, 11, 1)).cycleDay((short) 1)
                            .whealScoreHss((short) 2).itchScoreIss((short) 2).dailyUasScore((short) 4).dayStatus("HOP_LE").build(),
                    DailyUasLog.builder().dailyUasLogId(2L).trackingCycleId(CYCLE_ID).chronicMedicalRecordId(RECORD_ID)
                            .patientId(PATIENT_ID).scoreDate(LocalDate.of(2026, 11, 2)).cycleDay((short) 2)
                            .whealScoreHss((short) 1).itchScoreIss((short) 1).dailyUasScore((short) 2).dayStatus("HOP_LE").build()
            ));

            Uas7CycleDetailResponse detail = service.getCycleDetail(String.valueOf(CYCLE_ID));

            assertThat(detail.getCycleId()).isEqualTo(String.valueOf(CYCLE_ID));
            assertThat(detail.getSoNgayHopLe()).isEqualTo(2);
            assertThat(detail.getLogs()).hasSize(2);
            assertThat(detail.getLogs().get(0).getNgayChamDiem()).isEqualTo(LocalDate.of(2026, 11, 1));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing cycle throws 5005; foreign patient throws 3106")
        void getCycleDetail_errors() {
            when(cycleRepository.findById(999L)).thenReturn(Optional.empty());
            assertError(ErrorCode.UAS7_CYCLE_NOT_FOUND, () -> service.getCycleDetail("999"));

            Uas7TrackingCycle foreignCycle = activeCycle(LocalDate.of(2026, 11, 1));
            foreignCycle.setPatientId(OTHER_PATIENT_ID);
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(foreignCycle));
            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.getCycleDetail(String.valueOf(CYCLE_ID)));
        }
    }

    @Nested
    @DisplayName("API 64: updateCycleStatus")
    class UpdateCycleStatusTests {

        @Test
        @DisplayName("[Happy Path] DANG_THEO_DOI -> HOAN_THANH and DANG_THEO_DOI -> HUY")
        void updateCycleStatus_validTransitions() {
            Uas7TrackingCycle cycle = activeCycle(LocalDate.of(2026, 11, 1));
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle));
            when(cycleRepository.saveAndFlush(any(Uas7TrackingCycle.class))).thenAnswer(inv -> inv.getArgument(0));
            when(dailyUasLogRepository.countByTrackingCycleIdAndDayStatus(CYCLE_ID, "HOP_LE")).thenReturn(7L);

            Uas7CycleResponse completed = service.updateCycleStatus(String.valueOf(CYCLE_ID),
                    UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("HOAN_THANH").build());

            assertThat(completed.getTrangThaiChuKy()).isEqualTo("HOAN_THANH");
            assertThat(completed.getSoNgayHopLe()).isEqualTo(7);
            assertThat(completed.getThoiDiemCapNhat()).isNotNull();

            Uas7TrackingCycle cycle2 = activeCycle(LocalDate.of(2026, 11, 10));
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cycle2));
            Uas7CycleResponse cancelled = service.updateCycleStatus(String.valueOf(CYCLE_ID),
                    UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("HUY").build());
            assertThat(cancelled.getTrangThaiChuKy()).isEqualTo("HUY");
        }

        @Test
        @DisplayName("[Unhappy Path] Invalid transitions throw 5007; unknown enum throws 2003")
        void updateCycleStatus_invalidTransitions() {
            Uas7TrackingCycle completedCycle = activeCycle(LocalDate.of(2026, 11, 1));
            completedCycle.setCycleStatus("HOAN_THANH");
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(completedCycle));

            assertError(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("DANG_THEO_DOI").build()));

            Uas7TrackingCycle cancelledCycle = activeCycle(LocalDate.of(2026, 11, 1));
            cancelledCycle.setCycleStatus("HUY");
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(cancelledCycle));
            assertError(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("HOAN_THANH").build()));

            Uas7TrackingCycle active = activeCycle(LocalDate.of(2026, 11, 1));
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(active));
            assertError(ErrorCode.UAS7_INVALID_CYCLE_STATUS_TRANSITION,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("DANG_THEO_DOI").build()));

            assertError(ErrorCode.INVALID_PARAMETER_VALUE,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("UNKNOWN").build()));

            assertError(ErrorCode.MISSING_REQUIRED_PARAMETER,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy(" ").build()));
        }

        @Test
        @DisplayName("[Unhappy Path] Missing cycle throws 5005; foreign patient throws 3106")
        void updateCycleStatus_notFoundOrForbidden() {
            when(cycleRepository.findById(999L)).thenReturn(Optional.empty());
            assertError(ErrorCode.UAS7_CYCLE_NOT_FOUND,
                    () -> service.updateCycleStatus("999",
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("HOAN_THANH").build()));

            Uas7TrackingCycle foreignCycle = activeCycle(LocalDate.of(2026, 11, 1));
            foreignCycle.setPatientId(OTHER_PATIENT_ID);
            when(cycleRepository.findById(CYCLE_ID)).thenReturn(Optional.of(foreignCycle));
            assertError(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA,
                    () -> service.updateCycleStatus(String.valueOf(CYCLE_ID),
                            UpdateUas7CycleStatusRequest.builder().trangThaiChuKy("HOAN_THANH").build()));
        }
    }
}
