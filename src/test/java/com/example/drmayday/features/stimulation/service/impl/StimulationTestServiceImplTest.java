package com.example.drmayday.features.stimulation.service.impl;

import com.example.drmayday.common.exception.AppException;
import com.example.drmayday.common.exception.ErrorCode;
import com.example.drmayday.common.security.UserPrincipal;
import com.example.drmayday.entity.MedicalRecord;
import com.example.drmayday.entity.MedicalVisit;
import com.example.drmayday.entity.Person;
import com.example.drmayday.entity.ProvocationTestResult;
import com.example.drmayday.entity.ProvocationTestSession;
import com.example.drmayday.entity.ProvocationTestType;
import com.example.drmayday.entity.UserAccount;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultInput;
import com.example.drmayday.features.stimulation.dto.StimulationTestResultResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionDetailResponse;
import com.example.drmayday.features.stimulation.dto.StimulationTestSessionResponse;
import com.example.drmayday.features.stimulation.dto.UpsertStimulationTestSessionRequest;
import com.example.drmayday.repository.ChronicVisitRepository;
import com.example.drmayday.repository.MedicalRecordRepository;
import com.example.drmayday.repository.MedicalVisitRepository;
import com.example.drmayday.repository.ProvocationTestResultRepository;
import com.example.drmayday.repository.ProvocationTestSessionRepository;
import com.example.drmayday.repository.ProvocationTestTypeRepository;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StimulationTestServiceImplTest {

    @Mock private MedicalRecordRepository medicalRecordRepository;
    @Mock private MedicalVisitRepository medicalVisitRepository;
    @Mock private ChronicVisitRepository chronicVisitRepository;
    @Mock private ProvocationTestTypeRepository testTypeRepository;
    @Mock private ProvocationTestSessionRepository sessionRepository;
    @Mock private ProvocationTestResultRepository resultRepository;
    @Mock private UserAccountRepository userAccountRepository;

    @InjectMocks
    private StimulationTestServiceImpl stimulationService;

    private static final Long RECORD_ID = 2L;
    private static final Long VISIT_ID = 10L;

    @BeforeEach
    void setUpDoctor() {
        UserPrincipal principal = UserPrincipal.create(5L, "0900000002", "bac_si");
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities()));
    }

    @AfterEach
    void clearAuth() {
        SecurityContextHolder.clearContext();
    }

    private void stubChronicVisit() {
        when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(2L).medicalRecordType("MAN").build()));
        when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(
                MedicalVisit.builder().medicalVisitId(VISIT_ID).medicalRecordId(RECORD_ID).medicalRecordType("MAN").build()));
        when(chronicVisitRepository.existsById(VISIT_ID)).thenReturn(true);
    }

    @Nested
    @DisplayName("API 109-110: upsertSession & getSessionDetail")
    class SessionTests {

        @Test
        @DisplayName("[Happy Path] Upserts session for MAN visit")
        void upsertSession_success() {
            stubChronicVisit();
            when(sessionRepository.findByMedicalVisitId(VISIT_ID)).thenReturn(Optional.empty());
            when(sessionRepository.saveAndFlush(any(ProvocationTestSession.class))).thenAnswer(inv -> {
                ProvocationTestSession s = inv.getArgument(0);
                s.setProvocationTestSessionId(55L);
                return s;
            });

            StimulationTestSessionResponse res = stimulationService.upsertSession(RECORD_ID, VISIT_ID,
                    UpsertStimulationTestSessionRequest.builder()
                            .ngungKhangHistamine(true).thoiGianNgungHistamine(7)
                            .ngungCorticoid(false).thoiGianNgungCorticoid(null)
                            .build());

            assertEquals(55L, res.getMaPhienNghiemPhap());
            assertEquals(VISIT_ID, res.getMaPhienKham());
            assertEquals(7, res.getThoiGianNgungHistamine());
            assertFalse(res.getNgungCorticoid());
        }

        @Test
        @DisplayName("[Unhappy Path] CAP visit throws 4008; missing/inconsistent days throw 2001/2003")
        void upsertSession_validation() {
            when(medicalRecordRepository.findById(RECORD_ID)).thenReturn(Optional.of(
                    MedicalRecord.builder().medicalRecordId(RECORD_ID).patientId(2L).medicalRecordType("CAP").build()));
            when(medicalVisitRepository.findById(VISIT_ID)).thenReturn(Optional.of(
                    MedicalVisit.builder().medicalVisitId(VISIT_ID).medicalRecordId(RECORD_ID).medicalRecordType("CAP").build()));

            AppException exType = assertThrows(AppException.class, () ->
                    stimulationService.upsertSession(RECORD_ID, VISIT_ID, UpsertStimulationTestSessionRequest.builder()
                            .ngungKhangHistamine(false).ngungCorticoid(false).build()));
            assertEquals(ErrorCode.RECORD_TYPE_MISMATCH, exType.getErrorCode());

            stubChronicVisit();
            AppException exMissingDays = assertThrows(AppException.class, () ->
                    stimulationService.upsertSession(RECORD_ID, VISIT_ID, UpsertStimulationTestSessionRequest.builder()
                            .ngungKhangHistamine(true).thoiGianNgungHistamine(null).ngungCorticoid(false).build()));
            assertEquals(ErrorCode.MISSING_REQUIRED_PARAMETER, exMissingDays.getErrorCode());

            AppException exExtraDays = assertThrows(AppException.class, () ->
                    stimulationService.upsertSession(RECORD_ID, VISIT_ID, UpsertStimulationTestSessionRequest.builder()
                            .ngungKhangHistamine(false).thoiGianNgungHistamine(3).ngungCorticoid(false).build()));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exExtraDays.getErrorCode());
        }

        @Test
        @DisplayName("[Happy/Auth Path] getSessionDetail maps results + blocks foreign patient (3106)")
        void getSessionDetail_successAndOwnership() {
            stubChronicVisit();
            when(sessionRepository.findByMedicalVisitId(VISIT_ID)).thenReturn(Optional.of(
                    ProvocationTestSession.builder().provocationTestSessionId(55L).medicalVisitId(VISIT_ID)
                            .stoppedAntihistamines(true).histamineDiscontinuationDays(BigDecimal.valueOf(5))
                            .stoppedCorticosteroids(false).build()));
            when(resultRepository.findByProvocationTestSessionIdOrderByProvocationTestResultIdAsc(55L)).thenReturn(List.of(
                    ProvocationTestResult.builder().provocationTestResultId(1L).provocationTestSessionId(55L)
                            .provocationTestTypeId(1L).otherCause(false).testResult("Duong tinh")
                            .numericThreshold(BigDecimal.valueOf(2)).measurementUnit("diem").build()
            ));
            when(testTypeRepository.findAllById(any())).thenReturn(List.of(
                    ProvocationTestType.builder().provocationTestId(1L).testName("Da vẽ nổi").otherCause(false).build()
            ));

            StimulationTestSessionDetailResponse detail = stimulationService.getSessionDetail(RECORD_ID, VISIT_ID);
            assertEquals(55L, detail.getMaPhienNghiemPhap());
            assertEquals(1, detail.getResults().size());
            assertEquals("Da vẽ nổi", detail.getResults().get(0).getTenNghiemPhap());

            // Foreign patient blocked with 3106
            UserPrincipal otherPatient = UserPrincipal.create(99L, "0999999999", "benh_nhan");
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(otherPatient, null, otherPatient.getAuthorities()));
            when(userAccountRepository.findById(99L)).thenReturn(Optional.of(
                    UserAccount.builder().accountId(99L).person(Person.builder().personId(99L).build()).build()));

            AppException ex = assertThrows(AppException.class, () ->
                    stimulationService.getSessionDetail(RECORD_ID, VISIT_ID));
            assertEquals(ErrorCode.CANNOT_ACCESS_ANOTHER_USER_DATA, ex.getErrorCode());
        }
    }

    @Nested
    @DisplayName("API 111-113: createResultsBulk, updateResult & deleteResult")
    class ResultTests {

        @Test
        @DisplayName("[Happy Path] Bulk inserts normal + other-cause results in one transaction")
        void createResultsBulk_success() {
            stubChronicVisit();
            when(sessionRepository.findByMedicalVisitId(VISIT_ID)).thenReturn(Optional.of(
                    ProvocationTestSession.builder().provocationTestSessionId(55L).medicalVisitId(VISIT_ID).build()));
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(
                    ProvocationTestType.builder().provocationTestId(1L).testName("Da vẽ nổi").otherCause(false).build()));
            when(testTypeRepository.findById(9L)).thenReturn(Optional.of(
                    ProvocationTestType.builder().provocationTestId(9L).testName("Căn nguyên khác").otherCause(true).build()));
            when(resultRepository.saveAllAndFlush(anyList())).thenAnswer(inv -> {
                List<ProvocationTestResult> list = inv.getArgument(0);
                list.get(0).setProvocationTestResultId(101L);
                list.get(1).setProvocationTestResultId(102L);
                return list;
            });

            List<StimulationTestResultResponse> res = stimulationService.createResultsBulk(RECORD_ID, VISIT_ID, List.of(
                    StimulationTestResultInput.builder().maNghiemPhap(1L).laCanNguyenKhac(false)
                            .ketQua("Duong tinh").nguongGiaTriSo(BigDecimal.valueOf(2)).donViDo("diem")
                            .diemNguaNrs(5).diemDauNrs(1).diemBongRatNrs(2).build(),
                    StimulationTestResultInput.builder().maNghiemPhap(9L).laCanNguyenKhac(true)
                            .canNguyenKhacTen("Test rung lắc").ketQua("Am tinh")
                            .nguongGiaTriChu("Không có phản ứng").donViDo("diem").build()
            ));

            assertEquals(2, res.size());
            assertEquals(101L, res.get(0).getId());
            assertEquals("Test rung lắc", res.get(1).getCanNguyenKhacTen());
        }

        @Test
        @DisplayName("[Unhappy Path] Rejects composite FK mismatch (2003), dual thresholds (2003), NRS > 10 (7007)")
        void createResultsBulk_validations() {
            stubChronicVisit();
            when(sessionRepository.findByMedicalVisitId(VISIT_ID)).thenReturn(Optional.of(
                    ProvocationTestSession.builder().provocationTestSessionId(55L).medicalVisitId(VISIT_ID).build()));
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(
                    ProvocationTestType.builder().provocationTestId(1L).testName("Da vẽ nổi").otherCause(false).build()));

            // Mismatched (ma_nghiem_phap=1, la_can_nguyen_khac=true)
            AppException exFk = assertThrows(AppException.class, () ->
                    stimulationService.createResultsBulk(RECORD_ID, VISIT_ID, List.of(
                            StimulationTestResultInput.builder().maNghiemPhap(1L).laCanNguyenKhac(true)
                                    .canNguyenKhacTen("X").ketQua("Am tinh").build())));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exFk.getErrorCode());

            // Both numeric and text threshold
            AppException exDual = assertThrows(AppException.class, () ->
                    stimulationService.createResultsBulk(RECORD_ID, VISIT_ID, List.of(
                            StimulationTestResultInput.builder().maNghiemPhap(1L).laCanNguyenKhac(false)
                                    .ketQua("Duong tinh").nguongGiaTriSo(BigDecimal.ONE).nguongGiaTriChu("2").donViDo("diem").build())));
            assertEquals(ErrorCode.INVALID_PARAMETER_VALUE, exDual.getErrorCode());

            // NRS > 10
            AppException exNrs = assertThrows(AppException.class, () ->
                    stimulationService.createResultsBulk(RECORD_ID, VISIT_ID, List.of(
                            StimulationTestResultInput.builder().maNghiemPhap(1L).laCanNguyenKhac(false)
                                    .ketQua("Duong tinh").diemNguaNrs(11).build())));
            assertEquals(ErrorCode.SYMPTOM_SCORE_OUT_OF_RANGE, exNrs.getErrorCode());
        }

        @Test
        @DisplayName("[Happy/Unhappy Path] Update & Delete single result (8304 when absent)")
        void updateAndDelete_result() {
            when(resultRepository.findById(999L)).thenReturn(Optional.empty());
            assertEquals(ErrorCode.STIMULATION_TEST_RESULT_NOT_FOUND, assertThrows(AppException.class, () ->
                    stimulationService.updateResult(999L, StimulationTestResultInput.builder().build())).getErrorCode());

            ProvocationTestResult existing = ProvocationTestResult.builder()
                    .provocationTestResultId(10L).provocationTestSessionId(55L).provocationTestTypeId(1L).build();
            when(resultRepository.findById(10L)).thenReturn(Optional.of(existing));
            when(testTypeRepository.findById(1L)).thenReturn(Optional.of(
                    ProvocationTestType.builder().provocationTestId(1L).testName("Da vẽ nổi").otherCause(false).build()));
            when(resultRepository.saveAndFlush(any(ProvocationTestResult.class))).thenAnswer(inv -> inv.getArgument(0));

            StimulationTestResultResponse updated = stimulationService.updateResult(10L, StimulationTestResultInput.builder()
                    .maNghiemPhap(1L).laCanNguyenKhac(false).ketQua("Am tinh").diemNguaNrs(0).build());
            assertEquals("Am tinh", updated.getKetQua());

            stimulationService.deleteResult(10L);
            verify(resultRepository).delete(existing);
            verify(resultRepository).flush();
        }
    }
}
